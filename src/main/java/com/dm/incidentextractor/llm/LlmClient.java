package com.dm.incidentextractor.llm;

/**
 * Abstraction over a language model provider. Implementations turn an {@link LlmRequest}
 * into a single text completion.
 */
public interface LlmClient {

    /**
     * Sends the request to the model and returns the generated text.
     *
     * @throws LlmException if the call fails or the response cannot be interpreted
     */
    String complete(LlmRequest request);

    /** Human-readable identifier, e.g. {@code anthropic/claude-opus-5-5}. */
    String name();
}
