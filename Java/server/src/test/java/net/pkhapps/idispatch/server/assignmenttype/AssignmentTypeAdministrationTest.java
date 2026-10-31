package net.pkhapps.idispatch.server.assignmenttype;

import net.pkhapps.idispatch.server.ui.UseCaseTest;
import net.pkhapps.idispatch.server.ui.management.Administrator;
import org.junit.jupiter.api.Test;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * An administrator keeps the assignment types up to date. An assignment type has a code and a description in Finnish and
 * Swedish, of which one is enough.
 */
class AssignmentTypeAdministrationTest extends UseCaseTest {

    private static final String CODE = "Code";
    private static final String FINNISH_DESCRIPTION = "Description in Finnish";
    private static final String SWEDISH_DESCRIPTION = "Description in Swedish";

    private Administrator administratorAtWork() {
        return new Administrator(user("types-admin", "ADMIN"), AssignmentTypeManagementView.class, CODE);
    }

    @Test
    void administratorAddsAnAssignmentTypeAndKeepsEditingIt() {
        var code = unique("AT");
        var admin = administratorAtWork();

        admin.startsAdding();
        assertThat(admin.editorTitle()).isEqualTo("Add Assignment Type");
        assertThat(admin.canDo("Delete")).as("Nothing to delete yet").isFalse();
        admin.fillsIn(CODE, code);
        admin.fillsIn(FINNISH_DESCRIPTION, "Structure fire");
        admin.saves();

        assertThat(admin.row(code)).containsEntry(FINNISH_DESCRIPTION, "Structure fire");
        assertThat(admin.editorTitle()).isEqualTo("Edit Assignment Type");
        assertThat(admin.selected()).containsExactly(code);
    }

    @Test
    void administratorAddsAnAssignmentTypeWithOnlyASwedishDescription() {
        var code = unique("AT");
        var admin = administratorAtWork();

        admin.startsAdding();
        admin.fillsIn(CODE, code);
        admin.fillsIn(SWEDISH_DESCRIPTION, "Byggnadsbrand");
        admin.saves();

        assertThat(admin.editorTitle()).isEqualTo("Edit Assignment Type");
        assertThat(admin.row(code)).containsEntry(SWEDISH_DESCRIPTION, "Byggnadsbrand")
                .containsEntry(FINNISH_DESCRIPTION, "");
    }

    @Test
    void assignmentTypeWithoutCodeOrDescriptionIsNotSaved() {
        var admin = administratorAtWork();

        admin.startsAdding();
        admin.saves();

        assertThat(admin.errorOf(CODE)).isEqualTo("Please enter a code");
        assertThat(admin.seesItAsRequired(CODE)).isTrue();
        // Either language will do, so the error is on both fields and neither is marked as required
        for (var field : new String[]{FINNISH_DESCRIPTION, SWEDISH_DESCRIPTION}) {
            assertThat(admin.errorOf(field)).isEqualTo("Please enter a description in Finnish or Swedish");
            assertThat(admin.seesItAsRequired(field)).isFalse();
        }
        assertThat(admin.editorTitle()).isEqualTo("Add Assignment Type");
    }

    @Test
    void enteringADescriptionInEitherLanguageClearsTheErrorOnBothFields() {
        var admin = administratorAtWork();
        admin.startsAdding();
        admin.saves();

        admin.fillsIn(SWEDISH_DESCRIPTION, "Beskrivning");

        assertThat(admin.errorOf(FINNISH_DESCRIPTION)).isEmpty();
        assertThat(admin.errorOf(SWEDISH_DESCRIPTION)).isEmpty();
        assertThat(admin.errorOf(CODE)).as("Still missing").isEqualTo("Please enter a code");
    }

    @Test
    void administratorIsWarnedWhenTheCodeIsAlreadyInUse() {
        var code = unique("AT");
        given.assignmentType(code, "Existing");
        var admin = administratorAtWork();

        admin.startsAdding();
        admin.fillsIn(CODE, code);
        admin.fillsIn(FINNISH_DESCRIPTION, "Duplicate");
        admin.saves();

        assertThat(admin.wasToldThat("A data integrity violation has been detected.")).isTrue();
        assertThat(admin.isEditing()).isTrue();
    }

    @Test
    void administratorCancelsAddingAnAssignmentType() {
        var code = unique("AT");
        var admin = administratorAtWork();

        admin.startsAdding();
        admin.fillsIn(CODE, code);
        admin.cancels();

        assertThat(admin.isEditing()).isFalse();
        assertThat(admin.isAskedAnything()).as("Cancelling is the answer").isFalse();
        assertThat(admin.listed()).doesNotContain(code);
    }

    @Test
    void administratorChangesTheCodeOfAnAssignmentType() {
        var original = unique("AT");
        given.assignmentType(original, "Original");
        var changed = unique("AT");
        var admin = administratorAtWork();

        admin.opens(original);
        assertThat(admin.editorTitle()).isEqualTo("Edit Assignment Type");
        assertThat(admin.valueOf(CODE)).isEqualTo(original);
        assertThat(admin.valueOf(FINNISH_DESCRIPTION)).isEqualTo("Original");
        admin.fillsIn(CODE, changed);
        admin.saves();

        assertThat(admin.listed()).contains(changed).doesNotContain(original);
    }

    @Test
    void changeThatFailsLeavesTheListAsItWas() {
        var original = unique("AT");
        given.assignmentType(original, "Original");
        var taken = unique("AT");
        given.assignmentType(taken, "Taken");
        var admin = administratorAtWork();

        admin.opens(original);
        admin.fillsIn(CODE, taken);
        admin.saves();

        assertThat(admin.wasToldThat("A data integrity violation has been detected.")).isTrue();
        assertThat(admin.row(original)).containsEntry(FINNISH_DESCRIPTION, "Original");
    }

    @Test
    void administratorIsAskedBeforeUnsavedChangesAreThrownAway() {
        var first = unique("AT");
        given.assignmentType(first, "First");
        var second = unique("AT");
        given.assignmentType(second, "Second");
        var admin = administratorAtWork();
        admin.opens(first);
        admin.fillsIn(FINNISH_DESCRIPTION, "Edited");

        admin.opens(second);
        assertThat(admin.isAsked("Discard changes?")).isTrue();
        admin.keepsEditing();

        assertThat(admin.valueOf(FINNISH_DESCRIPTION)).isEqualTo("Edited");
        assertThat(admin.selected()).containsExactly(first);

        admin.opens(second);
        admin.discardsTheChanges();

        assertThat(admin.valueOf(FINNISH_DESCRIPTION)).isEqualTo("Second");
        assertThat(admin.row(first)).containsEntry(FINNISH_DESCRIPTION, "First");
    }



    @Test
    void puttingBackWhatWasThereIsNoChange() {
        var first = unique("AT");
        given.assignmentType(first, "First");
        var second = unique("AT");
        given.assignmentType(second, "Second");
        var admin = administratorAtWork();
        admin.opens(first);
        admin.fillsIn(FINNISH_DESCRIPTION, "Edited");
        admin.fillsIn(FINNISH_DESCRIPTION, "First");

        admin.opens(second);

        assertThat(admin.isAskedAnything()).as("Nothing to discard").isFalse();
        assertThat(admin.valueOf(FINNISH_DESCRIPTION)).isEqualTo("Second");
    }

    @Test
    void administratorChangesDeletesAndRestoresAnAssignmentType() {
        var code = unique("AT");
        given.assignmentType(code, "Original");
        var admin = administratorAtWork();

        admin.opens(code);
        admin.fillsIn(FINNISH_DESCRIPTION, "Changed");
        admin.saves();
        admin.opens(code);
        admin.deletes();
        assertThat(admin.listed()).doesNotContain(code);
        assertThat(admin.isEditing()).isFalse();

        admin.togglesShowingDeleted();
        admin.opens(code);
        assertThat(admin.canChange(CODE)).as("Deleted assignment types are read-only").isFalse();
        assertThat(admin.canChange(FINNISH_DESCRIPTION)).isFalse();
        assertThat(admin.canDo("Save")).isFalse();
        assertThat(admin.canDo("Delete")).isFalse();
        admin.restores();

        assertThat(admin.seesAsDeleted(code)).isFalse();
        assertThat(admin.row(code)).containsEntry(FINNISH_DESCRIPTION, "Changed");
    }

    @Test
    void deletedAssignmentTypesAreHiddenUnlessAskedFor() {
        var code = unique("AT");
        given.deletedAssignmentType(code, "Gone");
        var admin = administratorAtWork();

        assertThat(admin.listed()).doesNotContain(code);

        admin.togglesShowingDeleted();

        assertThat(admin.listed()).contains(code);
        assertThat(admin.seesAsDeleted(code)).isTrue();
    }

    @Test
    void administratorFindsAnAssignmentTypeByTheBeginningOfItsCodeOrByItsDescription() {
        var code = unique("SRCH");
        given.assignmentType(code, "Other text");
        var marker = unique("Desc");
        given.assignmentType(unique("AT"), "Has " + marker + " inside");
        given.assignmentType(unique("AT"), "Unrelated");
        var admin = administratorAtWork();

        admin.searchesFor(code.substring(0, code.length() - 2).toLowerCase());
        assertThat(admin.listed()).containsExactly(code);

        admin.searchesFor(marker.toLowerCase());
        assertThat(admin.listedIn(FINNISH_DESCRIPTION)).containsExactly("Has " + marker + " inside");
    }

    @Test
    void assignmentTypesAreListedByCode() {
        var prefix = unique("S");
        given.assignmentType(prefix + "B", "Second");
        given.assignmentType(prefix + "A", "First");

        var admin = administratorAtWork();

        assertThat(admin.listed().stream().filter(code -> code.startsWith(prefix)))
                .containsExactly(prefix + "A", prefix + "B");
    }
}
