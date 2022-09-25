package net.pkhapps.idispatch.server.boundary;

import net.pkhapps.idispatch.server.entity.Municipality;
import net.pkhapps.idispatch.server.entity.repository.MunicipalityRepository;
import org.springframework.stereotype.Service;

import javax.validation.Validator;

@Service
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
}
