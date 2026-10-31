package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.jooq.tables.records.ResourceStatusArchiveRecord;
import net.pkhapps.idispatch.server.jooq.tables.records.ResourceStatusRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static net.pkhapps.idispatch.server.jooq.Sequences.RESOURCE_STATUS_ARCHIVE_SEQ;
import static net.pkhapps.idispatch.server.jooq.Sequences.RESOURCE_STATUS_SEQ;
import static net.pkhapps.idispatch.server.jooq.Tables.RESOURCES;
import static net.pkhapps.idispatch.server.jooq.Tables.RESOURCE_STATUS;
import static net.pkhapps.idispatch.server.jooq.Tables.RESOURCE_STATUS_ARCHIVE;

/**
 * Stores the current {@link ResourceStatus} of each resource in the {@code resource_status} table, one row per resource,
 * and the {@link ArchivedResourceStatus}es in {@code resource_status_archive}. The {@code available} and
 * {@code assigned} columns are written from {@link ResourceStatus#isAvailable()} and {@link ResourceStatus#isAssigned()}
 * so that the lists can be queried by them.
 */
@Repository
class ResourceStatusRepository {

    private final DSLContext dsl;

    ResourceStatusRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Finds the status of the resource, provided that the resource has not been deleted.
     */
    Optional<ResourceStatus> findOfActiveResource(ResourceId resource) {
        return findOfActiveResources(RESOURCE_STATUS.RESOURCE_ID.eq(resource.value())).stream().findFirst();
    }

    /**
     * Finds the statuses of the resources not deleted, ordered by call sign.
     *
     * @param availableOnly whether to leave out the resources that are not available for new assignments
     */
    List<ResourceStatus> findOfActiveResources(boolean availableOnly) {
        return findOfActiveResources(availableOnly ? RESOURCE_STATUS.AVAILABLE.isTrue() : DSL.noCondition());
    }

    /**
     * Finds the statuses of the resources that are on the assignment.
     *
     * @see ResourceStatus#isAssigned()
     */
    List<ResourceStatus> findAssignedTo(AssignmentId assignment) {
        return dsl.selectFrom(RESOURCE_STATUS)
                .where(RESOURCE_STATUS.ASSIGNMENT_ID.eq(assignment.value()))
                .and(RESOURCE_STATUS.ASSIGNED.isTrue())
                .orderBy(RESOURCE_STATUS.ID)
                .fetch(ResourceStatusRepository::toStatus);
    }

    /**
     * Finds the statuses that refer to the assignment, whether the resources are still on it or not.
     */
    List<ResourceStatus> findReferringTo(AssignmentId assignment) {
        return dsl.selectFrom(RESOURCE_STATUS)
                .where(RESOURCE_STATUS.ASSIGNMENT_ID.eq(assignment.value()))
                .orderBy(RESOURCE_STATUS.ID)
                .fetch(ResourceStatusRepository::toStatus);
    }

    /**
     * Stores the first status of a new resource.
     */
    void insert(ResourceStatus status) {
        dsl.insertInto(RESOURCE_STATUS)
                .set(RESOURCE_STATUS.ID, dsl.nextval(RESOURCE_STATUS_SEQ))
                .set(RESOURCE_STATUS.VERSION, status.version())
                .set(RESOURCE_STATUS.RESOURCE_ID, status.resourceId().value())
                .set(RESOURCE_STATUS.STATE, status.state().name())
                .set(RESOURCE_STATUS.ASSIGNMENT_ID, assignmentId(status.assignment()))
                .set(RESOURCE_STATUS.TS, toDatabase(status.timestamp()))
                .set(RESOURCE_STATUS.AVAILABLE, status.isAvailable())
                .set(RESOURCE_STATUS.ASSIGNED, status.isAssigned())
                .execute();
    }

    /**
     * Replaces the status of the resource, provided that its version is still the stored one.
     *
     * @return the status with its new version
     * @throws OptimisticLockingFailureException if the status has been changed since this version
     */
    ResourceStatus update(ResourceStatus status) {
        var updated = dsl.update(RESOURCE_STATUS)
                .set(RESOURCE_STATUS.STATE, status.state().name())
                .set(RESOURCE_STATUS.ASSIGNMENT_ID, assignmentId(status.assignment()))
                .set(RESOURCE_STATUS.TS, toDatabase(status.timestamp()))
                .set(RESOURCE_STATUS.AVAILABLE, status.isAvailable())
                .set(RESOURCE_STATUS.ASSIGNED, status.isAssigned())
                .set(RESOURCE_STATUS.VERSION, status.version() + 1)
                .where(RESOURCE_STATUS.RESOURCE_ID.eq(status.resourceId().value()))
                .and(RESOURCE_STATUS.VERSION.eq(status.version()))
                .execute();
        if (updated == 0) {
            throw new OptimisticLockingFailureException(
                    "The status of resource %s has been changed since version %d".formatted(
                            status.resourceId().value(), status.version()));
        }
        return new ResourceStatus(status.resourceId(), status.version() + 1, status.state(), status.assignment(),
                status.timestamp());
    }

    void archive(ArchivedResourceStatus status) {
        dsl.insertInto(RESOURCE_STATUS_ARCHIVE)
                .set(RESOURCE_STATUS_ARCHIVE.ID, dsl.nextval(RESOURCE_STATUS_ARCHIVE_SEQ))
                .set(RESOURCE_STATUS_ARCHIVE.RESOURCE_ID, status.resourceId().value())
                .set(RESOURCE_STATUS_ARCHIVE.STATE, status.state().name())
                .set(RESOURCE_STATUS_ARCHIVE.ASSIGNMENT_ID, assignmentId(status.assignment()))
                .set(RESOURCE_STATUS_ARCHIVE.TS, toDatabase(status.timestamp()))
                .execute();
    }

    /**
     * Finds the state that the resource had before its current one, if any.
     */
    Optional<ArchivedResourceStatus> findLatestArchived(ResourceId resource) {
        return dsl.selectFrom(RESOURCE_STATUS_ARCHIVE)
                .where(RESOURCE_STATUS_ARCHIVE.RESOURCE_ID.eq(resource.value()))
                .orderBy(RESOURCE_STATUS_ARCHIVE.TS.desc(), RESOURCE_STATUS_ARCHIVE.ID.desc())
                .limit(1)
                .fetchOptional(ResourceStatusRepository::toArchived);
    }

    /**
     * Finds the past states of every resource that has been on the assignment, oldest first.
     */
    List<ArchivedResourceStatus> findArchivedOn(AssignmentId assignment) {
        return dsl.selectFrom(RESOURCE_STATUS_ARCHIVE)
                .where(RESOURCE_STATUS_ARCHIVE.ASSIGNMENT_ID.eq(assignment.value()))
                .orderBy(RESOURCE_STATUS_ARCHIVE.TS, RESOURCE_STATUS_ARCHIVE.ID)
                .fetch(ResourceStatusRepository::toArchived);
    }

    private List<ResourceStatus> findOfActiveResources(Condition condition) {
        return dsl.select(RESOURCE_STATUS.fields())
                .from(RESOURCE_STATUS)
                .join(RESOURCES).on(RESOURCES.ID.eq(RESOURCE_STATUS.RESOURCE_ID))
                .where(RESOURCES.ACTIVE.isTrue())
                .and(condition)
                .orderBy(RESOURCES.CALL_SIGN)
                .fetch(row -> toStatus(row.into(RESOURCE_STATUS)));
    }

    private static @Nullable Long assignmentId(@Nullable AssignmentId assignment) {
        return assignment == null ? null : assignment.value();
    }

    private static @Nullable AssignmentId assignmentId(@Nullable Long assignment) {
        return assignment == null ? null : new AssignmentId(assignment);
    }

    private static OffsetDateTime toDatabase(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private static ResourceStatus toStatus(ResourceStatusRecord row) {
        return new ResourceStatus(new ResourceId(row.getResourceId()), row.getVersion(),
                ResourceState.valueOf(row.getState()), assignmentId(row.getAssignmentId()), row.getTs().toInstant());
    }

    private static ArchivedResourceStatus toArchived(ResourceStatusArchiveRecord row) {
        return new ArchivedResourceStatus(new ResourceId(row.getResourceId()), ResourceState.valueOf(row.getState()),
                assignmentId(row.getAssignmentId()), row.getTs().toInstant());
    }
}
