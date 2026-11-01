package net.pkhapps.idispatch.server.assignment;

import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeId;
import net.pkhapps.idispatch.server.jooq.tables.records.AssignmentsRecord;
import net.pkhapps.idispatch.server.municipality.MunicipalityId;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record10;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static net.pkhapps.idispatch.server.jooq.Sequences.ASSIGNMENTS_SEQ;
import static net.pkhapps.idispatch.server.jooq.Tables.ASSIGNMENTS;
import static net.pkhapps.idispatch.server.jooq.Tables.ASSIGNMENT_TYPES;
import static net.pkhapps.idispatch.server.jooq.Tables.MUNICIPALITIES;

/**
 * Stores the {@link Assignment}s in the {@code assignments} table.
 */
@Repository
class AssignmentRepository {

    private final DSLContext dsl;

    AssignmentRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Stores a new, empty assignment opened now.
     */
    Assignment insertNew() {
        var assignment = new Assignment(new AssignmentId(dsl.nextval(ASSIGNMENTS_SEQ)), 0L, Instant.now(), null,
                AssignmentUrgency.N, null, null, "", "");
        dsl.insertInto(ASSIGNMENTS)
                .set(ASSIGNMENTS.ID, assignment.id().value())
                .set(ASSIGNMENTS.VERSION, assignment.version())
                .set(ASSIGNMENTS.OPENED, toDatabase(assignment.opened()))
                .set(ASSIGNMENTS.URGENCY, assignment.urgency().name())
                .set(ASSIGNMENTS.DESCRIPTION, assignment.description())
                .set(ASSIGNMENTS.ADDRESS, assignment.address())
                .execute();
        return assignment;
    }

    Optional<Assignment> findById(AssignmentId id) {
        return dsl.selectFrom(ASSIGNMENTS)
                .where(ASSIGNMENTS.ID.eq(id.value()))
                .fetchOptional(AssignmentRepository::toAssignment);
    }

    /**
     * Saves what a dispatcher has filled in, provided that the version is still the stored one and the assignment is
     * still open.
     *
     * @return the assignment with its new version
     * @throws OptimisticLockingFailureException if the assignment has been changed since this version, closed, or
     *                                           removed
     */
    Assignment update(Assignment assignment) {
        var updated = dsl.update(ASSIGNMENTS)
                .set(ASSIGNMENTS.URGENCY, assignment.urgency().name())
                .set(ASSIGNMENTS.TYPE_ID, assignment.typeId() == null ? null : assignment.typeId().value())
                .set(ASSIGNMENTS.MUNICIPALITY_ID,
                        assignment.municipalityId() == null ? null : assignment.municipalityId().value())
                .set(ASSIGNMENTS.ADDRESS, assignment.address())
                .set(ASSIGNMENTS.DESCRIPTION, assignment.description())
                .set(ASSIGNMENTS.VERSION, assignment.version() + 1)
                .where(ASSIGNMENTS.ID.eq(assignment.id().value()))
                .and(ASSIGNMENTS.VERSION.eq(assignment.version()))
                .and(ASSIGNMENTS.CLOSED.isNull())
                .execute();
        if (updated == 0) {
            throw new OptimisticLockingFailureException(
                    "Assignment %s has been changed, closed or removed since version %d".formatted(
                            assignment.id().value(), assignment.version()));
        }
        return findById(assignment.id()).orElseThrow();
    }

    /**
     * Closes the assignment now, unless it is already closed.
     *
     * @return the closed assignment, or empty if there was no open assignment to close
     */
    Optional<Assignment> close(AssignmentId id) {
        var updated = dsl.update(ASSIGNMENTS)
                .set(ASSIGNMENTS.CLOSED, toDatabase(Instant.now()))
                .set(ASSIGNMENTS.VERSION, ASSIGNMENTS.VERSION.plus(1))
                .where(ASSIGNMENTS.ID.eq(id.value()))
                .and(ASSIGNMENTS.CLOSED.isNull())
                .execute();
        return updated == 0 ? Optional.empty() : findById(id);
    }

    /**
     * Finds the open assignments, oldest first.
     */
    List<AssignmentListItem> findOpenListItems() {
        return findListItems(ASSIGNMENTS.CLOSED.isNull());
    }

    List<AssignmentListItem> findListItems(Collection<AssignmentId> ids) {
        return findListItems(ASSIGNMENTS.ID.in(ids.stream().map(AssignmentId::value).toList()));
    }

    private List<AssignmentListItem> findListItems(Condition condition) {
        return dsl.select(ASSIGNMENTS.ID, ASSIGNMENTS.OPENED, ASSIGNMENTS.CLOSED, ASSIGNMENTS.URGENCY,
                        ASSIGNMENT_TYPES.CODE, ASSIGNMENT_TYPES.DESCRIPTION_FI, ASSIGNMENT_TYPES.DESCRIPTION_SV,
                        MUNICIPALITIES.NAME_FI, MUNICIPALITIES.NAME_SV, ASSIGNMENTS.ADDRESS)
                .from(ASSIGNMENTS)
                .leftJoin(ASSIGNMENT_TYPES).on(ASSIGNMENT_TYPES.ID.eq(ASSIGNMENTS.TYPE_ID))
                .leftJoin(MUNICIPALITIES).on(MUNICIPALITIES.ID.eq(ASSIGNMENTS.MUNICIPALITY_ID))
                .where(condition)
                .orderBy(ASSIGNMENTS.ID)
                .fetch(AssignmentRepository::toListItem);
    }

    private static AssignmentListItem toListItem(
            Record10<Long, OffsetDateTime, OffsetDateTime, String, String, String, String, String, String, String> row) {
        return new AssignmentListItem(new AssignmentId(row.value1()), row.value2().toInstant(),
                row.value3() == null ? null : row.value3().toInstant(), AssignmentUrgency.valueOf(row.value4()),
                row.value5(), row.value6(), row.value7(), row.value8(), row.value9(), row.value10());
    }

    private static Assignment toAssignment(AssignmentsRecord row) {
        return new Assignment(new AssignmentId(row.getId()), row.getVersion(), row.getOpened().toInstant(),
                row.getClosed() == null ? null : row.getClosed().toInstant(),
                AssignmentUrgency.valueOf(row.getUrgency()),
                row.getTypeId() == null ? null : new AssignmentTypeId(row.getTypeId()),
                row.getMunicipalityId() == null ? null : new MunicipalityId(row.getMunicipalityId()),
                row.getAddress(), row.getDescription());
    }

    private static OffsetDateTime toDatabase(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
