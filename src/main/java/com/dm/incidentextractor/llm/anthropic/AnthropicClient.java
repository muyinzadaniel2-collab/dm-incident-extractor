package com.dm.incidentextractor.llm.anthropic;

import com.dm.incidentextractor.llm.HttpJsonClient;
import com.dm.incidentextractor.llm.LlmClient;
import com.dm.incidentextractor.llm.LlmException;
import com.dm.incidentextractor.llm.LlmRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.Map;

/**
 * {@link LlmClient} backed by the Anthropic Messages API.
 */
public class AnthropicClient implements LlmClient {

    public static final String DEFAULT_MODEL = "claude-opus-5-5";
    public static final URI DEFAULT_BASE_URI = URI.create("https://api.anthropic.com");
    static final String API_VERSION = "2023-06-01";

    private final HttpJsonClient http;
    private final URI messagesUri;
    private final String apiKey;
    private final String model;

    public AnthropicClient(String apiKey, String model) {
        this(new HttpJsonClient(), DEFAULT_BASE_URI, apiKey, model);
    }

    public AnthropicClient(HttpJsonClient http, URI baseUri, String apiKey, String model) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Anthropic API key is required");
        }
        this.http = http;
        this.messagesUri = baseUri.resolve("/v1/messages");
        this.apiKey = apiKey;
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model;
    }

    @Override
    public String complete(LlmRequest request) {
        ObjectNode body = http.mapper().createObjectNode()
                .put("model", model)
                .put("max_tokens", request.maxTokens());
        if (request.systemPrompt() != null) {
            body.put("system", request.systemPrompt());
        }
        body.putArray("messages").addObject()
                .put("role", "user")
                .put("content", request.userPrompt());

        JsonNode response = http.post(messagesUri,
                Map.of("x-api-key", apiKey, "anthropic-version", API_VERSION),
                body);

        if ("max_tokens".equals(response.path("stop_reason").asText())) {
            throw new LlmException("Anthropic response truncated at max_tokens=" + request.maxTokens());
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode block : response.path("content")) {
            if ("text".equals(block.path("type").asText())) {
                text.append(block.path("text").asText());
            }
        }
        if (text.isEmpty()) {
            throw new LlmException("Anthropic response contained no text: " + response);
        }
        return text.toString();
    }

    @Override
    public String name() {
        return "anthropic/" + model;
    }
}
