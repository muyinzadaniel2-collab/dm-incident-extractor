package com.dm.incidentextractor.model;

import java.util.List;

/**
 * Structured incident report extracted from free-text engineer notes.
 *
 * @param title               short descriptive title
 * @param summary             2-4 sentence overview of what happened
 * @param severity            assessed severity
 * @param severityRationale   why that severity was chosen
 * @param impact              user/business impact
 * @param affectedSystems     services, components, or infrastructure involved
 * @param detection           how the incident was detected
 * @param timeline            chronological events
 * @param rootCauses          underlying causes
 * @param contributingFactors conditions that made the incident worse or more likely
 * @param resolution          how the incident was mitigated or resolved
 * @param actionItems         follow-up tasks
 * @param openQuestions       information missing from the source text
 */
public record IncidentReport(
        String title,
        String summary,
        Severity severity,
        String severityRationale,
        String impact,
        List<String> affectedSystems,
        String detection,
        List<TimelineEvent> timeline,
        List<String> rootCauses,
        List<String> contributingFactors,
        String resolution,
        List<ActionItem> actionItems,
        List<String> openQuestions) {

    public IncidentReport {
        severity = severity == null ? Severity.UNKNOWN : severity;
        affectedSystems = nullToEmpty(affectedSystems);
        timeline = nullToEmpty(timeline);
        rootCauses = nullToEmpty(rootCauses);
        contributingFactors = nullToEmpty(contributingFactors);
        actionItems = nullToEmpty(actionItems);
        openQuestions = nullToEmpty(openQuestions);
    }

    private static <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : List.copyOf(list);
    }
}
