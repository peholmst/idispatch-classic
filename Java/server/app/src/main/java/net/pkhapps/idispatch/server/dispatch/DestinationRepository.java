package net.pkhapps.idispatch.server.dispatch;

import net.pkhapps.idispatch.server.jooq.tables.records.DestinationsRecord;
import net.pkhapps.idispatch.server.resource.ResourceId;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static net.pkhapps.idispatch.server.jooq.Sequences.DESTINATIONS_SEQ;
import static net.pkhapps.idispatch.server.jooq.Tables.DESTINATIONS;
import static net.pkhapps.idispatch.server.jooq.Tables.DESTINATION_RESOURCES;
import static net.pkhapps.idispatch.server.jooq.Tables.MOBILE_DESTINATIONS;
import static net.pkhapps.idispatch.server.jooq.Tables.RUNBOARD_DESTINATIONS;
import static net.pkhapps.idispatch.server.jooq.Tables.SMS_DESTINATIONS;
import static net.pkhapps.idispatch.server.jooq.Tables.SMS_DESTINATION_PHONE_NUMBERS;

/**
 * Stores the {@link Destination}s in the {@code destinations} table, with the runboard keys in
 * {@code runboard_destinations}, the phone numbers in {@code sms_destination_phone_numbers}, the hashes of the mobile
 * keys in {@code mobile_destinations} and the resources in {@code destination_resources}. A runboard key that is
 * already in use is refused by the table's unique constraint, which reaches the caller as a
 * {@link org.springframework.dao.DataIntegrityViolationException}.
 */
@Repository
class DestinationRepository {

    /**
     * A destination as listed, with the IDs of its resources, whose call signs the service looks up.
     */
    record ListEntry(DestinationId id, DestinationListItem.Kind kind, @Nullable String description, Locale locale,
                     Set<ResourceId> resources, boolean active) {
    }

    private final DSLContext dsl;

    DestinationRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Finds the destinations whose description contains the search term, ignoring case, or that have any of the
     * given resources, in the order they were added.
     */
    List<ListEntry> findListEntries(boolean includeDeleted, @Nullable String searchTerm,
                                    Collection<ResourceId> matchingResources) {
        var conditions = new ArrayList<Condition>();
        if (!includeDeleted) {
            conditions.add(DESTINATIONS.ACTIVE.isTrue());
        }
        if (searchTerm != null && !searchTerm.isEmpty()) {
            conditions.add(DESTINATIONS.DESCRIPTION.containsIgnoreCase(searchTerm)
                    .or(DESTINATIONS.ID.in(DSL.select(DESTINATION_RESOURCES.DESTINATION_ID)
                            .from(DESTINATION_RESOURCES)
                            .where(DESTINATION_RESOURCES.RESOURCE_ID.in(
                                    matchingResources.stream().map(ResourceId::value).toList())))));
        }
        return dsl.select(DESTINATIONS.ID, DESTINATIONS.DESCRIPTION, DESTINATIONS.LOCALE, DESTINATIONS.ACTIVE,
                        RUNBOARD_DESTINATIONS.ID, MOBILE_DESTINATIONS.ID)
                .from(DESTINATIONS)
                .leftJoin(RUNBOARD_DESTINATIONS).on(RUNBOARD_DESTINATIONS.ID.eq(DESTINATIONS.ID))
                .leftJoin(MOBILE_DESTINATIONS).on(MOBILE_DESTINATIONS.ID.eq(DESTINATIONS.ID))
                .where(DSL.and(conditions))
                .orderBy(DESTINATIONS.ID)
                .fetch(row -> new ListEntry(new DestinationId(row.value1()),
                        row.value5() != null ? DestinationListItem.Kind.RUNBOARD
                                : row.value6() != null ? DestinationListItem.Kind.MOBILE
                                : DestinationListItem.Kind.SMS,
                        row.value2(), toLocale(row.value3()), resourcesOf(row.value1()), row.value4()));
    }

    Optional<Destination> findById(DestinationId id) {
        return dsl.selectFrom(DESTINATIONS)
                .where(DESTINATIONS.ID.eq(id.value()))
                .fetchOptional()
                .map(this::toDestination);
    }

    /**
     * Finds the destinations not deleted that receive the notifications of any of the resources.
     */
    List<Destination> findActiveFor(Collection<ResourceId> resources) {
        return dsl.selectFrom(DESTINATIONS)
                .where(DESTINATIONS.ACTIVE.isTrue())
                .and(DESTINATIONS.ID.in(DSL.select(DESTINATION_RESOURCES.DESTINATION_ID)
                        .from(DESTINATION_RESOURCES)
                        .where(DESTINATION_RESOURCES.RESOURCE_ID.in(
                                resources.stream().map(ResourceId::value).toList()))))
                .orderBy(DESTINATIONS.ID)
                .fetch(this::toDestination);
    }

    /**
     * Inserts a runboard or SMS destination; a mobile destination is inserted with
     * {@link #insertMobile(MobileDestinationDetails, MobileKeyHash)}, as it needs its key.
     */
    Destination insert(DestinationDetails details) {
        if (details instanceof MobileDestinationDetails) {
            throw new IllegalArgumentException("A mobile destination is inserted with the hash of its key");
        }
        var id = insertDestination(details);
        switch (details) {
            case SmsDestinationDetails sms -> {
                dsl.insertInto(SMS_DESTINATIONS).set(SMS_DESTINATIONS.ID, id.value()).execute();
                insertPhoneNumbers(id, sms);
            }
            case RunboardDestinationDetails runboard -> dsl.insertInto(RUNBOARD_DESTINATIONS)
                    .set(RUNBOARD_DESTINATIONS.ID, id.value())
                    .set(RUNBOARD_DESTINATIONS.RUNBOARD_KEY, runboard.runboardKey().value())
                    .execute();
            case MobileDestinationDetails mobile -> throw new AssertionError("Refused above");
        }
        insertResources(id, details);
        return findById(id).orElseThrow();
    }

    MobileDestination insertMobile(MobileDestinationDetails details, MobileKeyHash keyHash) {
        var id = insertDestination(details);
        dsl.insertInto(MOBILE_DESTINATIONS)
                .set(MOBILE_DESTINATIONS.ID, id.value())
                .set(MOBILE_DESTINATIONS.KEY_HASH, keyHash.value())
                .execute();
        insertResources(id, details);
        return (MobileDestination) findById(id).orElseThrow();
    }

    /**
     * Replaces the hash of the mobile destination's key. This is not a change of the destination's details, so its
     * version stays, and an administrator editing the destination at the same time can still save.
     *
     * @return whether there is such a mobile destination
     */
    boolean replaceMobileKeyHash(DestinationId id, MobileKeyHash keyHash) {
        return dsl.update(MOBILE_DESTINATIONS)
                .set(MOBILE_DESTINATIONS.KEY_HASH, keyHash.value())
                .where(MOBILE_DESTINATIONS.ID.eq(id.value()))
                .execute() > 0;
    }

    private DestinationId insertDestination(DestinationDetails details) {
        var id = new DestinationId(dsl.nextval(DESTINATIONS_SEQ));
        dsl.insertInto(DESTINATIONS)
                .set(DESTINATIONS.ID, id.value())
                .set(DESTINATIONS.VERSION, 0L)
                .set(DESTINATIONS.ACTIVE, true)
                .set(DESTINATIONS.DESCRIPTION, description(details))
                .set(DESTINATIONS.LOCALE, details.locale().toString())
                .execute();
        return id;
    }

    /**
     * Saves the details of the destination, provided that its version is still the stored one.
     *
     * @return the destination with its new version
     * @throws OptimisticLockingFailureException if the destination has been changed since this version, or no longer
     *                                           exists
     */
    Destination update(Destination destination) {
        var id = destination.id();
        var details = destination.details();
        var updated = dsl.update(DESTINATIONS)
                .set(DESTINATIONS.DESCRIPTION, description(details))
                .set(DESTINATIONS.LOCALE, details.locale().toString())
                .set(DESTINATIONS.VERSION, destination.version() + 1)
                .where(DESTINATIONS.ID.eq(id.value()))
                .and(DESTINATIONS.VERSION.eq(destination.version()))
                .execute();
        if (updated == 0) {
            throw new OptimisticLockingFailureException(
                    "Destination %s has been changed or removed since version %d".formatted(id.value(),
                            destination.version()));
        }
        switch (details) {
            case SmsDestinationDetails sms -> {
                dsl.deleteFrom(SMS_DESTINATION_PHONE_NUMBERS)
                        .where(SMS_DESTINATION_PHONE_NUMBERS.SMS_DESTINATION_ID.eq(id.value())).execute();
                insertPhoneNumbers(id, sms);
            }
            case RunboardDestinationDetails runboard -> dsl.update(RUNBOARD_DESTINATIONS)
                    .set(RUNBOARD_DESTINATIONS.RUNBOARD_KEY, runboard.runboardKey().value())
                    .where(RUNBOARD_DESTINATIONS.ID.eq(id.value()))
                    .execute();
            case MobileDestinationDetails mobile -> {
                // The key is replaced on its own, with replaceMobileKeyHash
            }
        }
        dsl.deleteFrom(DESTINATION_RESOURCES).where(DESTINATION_RESOURCES.DESTINATION_ID.eq(id.value())).execute();
        insertResources(id, details);
        return findById(id).orElseThrow();
    }

    /**
     * Deletes the destination softly, or restores it. Does nothing if there is no such destination.
     */
    void setActive(DestinationId id, boolean active) {
        dsl.update(DESTINATIONS)
                .set(DESTINATIONS.ACTIVE, active)
                .set(DESTINATIONS.VERSION, DESTINATIONS.VERSION.plus(1))
                .where(DESTINATIONS.ID.eq(id.value()))
                .execute();
    }

    private void insertResources(DestinationId id, DestinationDetails details) {
        for (var resource : details.resources()) {
            dsl.insertInto(DESTINATION_RESOURCES)
                    .set(DESTINATION_RESOURCES.DESTINATION_ID, id.value())
                    .set(DESTINATION_RESOURCES.RESOURCE_ID, resource.value())
                    .execute();
        }
    }

    private void insertPhoneNumbers(DestinationId id, SmsDestinationDetails details) {
        for (var number : details.phoneNumbers()) {
            dsl.insertInto(SMS_DESTINATION_PHONE_NUMBERS)
                    .set(SMS_DESTINATION_PHONE_NUMBERS.SMS_DESTINATION_ID, id.value())
                    .set(SMS_DESTINATION_PHONE_NUMBERS.PHONE_NUMBERS, number.value())
                    .execute();
        }
    }

    private Destination toDestination(DestinationsRecord row) {
        var id = new DestinationId(row.getId());
        var description = DestinationDescription.of(row.getDescription());
        var locale = toLocale(row.getLocale());
        var resources = resourcesOf(row.getId());
        var runboardKey = dsl.select(RUNBOARD_DESTINATIONS.RUNBOARD_KEY)
                .from(RUNBOARD_DESTINATIONS)
                .where(RUNBOARD_DESTINATIONS.ID.eq(row.getId()))
                .fetchOptional(RUNBOARD_DESTINATIONS.RUNBOARD_KEY);
        if (runboardKey.isPresent()) {
            return new RunboardDestination(id, row.getVersion(), new RunboardDestinationDetails(description, locale,
                    resources, new RunboardKey(runboardKey.get())), row.getActive());
        }
        if (dsl.fetchExists(MOBILE_DESTINATIONS, MOBILE_DESTINATIONS.ID.eq(row.getId()))) {
            return new MobileDestination(id, row.getVersion(), new MobileDestinationDetails(description, locale,
                    resources.iterator().next()), row.getActive());
        }
        var phoneNumbers = dsl.select(SMS_DESTINATION_PHONE_NUMBERS.PHONE_NUMBERS)
                .from(SMS_DESTINATION_PHONE_NUMBERS)
                .where(SMS_DESTINATION_PHONE_NUMBERS.SMS_DESTINATION_ID.eq(row.getId()))
                .fetch(SMS_DESTINATION_PHONE_NUMBERS.PHONE_NUMBERS)
                .stream()
                .filter(PhoneNumber::isValid)
                .map(PhoneNumber::new)
                .collect(Collectors.toSet());
        return new SmsDestination(id, row.getVersion(),
                new SmsDestinationDetails(description, locale, resources, phoneNumbers), row.getActive());
    }

    private Set<ResourceId> resourcesOf(long destinationId) {
        return dsl.select(DESTINATION_RESOURCES.RESOURCE_ID)
                .from(DESTINATION_RESOURCES)
                .where(DESTINATION_RESOURCES.DESTINATION_ID.eq(destinationId))
                .fetch(DESTINATION_RESOURCES.RESOURCE_ID)
                .stream()
                .map(ResourceId::new)
                .collect(Collectors.toSet());
    }

    private static @Nullable String description(DestinationDetails details) {
        return details.description() == null ? null : details.description().value();
    }

    /**
     * Hibernate stored a locale as {@link Locale#toString()}, such as {@code sv} or {@code sv_FI}, and so does this.
     */
    private static Locale toLocale(String stored) {
        return Locale.forLanguageTag(stored.replace('_', '-'));
    }
}
