package net.pkhapps.idispatch.server.dispatch.mobile;

import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeListItem;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeQueries;
import net.pkhapps.idispatch.server.dispatch.AbstractDispatcher;
import net.pkhapps.idispatch.server.dispatch.DispatchNotification;
import net.pkhapps.idispatch.server.dispatch.DispatchNotificationId;
import net.pkhapps.idispatch.server.dispatch.DispatchNotificationSent;
import net.pkhapps.idispatch.server.dispatch.MobileDestination;
import net.pkhapps.idispatch.server.municipality.MunicipalityQueries;
import net.pkhapps.idispatch.server.resource.ResourceListItem;
import net.pkhapps.idispatch.server.resource.ResourceQueries;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Delivers the dispatch notifications to the mobile apps, over their event streams (see {@link MobileEventStreams}).
 * <p>
 * Unlike the runboards' queue, which is kept in memory, the notifications waiting for an app are those sent to its
 * destination that it has not confirmed, as stored: a phone that was out of reach, or a server restart, loses none.
 * They are sent again whenever the app connects, for {@code idispatch.mobile.redelivery-window} after they were sent,
 * and the app confirms each one once it has stored it, which stores a receipt.
 */
@Component
class MobileDispatcher extends AbstractDispatcher {

    private final MobileEventStreams eventStreams;
    private final AssignmentTypeQueries assignmentTypeQueries;
    private final MunicipalityQueries municipalityQueries;
    private final ResourceQueries resourceQueries;
    private final Duration redeliveryWindow;

    MobileDispatcher(MobileEventStreams eventStreams, AssignmentTypeQueries assignmentTypeQueries,
                     MunicipalityQueries municipalityQueries, ResourceQueries resourceQueries,
                     @Value("${idispatch.mobile.redelivery-window:24h}") Duration redeliveryWindow) {
        this.eventStreams = eventStreams;
        this.assignmentTypeQueries = assignmentTypeQueries;
        this.municipalityQueries = municipalityQueries;
        this.resourceQueries = resourceQueries;
        this.redeliveryWindow = redeliveryWindow;
    }

    @Override
    public void onApplicationEvent(DispatchNotificationSent event) {
        var notification = event.getDispatchNotification();
        event.getDestinationsOfType(MobileDestination.class).forEach(destination -> {
            logger.debug("Sending dispatch notification {} to mobile destination {}", notification.id(),
                    destination.id());
            eventStreams.sendNotification(destination.id(), response(notification, destination.locale()));
        });
    }

    /**
     * The notifications waiting for the destination's app, oldest first.
     */
    List<NotificationResponse> waitingFor(MobileDestination destination) {
        return findUnconfirmed(destination, Instant.now().minus(redeliveryWindow)).stream()
                .map(notification -> response(notification, destination.locale()))
                .toList();
    }

    /**
     * Stores that the destination's app received the notification. Confirming it again does nothing.
     *
     * @return whether the notification was sent to the destination
     */
    boolean confirm(MobileDestination destination, long notificationId) {
        logger.debug("Mobile destination {} confirms notification {}", destination.id(), notificationId);
        return confirm(destination, new DispatchNotificationId(notificationId));
    }

    private NotificationResponse response(DispatchNotification notification, Locale locale) {
        var type = assignmentTypeQueries.findById(notification.assignmentTypeId());
        return new NotificationResponse(notification.id().value(), notification.timestamp(),
                notification.urgency().name(),
                type.map(AssignmentTypeListItem::code).orElse("?"),
                type.map(it -> it.description(locale)).orElse("?"),
                municipalityQueries.findById(notification.municipalityId())
                        .map(municipality -> municipality.name(locale)).orElse("?"),
                notification.address(), notification.description(),
                resourceQueries.findByIds(notification.assignedResources()).stream()
                        .map(ResourceListItem::callSign).toList());
    }
}
