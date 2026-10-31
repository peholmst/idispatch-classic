package net.pkhapps.idispatch.server.assignmenttype;

import net.pkhapps.idispatch.server.shared.BilingualText;

import static java.util.Objects.requireNonNull;

/**
 * A type of assignment, such as a building fire. Assignments refer to their type by its {@link AssignmentTypeId}.
 *
 * @param version incremented on every change, so that a change based on an older version can be refused
 * @param active  {@code false} once the type has been deleted; deleted types are kept for the assignments that have
 *                them
 */
public record AssignmentType(AssignmentTypeId id, long version, AssignmentTypeCode code, BilingualText description,
                           boolean active) {

    public AssignmentType {
        requireNonNull(id);
        requireNonNull(code);
        requireNonNull(description);
    }

    public AssignmentTypeDetails details() {
        return new AssignmentTypeDetails(code, description);
    }

    /**
     * Returns this type with the given details, to be saved as a change of this version.
     */
    public AssignmentType withDetails(AssignmentTypeDetails details) {
        return new AssignmentType(id, version, details.code(), details.description(), active);
    }
}
