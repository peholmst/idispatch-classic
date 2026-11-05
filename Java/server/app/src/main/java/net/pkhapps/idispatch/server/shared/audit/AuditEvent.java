package net.pkhapps.idispatch.server.shared.audit;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.time.Instant;

import static java.util.Objects.requireNonNull;

/**
 * Something a user did to an entity, and when.
 *
 * @param part which part of the entity the event is about, such as one unit's part of a report, or {@code null} when it
 *             is about the whole entity; what the parts are is up to the feature that records them
 */
public record AuditEvent(AuditedEntity entity, AuditEventKind kind, @Nullable String part, AuditUser user,
                         Instant timestamp) implements Serializable {

    public AuditEvent {
        requireNonNull(entity);
        requireNonNull(kind);
        requireNonNull(user);
        requireNonNull(timestamp);
    }
}
