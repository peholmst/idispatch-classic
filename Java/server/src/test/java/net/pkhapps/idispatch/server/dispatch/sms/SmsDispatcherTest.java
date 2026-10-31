package net.pkhapps.idispatch.server.dispatch.sms;

import net.pkhapps.idispatch.server.assignment.Assignment;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.assignment.AssignmentUrgency;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeId;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeListItem;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeQueries;
import net.pkhapps.idispatch.server.dispatch.DispatchNotification;
import net.pkhapps.idispatch.server.municipality.MunicipalityId;
import net.pkhapps.idispatch.server.municipality.MunicipalityListItem;
import net.pkhapps.idispatch.server.municipality.MunicipalityQueries;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceListItem;
import net.pkhapps.idispatch.server.resource.ResourceQueries;
import net.pkhapps.idispatch.server.shared.Bilingual;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SmsDispatcherTest {

    private static final MunicipalityListItem PARAINEN =
            new MunicipalityListItem(new MunicipalityId(1), "Parainen", "Pargas", true);
    private static final AssignmentTypeListItem BUILDING_FIRE =
            new AssignmentTypeListItem(new AssignmentTypeId(2), "103", "Rakennuspalo", "", true);

    @Test
    void messageText_hasTheMunicipalityInTheLanguageOfTheDestination() {
        var assignment = new Assignment(new AssignmentId(3), 1, Instant.now(), null, AssignmentUrgency.A,
                BUILDING_FIRE.id(), PARAINEN.id(), "Kyrkoesplanaden 1", "Smoke");
        var notification = new DispatchNotification(assignment, Set.of(), Set.of());

        var dispatcher = new SmsDispatcher(new OneMunicipality(), new OneAssignmentType(), new NoResources());

        assertThat(dispatcher.buildMessageText(notification, Bilingual.FINNISH)).startsWith("Parainen,103A,");
        assertThat(dispatcher.buildMessageText(notification, Bilingual.SWEDISH)).startsWith("Pargas,103A,");
    }

    private static final class OneMunicipality implements MunicipalityQueries {

        @Override
        public List<MunicipalityListItem> findActive() {
            return List.of(PARAINEN);
        }

        @Override
        public Optional<MunicipalityListItem> findById(MunicipalityId id) {
            return Optional.of(PARAINEN).filter(municipality -> municipality.id().equals(id));
        }

        @Override
        public List<MunicipalityListItem> findByIds(Collection<MunicipalityId> ids) {
            return ids.contains(PARAINEN.id()) ? List.of(PARAINEN) : List.of();
        }
    }

    private static final class OneAssignmentType implements AssignmentTypeQueries {

        @Override
        public List<AssignmentTypeListItem> findActive() {
            return List.of(BUILDING_FIRE);
        }

        @Override
        public Optional<AssignmentTypeListItem> findById(AssignmentTypeId id) {
            return Optional.of(BUILDING_FIRE).filter(type -> type.id().equals(id));
        }

        @Override
        public List<AssignmentTypeListItem> findByIds(Collection<AssignmentTypeId> ids) {
            return ids.contains(BUILDING_FIRE.id()) ? List.of(BUILDING_FIRE) : List.of();
        }
    }

    private static final class NoResources implements ResourceQueries {

        @Override
        public List<ResourceListItem> findActive() {
            return List.of();
        }

        @Override
        public List<ResourceListItem> findByIds(Collection<ResourceId> ids) {
            return List.of();
        }
    }
}
