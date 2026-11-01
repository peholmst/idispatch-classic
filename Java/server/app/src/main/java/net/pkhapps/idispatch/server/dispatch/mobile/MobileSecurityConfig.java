package net.pkhapps.idispatch.server.dispatch.mobile;

import jakarta.servlet.DispatcherType;
import net.pkhapps.idispatch.server.dispatch.DestinationQueries;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutFilter;

/**
 * The mobile app and its API are outside the Keycloak login. Anyone may load the app, under {@value #APP_PATH}, which
 * holds nothing secret. The API, under {@value #API_PATH}, takes only requests with the key of a mobile destination
 * (see {@link MobileKeyAuthenticationFilter}), and answers any other with 401 rather than a redirect to the login page.
 * The app sends the key with every request, so the API keeps no session, and needs no CSRF protection, as browsers never
 * send the key on their own.
 */
@Configuration
class MobileSecurityConfig {

    static final String APP_PATH = "/mobile";
    static final String API_PATH = "/api/mobile";

    @Bean
    @Order(1)
    SecurityFilterChain mobileSecurityFilterChain(HttpSecurity http, DestinationQueries destinationQueries)
            throws Exception {
        http.securityMatcher(APP_PATH, APP_PATH + "/**", API_PATH + "/**")
                .authorizeHttpRequests(requests -> requests
                        // The event stream ends in an async dispatch, which carries no key of its own
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(API_PATH + "/**").authenticated()
                        .anyRequest().permitAll())
                // Not a bean, which Spring Boot would also register as a filter for every request
                .addFilterBefore(new MobileKeyAuthenticationFilter(destinationQueries), LogoutFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .csrf(csrf -> csrf.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }
}
