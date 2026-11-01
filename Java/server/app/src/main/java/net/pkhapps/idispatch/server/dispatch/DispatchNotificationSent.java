package net.pkhapps.idispatch.server.dispatch;

import org.springframework.context.ApplicationEvent;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Event published when a dispatch notification has been sent to a number of destinations. It carries the destinations
 * as they were when it was sent, for the dispatchers that deliver it.
 */
public class DispatchNotificationSent extends ApplicationEvent {

    private final DispatchNotification dispatchNotification;
    private final List<Destination> destinations;

    public DispatchNotificationSent(Object source, DispatchNotification dispatchNotification,
                                    List<Destination> destinations) {
        super(source);
        this.dispatchNotification = checkNotNull(dispatchNotification);
        this.destinations = List.copyOf(destinations);
    }

    public DispatchNotification getDispatchNotification() {
        return dispatchNotification;
    }

    /**
     * Returns the destinations of the given kind that the notification was sent to.
     */
    public <D extends Destination> List<D> getDestinationsOfType(Class<D> type) {
        return destinations.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
