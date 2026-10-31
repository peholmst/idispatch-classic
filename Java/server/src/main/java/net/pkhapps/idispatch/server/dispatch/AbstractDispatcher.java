package net.pkhapps.idispatch.server.dispatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

/**
 * Base class for the dispatchers that deliver the notifications to one kind of destination.
 */
public abstract class AbstractDispatcher implements ApplicationListener<DispatchNotificationSent> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private DispatchNotificationRepository dispatchNotificationRepository;

    @Autowired
    protected ApplicationContext applicationContext;

    /**
     * Stores that the destination received the notification, and publishes a {@link DispatchNotificationReceived}.
     */
    protected void createReceipt(Destination destination, DispatchNotification notification) {
        logger.debug("Storing receipt for destination {} and notification {}", destination.id(), notification.id());
        var receipt = dispatchNotificationRepository.insertReceipt(destination.id(), notification.id());
        applicationContext.publishEvent(new DispatchNotificationReceived(this, receipt));
    }
}
