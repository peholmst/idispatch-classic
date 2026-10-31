package net.pkhapps.idispatch.server.ui.management;

import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeManagementView;
import net.pkhapps.idispatch.server.resource.ResourceTypeManagementView;
import net.pkhapps.idispatch.server.ui.UseCaseTest;
import org.junit.jupiter.api.Test;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * An administrator keeps the assignment types and the resource types up to date. Both have a code and a description
 * in Finnish and Swedish.
 */
class TypeAdministrationTest extends UseCaseTest {

    private static final String CODE = "Code";
    private static final String FINNISH_DESCRIPTION = "Description in Finnish";

    private Administrator assignmentTypeAdministrator() {
        return new Administrator(user("types-admin", "ADMIN"), AssignmentTypeManagementView.class, CODE);
    }

    private Administrator resourceTypeAdministrator() {
        return new Administrator(user("types-admin", "ADMIN"), ResourceTypeManagementView.class, CODE);
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

    @Test
    void administratorAddsAResourceType() {
        var code = unique("RT");
        var admin = resourceTypeAdministrator();

        admin.startsAdding();
        assertThat(admin.editorTitle()).isEqualTo("Add Resource Type");
        admin.fillsIn(CODE, code);
        admin.fillsIn(FINNISH_DESCRIPTION, "Rescue unit");
        admin.saves();

        assertThat(admin.row(code)).containsEntry(FINNISH_DESCRIPTION, "Rescue unit");
    }

    @Test
    void administratorChangesDeletesAndRestoresAResourceType() {
        var code = unique("RT");
        given.resourceType(code, "Original");
        var admin = resourceTypeAdministrator();

        admin.opens(code);
        assertThat(admin.editorTitle()).isEqualTo("Edit Resource Type");
        admin.fillsIn(FINNISH_DESCRIPTION, "Changed");
        admin.saves();
        admin.opens(code);
        admin.deletes();
        assertThat(admin.listed()).doesNotContain(code);

        admin.togglesShowingDeleted();
        admin.opens(code);
        admin.restores();

        assertThat(admin.seesAsDeleted(code)).isFalse();
        assertThat(admin.row(code)).containsEntry(FINNISH_DESCRIPTION, "Changed");
    }

    @Test
    void deletedResourceTypesAreHiddenUnlessAskedFor() {
        var code = unique("RT");
        given.deletedResourceType(code, "Gone");
        var admin = resourceTypeAdministrator();

        assertThat(admin.listed()).doesNotContain(code);

        admin.togglesShowingDeleted();

        assertThat(admin.listed()).contains(code);
    }
}
