package net.pkhapps.idispatch.server.events;

import net.pkhapps.idispatch.server.entity.Assignment;

/**
 * Event published when an open assignment is closed.
 */
public class AssignmentClosed extends AssignmentEvent {

    public AssignmentClosed(Object source, Assignment assignment) {
        super(source, assignment);
    }
}
