package net.pkhapps.idispatch.server.dispatcher;

import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.assignment.AssignmentListItem;
import net.pkhapps.idispatch.server.assignment.AssignmentService;
import net.pkhapps.idispatch.server.resource.ResourceQueries;
import net.pkhapps.idispatch.server.resource.ResourceStatus;
import net.pkhapps.idispatch.server.resource.ResourceStatusService;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Loads every resource as a {@link ResourceRow}, for the views that list all resources.
 */
final class ResourceRows {

    private ResourceRows() {
    }

    /**
     * Loads the status of every active resource, with its call sign and the assignment its status refers to, in the
     * order the resource service returns them.
     */
    static List<ResourceRow> load(ResourceStatusService resourceStatusService, ResourceQueries resourceQueries,
                                  AssignmentService assignmentService) {
        var statuses = resourceStatusService.getStatusOfAllResources();
        var callSigns = resourceQueries.findCallSigns(statuses.stream().map(ResourceStatus::resourceId).toList());
        var assignments = new HashMap<AssignmentId, AssignmentListItem>();
        assignmentService.findOpenAssignments().forEach(item -> assignments.put(item.id(), item));
        // Resources can still refer to an assignment that has been closed
        var others = statuses.stream().map(ResourceStatus::assignment).filter(Objects::nonNull)
                .filter(id -> !assignments.containsKey(id)).collect(Collectors.toSet());
        assignmentService.findAssignments(others).forEach(item -> assignments.put(item.id(), item));
        return statuses.stream()
                .map(status -> new ResourceRow(status, callSigns.get(status.resourceId()),
                        status.assignment() == null ? null : assignments.get(status.assignment())))
                .toList();
    }
}
