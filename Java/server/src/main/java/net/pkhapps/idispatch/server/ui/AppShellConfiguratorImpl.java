package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Theme(variant = Lumo.DARK)
@Configuration
public class AppShellConfiguratorImpl implements AppShellConfigurator {

    @Bean
    CustomErrorHandler customErrorHandler() {
        return new CustomErrorHandler();
    }

    @Bean
    VaadinServiceInitListener vaadinServiceInitListener() {
        return serviceInitEvent -> serviceInitEvent.getSource()
                .addSessionInitListener(sessionInitEvent -> sessionInitEvent.getSession()
                        .setErrorHandler(customErrorHandler()));
    }
}
