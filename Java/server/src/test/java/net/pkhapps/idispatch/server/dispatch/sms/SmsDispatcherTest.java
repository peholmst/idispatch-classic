package net.pkhapps.idispatch.server.dispatch.sms;

import net.pkhapps.idispatch.server.assignment.Assignment;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.assignment.AssignmentUrgency;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeId;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeListItem;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeQueries;
import net.pkhapps.idispatch.server.dispatch.DispatchNotification;
import net.pkhapps.idispatch.server.dispatch.DispatchNotificationId;
import net.pkhapps.idispatch.server.municipality.MunicipalityId;
import net.pkhapps.idispatch.server.municipality.MunicipalityListItem;
import net.pkhapps.idispatch.server.municipality.MunicipalityQueries;
import net.pkhapps.idispatch.server.resource.ResourceQueries;
import net.pkhapps.idispatch.server.shared.Bilingual;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmsDispatcherTest {

    private static final MunicipalityListItem PARAINEN =
            new MunicipalityListItem(new MunicipalityId(1), "Parainen", "Pargas", true);
    private static final AssignmentTypeListItem BUILDING_FIRE =
            new AssignmentTypeListItem(new AssignmentTypeId(2), "103", "Rakennuspalo", "", true);

    @Test
    void messageText_hasTheMunicipalityInTheLanguageOfTheDestination() {
        var assignment = new Assignment(new AssignmentId(3), 1, Instant.now(), null, AssignmentUrgency.A,
                BUILDING_FIRE.id(), PARAINEN.id(), "Kyrkoesplanaden 1", "Smoke");
        var notification = DispatchNotification.of(new DispatchNotificationId(4), Instant.now(), assignment, Set.of(), Set.of());

        var municipalities = mock(MunicipalityQueries.class);
        when(municipalities.findById(PARAINEN.id())).thenReturn(Optional.of(PARAINEN));
        var assignmentTypes = mock(AssignmentTypeQueries.class);
        when(assignmentTypes.findById(BUILDING_FIRE.id())).thenReturn(Optional.of(BUILDING_FIRE));
        var resources = mock(ResourceQueries.class);
        when(resources.findByIds(Set.of())).thenReturn(List.of());

        var dispatcher = new SmsDispatcher(municipalities, assignmentTypes, resources);

        assertThat(dispatcher.buildMessageText(notification, Bilingual.FINNISH)).startsWith("Parainen,103A,");
        assertThat(dispatcher.buildMessageText(notification, Bilingual.SWEDISH)).startsWith("Pargas,103A,");
    }
}
