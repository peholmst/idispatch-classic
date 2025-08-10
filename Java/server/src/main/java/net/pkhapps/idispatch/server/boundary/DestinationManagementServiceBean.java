package net.pkhapps.idispatch.server.boundary;

import jakarta.validation.Validator;
import net.pkhapps.idispatch.server.entity.Destination;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.repository.DestinationRepository;
import net.pkhapps.idispatch.server.entity.repository.ResourceRepository;
import net.pkhapps.idispatch.server.security.Roles;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

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
        return repository.findAll(); // TODO Implement filter support
    }

    @Override
    public List<Resource> findApplicableResources() {
        return resourceRepository.findByActiveTrueOrderByCallSignAsc();
    }
}
