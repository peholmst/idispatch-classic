package net.pkhapps.idispatch.server.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.web.SecurityFilterChain;

import static com.vaadin.flow.spring.security.VaadinSecurityConfigurer.vaadin;

/**
 * Users log in through Keycloak (OpenID Connect). The application keeps no users of its own: the registration
 * {@value #REGISTRATION_ID} in {@code spring.security.oauth2.client} points at the Keycloak realm, and the user's
 * roles come from the ID token (see {@link KeycloakRolesMapper}).
 */
@EnableWebSecurity
@EnableMethodSecurity
@Configuration
class SecurityConfig {

    static final String REGISTRATION_ID = "keycloak";

    @Bean
    SecurityFilterChain vaadinSecurityFilterChain(HttpSecurity http) throws Exception {
        // Vaadin permits only the static resources it knows of, such as styles.css, and refuses the rest even to a
        // logged-in user, so the stylesheets that styles.css imports must be permitted here
        http.authorizeHttpRequests(r -> r.requestMatchers(
                        "/ws/**",
                        "/actuator/**",
                        "/styles/**"
                )
                .permitAll());
        // Logging out also ends the Keycloak session, and then returns to the application, which sends the user
        // back to the Keycloak login page.
        http.with(vaadin(), vaadin -> vaadin.oauth2LoginPage("/oauth2/authorization/" + REGISTRATION_ID, "{baseUrl}"));
        return http.build();
    }

    @Bean
    GrantedAuthoritiesMapper keycloakRolesMapper() {
        return new KeycloakRolesMapper();
    }
}
