package net.pkhapps.idispatch.server.events;

import net.pkhapps.idispatch.server.entity.Assignment;

/**
 * Event published when an open assignment is updated (but not closed).
 */
public class AssignmentUpdated extends AssignmentEvent {
    public AssignmentUpdated(Object source, Assignment assignment) {
        super(source, assignment);
    }
}
