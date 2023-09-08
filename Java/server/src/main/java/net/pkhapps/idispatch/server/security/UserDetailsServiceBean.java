package net.pkhapps.idispatch.server.security;

import net.pkhapps.idispatch.server.entity.User;
import net.pkhapps.idispatch.server.entity.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
class UserDetailsServiceBean implements UserDetailsService {

    private final UserRepository userRepository;

    UserDetailsServiceBean(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository
                .findByUsername(username)
                .map(UserDetailsImpl::new)
                .orElseThrow(() -> new UsernameNotFoundException("No such user"));
    }

    private static final class UserDetailsImpl implements UserDetails {

        private final String username;
        private final String password;
        private final Set<GrantedAuthority> authorities;
        private final boolean enabled;

        public UserDetailsImpl(User user) {
            username = user.getUsername();
            password = user.getEncodedPassword();

            var authorities = new HashSet<GrantedAuthority>();
            if (user.isAdmin()) {
                authorities.add(new SimpleGrantedAuthority(Roles.ROLE_ADMIN));
            }
            if (user.isDispatcher()) {
                authorities.add(new SimpleGrantedAuthority(Roles.ROLE_DISPATCHER));
            }
            if (user.isReportReader()) {
                authorities.add(new SimpleGrantedAuthority(Roles.ROLE_REPORT_READER));
            }
            this.authorities = Collections.unmodifiableSet(authorities);

            enabled = user.isEnabled();
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }
    }
}
