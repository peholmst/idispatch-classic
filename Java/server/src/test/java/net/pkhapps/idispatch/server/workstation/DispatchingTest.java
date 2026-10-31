package net.pkhapps.idispatch.server.workstation;

import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.ui.TestData.GivenResource;
import net.pkhapps.idispatch.server.ui.UseCaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A dispatcher opens an assignment for an emergency call, fills it in, assigns resources to it, dispatches them, follows
 * their states and closes the assignment when the resources are free again.
 */
class DispatchingTest extends UseCaseTest {

    private String fire;
    private String town;

    @BeforeEach
    void givenAnAssignmentTypeAndAMunicipality() {
        var code = unique("T");
        given.assignmentType(code, "Fire");
        fire = code + " - Fire";
        town = given.municipality(unique("Town ")).nameFi();
    }

    /**
     * A dispatcher who logs in and opens the dispatcher view, after the resources of the story are in place.
     */
    private Dispatcher dispatcherAtWork() {
        return new Dispatcher(user("dispatcher", "DISPATCHER"));
    }

    @Test
    void dispatcherStartsWithNoAssignmentSelected() {
        var dispatcher = dispatcherAtWork();
        assertThat(dispatcher.isToldNoAssignmentIsSelected()).isTrue();
        assertThat(dispatcher.canOpenANewAssignment()).isTrue();
    }

    @Test
    void dispatcherOpensANewAssignment() {
        var dispatcher = dispatcherAtWork();
        var number = dispatcher.opensANewAssignment();

        assertThat(dispatcher.hasSelected(number)).isTrue();
        assertThat(dispatcher.seesAnAssignmentToEdit()).isTrue();
        assertThat(dispatcher.urgency()).as("New assignments are not urgent").isEqualTo("N");
    }

    @Test
    void dispatcherFillsInTheAssignmentAndItIsSavedRightAway() {
        var dispatcher = dispatcherAtWork();
        var number = dispatcher.opensANewAssignment();

        dispatcher.choosesType(fire);
        dispatcher.choosesUrgency("A");
        dispatcher.choosesMunicipality(town);
        dispatcher.entersAddress("Main Street 1");

        assertThat(dispatcher.assignmentInList(number)).containsAllEntriesOf(Map.of(
                "Type", fire,
                "Urgency", "A",
                "Municipality", town,
                "Address", "Main Street 1"));
    }

    @Test
    void typesAndMunicipalitiesAddedAfterOpeningTheViewAreOfferedForTheNextAssignment() {
        var dispatcher = dispatcherAtWork();
        dispatcher.opensANewAssignment();
        var code = unique("T");
        given.assignmentType(code, "Added later");
        var laterTown = given.municipality(unique("Town ")).nameFi();

        dispatcher.opensANewAssignment();

        assertThat(dispatcher.typeChoices()).contains(code + " - Added later");
        assertThat(dispatcher.municipalityChoices()).contains(laterTown);
    }

    @Test
    void typeAndMunicipalityDeletedAfterTheyWereChosenStayOnTheAssignment() {
        var code = unique("T");
        var retiredType = given.assignmentType(code, "Retired type");
        var retiredTown = given.municipality(unique("Retired town "));
        var dispatcher = dispatcherAtWork();
        var number = dispatcher.opensANewAssignment();
        dispatcher.choosesType(code + " - Retired type");
        dispatcher.choosesMunicipality(retiredTown.nameFi());
        given.deleteAssignmentType(retiredType);
        given.deleteMunicipality(retiredTown);

        dispatcher.opensANewAssignment();
        dispatcher.selectsAssignment(number);

        assertThat(dispatcher.type()).isEqualTo(code + " - Retired type");
        assertThat(dispatcher.municipality()).isEqualTo(retiredTown.nameFi());
        assertThat(dispatcher.typeChoices()).as("Not offered for others").doesNotContain(code + " - Retired type");
        assertThat(dispatcher.municipalityChoices()).doesNotContain(retiredTown.nameFi());
        assertThat(dispatcher.assignmentInList(number)).containsEntry("Type", code + " - Retired type")
                .containsEntry("Municipality", retiredTown.nameFi());
    }

    @Test
    void englishSpeakingDispatcherIsOfferedTheFinnishNames() {
        var code = unique("T");
        given.assignmentType(code, "Rakennuspalo", "Byggnadsbrand");
        var parainen = unique("Parainen ");
        given.municipality(parainen, unique("Pargas "));
        var korpo = unique("Korpo ");
        given.municipality("", korpo);
        var dispatcher = dispatcherAtWork();

        dispatcher.opensANewAssignment();

        assertThat(dispatcher.typeChoices()).contains(code + " - Rakennuspalo");
        assertThat(dispatcher.municipalityChoices()).as("Swedish when there is no Finnish name")
                .contains(parainen, korpo);
    }

    @Test
    void swedishSpeakingDispatcherIsOfferedTheSwedishNames() {
        var code = unique("T");
        given.assignmentType(code, "Rakennuspalo", "Byggnadsbrand");
        var pargas = unique("Pargas ");
        given.municipality(unique("Parainen "), pargas);
        var dispatcher = new Dispatcher(keycloakUser("sv-dispatcher", Map.of("locale", "sv"), "DISPATCHER"),
                DispatcherPage.Language.SWEDISH);

        dispatcher.opensANewAssignment();

        assertThat(dispatcher.typeChoices()).contains(code + " - Byggnadsbrand");
        assertThat(dispatcher.municipalityChoices()).contains(pargas);
    }

    @Test
    void dispatcherSwitchesBetweenAssignments() {
        var dispatcher = dispatcherAtWork();
        var first = dispatcher.opensANewAssignment();
        dispatcher.entersAddress("First Street");
        dispatcher.opensANewAssignment();
        dispatcher.entersAddress("Second Street");

        dispatcher.selectsAssignment(first);

        assertThat(dispatcher.address()).isEqualTo("First Street");
    }

    @Test
    void dispatcherReservesAnAvailableResourceForTheAssignment() {
        var resource = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var dispatcher = dispatcherAtWork();
        var number = dispatcher.opensANewAssignment();

        dispatcher.addsResource(resource.callSign());

        assertThat(dispatcher.resourcesOnAssignment()).containsExactly(resource.callSign());
        assertThat(dispatcher.stateOf(resource.callSign())).isEqualTo("Reserved");
        assertThat(dispatcher.assignmentOf(resource.callSign())).endsWith("(" + number + ")");
        assertThat(dispatcher.availableResources()).doesNotContain(resource.callSign());
        assertThat(stateOf(resource)).isEqualTo(ResourceState.RESERVED);
    }

    @Test
    void resourcesThatAreOutOfServiceAreNotOffered() {
        var resource = given.resource(unique("CS"), ResourceState.OUT_OF_SERVICE);

        var dispatcher = dispatcherAtWork();
        dispatcher.opensANewAssignment();

        assertThat(dispatcher.availableResources()).doesNotContain(resource.callSign());
    }

    @Test
    void dispatcherReleasesAResourceFromTheAssignment() {
        var resource = given.resource(unique("CS"), ResourceState.AT_STATION);
        var dispatcher = dispatcherAtWork();
        dispatcher.opensANewAssignment();
        dispatcher.addsResource(resource.callSign());
        assertThat(dispatcher.canRemoveResources()).as("Nothing selected yet").isFalse();

        dispatcher.removesResource(resource.callSign());

        assertThat(dispatcher.resourcesOnAssignment()).isEmpty();
        assertThat(stateOf(resource)).isEqualTo(ResourceState.AT_STATION);
        assertThat(given.statusOf(resource).assignmentId()).isNull();
    }

    @Test
    void incompleteAssignmentCannotBeDispatched() {
        var resource = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var dispatcher = dispatcherAtWork();
        dispatcher.opensANewAssignment();
        dispatcher.addsResource(resource.callSign());

        dispatcher.dispatchesAll();

        assertThat(dispatcher.missingInformation()).containsExactlyInAnyOrder("Please select an assignment type",
                "Please select a municipality", "Please enter an address");
        assertThat(stateOf(resource)).isEqualTo(ResourceState.RESERVED);
    }

    @Test
    void dispatcherDispatchesAllResourcesOnTheAssignment() {
        var first = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var second = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var dispatcher = dispatcherAtWork();
        dispatcher.opensANewAssignment();
        dispatcher.fillsIn(fire, town, "Fire Street 1");
        dispatcher.addsResource(first.callSign());
        dispatcher.addsResource(second.callSign());

        dispatcher.dispatchesAll();

        assertThat(dispatcher.wasToldAnything()).isFalse();
        assertThat(dispatcher.statesOnAssignment()).containsOnly("Dispatched");
        assertThat(stateOf(first)).isEqualTo(ResourceState.DISPATCHED);
        assertThat(stateOf(second)).isEqualTo(ResourceState.DISPATCHED);
    }

    @Test
    void dispatcherDispatchesOnlyTheSelectedResource() {
        var selected = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var other = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var dispatcher = dispatcherAtWork();
        dispatcher.opensANewAssignment();
        dispatcher.fillsIn(fire, town, "Fire Street 1");
        dispatcher.addsResource(selected.callSign());
        dispatcher.addsResource(other.callSign());
        assertThat(dispatcher.canDispatchSelected()).as("Nothing selected yet").isFalse();

        dispatcher.dispatches(selected.callSign());

        assertThat(stateOf(selected)).isEqualTo(ResourceState.DISPATCHED);
        assertThat(stateOf(other)).isEqualTo(ResourceState.RESERVED);
    }

    @Test
    void dispatcherDispatchesOnlyTheReservedResources() {
        var reserved = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var enRoute = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var dispatcher = dispatcherAtWork();
        dispatcher.opensANewAssignment();
        dispatcher.fillsIn(fire, town, "Fire Street 1");
        dispatcher.addsResource(reserved.callSign());
        dispatcher.addsResource(enRoute.callSign());
        dispatcher.setsStateOnAssignment(enRoute.callSign(), "En route");

        dispatcher.dispatchesReserved();

        assertThat(stateOf(reserved)).isEqualTo(ResourceState.DISPATCHED);
        assertThat(stateOf(enRoute)).isEqualTo(ResourceState.EN_ROUTE);
    }

    @Test
    void dispatcherChangesTheStateOfAResourceByHand() {
        var resource = given.resource(unique("CS"), ResourceState.OUT_OF_SERVICE);
        var dispatcher = dispatcherAtWork();
        assertThat(dispatcher.stateOf(resource.callSign())).isEqualTo("Out of service");

        dispatcher.opensStateOf(resource.callSign());

        assertThat(dispatcher.stateQuestion()).isEqualTo("Change state of " + resource.callSign());
        assertThat(dispatcher.stateChoices()).as("Only the states a dispatcher may set by hand")
                .containsExactlyInAnyOrder("Available", "At station");

        dispatcher.choosesState("Available");

        assertThat(dispatcher.isChoosingAState()).isFalse();
        assertThat(dispatcher.stateOf(resource.callSign())).isEqualTo("Available");
        assertThat(stateOf(resource)).isEqualTo(ResourceState.AVAILABLE);
    }

    @Test
    void assignmentWithResourcesOnItCannotBeClosed() {
        var resource = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var dispatcher = dispatcherAtWork();
        var number = dispatcher.opensANewAssignment();
        dispatcher.addsResource(resource.callSign());

        dispatcher.closesTheAssignment();

        assertThat(dispatcher.seesAssignment(number)).isTrue();
        assertThat(dispatcher.seesAnAssignmentToEdit()).isTrue();
    }

    @Test
    void dispatcherClosesAnAssignmentWithoutResources() {
        var dispatcher = dispatcherAtWork();
        var number = dispatcher.opensANewAssignment();

        dispatcher.closesTheAssignment();

        assertThat(dispatcher.seesAssignment(number)).isFalse();
        assertThat(dispatcher.isToldNoAssignmentIsSelected()).isTrue();
    }

    @Test
    void closingTheAssignmentReleasesResourcesThatAreAvailableAgain() {
        var resource = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var dispatcher = dispatcherAtWork();
        var number = dispatcher.opensANewAssignment();
        dispatcher.fillsIn(fire, town, "Fire Street 1");
        dispatcher.addsResource(resource.callSign());
        dispatcher.dispatchesAll();
        dispatcher.setsStateOnAssignment(resource.callSign(), "Available");

        dispatcher.closesTheAssignment();

        assertThat(dispatcher.seesAssignment(number)).isFalse();
        assertThat(stateOf(resource)).isEqualTo(ResourceState.AVAILABLE);
        assertThat(given.statusOf(resource).assignmentId()).isNull();
    }

    private ResourceState stateOf(GivenResource resource) {
        return given.statusOf(resource).state();
    }
}
