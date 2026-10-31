package net.pkhapps.idispatch.server.dispatch.sms;

import net.pkhapps.idispatch.server.assignment.Assignment;
import net.pkhapps.idispatch.server.assignment.AssignmentUrgency;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentType;
import net.pkhapps.idispatch.server.dispatch.DispatchNotification;
import net.pkhapps.idispatch.server.municipality.Municipality;
import net.pkhapps.idispatch.server.shared.Bilingual;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SmsDispatcherTest {

    @Test
    void messageText_hasTheMunicipalityInTheLanguageOfTheDestination() {
        var municipality = new Municipality();
        municipality.setNameFi("Parainen");
        municipality.setNameSv("Pargas");
        var type = new AssignmentType();
        type.setCode("103");
        type.setDescriptionFi("Rakennuspalo");
        var assignment = new Assignment();
        assignment.setMunicipality(municipality);
        assignment.setType(type);
        assignment.setUrgency(AssignmentUrgency.A);
        assignment.setAddress("Kyrkoesplanaden 1");
        assignment.setDescription("Smoke");
        var notification = new DispatchNotification(assignment, Set.of(), Set.of());

        var dispatcher = new SmsDispatcher();

        assertThat(dispatcher.buildMessageText(notification, Bilingual.FINNISH)).startsWith("Parainen,103A,");
        assertThat(dispatcher.buildMessageText(notification, Bilingual.SWEDISH)).startsWith("Pargas,103A,");
    }
}
