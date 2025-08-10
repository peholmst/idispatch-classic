package net.pkhapps.idispatch.server.boundary;

import jakarta.validation.Validator;
import net.pkhapps.idispatch.server.entity.User;
import net.pkhapps.idispatch.server.entity.repository.UserRepository;
import net.pkhapps.idispatch.server.security.Roles;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Secured(Roles.ROLE_ADMIN)
class UserManagementServiceBean extends AbstractManagementServiceBean<User, UserRepository> implements UserManagementService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    UserManagementServiceBean(Validator validator, UserRepository repository, PasswordEncoder passwordEncoder) {
        super(validator);
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected UserRepository getRepository() {
        return repository;
    }

    @Override
    protected void doBeforeSave(User entity) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AccessDeniedException("No authenticated user");
        }
        if (authentication.getName().equals(entity.getUsername())) {
            throw new AccessDeniedException("You cannot make changes to your own account");
        }
        if (entity.getEncodedPassword() == null && entity.getUsername() != null) {
            entity.setEncodedPassword(passwordEncoder.encode(entity.getUsername()));
        }
    }

    @Override
    protected List<User> doFindAll(Filter filter) {
        return repository.findAll(); // TODO Implement filter support
    }
}
