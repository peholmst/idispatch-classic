package net.pkhapps.idispatch.server.dispatch;

import net.pkhapps.idispatch.server.TestSecurityConfiguration;
import net.pkhapps.idispatch.server.TestcontainersConfiguration;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.security.WithKeycloakUser;
import net.pkhapps.idispatch.server.shared.Bilingual;
import net.pkhapps.idispatch.server.shared.audit.AuditLog;
import net.pkhapps.idispatch.server.shared.audit.AuditTrailAssert;
import net.pkhapps.idispatch.server.shared.audit.AuditUser;
import net.pkhapps.idispatch.server.shared.audit.AuditedEntity;
import net.pkhapps.idispatch.server.ui.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import static net.pkhapps.idispatch.server.shared.audit.AuditEventKind.MOBILE_UNIT_CHANGED;
import static net.pkhapps.idispatch.server.shared.audit.AuditTrailAssert.assertEvents;
import static net.pkhapps.idispatch.server.shared.audit.AuditTrailAssert.kindsOf;
import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What moving a mobile phone to another unit relies on beyond what the stories show: that every move is in the audit
 * log, that a move to the unit it is already on is not, that a deleted phone or unit is refused, and that only
 * dispatchers may move phones.
 */
@SpringBootTest
@Import({TestData.class, TestcontainersConfiguration.class, TestSecurityConfiguration.class})
class MobileUnitServiceTest {

    private static final AuditUser DORA = new AuditUser("subject-dora", "Dora", "Dispatcher");

    @Autowired
    MobileUnitService service;
    @Autowired
    TestData given;
    @Autowired
    AuditLog auditLog;

    @Test
    @WithKeycloakUser(username = "dora", firstName = "Dora", lastName = "Dispatcher", roles = "DISPATCHER")
    void everyMoveIsRecordedAsTheDispatchers() {
        var first = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var second = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var mobile = given.mobile(first, Bilingual.FINNISH);
        var since = AuditTrailAssert.now();

        service.changeUnit(new DestinationId(mobile.id()), new ResourceId(second.id()));
        service.changeUnit(new DestinationId(mobile.id()), new ResourceId(first.id()));

        assertEvents(auditLog, audited(mobile), DORA, since, MOBILE_UNIT_CHANGED, MOBILE_UNIT_CHANGED);
        assertThat(given.mobileKeyHashesFor(first)).as("The phone keeps its key").hasSize(1);
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    void movingAPhoneToTheUnitItIsOnIsNotRecorded() {
        var resource = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var mobile = given.mobile(resource, Bilingual.FINNISH);

        service.changeUnit(new DestinationId(mobile.id()), new ResourceId(resource.id()));

        assertThat(kindsOf(auditLog, audited(mobile))).isEmpty();
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    void deletedPhoneIsNotMoved() {
        var mobile = given.mobile(given.resource(unique("CS"), ResourceState.AVAILABLE), Bilingual.FINNISH);
        var other = given.resource(unique("CS"), ResourceState.AVAILABLE);
        given.deleteDestination(mobile.id());

        assertThatThrownBy(() -> service.changeUnit(new DestinationId(mobile.id()), new ResourceId(other.id())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(given.mobileKeyHashesFor(other)).isEmpty();
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    void phoneIsNotMovedToADeletedUnit() {
        var resource = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var mobile = given.mobile(resource, Bilingual.FINNISH);
        var deleted = given.resource(unique("CS"), ResourceState.AVAILABLE);
        given.deleteResource(deleted);

        assertThatThrownBy(() -> service.changeUnit(new DestinationId(mobile.id()), new ResourceId(deleted.id())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(given.mobileKeyHashesFor(resource)).hasSize(1);
        assertThat(kindsOf(auditLog, audited(mobile))).isEmpty();
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    void otherDestinationsAreNotMovedOrListed() {
        var resource = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var runboard = given.runboard(resource, Bilingual.FINNISH);
        var other = given.resource(unique("CS"), ResourceState.AVAILABLE);

        assertThatThrownBy(() -> service.changeUnit(new DestinationId(runboard.id()), new ResourceId(other.id())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(service.findPhones()).extracting(MobilePhoneListItem::id)
                .doesNotContain(new DestinationId(runboard.id()));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "REPORT_READER", "REPORT_WRITER", "REPORT_REVIEWER"})
    void onlyDispatchersMovePhones() {
        var mobile = given.mobile(given.resource(unique("CS"), ResourceState.AVAILABLE), Bilingual.FINNISH);
        var other = given.resource(unique("CS"), ResourceState.AVAILABLE);

        assertThatThrownBy(() -> service.changeUnit(new DestinationId(mobile.id()), new ResourceId(other.id())))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.findPhones()).isInstanceOf(AccessDeniedException.class);
    }

    private static AuditedEntity audited(TestData.GivenMobile mobile) {
        return new AuditedEntity(AuditedEntity.Type.DESTINATION, mobile.id());
    }
}
