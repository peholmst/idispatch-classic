package net.pkhapps.idispatch.server.events;

import net.pkhapps.idispatch.server.entity.Assignment;

/**
 * Event published when a new assignment is opened.
 */
public class AssignmentOpened extends AssignmentEvent {
    public AssignmentOpened(Object source, Assignment assignment) {
        super(source, assignment);
    }
}
