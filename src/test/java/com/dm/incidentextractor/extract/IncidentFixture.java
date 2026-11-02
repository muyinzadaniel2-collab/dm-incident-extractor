package com.dm.incidentextractor.extract;

import com.dm.incidentextractor.model.IncidentReport;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * A sample incident under {@code src/test/resources/incidents/<name>/}:
 * <ul>
 *   <li>{@code notes.txt} – the free-text notes an engineer might write</li>
 *   <li>{@code expected.json} – {@link ReportExpectations} a correct report must meet</li>
 *   <li>{@code recorded-response.json} – optional model reply used by the offline tests</li>
 * </ul>
 * Adding a directory with these files adds a case to every fixture-driven test.
 */
record IncidentFixture(String name, String notes, ReportExpectations expected, String recordedResponse) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static List<IncidentFixture> all() {
        URL root = IncidentFixture.class.getResource("/incidents");
        if (root == null) {
            throw new IllegalStateException("Missing test resource directory /incidents");
        }
        try (Stream<Path> dirs = Files.list(Path.of(root.toURI()))) {
            return dirs.filter(Files::isDirectory).sorted().map(IncidentFixture::load).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static IncidentFixture load(Path dir) {
        try {
            Path recorded = dir.resolve("recorded-response.json");
            return new IncidentFixture(
                    dir.getFileName().toString(),
                    Files.readString(dir.resolve("notes.txt")),
                    MAPPER.readValue(dir.resolve("expected.json").toFile(), ReportExpectations.class),
                    Files.exists(recorded) ? Files.readString(recorded) : null);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load fixture " + dir, e);
        }
    }

    /** Fails with every violation listed, so one run shows all that is wrong with a report. */
    void assertMetBy(IncidentReport report) {
        List<String> violations = expected.violations(report, notes);
        if (!violations.isEmpty()) {
            fail("Report for '" + name + "' does not meet expectations:\n  - "
                    + String.join("\n  - ", violations) + "\nReport: " + report);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
