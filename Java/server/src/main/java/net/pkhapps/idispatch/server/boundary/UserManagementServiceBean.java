package net.pkhapps.idispatch.server.boundary;

import jakarta.validation.Validator;
import net.pkhapps.idispatch.server.entity.User;
import net.pkhapps.idispatch.server.entity.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
class UserManagementServiceBean extends AbstractManagementServiceBean<User, UserRepository> implements UserManagementService {

    private final UserRepository repository;

    UserManagementServiceBean(Validator validator, UserRepository repository) {
        super(validator);
        this.repository = repository;
    }

    @Override
    protected UserRepository getRepository() {
        return repository;
    }
}
