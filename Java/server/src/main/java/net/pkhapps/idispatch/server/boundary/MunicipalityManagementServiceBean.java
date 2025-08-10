package net.pkhapps.idispatch.server.boundary;

import jakarta.validation.Validator;
import net.pkhapps.idispatch.server.entity.Municipality;
import net.pkhapps.idispatch.server.entity.repository.MunicipalityRepository;
import net.pkhapps.idispatch.server.entity.repository.MunicipalitySpecifications;
import net.pkhapps.idispatch.server.security.Roles;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Secured(Roles.ROLE_ADMIN)
class MunicipalityManagementServiceBean extends AbstractSoftDeletableManagementServiceBean<Municipality, MunicipalityRepository> implements MunicipalityManagementService {

    private final MunicipalityRepository repository;

    MunicipalityManagementServiceBean(Validator validator, MunicipalityRepository repository) {
        super(validator);
        this.repository = repository;
    }

    @Override
    protected MunicipalityRepository getRepository() {
        return repository;
    }

    @Override
    protected List<Municipality> doFindAll(Filter filter) {
        var specs = new ArrayList<Specification<Municipality>>();
        if (filter instanceof SoftDeletableFilter softDeletableFilter && softDeletableFilter.includesActiveOnly()) {
            specs.add(MunicipalitySpecifications.activeOnly());
        }
        if (filter instanceof TextSearchFilter textSearchFilter) {
            textSearchFilter.doWithSearchTerm(st -> specs.add(MunicipalitySpecifications.searchTerm(st)));
        }
        return repository.findAll(Specification.allOf(specs));
    }
}
