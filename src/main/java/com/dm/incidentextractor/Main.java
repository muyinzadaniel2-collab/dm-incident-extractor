package com.dm.incidentextractor;

import com.dm.incidentextractor.extract.IncidentExtractionException;
import com.dm.incidentextractor.extract.IncidentExtractor;
import com.dm.incidentextractor.llm.LlmClient;
import com.dm.incidentextractor.llm.LlmException;
import com.dm.incidentextractor.llm.LlmProvider;
import com.dm.incidentextractor.model.IncidentReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Command-line entry point.
 *
 * <pre>
 * java -jar dm-incident-extractor.jar [--provider anthropic|openai] [--model MODEL] [FILE]
 * </pre>
 * Reads incident notes from FILE, or stdin if omitted, and prints the structured report as JSON.
 */
public class Main {

    private static final String USAGE =
            "Usage: dm-incident-extractor [--provider anthropic|openai] [--model MODEL] [FILE]";

    public static void main(String[] args) throws IOException {
        LlmProvider provider = LlmProvider.ANTHROPIC;
        String model = null;
        Path file = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--provider", "-p" -> provider = LlmProvider.parse(requireValue(args, ++i));
                case "--model", "-m" -> model = requireValue(args, ++i);
                case "--help", "-h" -> {
                    System.out.println(USAGE);
                    return;
                }
                default -> {
                    if (args[i].startsWith("-") || file != null) {
                        fail("Unexpected argument: " + args[i]);
                    }
                    file = Path.of(args[i]);
                }
            }
        }

        String text = file != null
                ? Files.readString(file)
                : new String(System.in.readAllBytes(), StandardCharsets.UTF_8);

        IncidentReport report;
        try {
            LlmClient client = provider.createClient(System::getenv, model);
            report = new IncidentExtractor(client).extract(text);
        } catch (IllegalArgumentException | IllegalStateException | LlmException | IncidentExtractionException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
            return;
        }

        ObjectMapper out = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        System.out.println(out.writeValueAsString(report));
    }

    private static String requireValue(String[] args, int i) {
        if (i >= args.length) {
            fail("Missing value for " + args[i - 1]);
        }
        return args[i];
    }

    private static void fail(String message) {
        System.err.println(message);
        System.err.println(USAGE);
        System.exit(2);
    }
}
