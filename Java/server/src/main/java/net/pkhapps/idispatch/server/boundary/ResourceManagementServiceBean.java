package net.pkhapps.idispatch.server.boundary;

import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.ResourceState;
import net.pkhapps.idispatch.server.entity.ResourceStatus;
import net.pkhapps.idispatch.server.entity.ValidationFailedException;
import net.pkhapps.idispatch.server.entity.repository.ResourceRepository;
import net.pkhapps.idispatch.server.entity.repository.ResourceStatusRepository;
import org.springframework.stereotype.Service;

import javax.validation.Validator;

@Service
class ResourceManagementServiceBean extends AbstractSoftDeletableManagementServiceBean<Resource, ResourceRepository> implements ResourceManagementService {

    private final ResourceRepository repository;
    private final ResourceStatusRepository resourceStatusRepository;

    ResourceManagementServiceBean(Validator validator, ResourceRepository repository, ResourceStatusRepository resourceStatusRepository) {
        super(validator);
        this.repository = repository;
        this.resourceStatusRepository = resourceStatusRepository;
    }

    @Override
    protected ResourceRepository getRepository() {
        return repository;
    }

    @Override
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
}
