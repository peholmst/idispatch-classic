package net.pkhapps.idispatch.server.dispatch;

import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceListItem;
import net.pkhapps.idispatch.server.resource.ResourceQueries;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.shared.management.DefaultFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Lets administrators decide where the dispatch notifications of each resource go. A runboard key that is already in
 * use is refused with a {@link org.springframework.dao.DataIntegrityViolationException}, and a change based on an
 * outdated version with an {@link org.springframework.dao.OptimisticLockingFailureException}.
 */
@Service
@PreAuthorize(Roles.HAS_ADMIN)
class DestinationManagementService {

    /**
     * A mobile destination with the key just generated for it. Only the key's hash is stored, so this is the only time
     * the key can be shown.
     */
    record KeyedMobileDestination(MobileDestination destination, MobileKey key) {
    }

    private static final Logger log = LoggerFactory.getLogger(DestinationManagementService.class);

    private final DestinationRepository repository;
    private final ResourceQueries resourceQueries;

    DestinationManagementService(DestinationRepository repository, ResourceQueries resourceQueries) {
        this.repository = repository;
        this.resourceQueries = resourceQueries;
    }

    /**
     * Finds the destinations matching the filter, whose description or the call sign of any of whose resources
     * contains the search term, in the order they were added.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<DestinationListItem> findAll(DefaultFilter filter) {
        var searchTerm = filter.searchTerm() == null ? "" : filter.searchTerm().trim();
        var matchingResources = searchTerm.isEmpty() ? List.<ResourceId>of()
                : resourceQueries.findByCallSignContaining(searchTerm).stream().map(ResourceListItem::id).toList();
        var entries = repository.findListEntries(filter.includesDeleted(), searchTerm, matchingResources);
        var allResources = new HashSet<ResourceId>();
        entries.forEach(entry -> allResources.addAll(entry.resources()));
        var callSigns = resourceQueries.findCallSigns(allResources);
        return entries.stream()
                .map(entry -> new DestinationListItem(entry.id(), entry.kind(), entry.description(), entry.locale(),
                        entry.resources().stream().map(callSigns::get).filter(Objects::nonNull).sorted()
                                .toList(),
                        entry.active()))
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<Destination> findById(DestinationId id) {
        return repository.findById(id);
    }

    /**
     * Creates a runboard or SMS destination; a mobile one is created with
     * {@link #createMobile(MobileDestinationDetails)}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Destination create(DestinationDetails details) {
        log.debug("Creating destination for resources {}", details.resources());
        return repository.insert(details);
    }

    /**
     * Creates a mobile destination with a new key.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public KeyedMobileDestination createMobile(MobileDestinationDetails details) {
        log.debug("Creating mobile destination for resource {}", details.resource());
        var key = MobileKey.random();
        return new KeyedMobileDestination(repository.insertMobile(details, key.hash()), key);
    }

    /**
     * Gives the mobile destination a new key, after which the app paired with the old one is no longer let in.
     *
     * @return the new key
     * @throws IllegalArgumentException if there is no such mobile destination
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MobileKey replaceMobileKey(DestinationId id) {
        log.debug("Replacing the key of mobile destination {}", id.value());
        var key = MobileKey.random();
        if (!repository.replaceMobileKeyHash(id, key.hash())) {
            throw new IllegalArgumentException("No mobile destination " + id.value());
        }
        return key;
    }

    /**
     * Saves the changed details of the destination.
     *
     * @return the destination as saved, with its new version
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Destination update(Destination destination) {
        log.debug("Updating destination {} version {}", destination.id().value(), destination.version());
        return repository.update(destination);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void delete(DestinationId id) {
        log.debug("Softly deleting destination {}", id.value());
        repository.setActive(id, false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restore(DestinationId id) {
        log.debug("Restoring destination {}", id.value());
        repository.setActive(id, true);
    }

    /**
     * Finds the resources that can be chosen for a destination, which are the ones not deleted, in call sign order.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<ResourceListItem> findApplicableResources() {
        return resourceQueries.findActive();
    }

    /**
     * Finds the resources with the given IDs, whether deleted or not, in call sign order.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<ResourceListItem> findResources(Collection<ResourceId> ids) {
        return resourceQueries.findByIds(ids);
    }
}
