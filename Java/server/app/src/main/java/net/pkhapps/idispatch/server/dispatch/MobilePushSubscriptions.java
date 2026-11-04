package net.pkhapps.idispatch.server.dispatch;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The Web Push subscriptions of the mobile apps, one per mobile destination at most: that of the device paired with
 * the destination's key. Giving the destination a new key removes it, so that no more pushes go to the old device.
 * Not secured, as the mobile channel authenticates the apps itself, by their keys.
 */
@Service
public class MobilePushSubscriptions {

    private final DestinationRepository repository;

    MobilePushSubscriptions(DestinationRepository repository) {
        this.repository = repository;
    }

    /**
     * Stores the subscription of the app that has the key of the given hash, replacing any earlier one. The key is
     * checked in the same update, so that an app whose key was replaced meanwhile subscribes nothing.
     *
     * @return whether a mobile destination, not deleted, has that key
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean subscribe(MobileKeyHash keyHash, PushSubscription subscription) {
        return repository.replacePushSubscription(keyHash, subscription);
    }

    /**
     * Removes the subscription of the app that has the key of the given hash, if it has one.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void unsubscribe(MobileKeyHash keyHash) {
        repository.replacePushSubscription(keyHash, null);
    }

    /**
     * Finds the subscription of the mobile destination, unless the destination is deleted.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<PushSubscription> find(DestinationId destination) {
        return repository.findActivePushSubscription(destination);
    }

    /**
     * Removes the subscription of the mobile destination, if it is still the given one, as when the push service says
     * it has expired. One that the app has replaced meanwhile stays.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expire(DestinationId destination, PushSubscription subscription) {
        repository.removePushSubscription(destination, subscription.endpoint());
    }
}
