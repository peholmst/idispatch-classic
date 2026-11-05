package net.pkhapps.idispatch.server.municipality;

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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What the administration view relies on the municipality service for, beyond what the stories show: that two
 * administrators cannot overwrite each other's changes, and that names stay unique in each language.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestSecurityConfiguration.class})
@WithMockUser(roles = "ADMIN")
class MunicipalityManagementServiceTest {

    @Autowired
    MunicipalityManagementService service;
    @Autowired
    AuditLog auditLog;

    @Test
    void changeBasedOnAnOutdatedVersionIsRefused() {
        var created = service.create(details(unique("Original "), ""));
        var changedByOther = service.update(created.withDetails(details(unique("First "), "")));

        assertThat(changedByOther.version()).isEqualTo(created.version() + 1);
        assertThatThrownBy(() -> service.update(created.withDetails(details(unique("Second "), ""))))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(service.findById(created.id()).orElseThrow().name()).isEqualTo(changedByOther.name());
    }

    @Test
    void nameInUseIsRefusedInEitherLanguage() {
        var finnish = unique("Parainen ");
        var swedish = unique("Pargas ");
        service.create(details(finnish, swedish));

        assertThatThrownBy(() -> service.create(details(finnish, "")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> service.create(details("", swedish)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void municipalitiesWithoutASwedishNameDoNotClash() {
        service.create(details(unique("Only Finnish "), ""));

        assertThatCode(() -> service.create(details(unique("Also only Finnish "), ""))).doesNotThrowAnyException();
    }

    @Test
    @WithKeycloakUser(username = "anna", firstName = "Anna", lastName = "Admin", roles = "ADMIN")
    void everyChangeIsRecordedWithWhoMadeItAndWhen() {
        var since = AuditTrailAssert.now();
        var created = service.create(details(unique("Audited "), ""));
        service.update(created.withDetails(details(unique("Edited "), "")));
        service.delete(created.id());
        service.restore(created.id());

        assertEvents(auditLog, audited(created.id().value()), ANNA, since, RESTORED, DELETED, EDITED, CREATED);
    }

    @Test
    void savesThatChangeNothingAndRefusedChangesAreNotRecorded() {
        var created = service.create(details(unique("Quiet "), ""));
        var unchanged = service.update(created.withDetails(created.details()));
        var edited = service.update(created.withDetails(details(unique("Edited "), "")));
        assertThatThrownBy(() -> service.update(created.withDetails(details(unique("Stale "), ""))))
                .isInstanceOf(OptimisticLockingFailureException.class);
        service.restore(created.id());
        service.delete(created.id());
        service.delete(created.id());

        assertThat(unchanged.version()).as("Nothing was saved").isEqualTo(created.version());
        assertThat(edited.version()).isEqualTo(created.version() + 1);
        assertThat(kindsOf(auditLog, audited(created.id().value())))
                .as("Not restored when active, nor deleted twice").containsExactly(DELETED, EDITED, CREATED);
    }

    private static MunicipalityDetails details(String finnish, String swedish) {
        return new MunicipalityDetails(new BilingualText(finnish, swedish));
    }

    private static AuditedEntity audited(long id) {
        return new AuditedEntity(AuditedEntity.Type.MUNICIPALITY, id);
    }
}
