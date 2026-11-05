package net.pkhapps.idispatch.server.dispatcher;

import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.shared.Bilingual;
import net.pkhapps.idispatch.server.ui.UseCaseTest;
import org.junit.jupiter.api.Test;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * At an explorer camp, the groups change vehicles several times a shift. A phone that stays in its vehicle keeps its
 * unit, but a phone that the group's officer carries has to follow the officer, so the dispatcher moves it to the
 * group's new vehicle. The phone is not paired again, and the move is recorded in the audit log.
 */
class MovingMobilePhonesTest extends UseCaseTest {

    @Test
    void dispatcherMovesTheOfficersPhoneToTheGroupsNewVehicle() {
        var prefix = unique("MP");
        var oldVehicle = given.resource(prefix + "-1", ResourceState.AVAILABLE);
        var newVehicle = given.resource(prefix + "-2", ResourceState.AVAILABLE);
        var phone = prefix + " Group 3 officer";
        given.mobile(oldVehicle, Bilingual.FINNISH, phone);
        var dispatcher = new Dispatcher(keycloakUser("dora", "Dora", "Dispatcher", "DISPATCHER"));
        dispatcher.opensMobilePhones();
        dispatcher.filtersPhonesBy(prefix);
        assertThat(dispatcher.unitOfPhone(phone)).isEqualTo(oldVehicle.callSign());

        dispatcher.startsMovingPhone(phone);
        assertThat(dispatcher.movingQuestion()).isEqualTo("Change Unit of " + phone);
        dispatcher.choosesNewUnit(newVehicle.callSign());
        dispatcher.savesTheNewUnit();

        assertThat(dispatcher.isMovingAPhone()).isFalse();
        assertThat(dispatcher.unitOfPhone(phone)).isEqualTo(newVehicle.callSign());
        assertThat(given.mobileKeyHashesFor(newVehicle)).as("The phone keeps its key").hasSize(1);
        assertThat(given.mobileKeyHashesFor(oldVehicle)).isEmpty();

        dispatcher.startsMovingPhone(phone);
        assertThat(dispatcher.seesWhoLastChangedIt()).startsWith("Unit changed by Dora Dispatcher on ");
    }

    @Test
    void dispatcherChoosesFromTheOtherUnitsNotDeleted() {
        var prefix = unique("MP");
        var current = given.resource(prefix + "-1", ResourceState.AVAILABLE);
        var other = given.resource(prefix + "-2", ResourceState.AVAILABLE);
        var deleted = given.resource(prefix + "-3", ResourceState.AVAILABLE);
        given.deleteResource(deleted);
        var phone = prefix + " Officer";
        given.mobile(current, Bilingual.FINNISH, phone);
        var dispatcher = new Dispatcher(user("dispatcher", "DISPATCHER"));
        dispatcher.opensMobilePhones();
        dispatcher.filtersPhonesBy(prefix);

        dispatcher.startsMovingPhone(phone);

        assertThat(dispatcher.newUnitChoices()).contains(other.callSign())
                .doesNotContain(current.callSign(), deleted.callSign());
    }

    @Test
    void dispatcherChangesTheirMindAboutMovingThePhone() {
        var prefix = unique("MP");
        var current = given.resource(prefix + "-1", ResourceState.AVAILABLE);
        var other = given.resource(prefix + "-2", ResourceState.AVAILABLE);
        var phone = prefix + " Officer";
        given.mobile(current, Bilingual.FINNISH, phone);
        var dispatcher = new Dispatcher(user("dispatcher", "DISPATCHER"));
        dispatcher.opensMobilePhones();
        dispatcher.filtersPhonesBy(prefix);

        dispatcher.startsMovingPhone(phone);
        dispatcher.choosesNewUnit(other.callSign());
        dispatcher.cancelsMovingThePhone();

        assertThat(dispatcher.unitOfPhone(phone)).isEqualTo(current.callSign());
        assertThat(given.mobileKeyHashesFor(other)).isEmpty();
    }

    @Test
    void phoneWithoutADescriptionIsNamedAfterItsUnit() {
        var resource = given.resource(unique("MP"), ResourceState.AVAILABLE);
        given.mobile(resource, Bilingual.FINNISH);
        var dispatcher = new Dispatcher(user("dispatcher", "DISPATCHER"));
        dispatcher.opensMobilePhones();

        dispatcher.filtersPhonesBy(resource.callSign());

        assertThat(dispatcher.listedPhones()).containsExactly("Phone of " + resource.callSign());
    }

    @Test
    void dispatcherFindsThePhoneByItsDescriptionOrItsUnit() {
        var prefix = unique("MP");
        var vehicle = given.resource(prefix + "-1", ResourceState.AVAILABLE);
        var officer = prefix + " Officer";
        given.mobile(vehicle, Bilingual.FINNISH, officer);
        given.mobile(given.resource(unique("MP"), ResourceState.AVAILABLE), Bilingual.FINNISH, unique("Other "));
        var dispatcher = new Dispatcher(user("dispatcher", "DISPATCHER"));
        dispatcher.opensMobilePhones();

        dispatcher.filtersPhonesBy(officer.toLowerCase());
        assertThat(dispatcher.listedPhones()).containsExactly(officer);

        dispatcher.filtersPhonesBy(vehicle.callSign());
        assertThat(dispatcher.listedPhones()).containsExactly(officer);

        dispatcher.filtersPhonesBy(unique("NOTHING"));
        assertThat(dispatcher.listedPhones()).isEmpty();
        assertThat(dispatcher.phonesEmptyText()).isEqualTo("No mobile phones match the filter");
    }

    @Test
    void phoneMovedByAnotherDispatcherShowsItsNewUnitRightAway() {
        var prefix = unique("MP");
        var oldVehicle = given.resource(prefix + "-1", ResourceState.AVAILABLE);
        var newVehicle = given.resource(prefix + "-2", ResourceState.AVAILABLE);
        var phone = prefix + " Officer";
        given.mobile(oldVehicle, Bilingual.FINNISH, phone);
        var watcher = new Dispatcher(user("alice", "DISPATCHER"));
        watcher.opensMobilePhones();
        watcher.filtersPhonesBy(prefix);
        var bob = new Dispatcher(user("bob", "DISPATCHER"));
        bob.opensMobilePhones();
        bob.filtersPhonesBy(prefix);

        bob.movesPhone(phone, newVehicle.callSign());
        watcher.receivesPhoneUpdates();

        assertThat(watcher.unitOfPhone(phone)).isEqualTo(newVehicle.callSign());
    }
}
