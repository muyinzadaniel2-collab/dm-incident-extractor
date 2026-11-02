package com.dm.incidentextractor.extract;

import com.dm.incidentextractor.llm.LlmClient;
import com.dm.incidentextractor.llm.LlmRequest;
import com.dm.incidentextractor.model.ActionItem;
import com.dm.incidentextractor.model.IncidentReport;
import com.dm.incidentextractor.model.Severity;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncidentExtractorTest {

    private static final String REPORT_JSON = """
            {
              "title": "Checkout API outage",
              "summary": "Checkout failed for 40 minutes after a bad config push.",
              "severity": "sev1",
              "severityRationale": "All customers unable to pay.",
              "impact": "100% of checkout requests failed.",
              "affectedSystems": ["checkout-api"],
              "detection": "PagerDuty alert on 5xx rate",
              "timeline": [{"time": "14:02", "description": "Config pushed"}],
              "rootCauses": ["Invalid DB pool size in config"],
              "contributingFactors": ["No config validation in CI"],
              "resolution": "Rolled back config",
              "actionItems": [{"description": "Add config validation", "owner": "platform", "priority": "HIGH"}],
              "openQuestions": [],
              "someExtraField": "ignored"
            }""";

    @Test
    void parsesReportAndSendsNotesToModel() {
        AtomicReference<LlmRequest> captured = new AtomicReference<>();
        IncidentExtractor extractor = new IncidentExtractor(fakeClient(captured, REPORT_JSON));

        IncidentReport report = extractor.extract("checkout died at 14:02 after config push");

        assertEquals("Checkout API outage", report.title());
        assertEquals(Severity.SEV1, report.severity());
        assertEquals("Invalid DB pool size in config", report.rootCauses().getFirst());
        assertEquals(ActionItem.Priority.HIGH, report.actionItems().getFirst().priority());
        assertEquals("14:02", report.timeline().getFirst().time());

        assertTrue(captured.get().userPrompt().contains("checkout died at 14:02 after config push"));
        assertTrue(captured.get().systemPrompt().contains("SEV1"));
    }

    @Test
    void toleratesCodeFencesAndMissingFields() {
        IncidentExtractor extractor = new IncidentExtractor(
                fakeClient(new AtomicReference<>(), "```json\n{\"title\": \"Minor blip\"}\n```"));

        IncidentReport report = extractor.extract("something small happened");

        assertEquals("Minor blip", report.title());
        assertEquals(Severity.UNKNOWN, report.severity());
        assertTrue(report.actionItems().isEmpty());
        assertTrue(report.rootCauses().isEmpty());
    }

    @Test
    void reportsUnparseableOutput() {
        IncidentExtractor extractor = new IncidentExtractor(
                fakeClient(new AtomicReference<>(), "Sorry, I can't help with that."));

        IncidentExtractionException e = assertThrows(IncidentExtractionException.class,
                () -> extractor.extract("notes"));
        assertTrue(e.getMessage().contains("fake/model"));
        assertTrue(e.getMessage().contains("Sorry"));
    }

    @Test
    void rejectsEmptyInput() {
        IncidentExtractor extractor = new IncidentExtractor(fakeClient(new AtomicReference<>(), "{}"));
        assertThrows(IllegalArgumentException.class, () -> extractor.extract("   "));
    }

    private static LlmClient fakeClient(AtomicReference<LlmRequest> captured, String response) {
        return new LlmClient() {
            @Override
            public String complete(LlmRequest request) {
                captured.set(request);
                return response;
            }

            @Override
            public String name() {
                return "fake/model";
            }
        };
    }
}
