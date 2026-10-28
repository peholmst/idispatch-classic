package net.pkhapps.idispatch.server.boundary;

import jakarta.validation.Validator;
import net.pkhapps.idispatch.server.entity.Destination;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.repository.DestinationRepository;
import net.pkhapps.idispatch.server.entity.repository.DestinationSpecifications;
import net.pkhapps.idispatch.server.entity.repository.ResourceRepository;
import net.pkhapps.idispatch.server.security.Roles;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Secured(Roles.ROLE_ADMIN)
class DestinationManagementServiceBean extends AbstractSoftDeletableManagementServiceBean<Destination, DestinationRepository> implements DestinationManagementService {

    private final DestinationRepository repository;
    private final ResourceRepository resourceRepository;

    DestinationManagementServiceBean(Validator validator, DestinationRepository repository,
                                     ResourceRepository resourceRepository) {
        super(validator);
        this.repository = repository;
        this.resourceRepository = resourceRepository;
    }

    @Override
    protected DestinationRepository getRepository() {
        return repository;
    }

    @Override
    protected List<Destination> doFindAll(Filter filter) {
        var specs = new ArrayList<Specification<Destination>>();
        if (filter instanceof SoftDeletableFilter softDeletableFilter && softDeletableFilter.includesActiveOnly()) {
            specs.add(DestinationSpecifications.activeOnly());
        }
        if (filter instanceof TextSearchFilter textSearchFilter) {
            textSearchFilter.doWithSearchTerm(st -> specs.add(DestinationSpecifications.searchTerm(st)));
        }
        return repository.findAll(Specification.allOf(specs));
    }

    @Override
    public List<Resource> findApplicableResources() {
        return resourceRepository.findByActiveTrueOrderByCallSignAsc();
    }
}
