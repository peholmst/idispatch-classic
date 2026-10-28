package net.pkhapps.idispatch.server;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * Replaces the Keycloak client registration with one that names its endpoints, so that the application context starts
 * without fetching the realm's OpenID configuration from a running Keycloak. Tests authenticate with
 * {@code @WithMockUser} and never log in through Keycloak.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestSecurityConfiguration {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        var realm = "http://keycloak.invalid/realms/idispatch";
        return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("keycloak")
                .clientId("idispatch")
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile")
                .issuerUri(realm)
                .authorizationUri(realm + "/protocol/openid-connect/auth")
                .tokenUri(realm + "/protocol/openid-connect/token")
                .jwkSetUri(realm + "/protocol/openid-connect/certs")
                .userInfoUri(realm + "/protocol/openid-connect/userinfo")
                .userNameAttributeName("preferred_username")
                .build());
    }
}
