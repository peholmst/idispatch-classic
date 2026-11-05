package net.pkhapps.idispatch.server.dispatch;

import net.pkhapps.idispatch.server.resource.ResourceId;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * A mobile destination as the dispatcher lists it, to tell which unit each phone is on: the description that tells the
 * phone apart, and the unit with the call sign it has now.
 */
public record MobilePhoneListItem(DestinationId id, @Nullable String description, ResourceId unit,
                                  @Nullable String callSign) {

    public MobilePhoneListItem {
        requireNonNull(id);
        requireNonNull(unit);
    }
}
