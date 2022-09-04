package net.pkhapps.idispatch.server.events;

import net.pkhapps.idispatch.server.entity.DispatchNotification;
import org.springframework.context.ApplicationEvent;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Event published when a dispatch notification has been sent to a number of destinations.
 */
public class DispatchNotificationSent extends ApplicationEvent {

    private final DispatchNotification dispatchNotification;

    public DispatchNotificationSent(Object source, DispatchNotification dispatchNotification) {
        super(source);
        this.dispatchNotification = checkNotNull(dispatchNotification);
    }

    public DispatchNotification getDispatchNotification() {
        return dispatchNotification;
    }
}
