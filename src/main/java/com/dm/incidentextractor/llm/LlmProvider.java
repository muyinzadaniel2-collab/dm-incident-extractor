package com.dm.incidentextractor.llm;

import com.dm.incidentextractor.llm.anthropic.AnthropicClient;
import com.dm.incidentextractor.llm.openai.OpenAiClient;

import java.util.Locale;
import java.util.function.Function;

/**
 * Supported model providers and how to build a client for each from environment configuration.
 */
public enum LlmProvider {

    ANTHROPIC("ANTHROPIC_API_KEY", "ANTHROPIC_MODEL"),
    OPENAI("OPENAI_API_KEY", "OPENAI_MODEL");

    private final String apiKeyEnv;
    private final String modelEnv;

    LlmProvider(String apiKeyEnv, String modelEnv) {
        this.apiKeyEnv = apiKeyEnv;
        this.modelEnv = modelEnv;
    }

    public static LlmProvider parse(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown provider '" + value + "'. Expected one of: anthropic, openai");
        }
    }

    /**
     * Builds a client using API key and default model from the given environment lookup.
     *
     * @param env           environment lookup, typically {@code System::getenv}
     * @param modelOverride model to use instead of the environment/default, may be null
     */
    public LlmClient createClient(Function<String, String> env, String modelOverride) {
        String apiKey = env.apply(apiKeyEnv);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(apiKeyEnv + " is not set");
        }
        String model = modelOverride != null ? modelOverride : env.apply(modelEnv);
        return switch (this) {
            case ANTHROPIC -> new AnthropicClient(apiKey, model);
            case OPENAI -> new OpenAiClient(apiKey, model);
        };
    }
}
