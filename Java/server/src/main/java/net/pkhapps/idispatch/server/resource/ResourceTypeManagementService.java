package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.shared.management.DefaultFilter;

import java.util.List;
import java.util.Optional;

/**
 * Lets administrators keep the resource types up to date. A code that is already in use is refused with a
 * {@link org.springframework.dao.DataIntegrityViolationException}, and a change based on an outdated version with an
 * {@link org.springframework.dao.OptimisticLockingFailureException}.
 */
public interface ResourceTypeManagementService {

    /**
     * Finds the resource types matching the filter, ordered by code.
     */
    List<ResourceTypeListItem> findAll(DefaultFilter filter);

    Optional<ResourceType> findById(ResourceTypeId id);

    ResourceType create(ResourceTypeDetails details);

    /**
     * Saves the changed details of the type.
     *
     * @return the type as saved, with its new version
     */
    ResourceType update(ResourceType type);

    void delete(ResourceTypeId id);

    void restore(ResourceTypeId id);
}
