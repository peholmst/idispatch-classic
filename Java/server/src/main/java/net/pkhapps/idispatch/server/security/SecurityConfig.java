package net.pkhapps.idispatch.server.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import net.pkhapps.idispatch.server.ui.LoginView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@EnableWebSecurity
@Configuration
class SecurityConfig extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(r -> r.requestMatchers(
                        "/rest/**",
                        "/actuator/**"
                )
                .permitAll());
        super.configure(http);
        setLoginView(http, LoginView.class);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
