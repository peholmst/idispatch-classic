package net.pkhapps.idispatch.server.dispatch;

import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceListItem;
import net.pkhapps.idispatch.server.shared.management.ManagementService;

import java.util.Collection;
import java.util.List;

public interface DestinationManagementService extends ManagementService.SoftDeletable<Destination> {

    /**
     * Finds the resources that can be chosen for a destination, which are the ones not deleted, in call sign order.
     */
    List<ResourceListItem> findApplicableResources();

    /**
     * Finds the resources with the given IDs, whether deleted or not, in call sign order.
     */
    List<ResourceListItem> findResources(Collection<ResourceId> ids);
}
