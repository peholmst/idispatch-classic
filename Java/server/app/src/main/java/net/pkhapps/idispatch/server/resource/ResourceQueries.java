package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.assignment.AssignmentId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Looks up resources for the features that refer to them by ID, such as the destinations, the dispatch notifications
 * and the reports. Read-only and not secured, as it also serves dispatching in the background; the views and services
 * that use it decide who may see what.
 */
@Service
@Transactional(readOnly = true)
public class ResourceQueries {

    private final ResourceRepository resourceRepository;
    private final ResourceStatusRepository resourceStatusRepository;

    ResourceQueries(ResourceRepository resourceRepository, ResourceStatusRepository resourceStatusRepository) {
        this.resourceRepository = resourceRepository;
        this.resourceStatusRepository = resourceStatusRepository;
    }

    /**
     * Finds the resources that can be assigned and dispatched, which are the ones not deleted, in call sign order.
     */
    public List<ResourceListItem> findActive() {
        return resourceRepository.findListItems(false, null);
    }

    /**
     * Finds the resources with the given IDs, whether deleted or not, in call sign order.
     */
    public List<ResourceListItem> findByIds(Collection<ResourceId> ids) {
        return ids.isEmpty() ? List.of() : resourceRepository.findListItems(ids);
    }

    /**
     * Finds the resources whose call sign contains the search term, ignoring case, whether deleted or not, in call
     * sign order.
     */
    public List<ResourceListItem> findByCallSignContaining(String searchTerm) {
        return resourceRepository.findListItems(true, searchTerm);
    }

    /**
     * Finds the current status of the resource, or empty if it is deleted or does not exist.
     */
    public Optional<ResourceStatus> findCurrentStatus(ResourceId resource) {
        return resourceStatusRepository.findOfActiveResource(resource);
    }

    /**
     * Finds the past states of every resource that has been on the assignment, oldest first.
     */
    public List<ArchivedResourceStatus> findPastStatesOn(AssignmentId assignment) {
        return resourceStatusRepository.findArchivedOn(assignment);
    }

    /**
     * Returns the call signs of the resources with the given IDs, whether deleted or not. An ID that matches no
     * resource is left out.
     */
    public Map<ResourceId, String> findCallSigns(Collection<ResourceId> ids) {
        var callSigns = new HashMap<ResourceId, String>();
        findByIds(ids).forEach(item -> callSigns.put(item.id(), item.callSign()));
        return callSigns;
    }
}
