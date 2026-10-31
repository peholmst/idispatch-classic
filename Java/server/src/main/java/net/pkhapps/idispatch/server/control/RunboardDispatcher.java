package net.pkhapps.idispatch.server.control;

import net.pkhapps.idispatch.server.entity.DispatchNotification;

import java.util.Collection;
import java.util.Locale;

public interface RunboardDispatcher {

    Collection<DispatchNotification> getDispatchNotifications(String runboardKey);

    /**
     * Returns the language of the runboard as it was when a notification was last queued for it, or Finnish if none
     * has been.
     */
    Locale getLocale(String runboardKey);

    void acknowledgeDispatchNotification(String runboardKey, Long notificationId);

    void cleanUp();
}
