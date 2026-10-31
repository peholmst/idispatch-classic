package net.pkhapps.idispatch.server.dispatch.runboard;

import net.pkhapps.idispatch.server.dispatch.DispatchNotification;
import net.pkhapps.idispatch.server.dispatch.DispatchNotificationId;

import java.util.Collection;
import java.util.Locale;

/**
 * Keeps the dispatch notifications that the runboards have not picked up yet, in memory, until they acknowledge them
 * or they expire.
 */
public interface RunboardDispatcher {

    Collection<DispatchNotification> getDispatchNotifications(String runboardKey);

    /**
     * Returns the language of the runboard as it was when a notification was last queued for it, or Finnish if none
     * has been.
     */
    Locale getLocale(String runboardKey);

    void acknowledgeDispatchNotification(String runboardKey, DispatchNotificationId notificationId);

    void cleanUp();
}
