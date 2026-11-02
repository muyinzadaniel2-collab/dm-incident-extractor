package com.dm.incidentextractor.model;

/**
 * A single point on the incident timeline.
 *
 * @param time        timestamp as stated in the source text (free-form, may be null if only ordering is known)
 * @param description what happened at that point
 */
public record TimelineEvent(String time, String description) {
}
