package net.pkhapps.idispatch.runboard;

import net.pkhapps.idispatch.runboard.client.DispatcherClientException;
import net.pkhapps.idispatch.runboard.client.Notification;
import net.pkhapps.idispatch.runboard.client.ServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

class Model extends Observable implements ServerConnection.Callback {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final List<Notification> notifications = new ArrayList<>();
    private final Set<Number> seenNotifications = new HashSet<>();
    private final Set<Notification> heldNotifications = new HashSet<>();
    private final Timer expirationTimer = new Timer();
    private final Duration shownFor;
    private DispatcherClientException.ErrorCode errorCode;

    /**
     * @param shownFor how long each notification is shown, counted from when it was sent
     */
    Model(Duration shownFor) {
        this(shownFor, true);
    }

    /**
     * @param expireOnTimer whether expired notifications are removed every second, or only when
     *                      {@link #removeExpiredNotifications(Instant)} is called, as in tests
     */
    Model(Duration shownFor, boolean expireOnTimer) {
        this.shownFor = shownFor;
        if (expireOnTimer) {
            expirationTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    removeExpiredNotifications(Instant.now());
                }
            }, 1000, 1000);
        }
    }

    /**
     * Keeps the notification shown after it has expired, until it is {@linkplain #release released}, as while it is
     * still being read out loud.
     */
    synchronized void hold(Notification notification) {
        heldNotifications.add(notification);
    }

    /**
     * Lets the notification expire again. If it already has, it is removed the next time expired notifications are.
     */
    synchronized void release(Notification notification) {
        heldNotifications.remove(notification);
    }

    synchronized void removeExpiredNotifications(Instant now) {
        for (Iterator<Notification> it = notifications.iterator(); it.hasNext(); ) {
            Notification notification = it.next();
            if (isExpired(now, notification) && !heldNotifications.contains(notification)) {
                logger.info("Removing expired notification {}", notification);
                it.remove();
                seenNotifications.remove(notification.getId());
                setChanged();
            }
        }
        notifyObservers();
    }

    private boolean isExpired(Instant now, Notification notification) {
        return notification.getTimestamp().toInstant().plus(shownFor).isBefore(now);
    }

    public synchronized DispatcherClientException.ErrorCode getErrorCode() {
        return errorCode;
    }

    @Override
    public synchronized void setErrorCode(DispatcherClientException.ErrorCode errorCode) {
        if (!Objects.equals(this.errorCode, errorCode)) {
            logger.info("Setting error code to {}", errorCode);
            this.errorCode = errorCode;
            setChanged();
        }
        notifyObservers();
    }

    public synchronized boolean hasError() {
        return errorCode != null;
    }

    private void addNotifications(Collection<Notification> notifications) {
        notifications.forEach(this::addNotification);
        notifyObservers();
    }

    private void addNotification(Notification notification) {
        if (!seenNotifications.contains(notification.getId())) {
            logger.info("Adding notification {} to model", notification);
            notifications.add(notification);
            seenNotifications.add(notification.getId());
            setChanged();
        }
    }

    public synchronized List<Notification> getVisibleNotifications() {
        // A copy, since the list changes on other threads while the caller goes through it
        return List.copyOf(notifications);
    }

    @Override
    public void clearErrorCode() {
        setErrorCode(null);
    }

    @Override
    public synchronized void notificationsReceived(Collection<Notification> notifications) {
        addNotifications(notifications);
    }
}
