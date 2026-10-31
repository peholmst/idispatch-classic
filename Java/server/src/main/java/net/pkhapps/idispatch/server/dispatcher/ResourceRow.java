package net.pkhapps.idispatch.server.dispatcher;

import net.pkhapps.idispatch.server.assignment.AssignmentListItem;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceStatus;
import org.jspecify.annotations.Nullable;

/**
 * A resource as the dispatcher view lists it: its status, with its call sign and the assignment it is on loaded
 * together.
 *
 * @param assignment the assignment the resource is on, or {@code null} when it is on none or the list does not show it
 */
record ResourceRow(ResourceStatus status, @Nullable String callSign, @Nullable AssignmentListItem assignment) {

    ResourceId resourceId() {
        return status.resourceId();
    }
}
