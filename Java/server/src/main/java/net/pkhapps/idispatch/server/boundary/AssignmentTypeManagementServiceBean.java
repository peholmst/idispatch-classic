package net.pkhapps.idispatch.server.boundary;

import jakarta.validation.Validator;
import net.pkhapps.idispatch.server.entity.AssignmentType;
import net.pkhapps.idispatch.server.entity.repository.AssignmentTypeRepository;
import net.pkhapps.idispatch.server.entity.repository.AssignmentTypeSpecifications;
import net.pkhapps.idispatch.server.security.Roles;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Secured(Roles.ROLE_ADMIN)
class AssignmentTypeManagementServiceBean extends AbstractSoftDeletableManagementServiceBean<AssignmentType, AssignmentTypeRepository> implements AssignmentTypeManagementService {

    private final AssignmentTypeRepository repository;

    AssignmentTypeManagementServiceBean(Validator validator, AssignmentTypeRepository repository) {
        super(validator);
        this.repository = repository;
    }

    @Override
    protected AssignmentTypeRepository getRepository() {
        return repository;
    }

    @Override
    protected List<AssignmentType> doFindAll(Filter filter) {
        var specs = new ArrayList<Specification<AssignmentType>>();
        if (filter instanceof SoftDeletableFilter softDeletableFilter && softDeletableFilter.includesActiveOnly()) {
            specs.add(AssignmentTypeSpecifications.activeOnly());
        }
        if (filter instanceof TextSearchFilter textSearchFilter) {
            textSearchFilter.doWithSearchTerm(st -> specs.add(AssignmentTypeSpecifications.searchTerm(st)));
        }
        return repository.findAll(Specification.allOf(specs));
    }
}
