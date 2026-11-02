package com.dm.incidentextractor.llm.openai;

import com.dm.incidentextractor.llm.HttpJsonClient;
import com.dm.incidentextractor.llm.LlmClient;
import com.dm.incidentextractor.llm.LlmException;
import com.dm.incidentextractor.llm.LlmRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.Map;

/**
 * {@link LlmClient} backed by the OpenAI Chat Completions API. Requests JSON-mode output.
 */
public class OpenAiClient implements LlmClient {

    public static final String DEFAULT_MODEL = "gpt-5";
    public static final URI DEFAULT_BASE_URI = URI.create("https://api.openai.com");

    private final HttpJsonClient http;
    private final URI completionsUri;
    private final String apiKey;
    private final String model;

    public OpenAiClient(String apiKey, String model) {
        this(new HttpJsonClient(), DEFAULT_BASE_URI, apiKey, model);
    }

    public OpenAiClient(HttpJsonClient http, URI baseUri, String apiKey, String model) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("OpenAI API key is required");
        }
        this.http = http;
        this.completionsUri = baseUri.resolve("/v1/chat/completions");
        this.apiKey = apiKey;
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model;
    }

    @Override
    public String complete(LlmRequest request) {
        ObjectNode body = http.mapper().createObjectNode()
                .put("model", model)
                .put("max_completion_tokens", request.maxTokens());
        body.putObject("response_format").put("type", "json_object");

        ArrayNode messages = body.putArray("messages");
        if (request.systemPrompt() != null) {
            messages.addObject().put("role", "system").put("content", request.systemPrompt());
        }
        messages.addObject().put("role", "user").put("content", request.userPrompt());

        JsonNode response = http.post(completionsUri, Map.of("authorization", "Bearer " + apiKey), body);

        JsonNode choice = response.path("choices").path(0);
        if ("length".equals(choice.path("finish_reason").asText())) {
            throw new LlmException("OpenAI response truncated at max_completion_tokens=" + request.maxTokens());
        }
        String content = choice.path("message").path("content").asText("");
        if (content.isEmpty()) {
            throw new LlmException("OpenAI response contained no content: " + response);
        }
        return content;
    }

    @Override
    public String name() {
        return "openai/" + model;
    }
}
