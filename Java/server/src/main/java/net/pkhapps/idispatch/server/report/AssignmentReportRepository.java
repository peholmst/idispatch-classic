package net.pkhapps.idispatch.server.report;

import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.jooq.tables.records.AssignmentReportsRecord;
import net.pkhapps.idispatch.server.resource.ResourceId;
import org.jooq.DSLContext;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Optional;

import static net.pkhapps.idispatch.server.jooq.Sequences.ASSIGNMENT_REPORTS_SEQ;
import static net.pkhapps.idispatch.server.jooq.Tables.ASSIGNMENT_REPORTS;
import static net.pkhapps.idispatch.server.jooq.Tables.ASSIGNMENT_REPORT_UNITS;

/**
 * Stores the {@link AssignmentReport}s in the {@code assignment_reports} table, one per assignment, with the unit
 * reports in {@code assignment_report_units}. A second report for the same assignment is refused by the table's unique
 * constraint, which reaches the caller as a {@link org.springframework.dao.DataIntegrityViolationException}.
 */
@Repository
class AssignmentReportRepository {

    private final DSLContext dsl;

    AssignmentReportRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    Optional<AssignmentReport> findByAssignment(AssignmentId assignment) {
        return dsl.selectFrom(ASSIGNMENT_REPORTS)
                .where(ASSIGNMENT_REPORTS.ASSIGNMENT_ID.eq(assignment.value()))
                .fetchOptional()
                .map(this::toReport);
    }

    /**
     * Stores the first version of the report of an assignment.
     *
     * @return the report with its version
     */
    AssignmentReport insert(AssignmentReport report) {
        var id = dsl.nextval(ASSIGNMENT_REPORTS_SEQ);
        var row = dsl.newRecord(ASSIGNMENT_REPORTS);
        row.setId(id);
        row.setVersion(0L);
        row.setAssignmentId(report.assignmentId().value());
        copy(report, row);
        row.insert();
        insertUnits(id, report);
        return findByAssignment(report.assignmentId()).orElseThrow();
    }

    /**
     * Replaces the stored report, provided that its version is still the stored one.
     *
     * @return the report with its new version
     * @throws OptimisticLockingFailureException if the report has been changed since this version
     */
    AssignmentReport update(AssignmentReport report) {
        var row = dsl.newRecord(ASSIGNMENT_REPORTS);
        copy(report, row);
        row.setVersion(report.version() + 1);
        var id = dsl.update(ASSIGNMENT_REPORTS)
                .set(row)
                .where(ASSIGNMENT_REPORTS.ASSIGNMENT_ID.eq(report.assignmentId().value()))
                .and(ASSIGNMENT_REPORTS.VERSION.eq(report.version()))
                .returning(ASSIGNMENT_REPORTS.ID)
                .fetchOptional(ASSIGNMENT_REPORTS.ID)
                .orElseThrow(() -> new OptimisticLockingFailureException(
                        "The report of assignment %s has been changed since version %d".formatted(
                                report.assignmentId().value(), report.version())));
        dsl.deleteFrom(ASSIGNMENT_REPORT_UNITS).where(ASSIGNMENT_REPORT_UNITS.REPORT_ID.eq(id)).execute();
        insertUnits(id, report);
        return findByAssignment(report.assignmentId()).orElseThrow();
    }

    private static void copy(AssignmentReport report, AssignmentReportsRecord row) {
        var scene = report.scene();
        row.setSituationDevelopment(scene.situationDevelopment().value());
        row.setPeopleInvolved(scene.peopleInvolved());
        row.setLightlyInjured(scene.lightlyInjured());
        row.setSeriouslyInjured(scene.seriouslyInjured());
        row.setDeceased(scene.deceased());
        var lastEdit = report.lastEdit();
        row.setLastEditedBySubject(lastEdit == null ? null : lastEdit.subject());
        row.setLastEditedByFirstName(lastEdit == null ? null : lastEdit.firstName());
        row.setLastEditedByLastName(lastEdit == null ? null : lastEdit.lastName());
        row.setLastEditedAt(lastEdit == null ? null : toDatabase(lastEdit.timestamp()));
        var closed = report.closed();
        row.setClosedBySubject(closed == null ? null : closed.subject());
        row.setClosedByFirstName(closed == null ? null : closed.firstName());
        row.setClosedByLastName(closed == null ? null : closed.lastName());
        row.setClosedAt(closed == null ? null : toDatabase(closed.timestamp()));
    }

    private void insertUnits(long reportId, AssignmentReport report) {
        report.units().forEach((resource, unit) -> {
            var lastEdit = unit.lastEdit();
            dsl.insertInto(ASSIGNMENT_REPORT_UNITS)
                    .set(ASSIGNMENT_REPORT_UNITS.REPORT_ID, reportId)
                    .set(ASSIGNMENT_REPORT_UNITS.RESOURCE_ID, resource.value())
                    .set(ASSIGNMENT_REPORT_UNITS.SITUATION_ON_ARRIVAL, unit.answers().situationOnArrival().value())
                    .set(ASSIGNMENT_REPORT_UNITS.ACTIONS_TAKEN, unit.answers().actionsTaken().value())
                    .set(ASSIGNMENT_REPORT_UNITS.LAST_EDITED_BY_SUBJECT, lastEdit == null ? null : lastEdit.subject())
                    .set(ASSIGNMENT_REPORT_UNITS.LAST_EDITED_BY_FIRST_NAME, lastEdit == null ? null : lastEdit.firstName())
                    .set(ASSIGNMENT_REPORT_UNITS.LAST_EDITED_BY_LAST_NAME, lastEdit == null ? null : lastEdit.lastName())
                    .set(ASSIGNMENT_REPORT_UNITS.LAST_EDITED_AT, lastEdit == null ? null : toDatabase(lastEdit.timestamp()))
                    .execute();
        });
    }

    private AssignmentReport toReport(AssignmentReportsRecord row) {
        var units = new HashMap<ResourceId, UnitReport>();
        dsl.selectFrom(ASSIGNMENT_REPORT_UNITS)
                .where(ASSIGNMENT_REPORT_UNITS.REPORT_ID.eq(row.getId()))
                .forEach(unit -> units.put(new ResourceId(unit.getResourceId()), new UnitReport(
                        new UnitAnswers(new ReportText(unit.getSituationOnArrival()), new ReportText(unit.getActionsTaken())),
                        stamp(unit.getLastEditedBySubject(), unit.getLastEditedByFirstName(),
                                unit.getLastEditedByLastName(), unit.getLastEditedAt()))));
        var scene = new SceneDetails(new ReportText(row.getSituationDevelopment()), row.getPeopleInvolved(),
                row.getLightlyInjured(), row.getSeriouslyInjured(), row.getDeceased());
        return new AssignmentReport(new AssignmentId(row.getAssignmentId()),
                row.getVersion(), scene,
                stamp(row.getLastEditedBySubject(), row.getLastEditedByFirstName(), row.getLastEditedByLastName(),
                        row.getLastEditedAt()),
                stamp(row.getClosedBySubject(), row.getClosedByFirstName(), row.getClosedByLastName(),
                        row.getClosedAt()),
                units);
    }

    private static @Nullable UserStamp stamp(@Nullable String subject, @Nullable String firstName,
                                             @Nullable String lastName, @Nullable OffsetDateTime at) {
        return subject == null || at == null ? null : new UserStamp(subject, firstName, lastName, at.toInstant());
    }

    private static OffsetDateTime toDatabase(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
