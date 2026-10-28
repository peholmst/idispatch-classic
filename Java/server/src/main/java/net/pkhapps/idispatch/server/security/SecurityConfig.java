package net.pkhapps.idispatch.server.security;

import net.pkhapps.idispatch.server.ui.LoginView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static com.vaadin.flow.spring.security.VaadinSecurityConfigurer.vaadin;

@EnableWebSecurity
@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain vaadinSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(r -> r.requestMatchers(
                        "/rest/**",
                        "/actuator/**"
                )
                .permitAll());
        http.with(vaadin(), vaadin -> vaadin.loginView(LoginView.class));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
