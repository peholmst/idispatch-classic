package net.pkhapps.idispatch.server.assignmenttype;

import net.pkhapps.idispatch.server.TestSecurityConfiguration;
import net.pkhapps.idispatch.server.TestcontainersConfiguration;
import net.pkhapps.idispatch.server.shared.BilingualText;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import net.pkhapps.idispatch.server.security.WithKeycloakUser;
import net.pkhapps.idispatch.server.shared.audit.AuditLog;
import net.pkhapps.idispatch.server.shared.audit.AuditTrailAssert;
import net.pkhapps.idispatch.server.shared.audit.AuditedEntity;
import org.springframework.security.test.context.support.WithMockUser;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static net.pkhapps.idispatch.server.shared.audit.AuditEventKind.CREATED;
import static net.pkhapps.idispatch.server.shared.audit.AuditEventKind.DELETED;
import static net.pkhapps.idispatch.server.shared.audit.AuditEventKind.EDITED;
import static net.pkhapps.idispatch.server.shared.audit.AuditEventKind.RESTORED;
import static net.pkhapps.idispatch.server.shared.audit.AuditTrailAssert.ANNA;
import static net.pkhapps.idispatch.server.shared.audit.AuditTrailAssert.assertEvents;
import static net.pkhapps.idispatch.server.shared.audit.AuditTrailAssert.kindsOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What the administration view relies on the assignment type service for, beyond what the stories show: that two
 * administrators cannot overwrite each other's changes, and that codes stay unique.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestSecurityConfiguration.class})
@WithMockUser(roles = "ADMIN")
class AssignmentTypeManagementServiceTest {

    @Autowired
    AssignmentTypeManagementService service;
    @Autowired
    AuditLog auditLog;

    @Test
    void changeBasedOnAnOutdatedVersionIsRefused() {
        var created = service.create(details(unique("AT"), "Original"));
        var changedByOther = service.update(created.withDetails(details(created.code().value(), "First change")));

        assertThat(changedByOther.version()).isEqualTo(created.version() + 1);
        assertThatThrownBy(() -> service.update(created.withDetails(details(created.code().value(), "Second change"))))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(service.findById(created.id()).orElseThrow().description().finnish()).isEqualTo("First change");
    }

    @Test
    void codeInUseIsRefused() {
        var code = unique("AT");
        service.create(details(code, "First"));

        assertThatThrownBy(() -> service.create(details(code, "Second")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingAndRestoringChangesTheVersion() {
        var created = service.create(details(unique("AT"), "Lifecycle"));

        service.delete(created.id());
        var deleted = service.findById(created.id()).orElseThrow();
        service.restore(created.id());
        var restored = service.findById(created.id()).orElseThrow();

        assertThat(deleted.active()).isFalse();
        assertThat(restored.active()).isTrue();
        assertThat(restored.version()).isEqualTo(created.version() + 2);
    }

    @Test
    @WithKeycloakUser(username = "anna", firstName = "Anna", lastName = "Admin", roles = "ADMIN")
    void everyChangeIsRecordedWithWhoMadeItAndWhen() {
        var since = AuditTrailAssert.now();
        var created = service.create(details(unique("A"), "Audited"));
        service.update(created.withDetails(details(created.code().value(), "Edited")));
        service.delete(created.id());
        service.restore(created.id());

        assertEvents(auditLog, audited(created.id().value()), ANNA, since, RESTORED, DELETED, EDITED, CREATED);
    }

    @Test
    void savesThatChangeNothingAndRefusedChangesAreNotRecorded() {
        var created = service.create(details(unique("Q"), "Quiet"));
        var unchanged = service.update(created.withDetails(created.details()));
        var edited = service.update(created.withDetails(details(created.code().value(), "Edited")));
        assertThatThrownBy(() -> service.update(created.withDetails(details(created.code().value(), "Stale"))))
                .isInstanceOf(OptimisticLockingFailureException.class);
        service.restore(created.id());
        service.delete(created.id());
        service.delete(created.id());

        assertThat(unchanged.version()).as("Nothing was saved").isEqualTo(created.version());
        assertThat(edited.version()).isEqualTo(created.version() + 1);
        assertThat(kindsOf(auditLog, audited(created.id().value())))
                .as("Not restored when active, nor deleted twice").containsExactly(DELETED, EDITED, CREATED);
    }

    private static AssignmentTypeDetails details(String code, String finnishDescription) {
        return new AssignmentTypeDetails(new AssignmentTypeCode(code), new BilingualText(finnishDescription, ""));
    }

    private static AuditedEntity audited(long id) {
        return new AuditedEntity(AuditedEntity.Type.ASSIGNMENT_TYPE, id);
    }
}
