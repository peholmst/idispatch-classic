package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.i18n.DefaultI18NProvider;
import com.vaadin.flow.i18n.I18NProvider;
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

    /**
     * Translations are in {@code vaadin-i18n/translations*.properties}. Listing the languages here, instead of letting
     * Vaadin find them from the file names, keeps English the default.
     */
    @Bean
    I18NProvider i18nProvider() {
        return new DefaultI18NProvider(UserLocale.SUPPORTED);
    }

    @Bean
    VaadinServiceInitListener vaadinServiceInitListener() {
        return serviceInitEvent -> {
            serviceInitEvent.getSource()
                    .addSessionInitListener(sessionInitEvent -> sessionInitEvent.getSession()
                            .setErrorHandler(customErrorHandler()));
            // The language comes from the ID token, not from the browser, and only changes when the user logs in again
            serviceInitEvent.getSource()
                    .addUIInitListener(uiInitEvent -> uiInitEvent.getUI().setLocale(UserLocale.current()));
        };
    }
}
