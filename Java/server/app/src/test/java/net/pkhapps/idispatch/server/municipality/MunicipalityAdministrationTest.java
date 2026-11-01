package net.pkhapps.idispatch.server.municipality;

import net.pkhapps.idispatch.server.ui.UseCaseTest;
import net.pkhapps.idispatch.server.ui.management.Administrator;
import org.junit.jupiter.api.Test;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * An administrator keeps the list of municipalities up to date. Every administration view works like this one, so the
 * stories of the editor that all of them share are told here: adding, editing, validation, unsaved changes, deleting,
 * restoring and searching.
 */
class MunicipalityAdministrationTest extends UseCaseTest {

    private static final String FINNISH_NAME = "Name in Finnish";
    private static final String SWEDISH_NAME = "Name in Swedish";

    private Administrator administratorAtWork() {
        return new Administrator(user("mgmt-admin", "ADMIN"), MunicipalityManagementView.class, FINNISH_NAME);
    }

    @Test
    void administratorSeesTheMunicipalitiesThatAreInUse() {
        var active = given.municipality(unique("Active ")).nameFi();
        var deleted = given.deletedMunicipality(unique("Deleted ")).nameFi();

        var admin = administratorAtWork();

        assertThat(admin.viewTitle()).isEqualTo("Municipalities");
        assertThat(admin.listed()).contains(active).doesNotContain(deleted);
        assertThat(admin.isEditing()).isFalse();
    }

    @Test
    void administratorShowsTheDeletedMunicipalitiesToo() {
        var deleted = given.deletedMunicipality(unique("Deleted ")).nameFi();
        var admin = administratorAtWork();

        admin.togglesShowingDeleted();

        assertThat(admin.listed()).contains(deleted);
        assertThat(admin.seesAsDeleted(deleted)).isTrue();
    }

    @Test
    void administratorFindsAMunicipalityByTheBeginningOfItsName() {
        var prefix = unique("Srch");
        var match = given.municipality(prefix + " North").nameFi();
        given.municipality(unique("Other "));
        var admin = administratorAtWork();

        admin.searchesFor(prefix.toLowerCase());

        assertThat(admin.listed()).containsExactly(match);
    }

    @Test
    void administratorFindsAMunicipalityByItsSwedishName() {
        var prefix = unique("Sok");
        var swedish = prefix + " Söder";
        given.municipality(unique("Etelä "), swedish);
        var admin = administratorAtWork();

        admin.searchesFor(prefix.toLowerCase());

        assertThat(admin.listedIn(SWEDISH_NAME)).containsExactly(swedish);
    }

    @Test
    void administratorAddsAMunicipalityAndKeepsEditingIt() {
        var name = unique("Added ");
        var admin = administratorAtWork();

        admin.startsAdding();
        assertThat(admin.editorTitle()).isEqualTo("Add Municipality");
        assertThat(admin.canDo("Delete")).as("Nothing to delete yet").isFalse();
        admin.fillsIn(FINNISH_NAME, name);
        admin.saves();

        assertThat(admin.listed()).contains(name);
        assertThat(admin.editorTitle()).isEqualTo("Edit Municipality");
        assertThat(admin.selected()).containsExactly(name);
    }

    @Test
    void administratorAddsAMunicipalityWithOnlyASwedishName() {
        var name = unique("Endast svenska ");
        var admin = administratorAtWork();

        admin.startsAdding();
        admin.fillsIn(SWEDISH_NAME, name);
        admin.saves();

        assertThat(admin.editorTitle()).isEqualTo("Edit Municipality");
        assertThat(admin.listedIn(SWEDISH_NAME)).contains(name);
        assertThat(admin.valueOf(FINNISH_NAME)).isEmpty();
    }

    @Test
    void municipalityWithoutANameIsNotSaved() {
        var admin = administratorAtWork();

        admin.startsAdding();
        admin.saves();

        // Either language will do, so the error is on both fields and neither is marked as required
        for (var field : new String[]{FINNISH_NAME, SWEDISH_NAME}) {
            assertThat(admin.errorOf(field)).isEqualTo("Please enter a name in Finnish or Swedish");
            assertThat(admin.seesItAsRequired(field)).isFalse();
        }
        assertThat(admin.editorTitle()).isEqualTo("Add Municipality");
    }

    @Test
    void enteringANameInEitherLanguageClearsTheErrorOnBothFields() {
        var admin = administratorAtWork();
        admin.startsAdding();
        admin.saves();

        admin.fillsIn(SWEDISH_NAME, unique("Namn "));

        assertThat(admin.errorOf(FINNISH_NAME)).isEmpty();
        assertThat(admin.errorOf(SWEDISH_NAME)).isEmpty();
    }

    @Test
    void administratorIsWarnedWhenTheMunicipalityAlreadyExists() {
        var name = given.municipality(unique("Duplicate ")).nameFi();
        var admin = administratorAtWork();

        admin.startsAdding();
        admin.fillsIn(FINNISH_NAME, name);
        admin.saves();

        assertThat(admin.wasToldThat("A data integrity violation has been detected.")).isTrue();
        assertThat(admin.isEditing()).isTrue();
    }

    @Test
    void administratorCancelsAddingAMunicipality() {
        var name = unique("Cancelled ");
        var admin = administratorAtWork();

        admin.startsAdding();
        admin.fillsIn(FINNISH_NAME, name);
        admin.cancels();

        assertThat(admin.isEditing()).isFalse();
        assertThat(admin.isAskedAnything()).as("Cancelling is the answer").isFalse();
        assertThat(admin.listed()).doesNotContain(name);
    }

    @Test
    void administratorRenamesAMunicipality() {
        var original = given.municipality(unique("Original ")).nameFi();
        var renamed = unique("Renamed ");
        var admin = administratorAtWork();

        admin.opens(original);
        assertThat(admin.editorTitle()).isEqualTo("Edit Municipality");
        assertThat(admin.valueOf(FINNISH_NAME)).isEqualTo(original);
        admin.fillsIn(FINNISH_NAME, renamed);
        admin.saves();

        assertThat(admin.listed()).contains(renamed).doesNotContain(original);
    }

    @Test
    void renameThatFailsLeavesTheListAsItWas() {
        var original = given.municipality(unique("Original ")).nameFi();
        var taken = given.municipality(unique("Taken ")).nameFi();
        var admin = administratorAtWork();

        admin.opens(original);
        admin.fillsIn(FINNISH_NAME, taken);
        admin.saves();

        assertThat(admin.listed()).contains(original);
    }

    @Test
    void administratorIsAskedBeforeUnsavedChangesAreThrownAway() {
        var first = given.municipality(unique("First ")).nameFi();
        var second = given.municipality(unique("Second ")).nameFi();
        var edited = unique("Edited ");
        var admin = administratorAtWork();
        admin.opens(first);
        admin.fillsIn(FINNISH_NAME, edited);

        admin.opens(second);
        assertThat(admin.isAsked("Discard changes?")).isTrue();
        admin.keepsEditing();

        assertThat(admin.valueOf(FINNISH_NAME)).isEqualTo(edited);
        assertThat(admin.selected()).containsExactly(first);

        admin.opens(second);
        admin.discardsTheChanges();

        assertThat(admin.valueOf(FINNISH_NAME)).isEqualTo(second);
        assertThat(admin.listed()).contains(first).doesNotContain(edited);
    }

    @Test
    void puttingBackWhatWasThereIsNoChange() {
        var first = given.municipality(unique("First ")).nameFi();
        var second = given.municipality(unique("Second ")).nameFi();
        var admin = administratorAtWork();
        admin.opens(first);
        admin.fillsIn(FINNISH_NAME, unique("Edited "));
        admin.fillsIn(FINNISH_NAME, first);

        admin.opens(second);

        assertThat(admin.isAskedAnything()).as("Nothing to discard").isFalse();
        assertThat(admin.valueOf(FINNISH_NAME)).isEqualTo(second);
    }

    @Test
    void administratorDeletesAMunicipalityAndRestoresIt() {
        var name = given.municipality(unique("Lifecycle ")).nameFi();
        var admin = administratorAtWork();

        admin.opens(name);
        admin.deletes();

        assertThat(admin.listed()).doesNotContain(name);
        assertThat(admin.isEditing()).isFalse();

        admin.togglesShowingDeleted();
        admin.opens(name);

        assertThat(admin.canChange(FINNISH_NAME)).as("Deleted municipalities are read-only").isFalse();
        assertThat(admin.canDo("Save")).isFalse();
        assertThat(admin.canDo("Delete")).isFalse();

        admin.restores();
        admin.togglesShowingDeleted();

        assertThat(admin.listed()).contains(name);
    }
}
