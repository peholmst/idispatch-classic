package net.pkhapps.idispatch.server.dispatch;

import net.pkhapps.idispatch.server.resource.ResourceId;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * The details of a {@link MobileDestination}. It has exactly one resource, since the app also changes that unit's
 * status.
 */
record MobileDestinationDetails(@Nullable DestinationDescription description, Locale locale,
                                ResourceId resource) implements DestinationDetails {

    public MobileDestinationDetails {
        requireNonNull(locale);
        requireNonNull(resource);
    }

    @Override
    public Set<ResourceId> resources() {
        return Set.of(resource);
    }
}
