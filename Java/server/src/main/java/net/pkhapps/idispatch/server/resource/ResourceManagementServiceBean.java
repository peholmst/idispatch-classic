package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.shared.management.DefaultFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Secured(Roles.ROLE_ADMIN)
class ResourceManagementServiceBean implements ResourceManagementService {

    private static final Logger log = LoggerFactory.getLogger(ResourceManagementServiceBean.class);

    private final ResourceRepository repository;
    private final ResourceStatusRepository resourceStatusRepository;
    private final ResourceTypeRepository resourceTypeRepository;

    ResourceManagementServiceBean(ResourceRepository repository, ResourceStatusRepository resourceStatusRepository,
                                  ResourceTypeRepository resourceTypeRepository) {
        this.repository = repository;
        this.resourceStatusRepository = resourceStatusRepository;
        this.resourceTypeRepository = resourceTypeRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<ResourceListItem> findAll(DefaultFilter filter) {
        var searchTerm = filter.searchTerm() == null ? "" : filter.searchTerm().trim();
        return repository.findListItems(filter.includesDeleted(), searchTerm);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<Resource> findById(ResourceId id) {
        return repository.findById(id);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Resource create(ResourceDetails details) {
        log.debug("Creating resource {}", details.callSign());
        var resource = repository.insert(details);
        resourceStatusRepository.insert(
                new ResourceStatus(resource.id(), 0L, ResourceState.OUT_OF_SERVICE, null, Instant.now()));
        return resource;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Resource update(Resource resource) {
        log.debug("Updating resource {} version {}", resource.id().value(), resource.version());
        return repository.update(resource);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void delete(ResourceId id) {
        log.debug("Softly deleting resource {}", id.value());
        repository.setActive(id, false);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restore(ResourceId id) {
        log.debug("Restoring resource {}", id.value());
        repository.setActive(id, true);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<ResourceTypeListItem> findApplicableTypes() {
        return resourceTypeRepository.findListItems(false, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<ResourceTypeListItem> findType(ResourceTypeId id) {
        return resourceTypeRepository.findListItem(id);
    }
}
