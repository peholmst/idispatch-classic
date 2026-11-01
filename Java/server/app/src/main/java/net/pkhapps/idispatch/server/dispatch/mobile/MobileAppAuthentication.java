package net.pkhapps.idispatch.server.dispatch.mobile;

import net.pkhapps.idispatch.server.dispatch.MobileDestination;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A request from the mobile app of a {@link MobileDestination}, authenticated by the destination's key. It has no roles:
 * the app may use only the mobile API, and only for its own unit.
 */
final class MobileAppAuthentication extends AbstractAuthenticationToken {

    private final MobileDestination destination;

    MobileAppAuthentication(MobileDestination destination) {
        super(List.of());
        this.destination = requireNonNull(destination);
        setAuthenticated(true);
    }

    @Override
    public MobileDestination getPrincipal() {
        return destination;
    }

    /**
     * Never the key, which is not kept.
     */
    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public String getName() {
        return "mobile-destination-" + destination.id().value();
    }
}
