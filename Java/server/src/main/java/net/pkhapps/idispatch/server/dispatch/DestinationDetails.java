package net.pkhapps.idispatch.server.dispatch;

import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.shared.Bilingual;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * What an administrator enters for a {@link Destination}: everything about it except its identity, version and whether
 * it has been deleted.
 */
public sealed interface DestinationDetails permits SmsDestinationDetails, RunboardDestinationDetails {

    @Nullable DestinationDescription description();

    /**
     * The language the destination receives notifications in. It picks the language of the municipality names and
     * assignment type descriptions, as described in {@link Bilingual#select}.
     */
    Locale locale();

    /**
     * The resources whose notifications the destination receives; never empty.
     */
    Set<ResourceId> resources();

    static void check(Locale locale, Set<ResourceId> resources) {
        requireNonNull(locale);
        if (resources.isEmpty()) {
            throw new IllegalArgumentException("A destination needs at least one resource");
        }
    }
}
