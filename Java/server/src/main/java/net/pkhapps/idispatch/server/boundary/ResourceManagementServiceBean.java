package net.pkhapps.idispatch.server.boundary;

import jakarta.validation.Validator;
import net.pkhapps.idispatch.server.entity.*;
import net.pkhapps.idispatch.server.entity.repository.ResourceRepository;
import net.pkhapps.idispatch.server.entity.repository.ResourceStatusRepository;
import net.pkhapps.idispatch.server.entity.repository.ResourceTypeRepository;
import net.pkhapps.idispatch.server.security.Roles;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
            final ResourceStatus resourceStatus = new ResourceStatus(saved, ResourceState.OUT_OF_SERVICE);
            resourceStatusRepository.saveAndFlush(resourceStatus);
            return saved;
        } else {
            return super.save(entity);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<ResourceType> findApplicableTypes() {
        return resourceTypeRepository.findByActiveTrueOrderByCodeAsc();
    }
}
