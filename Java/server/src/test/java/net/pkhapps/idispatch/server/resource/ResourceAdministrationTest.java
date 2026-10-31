package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.ui.UseCaseTest;
import net.pkhapps.idispatch.server.ui.management.Administrator;
import org.junit.jupiter.api.Test;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * An administrator keeps the list of resources, the units that dispatchers send out, up to date.
 */
class ResourceAdministrationTest extends UseCaseTest {

    private static final String CALL_SIGN = "Call sign";
    private static final String TYPE = "Type";

    private Administrator administratorAtWork() {
        return new Administrator(user("resource-admin", "ADMIN"), ResourceManagementView.class, CALL_SIGN);
    }

    @Test
    void administratorAddsAResourceThatStartsOutOfService() {
        var typeCode = unique("RT");
        given.resourceType(typeCode, "Ambulance");
        var callSign = unique("CS");
        var admin = administratorAtWork();

        admin.startsAdding();
        assertThat(admin.editorTitle()).isEqualTo("Add Resource");
        admin.fillsIn(CALL_SIGN, callSign);
        admin.chooses(TYPE, typeCode + " - Ambulance");
        admin.saves();

        assertThat(admin.row(callSign)).containsEntry(TYPE, "Ambulance");
        var added = given.resourceCalled(callSign);
        assertThat(given.statusOf(added).state()).isEqualTo(ResourceState.OUT_OF_SERVICE);
    }

    @Test
    void onlyResourceTypesInUseAreOffered() {
        var active = unique("RT");
        given.resourceType(active, "Active type");
        var deleted = unique("RT");
        given.deletedResourceType(deleted, "Deleted type");
        var admin = administratorAtWork();

        admin.startsAdding();

        assertThat(admin.choicesOf(TYPE)).contains(active + " - Active type")
                .doesNotContain(deleted + " - Deleted type");
    }

    @Test
    void resourceWithoutCallSignOrTypeIsNotSaved() {
        var admin = administratorAtWork();

        admin.startsAdding();
        admin.saves();

        assertThat(admin.errorOf(CALL_SIGN)).isEqualTo("Please enter a call sign");
        assertThat(admin.errorOf(TYPE)).isEqualTo("Please select a type");
        assertThat(admin.editorTitle()).isEqualTo("Add Resource");
    }

    @Test
    void resourceKeepsShowingItsTypeAfterTheTypeHasBeenDeleted() {
        var typeCode = unique("RT");
        var type = given.deletedResourceType(typeCode, "Retired type");
        var callSign = given.resource(unique("CS"), ResourceState.AVAILABLE, type).callSign();
        var admin = administratorAtWork();

        assertThat(admin.row(callSign)).containsEntry(TYPE, "Retired type");
        admin.opens(callSign);

        assertThat(admin.chosen(TYPE)).isEqualTo(typeCode + " - Retired type");
        assertThat(admin.choicesOf(TYPE)).as("Not offered for others").doesNotContain(typeCode + " - Retired type");
    }

    @Test
    void administratorChangesTheCallSignOfAResource() {
        var original = given.resource(unique("CS"), ResourceState.AVAILABLE).callSign();
        var renamed = unique("CS");
        var admin = administratorAtWork();

        admin.opens(original);
        assertThat(admin.editorTitle()).isEqualTo("Edit Resource");
        admin.fillsIn(CALL_SIGN, renamed);
        admin.saves();

        assertThat(admin.listed()).contains(renamed).doesNotContain(original);
    }

    @Test
    void administratorFindsAResourceByAnyPartOfItsCallSign() {
        var marker = unique("");
        var match = given.resource("X" + marker + "Y", ResourceState.AVAILABLE).callSign();
        given.resource(unique("CS"), ResourceState.AVAILABLE);
        var admin = administratorAtWork();

        admin.searchesFor(marker.toLowerCase());

        assertThat(admin.listed()).containsExactly(match);
    }

    @Test
    void administratorDeletesAResource() {
        var callSign = given.resource(unique("CS"), ResourceState.AVAILABLE).callSign();
        var admin = administratorAtWork();

        admin.opens(callSign);
        admin.deletes();

        assertThat(admin.listed()).doesNotContain(callSign);
    }
}
