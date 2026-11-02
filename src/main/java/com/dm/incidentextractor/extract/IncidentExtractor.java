package com.dm.incidentextractor.extract;

import com.dm.incidentextractor.llm.LlmClient;
import com.dm.incidentextractor.llm.LlmRequest;
import com.dm.incidentextractor.model.IncidentReport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Turns free-text incident notes into an {@link IncidentReport} using any {@link LlmClient}.
 */
public class IncidentExtractor {

    private static final String SYSTEM_PROMPT_RESOURCE = "/prompts/incident-system.txt";

    private final LlmClient client;
    private final ObjectMapper mapper;
    private final String systemPrompt;

    public IncidentExtractor(LlmClient client) {
        this.client = client;
        this.mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .build();
        this.systemPrompt = loadResource(SYSTEM_PROMPT_RESOURCE);
    }

    public IncidentReport extract(String incidentText) {
        if (incidentText == null || incidentText.isBlank()) {
            throw new IllegalArgumentException("Incident text is empty");
        }
        String userPrompt = "<incident_notes>\n" + incidentText.strip() + "\n</incident_notes>\n\n"
                + "Produce the JSON incident report for the notes above.";

        String raw = client.complete(new LlmRequest(systemPrompt, userPrompt));
        try {
            return mapper.readValue(stripCodeFence(raw), IncidentReport.class);
        } catch (JsonProcessingException e) {
            throw new IncidentExtractionException(
                    "Could not parse report from " + client.name() + ". Raw output:\n" + raw, e);
        }
    }

    /** Models occasionally wrap JSON in markdown fences despite instructions; tolerate that. */
    static String stripCodeFence(String text) {
        String trimmed = text.strip();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int closingFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && closingFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, closingFence).strip();
            }
        }
        return trimmed;
    }

    private static String loadResource(String path) {
        try (InputStream in = IncidentExtractor.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
