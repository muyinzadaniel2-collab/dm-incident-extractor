package com.dm.incidentextractor.support;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * How quickly a customer message needs attention, from least to most urgent.
 */
public enum Priority {
    /** Questions, feedback, feature requests; nothing is blocked. */
    @JsonProperty("low") LOW,
    /** Something is degraded but the user has a workaround. */
    @JsonProperty("medium") MEDIUM,
    /** The user is blocked and has no workaround. */
    @JsonProperty("high") HIGH,
    /** Locked out of paid service, security or data concern, or many users affected. */
    @JsonProperty("urgent") URGENT
}
