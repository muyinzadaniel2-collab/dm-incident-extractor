package com.dm.incidentextractor.support;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Structured triage of a single customer question or review.
 *
 * @param category      what the message is about
 * @param priority      how quickly it needs attention
 * @param sentiment     the customer's overall tone
 * @param summary       one sentence describing the customer's message
 * @param requiresHuman whether a person must handle it rather than an automated reply
 */
public record SupportTicket(
        Category category,
        Priority priority,
        Sentiment sentiment,
        String summary,
        @JsonProperty("requires_human") Boolean requiresHuman) {

    public SupportTicket {
        category = category == null ? Category.OTHER : category;
        // When the model omits this, err on the side of a person looking at it.
        requiresHuman = requiresHuman == null ? Boolean.TRUE : requiresHuman;
    }
}
