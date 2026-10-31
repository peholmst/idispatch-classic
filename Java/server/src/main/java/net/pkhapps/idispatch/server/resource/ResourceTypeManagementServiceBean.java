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

@Service
@Secured(Roles.ROLE_ADMIN)
class ResourceTypeManagementServiceBean implements ResourceTypeManagementService {

    private static final Logger log = LoggerFactory.getLogger(ResourceTypeManagementServiceBean.class);

    private final ResourceTypeRepository repository;

    ResourceTypeManagementServiceBean(ResourceTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<ResourceTypeListItem> findAll(DefaultFilter filter) {
        var searchTerm = filter.searchTerm() == null ? "" : filter.searchTerm().trim();
        return repository.findListItems(filter.includesDeleted(), searchTerm);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<ResourceType> findById(ResourceTypeId id) {
        return repository.findById(id);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ResourceType create(ResourceTypeDetails details) {
        log.debug("Creating resource type {}", details.code());
        return repository.insert(details);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ResourceType update(ResourceType type) {
        log.debug("Updating resource type {} version {}", type.id().value(), type.version());
        return repository.update(type);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void delete(ResourceTypeId id) {
        log.debug("Softly deleting resource type {}", id.value());
        repository.setActive(id, false);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restore(ResourceTypeId id) {
        log.debug("Restoring resource type {}", id.value());
        repository.setActive(id, true);
    }
}
