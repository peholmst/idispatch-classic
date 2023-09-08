package net.pkhapps.idispatch.server.security;

import jakarta.annotation.PostConstruct;
import net.pkhapps.idispatch.server.entity.User;
import net.pkhapps.idispatch.server.entity.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class InitialUserGenerator {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    InitialUserGenerator(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    void init() {
        if (!userRepository.existsByUsername("admin")) {
            var user = new User();
            user.setUsername("admin");
            user.setEncodedPassword(passwordEncoder.encode("changeme"));
            user.setAdmin(true);
            user.setDispatcher(true);
            user.setReportReader(true);
            user.setEnabled(true);
            userRepository.saveAndFlush(user);
        }
    }
}
