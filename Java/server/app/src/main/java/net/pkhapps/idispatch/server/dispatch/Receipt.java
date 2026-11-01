package net.pkhapps.idispatch.server.dispatch;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

/**
 * Confirms that a destination received a {@link DispatchNotification}. A destination confirms each notification at
 * most once, which the database checks.
 */
record Receipt(DestinationId destination, DispatchNotificationId notification, Instant timestamp) {

    public Receipt {
        requireNonNull(destination);
        requireNonNull(notification);
        requireNonNull(timestamp);
    }
}
