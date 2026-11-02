package com.dm.incidentextractor.llm;

import com.dm.incidentextractor.llm.openai.OpenAiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void sendsChatCompletionRequestInJsonMode() throws Exception {
        String response = """
                {"choices":[{"index":0,"finish_reason":"stop",
                  "message":{"role":"assistant","content":"{\\"a\\":1}"}}]}""";
        try (StubServer server = new StubServer(200, response)) {
            OpenAiClient client = new OpenAiClient(new HttpJsonClient(), server.baseUri(), "test-key", "gpt-test");

            String result = client.complete(new LlmRequest("sys", "hello", 1000));

            assertEquals("{\"a\":1}", result);
            assertEquals("/v1/chat/completions", server.lastPath());
            assertEquals("Bearer test-key", server.header("authorization"));

            JsonNode body = mapper.readTree(server.lastBody());
            assertEquals("gpt-test", body.get("model").asText());
            assertEquals(1000, body.get("max_completion_tokens").asInt());
            assertEquals("json_object", body.at("/response_format/type").asText());
            assertEquals("system", body.at("/messages/0/role").asText());
            assertEquals("sys", body.at("/messages/0/content").asText());
            assertEquals("user", body.at("/messages/1/role").asText());
            assertEquals("hello", body.at("/messages/1/content").asText());
        }
    }

    @Test
    void failsOnTruncatedResponse() throws Exception {
        String response = """
                {"choices":[{"finish_reason":"length","message":{"content":"{"}}]}""";
        try (StubServer server = new StubServer(200, response)) {
            OpenAiClient client = new OpenAiClient(new HttpJsonClient(), server.baseUri(), "k", null);
            LlmException e = assertThrows(LlmException.class, () -> client.complete(new LlmRequest("s", "u")));
            assertTrue(e.getMessage().contains("truncated"));
        }
    }

    @Test
    void surfacesHttpErrors() throws Exception {
        try (StubServer server = new StubServer(429, "{\"error\":{\"message\":\"rate limited\"}}")) {
            OpenAiClient client = new OpenAiClient(new HttpJsonClient(), server.baseUri(), "k", null);
            LlmException e = assertThrows(LlmException.class, () -> client.complete(new LlmRequest("s", "u")));
            assertTrue(e.getMessage().contains("429"));
        }
    }
}
