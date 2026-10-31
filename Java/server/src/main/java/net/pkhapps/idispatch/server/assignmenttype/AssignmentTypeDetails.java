package net.pkhapps.idispatch.server.assignmenttype;

import net.pkhapps.idispatch.server.shared.BilingualText;

import static java.util.Objects.requireNonNull;

/**
 * What an administrator enters for an {@link AssignmentType}: everything about it except its identity, version and
 * whether it has been deleted.
 */
public record AssignmentTypeDetails(AssignmentTypeCode code, BilingualText description) {

    public AssignmentTypeDetails {
        requireNonNull(code);
        requireNonNull(description);
    }
}
