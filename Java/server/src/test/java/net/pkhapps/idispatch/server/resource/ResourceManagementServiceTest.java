package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.TestSecurityConfiguration;
import net.pkhapps.idispatch.server.TestcontainersConfiguration;
import net.pkhapps.idispatch.server.shared.BilingualText;
import org.junit.jupiter.api.BeforeEach;
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
 * What the administration view relies on the resource service for, beyond what the stories show: that two
 * administrators cannot overwrite each other's changes, and that call signs stay unique.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestSecurityConfiguration.class})
@WithMockUser(roles = "ADMIN")
class ResourceManagementServiceTest {

    @Autowired
    ResourceManagementService service;
    @Autowired
    ResourceTypeManagementService typeService;

    private ResourceTypeId type;

    @BeforeEach
    void createType() {
        type = typeService.create(new ResourceTypeDetails(new ResourceTypeCode(unique("RT")),
                new BilingualText("Ambulance", ""))).id();
    }

    @Test
    void changeBasedOnAnOutdatedVersionIsRefused() {
        var created = service.create(details(unique("CS")));
        var firstChange = unique("CS");
        var changedByOther = service.update(created.withDetails(details(firstChange)));

        assertThat(changedByOther.version()).isEqualTo(created.version() + 1);
        assertThatThrownBy(() -> service.update(created.withDetails(details(unique("CS")))))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(service.findById(created.id()).orElseThrow().callSign().value()).isEqualTo(firstChange);
    }

    @Test
    void callSignInUseIsRefused() {
        var callSign = unique("CS");
        service.create(details(callSign));

        assertThatThrownBy(() -> service.create(details(callSign)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingAndRestoringChangesTheVersion() {
        var created = service.create(details(unique("CS")));

        service.delete(created.id());
        var deleted = service.findById(created.id()).orElseThrow();
        service.restore(created.id());
        var restored = service.findById(created.id()).orElseThrow();

        assertThat(deleted.active()).isFalse();
        assertThat(restored.active()).isTrue();
        assertThat(restored.version()).isEqualTo(created.version() + 2);
    }

    private ResourceDetails details(String callSign) {
        return new ResourceDetails(new CallSign(callSign), type);
    }
}
