package net.pkhapps.idispatch.server.dispatch;

import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * A destination that sends the notifications as SMS messages to phone numbers.
 */
public record SmsDestination(DestinationId id, long version, SmsDestinationDetails details, boolean active)
        implements Destination {

    public SmsDestination {
        requireNonNull(id);
        requireNonNull(details);
    }

    public Set<PhoneNumber> phoneNumbers() {
        return details.phoneNumbers();
    }

    /**
     * Returns this destination with the given details, to be saved as a change of this version.
     */
    public SmsDestination withDetails(SmsDestinationDetails details) {
        return new SmsDestination(id, version, details, active);
    }
}
