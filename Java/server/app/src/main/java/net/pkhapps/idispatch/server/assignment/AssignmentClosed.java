package net.pkhapps.idispatch.server.assignment;


/**
 * Event published when an open assignment is closed.
 */
class AssignmentClosed extends AssignmentEvent {

    public AssignmentClosed(Object source, Assignment assignment) {
        super(source, assignment);
    }
}
