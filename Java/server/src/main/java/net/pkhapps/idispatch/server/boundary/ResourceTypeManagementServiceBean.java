package net.pkhapps.idispatch.server.boundary;

import jakarta.validation.Validator;
import net.pkhapps.idispatch.server.entity.ResourceType;
import net.pkhapps.idispatch.server.entity.repository.ResourceTypeRepository;
import org.springframework.stereotype.Service;

@Service
class ResourceTypeManagementServiceBean extends AbstractSoftDeletableManagementServiceBean<ResourceType, ResourceTypeRepository> implements ResourceTypeManagementService {

    private final ResourceTypeRepository repository;

    ResourceTypeManagementServiceBean(Validator validator, ResourceTypeRepository repository) {
        super(validator);
        this.repository = repository;
    }

    @Override
    protected ResourceTypeRepository getRepository() {
        return repository;
    }
}
