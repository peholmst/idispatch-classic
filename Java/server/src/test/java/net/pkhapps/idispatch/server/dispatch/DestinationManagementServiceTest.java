package net.pkhapps.idispatch.server.dispatch;

import net.pkhapps.idispatch.server.TestSecurityConfiguration;
import net.pkhapps.idispatch.server.TestcontainersConfiguration;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.shared.Bilingual;
import net.pkhapps.idispatch.server.ui.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Set;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What the administration view relies on the destination service for, beyond what the stories show: that two
 * administrators cannot overwrite each other's changes, that runboard keys stay unique, and that each kind of
 * destination keeps its own details.
 */
@SpringBootTest
@Import({TestData.class, TestcontainersConfiguration.class, TestSecurityConfiguration.class})
@WithMockUser(roles = "ADMIN")
class DestinationManagementServiceTest {

    @Autowired
    DestinationManagementService service;
    @Autowired
    TestData testData;

    private ResourceId resource;

    @BeforeEach
    void createResource() {
        resource = new ResourceId(testData.resource(unique("CS"), ResourceState.AVAILABLE).id());
    }

    @Test
    void changeBasedOnAnOutdatedVersionIsRefused() {
        var created = (RunboardDestination) service.create(runboard(unique("key-"), "Original"));
        var changedByOther = service.update(created.withDetails(runboard(created.runboardKey().value(), "First")));

        assertThat(changedByOther.version()).isEqualTo(created.version() + 1);
        assertThatThrownBy(() -> service.update(created.withDetails(runboard(created.runboardKey().value(), "Second"))))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(service.findById(created.id()).orElseThrow().description())
                .isEqualTo(new DestinationDescription("First"));
    }

    @Test
    void runboardKeyInUseIsRefused() {
        var key = unique("key-");
        service.create(runboard(key, "First"));

        assertThatThrownBy(() -> service.create(runboard(key, "Second")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void smsDestinationKeepsItsNumbers_andChangingThemReplacesThem() {
        var created = (SmsDestination) service.create(sms(Set.of(new PhoneNumber("+3581"), new PhoneNumber("+3582"))));

        var changed = (SmsDestination) service.update(created.withDetails(sms(Set.of(new PhoneNumber("+3583")))));

        assertThat(created.phoneNumbers()).containsExactlyInAnyOrder(new PhoneNumber("+3581"), new PhoneNumber("+3582"));
        assertThat(changed.phoneNumbers()).containsExactly(new PhoneNumber("+3583"));
        assertThat(changed.resources()).containsExactly(resource);
    }

    @Test
    void deletingAndRestoringChangesTheVersion() {
        var created = service.create(runboard(unique("key-"), "Lifecycle"));

        service.delete(created.id());
        var deleted = service.findById(created.id()).orElseThrow();
        service.restore(created.id());
        var restored = service.findById(created.id()).orElseThrow();

        assertThat(deleted.active()).isFalse();
        assertThat(restored.active()).isTrue();
        assertThat(restored.version()).isEqualTo(created.version() + 2);
    }

    private RunboardDestinationDetails runboard(String key, String description) {
        return new RunboardDestinationDetails(new DestinationDescription(description), Bilingual.FINNISH,
                Set.of(resource), new RunboardKey(key));
    }

    private SmsDestinationDetails sms(Set<PhoneNumber> numbers) {
        return new SmsDestinationDetails(null, Bilingual.SWEDISH, Set.of(resource), numbers);
    }
}
