package net.pkhapps.idispatch.server.dispatch;

import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceListItem;
import net.pkhapps.idispatch.server.resource.ResourceQueries;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.shared.AbstractService;
import net.pkhapps.idispatch.server.shared.audit.AuditEventKind;
import net.pkhapps.idispatch.server.shared.audit.AuditLog;
import net.pkhapps.idispatch.server.shared.audit.AuditedEntity;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Lets dispatchers move a mobile app to another unit, as when the officer who carries the phone changes vehicles. The
 * phone keeps its key and is not paired again: its next request is already for the new unit, and its open event
 * stream gets the new unit at once (a {@link MobileUnitChanged} event). Alerts it got as the old unit stay in the app.
 * Every change is recorded in the audit log as {@link AuditEventKind#MOBILE_UNIT_CHANGED}.
 */
@Service
@PreAuthorize(Roles.HAS_DISPATCHER)
public class MobileUnitService extends AbstractService {

    private static final Comparator<MobilePhoneListItem> BY_DESCRIPTION_THEN_CALL_SIGN = Comparator
            .comparing(MobilePhoneListItem::description, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(MobilePhoneListItem::callSign, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    private final DestinationRepository repository;
    private final ResourceQueries resourceQueries;
    private final AuditLog auditLog;

    MobileUnitService(ApplicationContext applicationContext, PlatformTransactionManager txManager,
                      DestinationRepository repository, ResourceQueries resourceQueries, AuditLog auditLog) {
        super(applicationContext, txManager);
        this.repository = repository;
        this.resourceQueries = resourceQueries;
        this.auditLog = auditLog;
    }

    /**
     * Finds the mobile destinations not deleted, by description, then by call sign.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<MobilePhoneListItem> findPhones() {
        var entries = repository.findListEntries(false, null, List.of()).stream()
                .filter(entry -> entry.kind() == DestinationListItem.Kind.MOBILE)
                .toList();
        var callSigns = resourceQueries.findCallSigns(entries.stream()
                .flatMap(entry -> entry.resources().stream()).toList());
        return entries.stream()
                .map(entry -> {
                    var unit = entry.resources().iterator().next();
                    return new MobilePhoneListItem(entry.id(), entry.description(), unit, callSigns.get(unit));
                })
                .sorted(BY_DESCRIPTION_THEN_CALL_SIGN)
                .toList();
    }

    /**
     * Finds the units a phone can be moved to, which are the resources not deleted, in call sign order.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<ResourceListItem> findUnits() {
        return resourceQueries.findActive();
    }

    /**
     * Moves the mobile destination to the unit. Moving it to the unit it is already on does nothing, and is not
     * recorded.
     *
     * @throws IllegalArgumentException if there is no such mobile destination, or it is deleted, or the unit is
     *                                  deleted or doesn't exist
     */
    public void changeUnit(DestinationId id, ResourceId unit) {
        logger.debug("Moving mobile destination {} to resource {}", id.value(), unit.value());
        Optional<MobileDestination> changed = getTxTemplate().execute(tx -> {
            var destination = repository.findById(id)
                    .filter(MobileDestination.class::isInstance)
                    .map(MobileDestination.class::cast)
                    .filter(MobileDestination::active)
                    .orElseThrow(() -> new IllegalArgumentException("No mobile destination " + id.value()));
            if (destination.resource().equals(unit)) {
                return Optional.empty();
            }
            if (resourceQueries.findByIds(List.of(unit)).stream().noneMatch(ResourceListItem::active)) {
                throw new IllegalArgumentException("No resource " + unit.value());
            }
            var details = destination.details();
            var updated = (MobileDestination) repository.update(destination.withDetails(
                    new MobileDestinationDetails(details.description(), details.locale(), unit)));
            auditLog.record(new AuditedEntity(AuditedEntity.Type.DESTINATION, id.value()),
                    AuditEventKind.MOBILE_UNIT_CHANGED);
            return Optional.of(updated);
        });
        if (changed != null) {
            changed.ifPresent(destination -> getApplicationContext().publishEvent(
                    new MobileUnitChanged(this, destination)));
        }
    }
}
