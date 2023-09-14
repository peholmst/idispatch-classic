package net.pkhapps.idispatch.server.boundary;


import net.pkhapps.idispatch.server.entity.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class ChangePasswordServiceBean implements ChangePasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    ChangePasswordServiceBean(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void changePassword(String currentPassword, String newPassword) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AccessDeniedException("No authenticated user");
        }
        var user = userRepository.findByUsername(authentication.getName()).orElseThrow(() -> new AccessDeniedException("User does not exist"));
        if (!passwordEncoder.matches(currentPassword, user.getEncodedPassword())) {
            throw new AccessDeniedException("Current password is incorrect");
        }
        user.setEncodedPassword(passwordEncoder.encode(newPassword));
        userRepository.saveAndFlush(user);
    }
}
