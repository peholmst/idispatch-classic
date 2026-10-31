package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.shared.management.DefaultFilter;

import java.util.List;
import java.util.Optional;

/**
 * Lets administrators keep the resources up to date. A call sign that is already in use is refused with a
 * {@link org.springframework.dao.DataIntegrityViolationException}, and a change based on an outdated version with an
 * {@link org.springframework.dao.OptimisticLockingFailureException}.
 */
public interface ResourceManagementService {

    /**
     * Finds the resources matching the filter, ordered by call sign.
     */
    List<ResourceListItem> findAll(DefaultFilter filter);

    Optional<Resource> findById(ResourceId id);

    /**
     * Adds a resource, which starts out of service.
     */
    Resource create(ResourceDetails details);

    /**
     * Saves the changed details of the resource.
     *
     * @return the resource as saved, with its new version
     */
    Resource update(Resource resource);

    void delete(ResourceId id);

    void restore(ResourceId id);

    /**
     * Finds the types that a resource can be given, which are the ones not deleted, ordered by code.
     */
    List<ResourceTypeListItem> findApplicableTypes();

    /**
     * Finds the type with the given ID, whether deleted or not.
     */
    Optional<ResourceTypeListItem> findType(ResourceTypeId id);
}
