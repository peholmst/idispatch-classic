package net.pkhapps.idispatch.server.assignment;

import net.pkhapps.idispatch.server.TestSecurityConfiguration;
import net.pkhapps.idispatch.server.TestcontainersConfiguration;
import net.pkhapps.idispatch.server.security.WithKeycloakUser;
import net.pkhapps.idispatch.server.shared.UpdateResult;
import net.pkhapps.idispatch.server.shared.audit.AuditEvent;
import net.pkhapps.idispatch.server.shared.audit.AuditLog;
import net.pkhapps.idispatch.server.shared.audit.AuditTrailAssert;
import net.pkhapps.idispatch.server.shared.audit.AuditUser;
import net.pkhapps.idispatch.server.shared.audit.AuditedEntity;
import net.pkhapps.idispatch.server.ui.RunAs;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static net.pkhapps.idispatch.server.jooq.Tables.AUDIT_EVENTS;
import static net.pkhapps.idispatch.server.shared.audit.AuditEventKind.CLOSED;
import static net.pkhapps.idispatch.server.shared.audit.AuditEventKind.CREATED;
import static net.pkhapps.idispatch.server.shared.audit.AuditEventKind.EDITED;
import static net.pkhapps.idispatch.server.shared.audit.AuditTrailAssert.kindsOf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the assignment service records in the audit log. The dispatcher's form saves every field as soon as it changes,
 * so a dispatcher's edits close together are recorded as one edit, at the time of the last; an edit after any other
 * event, such as opening the assignment, starts a new one.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestSecurityConfiguration.class})
@WithKeycloakUser(username = "dora", firstName = "Dora", lastName = "Dispatcher", roles = "DISPATCHER")
class AssignmentServiceAuditTest {

    private static final AuditUser DORA = new AuditUser("subject-dora", "Dora", "Dispatcher");

    @Autowired
    AssignmentService service;
    @Autowired
    AuditLog auditLog;
    @Autowired
    DSLContext dsl;

    @Test
    void openingEditingAndClosingAreRecordedWithWhoDidItAndWhen() {
        var since = AuditTrailAssert.now();
        var id = service.openAssignment();
        save(load(id).withAddress("Storgatan 1"));
        service.closeAssignment(id);

        AuditTrailAssert.assertEvents(auditLog, audited(id), DORA, since, CLOSED, EDITED, CREATED);
    }

    @Test
    void editsByTheSameDispatcherCloseTogetherAreOneEdit() {
        var id = service.openAssignment();
        save(load(id).withAddress("Storgatan 1"));
        var first = latest(id);

        save(load(id).withDescription("Smoke from the roof"));

        assertThat(kindsOf(auditLog, audited(id))).containsExactly(EDITED, CREATED);
        assertThat(latest(id).timestamp()).as("Moved forward").isAfter(first.timestamp());
    }

    @Test
    void editsByAnotherDispatcherAreNotMerged() {
        var id = service.openAssignment();
        save(load(id).withAddress("Storgatan 1"));

        RunAs.dispatcher(() -> save(load(id).withDescription("Smoke from the roof")));

        var events = auditLog.findEvents(audited(id));
        assertThat(events).extracting(AuditEvent::kind).containsExactly(EDITED, EDITED, CREATED);
        assertThat(events.getFirst().user().subject()).isEqualTo("fixture");
        assertThat(events.get(1).user()).isEqualTo(DORA);
    }

    @Test
    void editAfterTheMergeWindowIsANewEdit() {
        var id = service.openAssignment();
        save(load(id).withAddress("Storgatan 1"));
        // As if it all happened longer ago than the merge window
        var shift = AssignmentService.EDIT_MERGE_WINDOW.plusSeconds(1);
        dsl.selectFrom(AUDIT_EVENTS)
                .where(AUDIT_EVENTS.ENTITY_TYPE.eq(AuditedEntity.Type.ASSIGNMENT.name()))
                .and(AUDIT_EVENTS.ENTITY_ID.eq(id.value()))
                .forEach(event -> {
                    event.setOccurredAt(event.getOccurredAt().minus(shift));
                    event.update();
                });

        save(load(id).withDescription("Smoke from the roof"));

        assertThat(kindsOf(auditLog, audited(id))).containsExactly(EDITED, EDITED, CREATED);
    }

    @Test
    void savesThatChangeNothingAndRefusedSavesAreNotRecorded() {
        var id = service.openAssignment();
        var opened = load(id);

        var unchanged = service.updateAssignment(opened);
        save(opened.withAddress("Storgatan 1"));
        var stale = service.updateAssignment(opened.withAddress("Kungsgatan 2"));

        assertThat(unchanged).isInstanceOf(UpdateResult.Success.class);
        assertThat(unchanged.getData()).contains(opened);
        assertThat(stale).isInstanceOf(UpdateResult.Conflict.class);
        assertThat(kindsOf(auditLog, audited(id))).containsExactly(EDITED, CREATED);
    }

    private Assignment load(AssignmentId id) {
        return service.findAssignment(id).orElseThrow();
    }

    private Assignment save(Assignment assignment) {
        var result = service.updateAssignment(assignment);
        assertThat(result).isInstanceOf(UpdateResult.Success.class);
        return result.getData().orElseThrow();
    }

    private AuditEvent latest(AssignmentId id) {
        return auditLog.findLatest(audited(id)).orElseThrow();
    }

    private static AuditedEntity audited(AssignmentId id) {
        return new AuditedEntity(AuditedEntity.Type.ASSIGNMENT, id.value());
    }
}
