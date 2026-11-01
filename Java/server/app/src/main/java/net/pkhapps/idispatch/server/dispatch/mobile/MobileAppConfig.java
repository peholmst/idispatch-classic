package net.pkhapps.idispatch.server.dispatch.mobile;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static net.pkhapps.idispatch.server.dispatch.mobile.MobileSecurityConfig.APP_PATH;

/**
 * Serves the mobile app, which the {@code idispatch-mobile} module builds into {@code META-INF/resources/mobile} and
 * Spring serves from there. Vaadin leaves {@code /mobile/**} alone ({@code vaadin.exclude-urls}), and the app's page is
 * served for the folder itself, since the pairing link points at the folder.
 */
@Configuration
class MobileAppConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController(APP_PATH, APP_PATH + "/");
        registry.addViewController(APP_PATH + "/").setViewName("forward:" + APP_PATH + "/index.html");
    }
}
