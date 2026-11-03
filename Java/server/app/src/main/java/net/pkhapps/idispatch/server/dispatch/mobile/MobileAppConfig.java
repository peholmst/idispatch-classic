package net.pkhapps.idispatch.server.dispatch.mobile;

import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
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

    /**
     * The app's manifest, which would otherwise be served as {@code application/octet-stream}: Spring serves static
     * files with the servlet container's type for their extension, and Tomcat has none for {@code .webmanifest}.
     * Spring Boot 4 has no property for it.
     */
    @Bean
    WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> webManifestMimeType() {
        return factory -> {
            var mappings = new MimeMappings();
            mappings.add("webmanifest", "application/manifest+json");
            factory.addMimeMappings(mappings);
        };
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController(APP_PATH, APP_PATH + "/");
        registry.addViewController(APP_PATH + "/").setViewName("forward:" + APP_PATH + "/index.html");
    }
}
