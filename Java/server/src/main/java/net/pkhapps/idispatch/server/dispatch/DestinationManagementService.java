package net.pkhapps.idispatch.server.dispatch;

import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceListItem;
import net.pkhapps.idispatch.server.shared.management.DefaultFilter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Lets administrators decide where the dispatch notifications of each resource go. A runboard key that is already in
 * use is refused with a {@link org.springframework.dao.DataIntegrityViolationException}, and a change based on an
 * outdated version with an {@link org.springframework.dao.OptimisticLockingFailureException}.
 */
public interface DestinationManagementService {

    /**
     * Finds the destinations matching the filter, whose description or the call sign of any of whose resources
     * contains the search term, in the order they were added.
     */
    List<DestinationListItem> findAll(DefaultFilter filter);

    Optional<Destination> findById(DestinationId id);

    Destination create(DestinationDetails details);

    /**
     * Saves the changed details of the destination.
     *
     * @return the destination as saved, with its new version
     */
    Destination update(Destination destination);

    void delete(DestinationId id);

    void restore(DestinationId id);

    /**
     * Finds the resources that can be chosen for a destination, which are the ones not deleted, in call sign order.
     */
    List<ResourceListItem> findApplicableResources();

    /**
     * Finds the resources with the given IDs, whether deleted or not, in call sign order.
     */
    List<ResourceListItem> findResources(Collection<ResourceId> ids);
}
