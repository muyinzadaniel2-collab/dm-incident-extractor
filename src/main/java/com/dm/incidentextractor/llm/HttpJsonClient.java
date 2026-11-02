package com.dm.incidentextractor.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Small helper for POSTing JSON to provider REST APIs.
 */
public final class HttpJsonClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(3);

    private final HttpClient http;
    private final ObjectMapper mapper;

    public HttpJsonClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build(), new ObjectMapper());
    }

    public HttpJsonClient(HttpClient http, ObjectMapper mapper) {
        this.http = http;
        this.mapper = mapper;
    }

    public ObjectMapper mapper() {
        return mapper;
    }

    public JsonNode post(URI uri, Map<String, String> headers, JsonNode body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            headers.forEach(builder::header);

            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new LlmException("HTTP " + response.statusCode() + " from " + uri + ": " + response.body());
            }
            return mapper.readTree(response.body());
        } catch (IOException e) {
            throw new LlmException("Request to " + uri + " failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("Request to " + uri + " interrupted", e);
        }
    }
}
