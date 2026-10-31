package net.pkhapps.idispatch.server.ui;

import net.pkhapps.idispatch.server.resource.ResourceState;
import org.jooq.DSLContext;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.test.context.TestComponent;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static net.pkhapps.idispatch.server.jooq.Sequences.ASSIGNMENTS_SEQ;
import static net.pkhapps.idispatch.server.jooq.Sequences.ASSIGNMENT_TYPES_SEQ;
import static net.pkhapps.idispatch.server.jooq.Sequences.MUNICIPALITIES_SEQ;
import static net.pkhapps.idispatch.server.jooq.Sequences.RESOURCES_SEQ;
import static net.pkhapps.idispatch.server.jooq.Sequences.RESOURCE_STATUS_SEQ;
import static net.pkhapps.idispatch.server.jooq.Sequences.RESOURCE_TYPES_SEQ;
import static net.pkhapps.idispatch.server.jooq.Tables.ASSIGNMENTS;
import static net.pkhapps.idispatch.server.jooq.Tables.ASSIGNMENT_TYPES;
import static net.pkhapps.idispatch.server.jooq.Tables.DESTINATIONS;
import static net.pkhapps.idispatch.server.jooq.Tables.DESTINATION_RESOURCES;
import static net.pkhapps.idispatch.server.jooq.Tables.MUNICIPALITIES;
import static net.pkhapps.idispatch.server.jooq.Tables.RESOURCES;
import static net.pkhapps.idispatch.server.jooq.Tables.RESOURCE_STATUS;
import static net.pkhapps.idispatch.server.jooq.Tables.RESOURCE_TYPES;
import static net.pkhapps.idispatch.server.jooq.Tables.SMS_DESTINATION_PHONE_NUMBERS;

/**
 * Creates test fixtures directly in the database with jOOQ, bypassing the secured services, and reads back what the
 * stories check besides the screen. It works on the schema only, so it does not change when a feature moves from JPA to
 * jOOQ. All services use {@code REQUIRES_NEW} transactions, so tests cannot roll back their changes; instead, every
 * fixture gets a unique name so that tests sharing the same application context do not interfere with each other.
 * <p>
 * IDs come from the same sequences that Hibernate allocates from. Hibernate hands out blocks of IDs ending or starting
 * at the value it gets from a sequence, so a value taken here is never in one of its blocks.
 */
@TestComponent
public class TestData {

    /**
     * A name or description that is not given in Swedish is empty, as the entities store it.
     */
    public record GivenMunicipality(long id, String nameFi, String nameSv) {
    }

    public record GivenAssignmentType(long id, String code, String descriptionFi, String descriptionSv) {
    }

    public record GivenResourceType(long id, String code, String descriptionFi) {
    }

    public record GivenResource(long id, String callSign) {
    }

    public record GivenStatus(ResourceState state, @Nullable Long assignmentId) {
    }

    /**
     * A destination as stored; the phone numbers are empty for a runboard.
     */
    public record GivenDestination(@Nullable String description, Locale locale, Set<String> phoneNumbers) {
    }

    private final DSLContext dsl;

    TestData(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Returns a short string that is unique within the test run, prefixed with the given prefix.
     */
    public static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public GivenMunicipality municipality(String name) {
        return municipality(name, "", true);
    }

    public GivenMunicipality municipality(String nameFi, String nameSv) {
        return municipality(nameFi, nameSv, true);
    }

    public GivenMunicipality deletedMunicipality(String name) {
        return municipality(name, "", false);
    }

    private GivenMunicipality municipality(String nameFi, String nameSv, boolean active) {
        var id = dsl.nextval(MUNICIPALITIES_SEQ);
        dsl.insertInto(MUNICIPALITIES)
                .set(MUNICIPALITIES.ID, id)
                .set(MUNICIPALITIES.VERSION, 0L)
                .set(MUNICIPALITIES.NAME_FI, nameFi)
                .set(MUNICIPALITIES.NAME_SV, nameSv)
                .set(MUNICIPALITIES.ACTIVE, active)
                .execute();
        return new GivenMunicipality(id, nameFi, nameSv);
    }

    public GivenAssignmentType assignmentType(String code, String description) {
        return assignmentType(code, description, "");
    }

    public GivenAssignmentType deletedAssignmentType(String code, String description) {
        var type = assignmentType(code, description);
        deleteAssignmentType(type);
        return type;
    }

    public GivenAssignmentType assignmentType(String code, String descriptionFi, String descriptionSv) {
        var id = dsl.nextval(ASSIGNMENT_TYPES_SEQ);
        dsl.insertInto(ASSIGNMENT_TYPES)
                .set(ASSIGNMENT_TYPES.ID, id)
                .set(ASSIGNMENT_TYPES.VERSION, 0L)
                .set(ASSIGNMENT_TYPES.CODE, code)
                .set(ASSIGNMENT_TYPES.DESCRIPTION_FI, descriptionFi)
                .set(ASSIGNMENT_TYPES.DESCRIPTION_SV, descriptionSv)
                .set(ASSIGNMENT_TYPES.ACTIVE, true)
                .execute();
        return new GivenAssignmentType(id, code, descriptionFi, descriptionSv);
    }

    /**
     * Deletes the assignment type softly, as an administrator does, after it has been put to use.
     */
    public void deleteAssignmentType(GivenAssignmentType type) {
        dsl.update(ASSIGNMENT_TYPES).set(ASSIGNMENT_TYPES.ACTIVE, false)
                .set(ASSIGNMENT_TYPES.VERSION, ASSIGNMENT_TYPES.VERSION.plus(1))
                .where(ASSIGNMENT_TYPES.ID.eq(type.id())).execute();
    }

    /**
     * Deletes the municipality softly, as an administrator does, after it has been put to use.
     */
    public void deleteMunicipality(GivenMunicipality municipality) {
        dsl.update(MUNICIPALITIES).set(MUNICIPALITIES.ACTIVE, false)
                .set(MUNICIPALITIES.VERSION, MUNICIPALITIES.VERSION.plus(1))
                .where(MUNICIPALITIES.ID.eq(municipality.id())).execute();
    }

    /**
     * Deletes the resource softly, as an administrator does, after it has been put to use.
     */
    public void deleteResource(GivenResource resource) {
        dsl.update(RESOURCES).set(RESOURCES.ACTIVE, false)
                .set(RESOURCES.VERSION, RESOURCES.VERSION.plus(1))
                .where(RESOURCES.ID.eq(resource.id())).execute();
    }

    /**
     * An open assignment of urgency A, filled in so that resources can be dispatched to it.
     *
     * @return the assignment's ID
     */
    public long openAssignment(GivenMunicipality municipality, GivenAssignmentType type, String address) {
        var id = dsl.nextval(ASSIGNMENTS_SEQ);
        dsl.insertInto(ASSIGNMENTS)
                .set(ASSIGNMENTS.ID, id)
                .set(ASSIGNMENTS.VERSION, 0L)
                .set(ASSIGNMENTS.OPENED, OffsetDateTime.now())
                .set(ASSIGNMENTS.URGENCY, "A")
                .set(ASSIGNMENTS.TYPE_ID, type.id())
                .set(ASSIGNMENTS.MUNICIPALITY_ID, municipality.id())
                .set(ASSIGNMENTS.ADDRESS, address)
                .set(ASSIGNMENTS.DESCRIPTION, "")
                .execute();
        return id;
    }

    public GivenResourceType resourceType(String code, String description) {
        return resourceType(code, description, true);
    }

    public GivenResourceType deletedResourceType(String code, String description) {
        return resourceType(code, description, false);
    }

    private GivenResourceType resourceType(String code, String description, boolean active) {
        var id = dsl.nextval(RESOURCE_TYPES_SEQ);
        dsl.insertInto(RESOURCE_TYPES)
                .set(RESOURCE_TYPES.ID, id)
                .set(RESOURCE_TYPES.VERSION, 0L)
                .set(RESOURCE_TYPES.CODE, code)
                .set(RESOURCE_TYPES.DESCRIPTION_FI, description)
                .set(RESOURCE_TYPES.DESCRIPTION_SV, "")
                .set(RESOURCE_TYPES.ACTIVE, active)
                .execute();
        return new GivenResourceType(id, code, description);
    }

    /**
     * An active resource of a type of its own, in the given state and on no assignment.
     */
    public GivenResource resource(String callSign, ResourceState initialState) {
        return resource(callSign, initialState, resourceType(unique("RT"), "Test resource type"));
    }

    /**
     * An active resource of the given type, in the given state and on no assignment.
     */
    public GivenResource resource(String callSign, ResourceState initialState, GivenResourceType type) {
        var id = dsl.nextval(RESOURCES_SEQ);
        dsl.insertInto(RESOURCES)
                .set(RESOURCES.ID, id)
                .set(RESOURCES.VERSION, 0L)
                .set(RESOURCES.CALL_SIGN, callSign)
                .set(RESOURCES.TYPE_ID, type.id())
                .set(RESOURCES.ACTIVE, true)
                .execute();
        // Without an assignment, the resource is not assigned; see ResourceStatus
        dsl.insertInto(RESOURCE_STATUS)
                .set(RESOURCE_STATUS.ID, dsl.nextval(RESOURCE_STATUS_SEQ))
                .set(RESOURCE_STATUS.VERSION, 0L)
                .set(RESOURCE_STATUS.TS, OffsetDateTime.now())
                .set(RESOURCE_STATUS.STATE, initialState.name())
                .set(RESOURCE_STATUS.RESOURCE_ID, id)
                .set(RESOURCE_STATUS.AVAILABLE, initialState.isAvailableForNewAssignments())
                .set(RESOURCE_STATUS.ASSIGNED, false)
                .execute();
        return new GivenResource(id, callSign);
    }

    public GivenResource resourceCalled(String callSign) {
        return dsl.select(RESOURCES.ID, RESOURCES.CALL_SIGN)
                .from(RESOURCES)
                .where(RESOURCES.CALL_SIGN.eq(callSign))
                .fetchSingle(record -> new GivenResource(record.value1(), record.value2()));
    }

    public GivenStatus statusOf(GivenResource resource) {
        return dsl.select(RESOURCE_STATUS.STATE, RESOURCE_STATUS.ASSIGNMENT_ID)
                .from(RESOURCE_STATUS)
                .where(RESOURCE_STATUS.RESOURCE_ID.eq(resource.id()))
                .fetchSingle(record -> new GivenStatus(ResourceState.valueOf(record.value1()), record.value2()));
    }

    /**
     * Returns the active destinations that receive the notifications of the given resource.
     */
    public List<GivenDestination> destinationsFor(GivenResource resource) {
        return dsl.select(DESTINATIONS.ID, DESTINATIONS.DESCRIPTION, DESTINATIONS.LOCALE)
                .from(DESTINATIONS)
                .join(DESTINATION_RESOURCES).on(DESTINATION_RESOURCES.DESTINATION_ID.eq(DESTINATIONS.ID))
                .where(DESTINATION_RESOURCES.RESOURCE_ID.eq(resource.id()))
                .and(DESTINATIONS.ACTIVE.isTrue())
                .fetch(record -> new GivenDestination(record.value2(), toLocale(record.value3()),
                        phoneNumbersOf(record.value1())));
    }

    private Set<String> phoneNumbersOf(long destinationId) {
        return dsl.select(SMS_DESTINATION_PHONE_NUMBERS.PHONE_NUMBERS)
                .from(SMS_DESTINATION_PHONE_NUMBERS)
                .where(SMS_DESTINATION_PHONE_NUMBERS.SMS_DESTINATION_ID.eq(destinationId))
                .fetch(SMS_DESTINATION_PHONE_NUMBERS.PHONE_NUMBERS)
                .stream()
                .collect(Collectors.toSet());
    }

    /**
     * Hibernate stores a locale as {@link Locale#toString()}, such as {@code sv} or {@code sv_FI}.
     */
    private static Locale toLocale(String stored) {
        return Locale.forLanguageTag(stored.replace('_', '-'));
    }
}
