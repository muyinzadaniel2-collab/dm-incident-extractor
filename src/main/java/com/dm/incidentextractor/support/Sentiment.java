package com.dm.incidentextractor.support;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The customer's overall tone in a message.
 */
public enum Sentiment {
    @JsonProperty("positive") POSITIVE,
    @JsonProperty("neutral") NEUTRAL,
    @JsonProperty("negative") NEGATIVE,
    /** Clearly both positive and negative, e.g. praise followed by a complaint. */
    @JsonProperty("mixed") MIXED
}
