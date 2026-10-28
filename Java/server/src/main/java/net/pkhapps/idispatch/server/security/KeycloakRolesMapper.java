package net.pkhapps.idispatch.server.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Grants the application roles that Keycloak puts in the {@value #ROLES_CLAIM} claim of the ID token. The claim is
 * added by a "User Client Role" mapper on the {@code idispatch} client, so it holds the client roles {@code ADMIN},
 * {@code DISPATCHER} and {@code REPORT_READER}, which become {@link Roles#ROLE_ADMIN} and so on. Roles that the
 * application does not know are ignored.
 */
class KeycloakRolesMapper implements GrantedAuthoritiesMapper {

    static final String ROLES_CLAIM = "roles";

    private static final Set<String> KNOWN_ROLES = Set.of(Roles.ROLE_ADMIN, Roles.ROLE_DISPATCHER,
            Roles.ROLE_REPORT_READER);

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        var mapped = new LinkedHashSet<GrantedAuthority>(authorities);
        authorities.stream()
                .filter(OidcUserAuthority.class::isInstance)
                .map(OidcUserAuthority.class::cast)
                .map(authority -> authority.getIdToken().getClaimAsStringList(ROLES_CLAIM))
                .filter(roles -> roles != null)
                .flatMap(Collection::stream)
                .map(role -> "ROLE_" + role)
                .filter(KNOWN_ROLES::contains)
                .map(SimpleGrantedAuthority::new)
                .forEach(mapped::add);
        return mapped;
    }
}
