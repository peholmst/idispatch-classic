package net.pkhapps.idispatch.server.report;

import net.pkhapps.idispatch.server.TestSecurityConfiguration;
import net.pkhapps.idispatch.server.TestcontainersConfiguration;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.security.WithKeycloakUser;
import net.pkhapps.idispatch.server.shared.UpdateResult;
import net.pkhapps.idispatch.server.shared.audit.AuditEvent;
import net.pkhapps.idispatch.server.shared.audit.AuditLog;
import net.pkhapps.idispatch.server.shared.audit.AuditTrailAssert;
import net.pkhapps.idispatch.server.shared.audit.AuditUser;
import net.pkhapps.idispatch.server.shared.audit.AuditedEntity;
import net.pkhapps.idispatch.server.ui.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Map;

import static net.pkhapps.idispatch.server.shared.audit.AuditEventKind.CLOSED;
import static net.pkhapps.idispatch.server.shared.audit.AuditEventKind.CREATED;
import static net.pkhapps.idispatch.server.shared.audit.AuditEventKind.EDITED;
import static net.pkhapps.idispatch.server.shared.audit.AuditEventKind.REOPENED;
import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * What the report service records in the audit log: the first save creates the report, every save an edit of each part
 * it changed, the scene or a unit's report, and reviewers close and re-open it. Saves that change nothing, and saves
 * refused because the report changed or was closed meanwhile, record nothing.
 */
@SpringBootTest
@Import({TestData.class, HandledAssignments.class, TestcontainersConfiguration.class, TestSecurityConfiguration.class})
@WithKeycloakUser(username = "rasmus", firstName = "Rasmus", lastName = "Reporter",
        roles = {"REPORT_WRITER", "REPORT_REVIEWER"})
class ReportServiceAuditTest {

    private static final AuditUser RASMUS = new AuditUser("subject-rasmus", "Rasmus", "Reporter");

    @Autowired
    ReportService service;
    @Autowired
    HandledAssignments handled;
    @Autowired
    AuditLog auditLog;

    @Test
    void everyChangeIsRecordedWithThePartChangedAndWhoChangedItAndWhen() {
        var report = loadReportOf(handled.closedAssignment(unique("CS")));
        var unit = report.getRespondingResources().getFirst().id();
        var since = AuditTrailAssert.now();

        var saved = success(service.saveReport(report.getReport(), draft("Fire", unit, "Smoke")));
        var edited = success(service.saveReport(saved, draft("Fire spread to the attic", unit, "Smoke")));
        var closed = success(service.closeReport(edited));
        success(service.reopenReport(closed));

        var events = auditLog.findEvents(audited(report));
        assertThat(events).extracting(AuditEvent::kind, AuditEvent::part).containsExactly(
                tuple(REOPENED, null),
                tuple(CLOSED, null),
                tuple(EDITED, ReportPart.SCENE),
                tuple(EDITED, ReportPart.unit(unit)),
                tuple(EDITED, ReportPart.SCENE),
                tuple(CREATED, null));
        var until = AuditTrailAssert.now();
        assertThat(events).allSatisfy(event -> {
            assertThat(event.user()).isEqualTo(RASMUS);
            assertThat(event.timestamp()).isBetween(since, until);
        });
    }

    @Test
    void changesThatChangeNothingOrAreRefusedAreNotRecorded() {
        var report = loadReportOf(handled.closedAssignment(unique("CS")));
        var unit = report.getRespondingResources().getFirst().id();

        var nothingWritten = service.saveReport(report.getReport(), draft("", unit, ""));
        var saved = success(service.saveReport(report.getReport(), draft("Fire", unit, "")));
        var unchanged = service.saveReport(saved, draft("Fire", unit, ""));
        var stale = service.saveReport(report.getReport(), draft("Stale", unit, ""));
        service.reopenReport(saved);
        var closed = success(service.closeReport(saved));
        service.closeReport(closed);
        var closedMeanwhile = service.saveReport(closed, draft("Too late", unit, ""));

        assertThat(nothingWritten.getData().orElseThrow().isNew()).as("Nothing was saved").isTrue();
        assertThat(unchanged.getData().orElseThrow().version()).isEqualTo(saved.version());
        assertThat(stale).isInstanceOf(UpdateResult.Conflict.class);
        assertThat(closedMeanwhile).isInstanceOf(UpdateResult.NoChange.class);
        assertThat(auditLog.findEvents(audited(report))).extracting(AuditEvent::kind)
                .containsExactly(CLOSED, EDITED, CREATED);
    }

    private ReportDTO loadReportOf(HandledAssignments.Handled assignment) {
        return service.findReportByAssignmentId(assignment.assignment().id().value()).orElseThrow();
    }

    private static AssignmentReport success(UpdateResult<AssignmentReport> result) {
        assertThat(result).isInstanceOf(UpdateResult.Success.class);
        return result.getData().orElseThrow();
    }

    private static ReportDraft draft(String situationDevelopment, ResourceId unit, String actionsTaken) {
        return new ReportDraft(new SceneDetails(new ReportText(situationDevelopment), null, null, null, null),
                Map.of(unit, new UnitAnswers(new ReportText(""), new ReportText(actionsTaken))));
    }

    private static AuditedEntity audited(ReportDTO report) {
        return new AuditedEntity(AuditedEntity.Type.ASSIGNMENT_REPORT, report.getNumber());
    }
}
