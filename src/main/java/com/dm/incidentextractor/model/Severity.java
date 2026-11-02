package com.dm.incidentextractor.model;

/**
 * Incident severity, from most (SEV1) to least (SEV4) severe.
 */
public enum Severity {
    /** Critical: full outage, data loss, or security breach affecting customers. */
    SEV1,
    /** Major: significant degradation or a core feature unavailable for many users. */
    SEV2,
    /** Minor: partial degradation with a workaround, or limited user impact. */
    SEV3,
    /** Low: cosmetic issue or internal-only impact. */
    SEV4,
    /** The text does not contain enough information to judge severity. */
    UNKNOWN
}
