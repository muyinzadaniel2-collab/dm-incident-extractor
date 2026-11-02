package com.dm.incidentextractor.llm;

/**
 * A provider-agnostic request to a language model.
 *
 * @param systemPrompt instructions that frame the model's behaviour
 * @param userPrompt   the user turn content
 * @param maxTokens    upper bound on generated output tokens
 */
public record LlmRequest(String systemPrompt, String userPrompt, int maxTokens) {

    public static final int DEFAULT_MAX_TOKENS = 4096;

    public LlmRequest(String systemPrompt, String userPrompt) {
        this(systemPrompt, userPrompt, DEFAULT_MAX_TOKENS);
    }
}
