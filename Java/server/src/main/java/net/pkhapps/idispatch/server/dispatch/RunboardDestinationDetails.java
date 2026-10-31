package net.pkhapps.idispatch.server.dispatch;

import net.pkhapps.idispatch.server.resource.ResourceId;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * The details of a {@link RunboardDestination}, which the runboard with the key picks up.
 */
public record RunboardDestinationDetails(@Nullable DestinationDescription description, Locale locale,
                                         Set<ResourceId> resources, RunboardKey runboardKey)
        implements DestinationDetails {

    public RunboardDestinationDetails {
        DestinationDetails.check(locale, resources);
        requireNonNull(runboardKey);
        resources = Set.copyOf(resources);
    }
}
