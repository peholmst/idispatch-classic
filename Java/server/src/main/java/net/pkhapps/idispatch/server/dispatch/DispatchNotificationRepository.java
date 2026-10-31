package net.pkhapps.idispatch.server.dispatch;

import net.pkhapps.idispatch.server.assignment.Assignment;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.assignment.AssignmentUrgency;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeId;
import net.pkhapps.idispatch.server.jooq.tables.records.DispatchNotificationsRecord;
import net.pkhapps.idispatch.server.municipality.MunicipalityId;
import net.pkhapps.idispatch.server.resource.ResourceId;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static net.pkhapps.idispatch.server.jooq.Sequences.DISPATCH_NOTIFICATIONS_SEQ;
import static net.pkhapps.idispatch.server.jooq.Sequences.RECEIPTS_SEQ;
import static net.pkhapps.idispatch.server.jooq.Tables.DISPATCH_NOTIFICATIONS;
import static net.pkhapps.idispatch.server.jooq.Tables.DISPATCH_NOTIFICATION_ASSIGNED_RESOURCES;
import static net.pkhapps.idispatch.server.jooq.Tables.DISPATCH_NOTIFICATION_DESTINATIONS;
import static net.pkhapps.idispatch.server.jooq.Tables.RECEIPTS;

/**
 * Stores the {@link DispatchNotification}s in the {@code dispatch_notifications} table, with their resources and
 * destinations in join tables, and the {@link Receipt}s in {@code receipts}. A second receipt from the same destination
 * for the same notification is refused by the table's unique constraint.
 */
@Repository
class DispatchNotificationRepository {

    private final DSLContext dsl;

    DispatchNotificationRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Stores a new notification for the assignment, sent now.
     */
    DispatchNotification insert(Assignment assignment, Set<ResourceId> assignedResources,
                                Set<DestinationId> destinations) {
        var notification = DispatchNotification.of(new DispatchNotificationId(dsl.nextval(DISPATCH_NOTIFICATIONS_SEQ)),
                Instant.now(), assignment, assignedResources, destinations);
        var id = notification.id().value();
        dsl.insertInto(DISPATCH_NOTIFICATIONS)
                .set(DISPATCH_NOTIFICATIONS.ID, id)
                .set(DISPATCH_NOTIFICATIONS.TS, toDatabase(notification.timestamp()))
                .set(DISPATCH_NOTIFICATIONS.ASSIGNMENT_ID, notification.assignmentId().value())
                .set(DISPATCH_NOTIFICATIONS.ASSIGNMENT_MUNICIPALITY_ID, notification.municipalityId().value())
                .set(DISPATCH_NOTIFICATIONS.ASSIGNMENT_ADDRESS, notification.address())
                .set(DISPATCH_NOTIFICATIONS.ASSIGNMENT_TYPE_ID, notification.assignmentTypeId().value())
                .set(DISPATCH_NOTIFICATIONS.ASSIGNMENT_URGENCY, notification.urgency().name())
                .set(DISPATCH_NOTIFICATIONS.ASSIGNMENT_DESCRIPTION, notification.description())
                .execute();
        for (var resource : notification.assignedResources()) {
            dsl.insertInto(DISPATCH_NOTIFICATION_ASSIGNED_RESOURCES)
                    .set(DISPATCH_NOTIFICATION_ASSIGNED_RESOURCES.DISPATCH_NOTIFICATION_ID, id)
                    .set(DISPATCH_NOTIFICATION_ASSIGNED_RESOURCES.RESOURCE_ID, resource.value())
                    .execute();
        }
        for (var destination : notification.destinations()) {
            dsl.insertInto(DISPATCH_NOTIFICATION_DESTINATIONS)
                    .set(DISPATCH_NOTIFICATION_DESTINATIONS.DISPATCH_NOTIFICATION_ID, id)
                    .set(DISPATCH_NOTIFICATION_DESTINATIONS.DESTINATION_ID, destination.value())
                    .execute();
        }
        return notification;
    }

    /**
     * Finds the notifications sent for the assignment, oldest first.
     */
    List<DispatchNotification> findByAssignment(AssignmentId assignment) {
        return dsl.selectFrom(DISPATCH_NOTIFICATIONS)
                .where(DISPATCH_NOTIFICATIONS.ASSIGNMENT_ID.eq(assignment.value()))
                .orderBy(DISPATCH_NOTIFICATIONS.TS, DISPATCH_NOTIFICATIONS.ID)
                .fetch(this::toNotification);
    }

    /**
     * Stores that the destination received the notification now.
     *
     * @throws org.springframework.dao.DataIntegrityViolationException if the destination has already confirmed it
     */
    Receipt insertReceipt(DestinationId destination, DispatchNotificationId notification) {
        var receipt = new Receipt(destination, notification, Instant.now());
        dsl.insertInto(RECEIPTS)
                .set(RECEIPTS.ID, dsl.nextval(RECEIPTS_SEQ))
                .set(RECEIPTS.TS, toDatabase(receipt.timestamp()))
                .set(RECEIPTS.DESTINATION_ID, destination.value())
                .set(RECEIPTS.NOTIFICATION_ID, notification.value())
                .execute();
        return receipt;
    }

    List<Receipt> findReceipts(DispatchNotificationId notification) {
        return dsl.selectFrom(RECEIPTS)
                .where(RECEIPTS.NOTIFICATION_ID.eq(notification.value()))
                .orderBy(RECEIPTS.TS, RECEIPTS.ID)
                .fetch(row -> new Receipt(new DestinationId(row.getDestinationId()),
                        new DispatchNotificationId(row.getNotificationId()), row.getTs().toInstant()));
    }

    private DispatchNotification toNotification(DispatchNotificationsRecord row) {
        var resources = dsl.select(DISPATCH_NOTIFICATION_ASSIGNED_RESOURCES.RESOURCE_ID)
                .from(DISPATCH_NOTIFICATION_ASSIGNED_RESOURCES)
                .where(DISPATCH_NOTIFICATION_ASSIGNED_RESOURCES.DISPATCH_NOTIFICATION_ID.eq(row.getId()))
                .fetch(DISPATCH_NOTIFICATION_ASSIGNED_RESOURCES.RESOURCE_ID)
                .stream().map(ResourceId::new).collect(Collectors.toSet());
        var destinations = dsl.select(DISPATCH_NOTIFICATION_DESTINATIONS.DESTINATION_ID)
                .from(DISPATCH_NOTIFICATION_DESTINATIONS)
                .where(DISPATCH_NOTIFICATION_DESTINATIONS.DISPATCH_NOTIFICATION_ID.eq(row.getId()))
                .fetch(DISPATCH_NOTIFICATION_DESTINATIONS.DESTINATION_ID)
                .stream().map(DestinationId::new).collect(Collectors.toSet());
        return new DispatchNotification(new DispatchNotificationId(row.getId()), row.getTs().toInstant(),
                new AssignmentId(row.getAssignmentId()), new MunicipalityId(row.getAssignmentMunicipalityId()),
                row.getAssignmentAddress(), new AssignmentTypeId(row.getAssignmentTypeId()),
                AssignmentUrgency.valueOf(row.getAssignmentUrgency()), row.getAssignmentDescription(), resources,
                destinations);
    }

    private static OffsetDateTime toDatabase(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
