package com.dm.incidentextractor.extract;

import com.dm.incidentextractor.model.ActionItem;
import com.dm.incidentextractor.model.IncidentReport;
import com.dm.incidentextractor.model.Severity;
import com.dm.incidentextractor.model.TimelineEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * What a correct report for one incident fixture must contain, loaded from {@code expected.json}.
 * <p>
 * Content checks are keyword based so they hold for any reasonable wording a model picks: each entry in a
 * mention list must appear (case-insensitively) somewhere in that field, and {@code a|b} accepts either word.
 * On top of the per-fixture checks, {@link #violations} enforces the system prompt's rules for every report:
 * no invented timestamps or owners, and no blank required fields.
 *
 * @param severity            severities that are acceptable; empty accepts any
 * @param affectedSystems     terms that must each be mentioned in affectedSystems
 * @param detection           terms that must each be mentioned in detection
 * @param rootCauses          terms that must each be mentioned in rootCauses
 * @param contributingFactors terms that must each be mentioned in contributingFactors
 * @param resolution          terms that must each be mentioned in resolution
 * @param actionItems         terms that must each be mentioned in some action item description
 * @param minTimelineEvents   minimum number of timeline events
 * @param maxRootCauses       maximum number of root causes, or null for no limit (0 means none may be guessed)
 * @param expectOpenQuestions whether the notes are incomplete enough that open questions are required
 */
record ReportExpectations(
        List<Severity> severity,
        List<String> affectedSystems,
        List<String> detection,
        List<String> rootCauses,
        List<String> contributingFactors,
        List<String> resolution,
        List<String> actionItems,
        int minTimelineEvents,
        Integer maxRootCauses,
        boolean expectOpenQuestions) {

    ReportExpectations {
        severity = nullToEmpty(severity);
        affectedSystems = nullToEmpty(affectedSystems);
        detection = nullToEmpty(detection);
        rootCauses = nullToEmpty(rootCauses);
        contributingFactors = nullToEmpty(contributingFactors);
        resolution = nullToEmpty(resolution);
        actionItems = nullToEmpty(actionItems);
    }

    /** Returns a description of every way the report misses expectations; empty means it passes. */
    List<String> violations(IncidentReport report, String notes) {
        List<String> violations = new ArrayList<>();
        String lowerNotes = notes.toLowerCase(Locale.ROOT);

        requireText(violations, "title", report.title());
        requireText(violations, "summary", report.summary());
        requireText(violations, "severityRationale", report.severityRationale());

        if (!severity.isEmpty() && !severity.contains(report.severity())) {
            violations.add("severity was " + report.severity() + ", expected one of " + severity);
        }
        requireMentions(violations, "affectedSystems", affectedSystems, report.affectedSystems());
        requireMentions(violations, "detection", detection, optional(report.detection()));
        requireMentions(violations, "rootCauses", rootCauses, report.rootCauses());
        requireMentions(violations, "contributingFactors", contributingFactors, report.contributingFactors());
        requireMentions(violations, "resolution", resolution, optional(report.resolution()));
        requireMentions(violations, "actionItems", actionItems,
                report.actionItems().stream().map(ActionItem::description).toList());

        if (report.timeline().size() < minTimelineEvents) {
            violations.add("timeline has " + report.timeline().size() + " events, expected at least "
                    + minTimelineEvents);
        }
        if (maxRootCauses != null && report.rootCauses().size() > maxRootCauses) {
            violations.add("rootCauses has " + report.rootCauses().size() + " entries, expected at most "
                    + maxRootCauses + ": " + report.rootCauses());
        }
        if (expectOpenQuestions && report.openQuestions().isEmpty()) {
            violations.add("openQuestions is empty, but the notes leave important gaps");
        }

        for (TimelineEvent event : report.timeline()) {
            requireText(violations, "timeline event description", event.description());
            for (String token : timeTokensMissingFromNotes(event.time(), lowerNotes)) {
                violations.add("timeline time '" + event.time() + "' contains '" + token
                        + "', which is not in the notes");
            }
        }
        for (ActionItem item : report.actionItems()) {
            requireText(violations, "action item description", item.description());
            if (item.priority() == null) {
                violations.add("action item '" + item.description() + "' has no priority");
            }
            if (item.owner() != null && !lowerNotes.contains(normalizeOwner(item.owner()))) {
                violations.add("action item owner '" + item.owner() + "' does not appear in the notes");
            }
        }
        return violations;
    }

    private static void requireText(List<String> violations, String field, String value) {
        if (value == null || value.isBlank()) {
            violations.add(field + " is blank");
        }
    }

    private static void requireMentions(List<String> violations, String field, List<String> required,
                                        List<String> values) {
        String haystack = String.join("\n", values).toLowerCase(Locale.ROOT);
        for (String requirement : required) {
            List<String> alternatives = Arrays.stream(requirement.split("\\|"))
                    .map(s -> s.strip().toLowerCase(Locale.ROOT))
                    .toList();
            if (alternatives.stream().noneMatch(haystack::contains)) {
                violations.add(field + " should mention " + String.join(" or ", alternatives.stream()
                        .map(a -> "'" + a + "'").toList()) + " but was " + values);
            }
        }
    }

    /**
     * Models may add context such as a weekday or "~" around a timestamp, which is fine; what the prompt
     * forbids is inventing one. So every token containing a digit must appear verbatim in the notes.
     */
    private static List<String> timeTokensMissingFromNotes(String time, String lowerNotes) {
        if (time == null) {
            return List.of();
        }
        return Arrays.stream(time.toLowerCase(Locale.ROOT).split("\\s+"))
                .map(token -> token.replaceAll("^[^\\p{Alnum}]+|[^\\p{Alnum}]+$", ""))
                .filter(token -> token.chars().anyMatch(Character::isDigit))
                .filter(token -> !lowerNotes.contains(token))
                .toList();
    }

    /** Treats "the platform team" in a report as matching "platform" in the notes. */
    private static String normalizeOwner(String owner) {
        return owner.strip().toLowerCase(Locale.ROOT)
                .replaceFirst("^the ", "")
                .replaceFirst(" team$", "");
    }

    private static List<String> optional(String value) {
        return value == null ? List.of() : List.of(value);
    }

    private static <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : List.copyOf(list);
    }
}
