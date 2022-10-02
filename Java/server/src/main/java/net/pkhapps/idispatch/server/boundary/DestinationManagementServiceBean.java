package net.pkhapps.idispatch.server.boundary;

import net.pkhapps.idispatch.server.entity.Destination;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.repository.DestinationRepository;
import net.pkhapps.idispatch.server.entity.repository.ResourceRepository;
import org.springframework.stereotype.Service;

import javax.validation.Validator;
import java.util.List;

@Service
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
    public List<Resource> findApplicableResources() {
        return resourceRepository.findByActiveTrueOrderByCallSignAsc();
    }
}
