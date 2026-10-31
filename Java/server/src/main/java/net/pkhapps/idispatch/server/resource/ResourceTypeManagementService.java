package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.shared.management.DefaultFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Lets administrators keep the resource types up to date. A code that is already in use is refused with a
 * {@link org.springframework.dao.DataIntegrityViolationException}, and a change based on an outdated version with an
 * {@link org.springframework.dao.OptimisticLockingFailureException}.
 */
@Service
@Secured(Roles.ROLE_ADMIN)
public class ResourceTypeManagementService {

    private static final Logger log = LoggerFactory.getLogger(ResourceTypeManagementService.class);

    private final ResourceTypeRepository repository;

    ResourceTypeManagementService(ResourceTypeRepository repository) {
        this.repository = repository;
    }

    /**
     * Finds the resource types matching the filter, ordered by code.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<ResourceTypeListItem> findAll(DefaultFilter filter) {
        var searchTerm = filter.searchTerm() == null ? "" : filter.searchTerm().trim();
        return repository.findListItems(filter.includesDeleted(), searchTerm);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<ResourceType> findById(ResourceTypeId id) {
        return repository.findById(id);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ResourceType create(ResourceTypeDetails details) {
        log.debug("Creating resource type {}", details.code());
        return repository.insert(details);
    }

    /**
     * Saves the changed details of the type.
     *
     * @return the type as saved, with its new version
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ResourceType update(ResourceType type) {
        log.debug("Updating resource type {} version {}", type.id().value(), type.version());
        return repository.update(type);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void delete(ResourceTypeId id) {
        log.debug("Softly deleting resource type {}", id.value());
        repository.setActive(id, false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restore(ResourceTypeId id) {
        log.debug("Restoring resource type {}", id.value());
        repository.setActive(id, true);
    }
}
