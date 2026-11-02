package com.dm.incidentextractor.llm;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LlmProviderTest {

    @Test
    void parsesCaseInsensitively() {
        assertEquals(LlmProvider.OPENAI, LlmProvider.parse("OpenAI"));
        assertEquals(LlmProvider.ANTHROPIC, LlmProvider.parse(" anthropic "));
        assertThrows(IllegalArgumentException.class, () -> LlmProvider.parse("gemini"));
    }

    @Test
    void buildsClientFromEnvironment() {
        Map<String, String> env = Map.of("OPENAI_API_KEY", "k", "OPENAI_MODEL", "env-model");
        assertEquals("openai/env-model", LlmProvider.OPENAI.createClient(env::get, null).name());
        assertEquals("openai/flag-model", LlmProvider.OPENAI.createClient(env::get, "flag-model").name());
    }

    @Test
    void requiresApiKey() {
        Map<String, String> env = Map.of();
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> LlmProvider.ANTHROPIC.createClient(env::get, null));
        assertEquals("ANTHROPIC_API_KEY is not set", e.getMessage());
    }
}
