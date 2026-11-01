package net.pkhapps.idispatch.server.dispatch.mobile;

import net.pkhapps.idispatch.server.dispatch.MobileDestination;
import net.pkhapps.idispatch.server.resource.ResourceQueries;
import net.pkhapps.idispatch.server.resource.ResourceState;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static java.util.Objects.requireNonNullElse;

/**
 * The API of the mobile app, for the unit of the destination whose key the app sent (see {@link MobileSecurityConfig}).
 */
@RestController
@RequestMapping(MobileSecurityConfig.API_PATH)
class MobileApiController {

    private final ResourceQueries resourceQueries;

    MobileApiController(ResourceQueries resourceQueries) {
        this.resourceQueries = resourceQueries;
    }

    /**
     * The unit the app is paired with, and its current status.
     */
    @GetMapping("/unit")
    UnitResponse unit(@AuthenticationPrincipal MobileDestination destination) {
        var resource = destination.resource();
        var locale = destination.locale();
        var callSign = requireNonNullElse(resourceQueries.findCallSigns(List.of(resource)).get(resource), "");
        var status = resourceQueries.findCurrentStatus(resource)
                .map(current -> new UnitResponse.Status(current.state().name(), label(current.state(), locale)))
                .orElse(null);
        return new UnitResponse(callSign, locale.getLanguage(), status);
    }

    /**
     * The name of the state in the given language, from the same translations as the rest of the UI. Vaadin's
     * translation provider needs a Vaadin request, which this is not, so the bundle is read directly.
     */
    private static String label(ResourceState state, Locale locale) {
        var bundle = ResourceBundle.getBundle("vaadin-i18n/translations", locale,
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
        return bundle.getString("resource-state." + state.name());
    }
}
