package com.dm.incidentextractor.extract;

import com.dm.incidentextractor.model.ActionItem;
import com.dm.incidentextractor.model.IncidentReport;
import com.dm.incidentextractor.model.Severity;
import com.dm.incidentextractor.model.TimelineEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks that the expectation checker itself catches bad reports, so a passing fixture test means something. */
class ReportExpectationsTest {

    private static final String NOTES = """
            14:02 config push broke checkout-api, pool size 0. rolled back at 14:24.
            platform team to add config validation.""";

    private static final ReportExpectations EXPECTED = new ReportExpectations(
            List.of(Severity.SEV1), List.of("checkout"), null, List.of("pool"), null,
            List.of("rollback|rolled back"), List.of("validat"), 2, null, false);

    @Test
    void acceptsMatchingReport() {
        assertEquals(List.of(), EXPECTED.violations(goodReport(), NOTES));
    }

    @Test
    void acceptsAnyAlternativeAndIgnoresCase() {
        IncidentReport report = withResolution(goodReport(), "ROLLBACK to previous config");
        assertEquals(List.of(), EXPECTED.violations(report, NOTES));
    }

    @Test
    void flagsWrongSeverity() {
        IncidentReport report = withSeverity(goodReport(), Severity.SEV3);
        assertSingleViolation(report, "severity was SEV3");
    }

    @Test
    void flagsMissingMention() {
        IncidentReport report = withResolution(goodReport(), "Restarted the pods");
        assertSingleViolation(report, "resolution should mention 'rollback' or 'rolled back'");
    }

    @Test
    void flagsInventedTimestamp() {
        IncidentReport report = withTimeline(goodReport(), List.of(
                new TimelineEvent("14:02", "Config pushed"),
                new TimelineEvent("14:20", "Rollback started")));
        assertSingleViolation(report, "'14:20', which is not in the notes");
    }

    @Test
    void allowsContextAroundRealTimestamp() {
        IncidentReport report = withTimeline(goodReport(), List.of(
                new TimelineEvent("~14:02 UTC", "Config pushed"),
                new TimelineEvent(null, "Rolled back")));
        assertEquals(List.of(), EXPECTED.violations(report, NOTES));
    }

    @Test
    void flagsInventedOwner() {
        IncidentReport report = withActionItems(goodReport(),
                List.of(new ActionItem("Add config validation", "SRE team", ActionItem.Priority.HIGH)));
        assertSingleViolation(report, "owner 'SRE team' does not appear in the notes");
    }

    @Test
    void flagsTooFewTimelineEvents() {
        IncidentReport report = withTimeline(goodReport(), List.of(new TimelineEvent("14:02", "Config pushed")));
        assertSingleViolation(report, "timeline has 1 events, expected at least 2");
    }

    @Test
    void flagsGuessedRootCauseAndMissingOpenQuestions() {
        ReportExpectations vague = new ReportExpectations(
                null, null, null, null, null, null, null, 0, 0, true);
        List<String> violations = vague.violations(goodReport(), NOTES);
        assertEquals(2, violations.size(), violations.toString());
        assertTrue(violations.get(0).startsWith("rootCauses has 1 entries, expected at most 0"));
        assertTrue(violations.get(1).startsWith("openQuestions is empty"));
    }

    @Test
    void flagsBlankRequiredFieldsFromEmptyReply() {
        IncidentReport empty = new IncidentReport(null, null, null, null, null, null, null, null, null, null,
                null, null, null);
        List<String> violations = EXPECTED.violations(empty, NOTES);
        assertTrue(violations.contains("title is blank"), violations.toString());
        assertTrue(violations.contains("summary is blank"), violations.toString());
        assertTrue(violations.contains("severity was UNKNOWN, expected one of [SEV1]"), violations.toString());
    }

    private static void assertSingleViolation(IncidentReport report, String expectedFragment) {
        List<String> violations = EXPECTED.violations(report, NOTES);
        assertEquals(1, violations.size(), violations.toString());
        assertTrue(violations.getFirst().contains(expectedFragment), violations.getFirst());
    }

    private static IncidentReport goodReport() {
        return new IncidentReport(
                "Checkout outage", "Config push set pool size to 0.", Severity.SEV1, "All payments failed.",
                "Checkout down", List.of("checkout-api"), "Alert",
                List.of(new TimelineEvent("14:02", "Config pushed"), new TimelineEvent("14:24", "Rolled back")),
                List.of("DB pool size set to 0"), List.of(), "Rolled back config",
                List.of(new ActionItem("Add config validation", "platform team", ActionItem.Priority.HIGH)),
                List.of());
    }

    private static IncidentReport withSeverity(IncidentReport r, Severity severity) {
        return new IncidentReport(r.title(), r.summary(), severity, r.severityRationale(), r.impact(),
                r.affectedSystems(), r.detection(), r.timeline(), r.rootCauses(), r.contributingFactors(),
                r.resolution(), r.actionItems(), r.openQuestions());
    }

    private static IncidentReport withResolution(IncidentReport r, String resolution) {
        return new IncidentReport(r.title(), r.summary(), r.severity(), r.severityRationale(), r.impact(),
                r.affectedSystems(), r.detection(), r.timeline(), r.rootCauses(), r.contributingFactors(),
                resolution, r.actionItems(), r.openQuestions());
    }

    private static IncidentReport withTimeline(IncidentReport r, List<TimelineEvent> timeline) {
        return new IncidentReport(r.title(), r.summary(), r.severity(), r.severityRationale(), r.impact(),
                r.affectedSystems(), r.detection(), timeline, r.rootCauses(), r.contributingFactors(),
                r.resolution(), r.actionItems(), r.openQuestions());
    }

    private static IncidentReport withActionItems(IncidentReport r, List<ActionItem> actionItems) {
        return new IncidentReport(r.title(), r.summary(), r.severity(), r.severityRationale(), r.impact(),
                r.affectedSystems(), r.detection(), r.timeline(), r.rootCauses(), r.contributingFactors(),
                r.resolution(), actionItems, r.openQuestions());
    }
}
