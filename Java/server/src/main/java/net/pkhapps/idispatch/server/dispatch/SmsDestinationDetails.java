package net.pkhapps.idispatch.server.dispatch;

import net.pkhapps.idispatch.server.resource.ResourceId;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Set;

/**
 * The details of an {@link SmsDestination}. The form asks for at least one phone number, but a destination stored
 * before the numbers were checked can have none that is valid, and then sends nothing.
 */
record SmsDestinationDetails(@Nullable DestinationDescription description, Locale locale,
                                    Set<ResourceId> resources, Set<PhoneNumber> phoneNumbers)
        implements DestinationDetails {

    public SmsDestinationDetails {
        DestinationDetails.check(locale, resources);
        resources = Set.copyOf(resources);
        phoneNumbers = Set.copyOf(phoneNumbers);
    }
}
