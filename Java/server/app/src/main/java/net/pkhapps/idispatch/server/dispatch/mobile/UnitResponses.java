package net.pkhapps.idispatch.server.dispatch.mobile;

import net.pkhapps.idispatch.server.dispatch.MobileDestination;
import net.pkhapps.idispatch.server.resource.ResourceQueries;
import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.resource.ResourceStatus;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static java.util.Objects.requireNonNullElse;

/**
 * Tells the mobile app about its unit, in the destination's language.
 */
@Component
class UnitResponses {

    private final ResourceQueries resourceQueries;

    UnitResponses(ResourceQueries resourceQueries) {
        this.resourceQueries = resourceQueries;
    }

    /**
     * The unit of the destination, with its current status.
     */
    UnitResponse of(MobileDestination destination) {
        return of(destination, resourceQueries.findCurrentStatus(destination.resource()).orElse(null));
    }

    /**
     * The unit of the destination, with the given status, such as the one just set.
     */
    UnitResponse of(MobileDestination destination, @Nullable ResourceStatus status) {
        var resource = destination.resource();
        var locale = destination.locale();
        var callSign = requireNonNullElse(resourceQueries.findCallSigns(List.of(resource)).get(resource), "");
        if (status == null) {
            return new UnitResponse(callSign, locale.getLanguage(), null, List.of());
        }
        return new UnitResponse(callSign, locale.getLanguage(), state(status.state(), locale),
                status.manualValidNextStates().stream().sorted().map(state -> state(state, locale)).toList());
    }

    private static UnitResponse.State state(ResourceState state, Locale locale) {
        return new UnitResponse.State(state.name(), label(state, locale));
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
