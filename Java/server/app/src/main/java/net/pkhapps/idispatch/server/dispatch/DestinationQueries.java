package net.pkhapps.idispatch.server.dispatch;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Looks up destinations for the delivery channels. Read-only and not secured, as the channels authenticate the
 * destinations themselves, such as a mobile app by its key.
 */
@Service
public class DestinationQueries {

    private final DestinationRepository repository;

    DestinationQueries(DestinationRepository repository) {
        this.repository = repository;
    }

    /**
     * Finds the mobile destination, not deleted, that has the given key.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<MobileDestination> findActiveMobile(MobileKey key) {
        return repository.findActiveMobileByKeyHash(key.hash());
    }
}
