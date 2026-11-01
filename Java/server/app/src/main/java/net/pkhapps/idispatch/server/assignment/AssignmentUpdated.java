package net.pkhapps.idispatch.server.assignment;


/**
 * Event published when an open assignment is updated (but not closed).
 */
class AssignmentUpdated extends AssignmentEvent {
    public AssignmentUpdated(Object source, Assignment assignment) {
        super(source, assignment);
    }
}
