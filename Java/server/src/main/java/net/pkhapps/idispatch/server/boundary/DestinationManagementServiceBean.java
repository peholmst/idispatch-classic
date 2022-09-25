package net.pkhapps.idispatch.server.boundary;

import net.pkhapps.idispatch.server.entity.Destination;
import net.pkhapps.idispatch.server.entity.repository.DestinationRepository;
import org.springframework.stereotype.Service;

import javax.validation.Validator;

@Service
class DestinationManagementServiceBean extends AbstractSoftDeletableManagementServiceBean<Destination, DestinationRepository> implements DestinationManagementService {

    private final DestinationRepository repository;

    DestinationManagementServiceBean(Validator validator, DestinationRepository repository) {
        super(validator);
        this.repository = repository;
    }

    @Override
    protected DestinationRepository getRepository() {
        return repository;
    }
}
