package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.assignment.Assignment;
import net.pkhapps.idispatch.server.assignment.ResourcesOnAssignment;
import org.springframework.stereotype.Component;

/**
 * Answers for the resources on an assignment when it is closed. Goes through the secured {@link ResourceStatusService},
 * so the caller needs the same role as for calling the service directly.
 */
@Component
class ResourcesOnAssignmentAdapter implements ResourcesOnAssignment {

    private final ResourceStatusService resourceStatusService;

    ResourcesOnAssignmentAdapter(ResourceStatusService resourceStatusService) {
        this.resourceStatusService = resourceStatusService;
    }

    @Override
    public boolean anyAssignedTo(Assignment assignment) {
        return !resourceStatusService.getStatusOfResourcesAssignedToAssignment(assignment).isEmpty();
    }

    @Override
    public void releaseAllFrom(Assignment assignment) {
        resourceStatusService.clearAssignmentFromAllResources(assignment);
    }
}
