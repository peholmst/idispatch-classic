package net.pkhapps.idispatch.server.dispatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;

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
     * Finds the notifications sent to the destination since the given moment that it has not confirmed, oldest first.
     */
    protected List<DispatchNotification> findUnconfirmed(Destination destination, Instant since) {
        return dispatchNotificationRepository.findUnconfirmed(destination.id(), since);
    }

    /**
     * Stores that the destination received the notification, unless it already has, and publishes a
     * {@link DispatchNotificationReceived} the first time.
     *
     * @return whether the notification was sent to the destination
     */
    protected boolean confirm(Destination destination, DispatchNotificationId notification) {
        var sent = dispatchNotificationRepository.findSentTo(destination.id(), notification);
        if (sent.isEmpty()) {
            return false;
        }
        if (!dispatchNotificationRepository.hasReceipt(destination.id(), notification)) {
            try {
                createReceipt(destination, sent.get());
            } catch (DataIntegrityViolationException ex) {
                logger.debug("Destination {} confirmed notification {} twice at once", destination.id(),
                        notification);
            }
        }
        return true;
    }

    /**
     * Stores that the destination received the notification, and publishes a {@link DispatchNotificationReceived}.
     */
    protected void createReceipt(Destination destination, DispatchNotification notification) {
        logger.debug("Storing receipt for destination {} and notification {}", destination.id(), notification.id());
        var receipt = dispatchNotificationRepository.insertReceipt(destination.id(), notification.id());
        applicationContext.publishEvent(new DispatchNotificationReceived(this, receipt));
    }
}
