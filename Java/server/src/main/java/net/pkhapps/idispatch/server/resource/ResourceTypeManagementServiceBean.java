package net.pkhapps.idispatch.server.resource;

import jakarta.validation.Validator;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.shared.management.AbstractSoftDeletableManagementServiceBean;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Secured(Roles.ROLE_ADMIN)
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

    @Override
    protected List<ResourceType> doFindAll(Filter filter) {
        var specs = new ArrayList<Specification<ResourceType>>();
        if (filter instanceof SoftDeletableFilter softDeletableFilter && softDeletableFilter.includesActiveOnly()) {
            specs.add(ResourceTypeSpecifications.activeOnly());
        }
        if (filter instanceof TextSearchFilter textSearchFilter) {
            textSearchFilter.doWithSearchTerm(st -> specs.add(ResourceTypeSpecifications.searchTerm(st)));
        }
        return repository.findAll(Specification.allOf(specs));
    }
}
