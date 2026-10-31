package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.TestSecurityConfiguration;
import net.pkhapps.idispatch.server.TestcontainersConfiguration;
import net.pkhapps.idispatch.server.shared.BilingualText;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.test.context.support.WithMockUser;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What the administration view relies on the resource type service for, beyond what the stories show: that two
 * administrators cannot overwrite each other's changes, and that codes stay unique.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestSecurityConfiguration.class})
@WithMockUser(roles = "ADMIN")
class ResourceTypeManagementServiceTest {

    @Autowired
    ResourceTypeManagementService service;

    @Test
    void changeBasedOnAnOutdatedVersionIsRefused() {
        var created = service.create(details(unique("RT"), "Original"));
        var changedByOther = service.update(created.withDetails(details(created.code().value(), "First change")));

        assertThat(changedByOther.version()).isEqualTo(created.version() + 1);
        assertThatThrownBy(() -> service.update(created.withDetails(details(created.code().value(), "Second change"))))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(service.findById(created.id()).orElseThrow().description().finnish()).isEqualTo("First change");
    }

    @Test
    void codeInUseIsRefused() {
        var code = unique("RT");
        service.create(details(code, "First"));

        assertThatThrownBy(() -> service.create(details(code, "Second")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingAndRestoringChangesTheVersion() {
        var created = service.create(details(unique("RT"), "Lifecycle"));

        service.delete(created.id());
        var deleted = service.findById(created.id()).orElseThrow();
        service.restore(created.id());
        var restored = service.findById(created.id()).orElseThrow();

        assertThat(deleted.active()).isFalse();
        assertThat(restored.active()).isTrue();
        assertThat(restored.version()).isEqualTo(created.version() + 2);
    }

    private static ResourceTypeDetails details(String code, String finnishDescription) {
        return new ResourceTypeDetails(new ResourceTypeCode(code), new BilingualText(finnishDescription, ""));
    }
}
