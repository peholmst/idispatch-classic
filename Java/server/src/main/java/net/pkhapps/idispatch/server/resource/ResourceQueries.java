package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.assignment.AssignmentId;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Looks up resources for the features that refer to them by ID, such as the destinations, the dispatch notifications
 * and the reports. Read-only and not secured, as it also serves dispatching in the background; the views and services
 * that use it decide who may see what.
 */
public interface ResourceQueries {

    /**
     * Finds the resources that can be assigned and dispatched, which are the ones not deleted, in call sign order.
     */
    List<ResourceListItem> findActive();

    /**
     * Finds the resources with the given IDs, whether deleted or not, in call sign order.
     */
    List<ResourceListItem> findByIds(Collection<ResourceId> ids);

    /**
     * Finds the resources whose call sign contains the search term, ignoring case, whether deleted or not, in call
     * sign order.
     */
    List<ResourceListItem> findByCallSignContaining(String searchTerm);

    /**
     * Finds the past states of every resource that has been on the assignment, oldest first.
     */
    List<ArchivedResourceStatus> findPastStatesOn(AssignmentId assignment);

    /**
     * Returns the call signs of the resources with the given IDs, whether deleted or not. An ID that matches no
     * resource is left out.
     */
    default Map<ResourceId, String> findCallSigns(Collection<ResourceId> ids) {
        var callSigns = new HashMap<ResourceId, String>();
        findByIds(ids).forEach(item -> callSigns.put(item.id(), item.callSign()));
        return callSigns;
    }
}
