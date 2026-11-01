package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.TestSecurityConfiguration;
import net.pkhapps.idispatch.server.TestcontainersConfiguration;
import net.pkhapps.idispatch.server.ui.RunAs;
import net.pkhapps.idispatch.server.ui.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;

import java.util.function.Supplier;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A unit's mobile app may set the state of its own unit by hand, as a dispatcher may for any unit, but nothing else.
 */
@SpringBootTest
@Import({TestData.class, TestcontainersConfiguration.class, TestSecurityConfiguration.class})
class ResourceStatusServiceUnitTest {

    @Autowired
    ResourceStatusService service;
    @Autowired
    TestData given;

    @Test
    void unitSetsTheStateOfItsOwnResourceByHand() {
        var resource = new ResourceId(given.resource(unique("CS"), ResourceState.AT_STATION).id());

        var status = asUnitOf(resource, () -> service.setResourceStateManually(resource, ResourceState.AVAILABLE));

        assertThat(status).hasValueSatisfying(saved -> assertThat(saved.state()).isEqualTo(ResourceState.AVAILABLE));
    }

    @Test
    void unitMayNotSetTheStateOfAnotherResource() {
        var own = new ResourceId(given.resource(unique("CS"), ResourceState.AT_STATION).id());
        var other = new ResourceId(given.resource(unique("CS"), ResourceState.AT_STATION).id());

        assertThatThrownBy(() -> asUnitOf(own, () -> service.setResourceStateManually(other, ResourceState.AVAILABLE)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unitMayNotUseTheDispatchersOtherStatusChanges() {
        var own = new ResourceId(given.resource(unique("CS"), ResourceState.AT_STATION).id());

        assertThatThrownBy(() -> asUnitOf(own, () -> {
            service.setResourceState(own, ResourceState.AVAILABLE);
            return null;
        })).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void stateThatMayNotBeSetByHandIsRefused() {
        var given = this.given.resource(unique("CS"), ResourceState.AVAILABLE);
        var resource = new ResourceId(given.id());

        var status = asUnitOf(resource, () -> service.setResourceStateManually(resource, ResourceState.RESERVED));

        assertThat(status).isEmpty();
        assertThat(this.given.stateOf(given)).isEqualTo(ResourceState.AVAILABLE);
    }

    private static <T> T asUnitOf(ResourceId resource, Supplier<T> action) {
        return RunAs.unit(resource.value(), action);
    }
}
