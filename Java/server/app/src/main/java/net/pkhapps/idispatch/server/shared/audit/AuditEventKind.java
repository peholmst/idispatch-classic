package net.pkhapps.idispatch.server.shared.audit;

/**
 * What a user did to an {@link AuditedEntity}.
 */
public enum AuditEventKind {
    CREATED,
    EDITED,
    /**
     * Deleted softly; only administration records are.
     */
    DELETED,
    /**
     * Restored after being deleted; only administration records are.
     */
    RESTORED,
    /**
     * Closed: an assignment once it is handled, or a report once a reviewer has checked it.
     */
    CLOSED,
    /**
     * Re-opened; only reports are.
     */
    REOPENED,
    /**
     * Given a new key; only mobile destinations are.
     */
    MOBILE_KEY_REPLACED,
    /**
     * Moved to another unit by a dispatcher; only mobile destinations are.
     */
    MOBILE_UNIT_CHANGED
}
