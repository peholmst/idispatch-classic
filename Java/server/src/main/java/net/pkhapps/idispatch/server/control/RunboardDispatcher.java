package net.pkhapps.idispatch.server.control;

import net.pkhapps.idispatch.server.entity.DispatchNotification;

import java.util.Collection;

public interface RunboardDispatcher {

    Collection<DispatchNotification> getDispatchNotifications(String runboardKey);

    void acknowledgeDispatchNotification(String runboardKey, Long notificationId);

    void cleanUp();
}
