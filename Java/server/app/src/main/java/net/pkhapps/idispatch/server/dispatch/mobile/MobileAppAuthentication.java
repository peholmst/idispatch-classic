package net.pkhapps.idispatch.server.dispatch.mobile;

import net.pkhapps.idispatch.server.dispatch.MobileDestination;
import net.pkhapps.idispatch.server.dispatch.MobileKeyHash;
import net.pkhapps.idispatch.server.security.Roles;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

import static java.util.Objects.requireNonNull;

/**
 * A request from the mobile app of a {@link MobileDestination}, authenticated by the destination's key. Its only
 * authority is {@link Roles#UNIT_PREFIX UNIT_}{@code <resource ID>}, which lets the app change the status of its own
 * unit and nothing else.
 */
final class MobileAppAuthentication extends AbstractAuthenticationToken {

    private final MobileDestination destination;
    private final MobileKeyHash keyHash;

    MobileAppAuthentication(MobileDestination destination, MobileKeyHash keyHash) {
        super(AuthorityUtils.createAuthorityList(Roles.UNIT_PREFIX + destination.resource().value()));
        this.destination = requireNonNull(destination);
        this.keyHash = requireNonNull(keyHash);
        setAuthenticated(true);
    }

    @Override
    public MobileDestination getPrincipal() {
        return destination;
    }

    /**
     * The hash of the key the request came with, to tell later whether the destination still has that key.
     */
    MobileKeyHash keyHash() {
        return keyHash;
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
