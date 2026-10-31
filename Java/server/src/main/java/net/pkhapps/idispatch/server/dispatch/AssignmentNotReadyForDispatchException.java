package net.pkhapps.idispatch.server.dispatch;

import net.pkhapps.idispatch.server.assignment.MissingForDispatch;

import java.util.Set;

/**
 * Thrown when resources are dispatched to an assignment that still lacks something a dispatch notification needs.
 */
public class AssignmentNotReadyForDispatchException extends Exception {

    private final Set<MissingForDispatch> missing;

    public AssignmentNotReadyForDispatchException(Set<MissingForDispatch> missing) {
        super("The assignment is missing " + missing);
        this.missing = Set.copyOf(missing);
    }

    /**
     * What the assignment is missing.
     */
    public Set<MissingForDispatch> getMissing() {
        return missing;
    }
}
