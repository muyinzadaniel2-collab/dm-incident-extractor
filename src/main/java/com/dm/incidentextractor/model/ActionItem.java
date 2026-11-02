package com.dm.incidentextractor.model;

/**
 * A follow-up task arising from the incident.
 *
 * @param description what needs to be done
 * @param owner       person or team responsible, or null if not stated
 * @param priority    priority of the action item
 */
public record ActionItem(String description, String owner, Priority priority) {

    public enum Priority {
        HIGH, MEDIUM, LOW
    }
}
