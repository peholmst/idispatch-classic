package net.pkhapps.idispatch.server.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRolesMapperTest {

    private final KeycloakRolesMapper mapper = new KeycloakRolesMapper();

    @Test
    void rolesClaim_isMappedToApplicationRoles() {
        var authorities = mapper.mapAuthorities(List.of(oidcAuthority(Map.of("roles", List.of("DISPATCHER", "ADMIN")))));

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .contains(Roles.ROLE_DISPATCHER, Roles.ROLE_ADMIN)
                .doesNotContain(Roles.ROLE_REPORT_READER);
    }

    @Test
    void reportWriterRole_isMapped() {
        var authorities = mapper.mapAuthorities(List.of(oidcAuthority(Map.of("roles", List.of("REPORT_WRITER")))));

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .contains(Roles.ROLE_REPORT_WRITER)
                .doesNotContain(Roles.ROLE_REPORT_READER);
    }

    @Test
    void reportReviewerRole_isMapped() {
        var authorities = mapper.mapAuthorities(List.of(oidcAuthority(Map.of("roles", List.of("REPORT_REVIEWER")))));

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .contains(Roles.ROLE_REPORT_REVIEWER)
                .doesNotContain(Roles.ROLE_REPORT_READER, Roles.ROLE_REPORT_WRITER);
    }

    @Test
    void unknownRoles_areIgnored() {
        var authorities = mapper.mapAuthorities(List.of(oidcAuthority(Map.of("roles", List.of("REPORT_READER", "uma_authorization")))));

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .contains(Roles.ROLE_REPORT_READER)
                .doesNotContain("ROLE_uma_authorization");
    }

    @Test
    void missingRolesClaim_grantsNoApplicationRoles() {
        var authorities = mapper.mapAuthorities(List.of(oidcAuthority(Map.of())));

        assertThat(authorities).extracting(GrantedAuthority::getAuthority).doesNotContainAnyElementsOf(
                List.of(Roles.ROLE_ADMIN, Roles.ROLE_DISPATCHER, Roles.ROLE_REPORT_READER));
    }

    @Test
    void existingAuthorities_areKept() {
        var scope = new SimpleGrantedAuthority("SCOPE_openid");
        var authorities = mapper.mapAuthorities(List.of(scope, oidcAuthority(Map.of("roles", List.of("ADMIN")))));

        assertThat(authorities).extracting(GrantedAuthority::getAuthority).contains("SCOPE_openid");
    }

    private static OidcUserAuthority oidcAuthority(Map<String, Object> claims) {
        var idToken = OidcIdToken.withTokenValue("id-token")
                .subject("f2b4c1d0")
                .claims(c -> c.putAll(claims))
                .build();
        return new OidcUserAuthority(idToken);
    }
}
