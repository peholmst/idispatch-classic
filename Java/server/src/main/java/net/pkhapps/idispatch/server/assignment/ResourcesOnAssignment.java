package net.pkhapps.idispatch.server.assignment;

/**
 * What closing an assignment needs to know and do about the resources on it. The resource feature implements this,
 * because resources refer to assignments and not the other way around.
 */
public interface ResourcesOnAssignment {

    /**
     * Returns whether any resource is still assigned to the assignment, which keeps it from being closed.
     */
    boolean anyAssignedTo(Assignment assignment);

    /**
     * Releases every resource that still refers to the assignment, once it has been closed.
     */
    void releaseAllFrom(Assignment assignment);
}
