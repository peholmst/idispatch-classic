package net.pkhapps.idispatch.server.boundary;

import net.pkhapps.idispatch.server.entity.AssignmentType;
import net.pkhapps.idispatch.server.entity.repository.AssignmentTypeRepository;
import org.springframework.stereotype.Service;

import javax.validation.Validator;

@Service
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
}
