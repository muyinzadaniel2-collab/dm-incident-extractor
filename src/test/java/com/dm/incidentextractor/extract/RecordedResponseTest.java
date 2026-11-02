package com.dm.incidentextractor.extract;

import com.dm.incidentextractor.llm.LlmClient;
import com.dm.incidentextractor.llm.LlmRequest;
import com.dm.incidentextractor.model.IncidentReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Runs each fixture's recorded model reply through the extractor and checks the resulting report against
 * the fixture's expectations. Needs no network or API keys.
 */
class RecordedResponseTest {

    static Stream<IncidentFixture> fixturesWithRecording() {
        return IncidentFixture.all().stream().filter(f -> f.recordedResponse() != null);
    }

    @Test
    void fixturesAreDiscovered() {
        assertFalse(fixturesWithRecording().toList().isEmpty(), "No fixtures found under /incidents");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixturesWithRecording")
    void recordedResponseMeetsExpectations(IncidentFixture fixture) {
        LlmClient replay = new LlmClient() {
            @Override
            public String complete(LlmRequest request) {
                return fixture.recordedResponse();
            }

            @Override
            public String name() {
                return "recorded/" + fixture.name();
            }
        };

        IncidentReport report = new IncidentExtractor(replay).extract(fixture.notes());

        fixture.assertMetBy(report);
    }
}
