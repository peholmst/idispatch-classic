package net.pkhapps.idispatch.server;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

@EnableWebSecurity
@Configuration
public class SecurityConfig extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        http.formLogin();
    }

    @Override
    protected void configure(WebSecurity web) throws Exception {
        super.configure(web);
        web.ignoring().antMatchers("/rest/**");
    }

    @Bean
    public UserDetailsManager userDetailsService(@Value("${security.users}") URL usersFile) throws IOException {
        var users = new Properties();
        try (var is = usersFile.openStream()) {
            LoggerFactory.getLogger(getClass()).info("Loading users from {}", usersFile);
            users.load(is);
        }
        return new InMemoryUserDetailsManager(users);
    }
}
