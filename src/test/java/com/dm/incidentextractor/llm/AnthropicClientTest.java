package com.dm.incidentextractor.llm;

import com.dm.incidentextractor.llm.anthropic.AnthropicClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnthropicClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void sendsMessagesRequestAndConcatenatesTextBlocks() throws Exception {
        String response = """
                {"content":[{"type":"text","text":"{\\"a\\":"},{"type":"text","text":"1}"}],
                 "stop_reason":"end_turn"}""";
        try (StubServer server = new StubServer(200, response)) {
            AnthropicClient client = new AnthropicClient(new HttpJsonClient(), server.baseUri(), "test-key", "claude-test");

            String result = client.complete(new LlmRequest("sys", "hello", 1000));

            assertEquals("{\"a\":1}", result);
            assertEquals("/v1/messages", server.lastPath());
            assertEquals("test-key", server.header("x-api-key"));
            assertEquals("2023-06-01", server.header("anthropic-version"));

            JsonNode body = mapper.readTree(server.lastBody());
            assertEquals("claude-test", body.get("model").asText());
            assertEquals(1000, body.get("max_tokens").asInt());
            assertEquals("sys", body.get("system").asText());
            assertEquals("user", body.at("/messages/0/role").asText());
            assertEquals("hello", body.at("/messages/0/content").asText());
        }
    }

    @Test
    void failsOnTruncatedResponse() throws Exception {
        String response = """
                {"content":[{"type":"text","text":"{"}],"stop_reason":"max_tokens"}""";
        try (StubServer server = new StubServer(200, response)) {
            AnthropicClient client = new AnthropicClient(new HttpJsonClient(), server.baseUri(), "k", null);
            LlmException e = assertThrows(LlmException.class, () -> client.complete(new LlmRequest("s", "u")));
            assertTrue(e.getMessage().contains("truncated"));
        }
    }

    @Test
    void surfacesHttpErrors() throws Exception {
        try (StubServer server = new StubServer(401, "{\"error\":{\"message\":\"bad key\"}}")) {
            AnthropicClient client = new AnthropicClient(new HttpJsonClient(), server.baseUri(), "k", null);
            LlmException e = assertThrows(LlmException.class, () -> client.complete(new LlmRequest("s", "u")));
            assertTrue(e.getMessage().contains("401"));
            assertTrue(e.getMessage().contains("bad key"));
        }
    }

    @Test
    void usesDefaultModelWhenNoneGiven() {
        assertEquals("anthropic/" + AnthropicClient.DEFAULT_MODEL, new AnthropicClient("k", null).name());
    }
}
