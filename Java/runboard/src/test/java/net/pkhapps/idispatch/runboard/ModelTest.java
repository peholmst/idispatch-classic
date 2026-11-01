package net.pkhapps.idispatch.runboard;

import net.pkhapps.idispatch.runboard.client.Notification;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    private static final Instant SENT = Instant.parse("2026-11-01T12:00:00Z");
    private static final Duration SHOWN_FOR = Duration.ofSeconds(180);
    private static final Instant AFTER_EXPIRY = SENT.plus(SHOWN_FOR).plusSeconds(1);

    private final Model model = new Model(SHOWN_FOR, false);
    private final Notification notification = new Notification(Map.of("id", 1L, "timestamp", SENT.toEpochMilli()));

    @Test
    void expiredNotification_isRemoved() {
        model.notificationsReceived(List.of(notification));

        model.removeExpiredNotifications(AFTER_EXPIRY);

        assertTrue(model.getVisibleNotifications().isEmpty());
    }

    @Test
    void heldNotification_staysShownAfterExpiring() {
        model.notificationsReceived(List.of(notification));
        model.hold(notification);

        model.removeExpiredNotifications(AFTER_EXPIRY);

        assertEquals(List.of(notification), model.getVisibleNotifications());
    }

    @Test
    void releasedNotification_isRemovedOnceExpired() {
        model.notificationsReceived(List.of(notification));
        model.hold(notification);
        model.removeExpiredNotifications(AFTER_EXPIRY);

        model.release(notification);
        model.removeExpiredNotifications(AFTER_EXPIRY);

        assertTrue(model.getVisibleNotifications().isEmpty());
    }
}
