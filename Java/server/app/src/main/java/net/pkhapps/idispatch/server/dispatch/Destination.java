package net.pkhapps.idispatch.server.dispatch;

import net.pkhapps.idispatch.server.resource.ResourceId;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Set;

/**
 * Where the dispatch notifications of some resources go: a runboard at the station, phone numbers by SMS, or the
 * mobile app of a unit.
 */
sealed interface Destination permits SmsDestination, RunboardDestination, MobileDestination {

    DestinationId id();

    /**
     * Incremented on every change, so that a change based on an older version can be refused.
     */
    long version();

    DestinationDetails details();

    /**
     * {@code false} once the destination has been deleted. A deleted destination receives nothing.
     */
    boolean active();

    default @Nullable DestinationDescription description() {
        return details().description();
    }

    default Locale locale() {
        return details().locale();
    }

    default Set<ResourceId> resources() {
        return details().resources();
    }
}
