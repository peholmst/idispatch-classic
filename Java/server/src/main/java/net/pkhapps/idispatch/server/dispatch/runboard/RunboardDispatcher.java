package net.pkhapps.idispatch.server.dispatch.runboard;

import net.pkhapps.idispatch.server.dispatch.AbstractDispatcher;
import net.pkhapps.idispatch.server.dispatch.DestinationId;
import net.pkhapps.idispatch.server.dispatch.DispatchNotification;
import net.pkhapps.idispatch.server.dispatch.DispatchNotificationId;
import net.pkhapps.idispatch.server.dispatch.DispatchNotificationSent;
import net.pkhapps.idispatch.server.dispatch.RunboardDestination;
import net.pkhapps.idispatch.server.shared.Bilingual;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the dispatch notifications that the runboards have not picked up yet, in memory, until they acknowledge them
 * or they expire.
 * <p>
 * Queues the notifications per runboard destination. A runboard is known by its key, as the destination was when a
 * notification was last queued for it.
 */
@Component
public class RunboardDispatcher extends AbstractDispatcher {

    private static final Duration EXPIRY = Duration.ofMinutes(2);

    private final Map<String, RunboardDestination> runboardsByKey = new ConcurrentHashMap<>();
    private final Map<DestinationId, Map<DispatchNotificationId, DispatchNotification>> waitingNotifications =
            new ConcurrentHashMap<>();

    @Override
    @Async
    public void onApplicationEvent(DispatchNotificationSent event) {
        final DispatchNotification notification = event.getDispatchNotification();
        event.getDestinationsOfType(RunboardDestination.class).forEach(runboard -> {
            runboardsByKey.put(runboard.runboardKey().value(), runboard);
            logger.debug("Adding dispatch notification {} for destination {}", notification.id(), runboard.id());
            waitingNotifications.computeIfAbsent(runboard.id(), id -> new ConcurrentHashMap<>())
                    .put(notification.id(), notification);
            applicationContext.publishEvent(new RunboardNotificationQueued(this, runboard.runboardKey().value()));
        });
    }

    public Collection<DispatchNotification> getDispatchNotifications(String runboardKey) {
        logger.trace("Looking up dispatch notifications for runboard {}", runboardKey);
        return getDestinationByKey(runboardKey)
                .map(runboard -> waitingNotifications.getOrDefault(runboard.id(), Map.of()))
                .<Collection<DispatchNotification>>map(notifications -> List.copyOf(notifications.values()))
                .orElse(List.of());
    }

    /**
     * Returns the language of the runboard as it was when a notification was last queued for it, or Finnish if none
     * has been.
     */
    public Locale getLocale(String runboardKey) {
        return getDestinationByKey(runboardKey).map(RunboardDestination::locale).orElse(Bilingual.FINNISH);
    }

    private Optional<RunboardDestination> getDestinationByKey(String runboardKey) {
        return Optional.ofNullable(runboardsByKey.get(runboardKey));
    }

    @Async
    public void acknowledgeDispatchNotification(String runboardKey, DispatchNotificationId notificationId) {
        logger.debug("Acknowledging notification {} for runboard {}", notificationId, runboardKey);
        getDestinationByKey(runboardKey).ifPresent(runboard -> {
            var notifications = waitingNotifications.get(runboard.id());
            var notification = notifications == null ? null : notifications.remove(notificationId);
            if (notification != null) {
                createReceipt(runboard, notification);
            }
        });
    }

    @Scheduled(fixedDelay = 60000)
    public void cleanUp() {
        logger.trace("Cleaning up expired dispatch notifications");
        var expired = Instant.now().minus(EXPIRY);
        waitingNotifications.values().forEach(notifications -> notifications.values().removeIf(notification -> {
            if (notification.timestamp().isBefore(expired)) {
                logger.debug("Removing expired notification {}", notification.id());
                return true;
            }
            return false;
        }));
    }
}
