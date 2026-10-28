package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@StyleSheet(Lumo.STYLESHEET)
@ColorScheme(ColorScheme.Value.DARK)
@Push
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
