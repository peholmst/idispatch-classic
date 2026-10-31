package net.pkhapps.idispatch.server.dashboard;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static net.pkhapps.idispatch.server.jooq.Tables.ASSIGNMENTS;
import static net.pkhapps.idispatch.server.jooq.Tables.ASSIGNMENT_REPORTS;
import static net.pkhapps.idispatch.server.jooq.Tables.RESOURCES;
import static net.pkhapps.idispatch.server.jooq.Tables.RESOURCE_STATUS;
import static net.pkhapps.idispatch.server.jooq.Tables.RESOURCE_STATUS_ARCHIVE;

/**
 * Reads the dashboard's figures straight from the tables of the assignments, their reports and the units' statuses.
 * <p>
 * A unit's history is its archived statuses and its current one, each in effect from its timestamp until the next. A
 * unit has been on an assignment during a window if any status in effect during the window refers to the assignment:
 * one that started within the window, or the one the unit was in when the window started.
 */
@Repository
class DashboardQuery {

    private final DSLContext dsl;

    DashboardQuery(DSLContext dsl) {
        this.dsl = dsl;
    }

    int countActiveAssignments() {
        return dsl.fetchCount(ASSIGNMENTS, ASSIGNMENTS.CLOSED.isNull());
    }

    /**
     * Counts the closed assignments whose report no reviewer has closed, as the reports list them as open.
     */
    int countOpenReports() {
        return dsl.fetchCount(ASSIGNMENTS, ASSIGNMENTS.CLOSED.isNotNull().andNotExists(DSL.selectOne()
                .from(ASSIGNMENT_REPORTS)
                .where(ASSIGNMENT_REPORTS.ASSIGNMENT_ID.eq(ASSIGNMENTS.ID))
                .and(ASSIGNMENT_REPORTS.CLOSED_BY_SUBJECT.isNotNull())));
    }

    /**
     * Returns every unit that has not been deleted with the number of different assignments it has been on since the
     * given moment, the most used first and by call sign among equals.
     */
    List<UnitUtilization> findUnitUtilization(Instant since) {
        var from = since.atOffset(ZoneOffset.UTC);

        var history = DSL.select(RESOURCE_STATUS_ARCHIVE.RESOURCE_ID.as("resource_id"),
                        RESOURCE_STATUS_ARCHIVE.ASSIGNMENT_ID.as("assignment_id"), RESOURCE_STATUS_ARCHIVE.TS.as("ts"))
                .from(RESOURCE_STATUS_ARCHIVE)
                .unionAll(DSL.select(RESOURCE_STATUS.RESOURCE_ID, RESOURCE_STATUS.ASSIGNMENT_ID, RESOURCE_STATUS.TS)
                        .from(RESOURCE_STATUS))
                .asTable("history");
        var resourceId = history.field("resource_id", Long.class);
        var assignmentId = history.field("assignment_id", Long.class);
        var ts = history.field("ts", OffsetDateTime.class);

        // The status each unit was in when the window started
        var atStart = DSL.selectDistinct(resourceId, assignmentId).on(resourceId)
                .from(history)
                .where(ts.lt(from))
                .orderBy(resourceId, ts.desc());
        var onAssignments = DSL.select(resourceId, assignmentId)
                .from(history)
                .where(ts.ge(from))
                .unionAll(atStart)
                .asTable("on_assignments");
        var unit = onAssignments.field(resourceId);
        var count = DSL.countDistinct(onAssignments.field(assignmentId)).as("assignments");
        var counts = DSL.select(unit, count)
                .from(onAssignments)
                .groupBy(unit)
                .asTable("counts");

        var assignments = DSL.coalesce(counts.field(count), 0);
        return dsl.select(RESOURCES.ID, RESOURCES.CALL_SIGN, assignments)
                .from(RESOURCES)
                .leftJoin(counts).on(counts.field(unit).eq(RESOURCES.ID))
                .where(RESOURCES.ACTIVE.isTrue())
                .orderBy(assignments.desc(), RESOURCES.CALL_SIGN)
                .fetch(row -> new UnitUtilization(row.value1(), row.value2(), row.value3()));
    }
}
