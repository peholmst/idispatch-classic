package net.pkhapps.idispatch.server.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.stream.Stream;

/**
 * Runs a service test as a user logged in through Keycloak, whose ID token has the subject {@code subject-<username>}
 * and the given first and last name, like the users that {@code UseCaseTest.keycloakUser} logs in.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@WithSecurityContext(factory = WithKeycloakUser.Factory.class)
public @interface WithKeycloakUser {

    String username();

    String firstName();

    String lastName();

    /**
     * The roles without the {@code ROLE_} prefix, such as {@code ADMIN}.
     */
    String[] roles();

    class Factory implements WithSecurityContextFactory<WithKeycloakUser> {

        @Override
        public SecurityContext createSecurityContext(WithKeycloakUser user) {
            var idToken = OidcIdToken.withTokenValue("id-token")
                    .issuer("http://keycloak.invalid/realms/idispatch")
                    .subject("subject-" + user.username())
                    .claim("preferred_username", user.username())
                    .claim("given_name", user.firstName())
                    .claim("family_name", user.lastName())
                    .build();
            var authorities = Stream.of(user.roles()).map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
            var oidcUser = new DefaultOidcUser(authorities, idToken, "preferred_username");
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new OAuth2AuthenticationToken(oidcUser, authorities, "keycloak"));
            return context;
        }
    }
}
