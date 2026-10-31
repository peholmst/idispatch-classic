package net.pkhapps.idispatch.server.dispatch;

import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.shared.Bilingual;
import net.pkhapps.idispatch.server.ui.TestData.GivenDestination;
import net.pkhapps.idispatch.server.ui.UseCaseTest;
import net.pkhapps.idispatch.server.ui.management.Administrator;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * An administrator decides where the dispatch notifications of each resource go: to a runboard at the station, or by
 * SMS to phone numbers. In the stories, every destination is for a resource of its own, and is found in the list by
 * that resource's call sign.
 */
class DestinationAdministrationTest extends UseCaseTest {

    private static final String RESOURCES = "Resources";
    private static final String RUNBOARD = "Runboard Destination";
    private static final String SMS = "SMS Destination";

    private Administrator administratorAtWork() {
        return new Administrator(user("destination-admin", "ADMIN"), DestinationManagementView.class, RESOURCES);
    }

    @Test
    void administratorChoosesWhichKindOfDestinationToAdd() {
        var admin = administratorAtWork();

        assertThat(admin.addChoices()).containsExactly(RUNBOARD, SMS);

        admin.startsAdding(SMS);
        assertThat(admin.editorTitle()).isEqualTo("Add SMS Destination");
        admin.startsAdding(RUNBOARD);
        assertThat(admin.editorTitle()).isEqualTo("Add Runboard Destination");
    }

    @Test
    void administratorAddsARunboardDestinationInSwedish() {
        var callSign = given.resource(unique("CS"), ResourceState.AVAILABLE).callSign();
        var description = unique("Station hall ");
        var admin = administratorAtWork();

        admin.startsAdding(RUNBOARD);
        assertThat(admin.chosen("Language")).as("New destinations are in Finnish").isEqualTo("Finnish");
        admin.fillsIn("Description", description);
        admin.chooses("Language", "Swedish");
        admin.fillsIn("Runboard Key", unique("key-"));
        admin.picks(RESOURCES, callSign);
        admin.saves();

        assertThat(admin.row(callSign)).containsAllEntriesOf(Map.of(
                "Type", RUNBOARD,
                "Description", description,
                "Language", "Swedish"));
        assertThat(destinationOf(callSign).locale()).isEqualTo(Bilingual.SWEDISH);
    }

    @Test
    void destinationWithoutADescriptionHasNone() {
        var callSign = given.resource(unique("CS"), ResourceState.AVAILABLE).callSign();
        var admin = administratorAtWork();

        addsRunboardDestination(admin, callSign);

        assertThat(destinationOf(callSign).description()).as("Not even an empty one").isNull();
    }

    @Test
    void administratorDescribesADestinationAndFindsItByTheDescription() {
        var callSign = given.resource(unique("CS"), ResourceState.AVAILABLE).callSign();
        var description = unique("Garage ");
        var admin = administratorAtWork();
        addsRunboardDestination(admin, callSign);

        admin.opens(callSign);
        assertThat(admin.valueOf("Description")).isEmpty();
        admin.fillsIn("Description", description);
        admin.saves();
        admin.searchesFor(description.toLowerCase());

        assertThat(admin.listedIn("Description")).containsExactly(description);
    }

    @Test
    void administratorAddsAnSmsDestinationWithNumbersSeparatedByCommasAndLines() {
        var callSign = given.resource(unique("CS"), ResourceState.AVAILABLE).callSign();
        var number = "+358" + Math.abs(callSign.hashCode());
        var admin = administratorAtWork();

        admin.startsAdding(SMS);
        admin.writes("Phone numbers", number + "1, " + number + "2\n" + number + "3");
        admin.picks(RESOURCES, callSign);
        admin.saves();

        assertThat(admin.row(callSign)).containsEntry("Type", SMS);
        assertThat(destinationOf(callSign).phoneNumbers())
                .containsExactlyInAnyOrder(number + "1", number + "2", number + "3");
    }

    @Test
    void administratorOpensAnSmsDestinationAndSeesItsNumbersAndResources() {
        var callSign = given.resource(unique("CS"), ResourceState.AVAILABLE).callSign();
        var number = "+358" + Math.abs(callSign.hashCode());
        var admin = administratorAtWork();
        admin.startsAdding(SMS);
        admin.writes("Phone numbers", number);
        admin.picks(RESOURCES, callSign);
        admin.saves();

        admin.opens(callSign);

        assertThat(admin.editorTitle()).isEqualTo("Edit SMS Destination");
        assertThat(admin.textIn("Phone numbers")).isEqualTo(number);
        assertThat(admin.chosenItems(RESOURCES)).containsExactly(callSign);
    }

    @Test
    void smsDestinationWithoutResourcesIsNotSaved() {
        var admin = administratorAtWork();

        admin.startsAdding(SMS);
        admin.saves();

        assertThat(admin.errorOf(RESOURCES)).isEqualTo("Please select at least one resource");
        assertThat(admin.isEditing()).isTrue();
    }

    @Test
    void administratorFindsADestinationByTheCallSignOfItsResource() {
        var marker = unique("");
        var callSign = given.resource("D" + marker, ResourceState.AVAILABLE).callSign();
        var other = given.resource(unique("CS"), ResourceState.AVAILABLE).callSign();
        var admin = administratorAtWork();
        addsRunboardDestination(admin, callSign);
        addsRunboardDestination(admin, other);

        admin.searchesFor(marker.toLowerCase());

        assertThat(admin.listed()).containsExactly(callSign);
    }

    @Test
    void deletedDestinationsAreHiddenUnlessAskedFor() {
        var callSign = given.resource(unique("CS"), ResourceState.AVAILABLE).callSign();
        var admin = administratorAtWork();
        addsRunboardDestination(admin, callSign);
        admin.searchesFor(callSign.toLowerCase());

        admin.opensTheFirstItem();
        admin.deletes();

        assertThat(admin.listed()).isEmpty();
        assertThat(given.destinationsFor(given.resourceCalled(callSign))).isEmpty();

        admin.togglesShowingDeleted();

        assertThat(admin.listed()).containsExactly(callSign);
        assertThat(admin.seesAsDeleted(callSign)).isTrue();
    }

    private static void addsRunboardDestination(Administrator admin, String callSign) {
        admin.startsAdding(RUNBOARD);
        admin.fillsIn("Runboard Key", unique("key-"));
        admin.picks(RESOURCES, callSign);
        admin.saves();
    }

    private GivenDestination destinationOf(String callSign) {
        var resource = given.resourceCalled(callSign);
        var destinations = given.destinationsFor(resource);
        assertThat(destinations).as("Destinations of " + callSign).hasSize(1);
        return destinations.getFirst();
    }
}
