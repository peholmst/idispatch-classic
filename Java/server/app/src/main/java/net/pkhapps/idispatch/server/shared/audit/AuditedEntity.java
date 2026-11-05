package net.pkhapps.idispatch.server.shared.audit;

import static java.util.Objects.requireNonNull;

/**
 * An entity that the {@link AuditLog} keeps the history of, told by its type and ID. A report's ID is the ID of its
 * assignment, since a report is created only when it is first saved.
 */
public record AuditedEntity(Type type, long id) {

    public AuditedEntity {
        requireNonNull(type);
    }

    public enum Type {
        MUNICIPALITY,
        ASSIGNMENT_TYPE,
        RESOURCE_TYPE,
        RESOURCE,
        DESTINATION,
        ASSIGNMENT,
        ASSIGNMENT_REPORT
    }
}
