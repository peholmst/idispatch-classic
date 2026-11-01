package net.pkhapps.idispatch.server.dispatch;

import net.pkhapps.idispatch.server.resource.ResourceId;

import static java.util.Objects.requireNonNull;

/**
 * A destination that is the mobile app of one unit, which receives the unit's notifications and reports its status.
 * The app authenticates with a {@link MobileKey}, of which only the hash is stored; the key is not part of the record,
 * as it can't be read back.
 */
public record MobileDestination(DestinationId id, long version, MobileDestinationDetails details, boolean active)
        implements Destination {

    public MobileDestination {
        requireNonNull(id);
        requireNonNull(details);
    }

    /**
     * The unit whose app this is.
     */
    public ResourceId resource() {
        return details.resource();
    }

    /**
     * Returns this destination with the given details, to be saved as a change of this version.
     */
    public MobileDestination withDetails(MobileDestinationDetails details) {
        return new MobileDestination(id, version, details, active);
    }
}
