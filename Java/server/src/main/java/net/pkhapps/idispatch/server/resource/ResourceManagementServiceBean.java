package net.pkhapps.idispatch.server.resource;

import jakarta.validation.Validator;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.shared.ValidationFailedException;
import net.pkhapps.idispatch.server.shared.management.AbstractSoftDeletableManagementServiceBean;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Secured(Roles.ROLE_ADMIN)
class ResourceManagementServiceBean extends AbstractSoftDeletableManagementServiceBean<Resource, ResourceRepository> implements ResourceManagementService {

    private final ResourceRepository repository;
    private final ResourceStatusRepository resourceStatusRepository;
    private final ResourceTypeRepository resourceTypeRepository;

    ResourceManagementServiceBean(Validator validator, ResourceRepository repository, ResourceStatusRepository resourceStatusRepository, ResourceTypeRepository resourceTypeRepository) {
        super(validator);
        this.repository = repository;
        this.resourceStatusRepository = resourceStatusRepository;
        this.resourceTypeRepository = resourceTypeRepository;
    }

    @Override
    protected ResourceRepository getRepository() {
        return repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Resource save(Resource entity) throws ValidationFailedException {
        if (entity.isNew()) {
            final Resource saved = super.save(entity);
            logger.debug("Creating new status entry for resource {}", saved);
            final ResourceStatus resourceStatus = new ResourceStatus(saved.getResourceId(), ResourceState.OUT_OF_SERVICE);
            resourceStatusRepository.saveAndFlush(resourceStatus);
            return saved;
        } else {
            return super.save(entity);
        }
    }

    @Override
    protected List<Resource> doFindAll(Filter filter) {
        var specs = new ArrayList<Specification<Resource>>();
        if (filter instanceof SoftDeletableFilter softDeletableFilter && softDeletableFilter.includesActiveOnly()) {
            specs.add(ResourceSpecifications.activeOnly());
        }
        if (filter instanceof TextSearchFilter textSearchFilter) {
            textSearchFilter.doWithSearchTerm(st -> specs.add(ResourceSpecifications.searchTerm(st)));
        }
        return getRepository().findAll(Specification.allOf(specs));
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
