package com.dm.incidentextractor.extract;

import com.dm.incidentextractor.llm.LlmClient;
import com.dm.incidentextractor.llm.LlmProvider;
import com.dm.incidentextractor.llm.LlmRequest;
import com.dm.incidentextractor.model.IncidentReport;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Sends every fixture to every provider and checks the real model output against the fixture's
 * expectations. These calls are billed, so the test only runs when asked for:
 * <pre>
 * mvn test -Dtest=LiveExtractionTest -Dlive=true
 * </pre>
 * Add {@code -Dlive.provider=openai} and/or {@code -Dlive.fixture=vague-login-logouts} to run a subset.
 * Providers without an API key are skipped. Each raw reply is saved to
 * {@code target/live-results/<provider>/<fixture>.json}; copy one over a fixture's
 * {@code recorded-response.json} to make it part of the offline suite.
 */
@EnabledIfSystemProperty(named = "live", matches = "true")
class LiveExtractionTest {

    private static final Path RESULTS_DIR = Path.of("target", "live-results");

    static Stream<Arguments> providersAndFixtures() {
        String onlyProvider = System.getProperty("live.provider");
        String onlyFixture = System.getProperty("live.fixture");
        return Stream.of(LlmProvider.values())
                .filter(p -> onlyProvider == null || p == LlmProvider.parse(onlyProvider))
                .flatMap(provider -> IncidentFixture.all().stream()
                        .filter(f -> onlyFixture == null || f.name().equals(onlyFixture))
                        .map(f -> Arguments.of(provider, f)));
    }

    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("providersAndFixtures")
    void modelOutputMeetsExpectations(LlmProvider provider, IncidentFixture fixture) {
        String keyEnv = provider.name() + "_API_KEY";
        String key = System.getenv(keyEnv);
        assumeTrue(key != null && !key.isBlank(), keyEnv + " is not set");

        LlmClient client = provider.createClient(System::getenv, null);
        Path resultFile = RESULTS_DIR.resolve(provider.name().toLowerCase(Locale.ROOT))
                .resolve(fixture.name() + ".json");

        IncidentReport report = new IncidentExtractor(saving(client, resultFile)).extract(fixture.notes());

        fixture.assertMetBy(report);
    }

    /** Saves the raw reply before it is parsed, so it is kept even when parsing fails. */
    private static LlmClient saving(LlmClient delegate, Path file) {
        return new LlmClient() {
            @Override
            public String complete(LlmRequest request) {
                String response = delegate.complete(request);
                try {
                    Files.createDirectories(file.getParent());
                    Files.writeString(file, response);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return response;
            }

            @Override
            public String name() {
                return delegate.name();
            }
        };
    }
}
