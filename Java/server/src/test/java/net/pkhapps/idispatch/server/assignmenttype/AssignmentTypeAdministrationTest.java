package net.pkhapps.idispatch.server.assignmenttype;

import net.pkhapps.idispatch.server.ui.UseCaseTest;
import net.pkhapps.idispatch.server.ui.management.Administrator;
import org.junit.jupiter.api.Test;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * An administrator keeps the assignment types up to date. An assignment type has a code and a description in Finnish
 * and Swedish.
 */
class AssignmentTypeAdministrationTest extends UseCaseTest {

    private static final String CODE = "Code";
    private static final String FINNISH_DESCRIPTION = "Description in Finnish";

    private Administrator assignmentTypeAdministrator() {
        return new Administrator(user("types-admin", "ADMIN"), AssignmentTypeManagementView.class, CODE);
    }

    @Test
    void administratorAddsAnAssignmentType() {
        var code = unique("AT");
        var admin = assignmentTypeAdministrator();

        admin.startsAdding();
        assertThat(admin.editorTitle()).isEqualTo("Add Assignment Type");
        admin.fillsIn(CODE, code);
        admin.fillsIn(FINNISH_DESCRIPTION, "Structure fire");
        admin.saves();

        assertThat(admin.row(code)).containsEntry(FINNISH_DESCRIPTION, "Structure fire");
    }

    @Test
    void assignmentTypeWithoutCodeOrDescriptionIsNotSaved() {
        var admin = assignmentTypeAdministrator();

        admin.startsAdding();
        admin.saves();

        assertThat(admin.errorOf(CODE)).isEqualTo("Please enter a code");
        assertThat(admin.errorOf(FINNISH_DESCRIPTION)).isEqualTo("Please enter a description in Finnish or Swedish");
    }

    @Test
    void administratorChangesTheDescriptionOfAnAssignmentType() {
        var code = unique("AT");
        given.assignmentType(code, "Original");
        var admin = assignmentTypeAdministrator();

        admin.opens(code);
        assertThat(admin.editorTitle()).isEqualTo("Edit Assignment Type");
        admin.fillsIn(FINNISH_DESCRIPTION, "Changed");
        admin.saves();

        assertThat(admin.row(code)).containsEntry(FINNISH_DESCRIPTION, "Changed");
    }

    @Test
    void administratorDeletesAnAssignmentType() {
        var code = unique("AT");
        given.assignmentType(code, "Original");
        var admin = assignmentTypeAdministrator();

        admin.opens(code);
        admin.deletes();

        assertThat(admin.listed()).doesNotContain(code);
        admin.togglesShowingDeleted();
        assertThat(admin.seesAsDeleted(code)).isTrue();
    }

    @Test
    void administratorFindsAnAssignmentTypeByTheBeginningOfItsCodeOrByItsDescription() {
        var code = unique("SRCH");
        given.assignmentType(code, "Other text");
        var marker = unique("Desc");
        given.assignmentType(unique("AT"), "Has " + marker + " inside");
        given.assignmentType(unique("AT"), "Unrelated");
        var admin = assignmentTypeAdministrator();

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

        var admin = assignmentTypeAdministrator();

        assertThat(admin.listed().stream().filter(code -> code.startsWith(prefix)))
                .containsExactly(prefix + "A", prefix + "B");
    }
}
