package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import net.pkhapps.idispatch.server.entity.Municipality;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the municipality management view, and through it the shared behavior of {@link AbstractManagementView} and
 * {@link ManagementForm}: adding, editing, validation, unsaved changes, soft deletion, restoring and searching.
 */
@WithMockUser(username = "mgmt-admin", roles = "ADMIN")
class MunicipalityManagementViewTest extends AbstractManagementViewTest {

    @Test
    void view_showsItsOwnTitle() {
        navigate(MunicipalityManagementView.class);

        assertThat(find(H2.class).single().getText()).isEqualTo("Municipalities");
    }

    @Test
    void view_listsActiveMunicipalitiesOnly() {
        var active = unique("Active ");
        var deleted = unique("Deleted ");
        testData.municipality(active);
        testData.deletedMunicipality(deleted);

        navigate(MunicipalityManagementView.class);

        assertThat(names()).contains(active).doesNotContain(deleted);
        assertThat(isEditorOpen()).isFalse();
    }

    @Test
    void includeDeleted_showsDeletedMunicipalities() {
        var deleted = unique("Deleted ");
        testData.deletedMunicipality(deleted);
        navigate(MunicipalityManagementView.class);

        includeDeleted();

        assertThat(names()).contains(deleted);
        var row = rowOf(this.<Municipality>grid(), Municipality::getNameFi, deleted);
        Grid<Municipality> grid = grid();
        assertThat(grid.getPartNameGenerator().apply(test(grid).getRow(row))).isEqualTo("deleted");
    }

    @Test
    void search_filtersByNamePrefix() {
        var prefix = unique("Srch");
        var match = prefix + " North";
        var other = unique("Other ");
        testData.municipality(match);
        testData.municipality(other);
        navigate(MunicipalityManagementView.class);

        search(prefix.toLowerCase());

        assertThat(names()).containsExactly(match);
    }

    @Test
    void search_matchesSwedishNamesToo() {
        var prefix = unique("Sok");
        var swedish = prefix + " Söder";
        testData.municipality(unique("Etelä "), swedish);
        navigate(MunicipalityManagementView.class);

        search(prefix.toLowerCase());

        assertThat(items(this.<Municipality>grid())).extracting(Municipality::getNameSv).containsExactly(swedish);
    }

    @Test
    void add_withOnlySwedishName_isSaved() {
        var name = unique("Endast svenska ");
        navigate(MunicipalityManagementView.class);

        add();
        test(editorTextField("Name in Swedish")).setValue(name);
        save();

        assertThat(editorTitle()).isEqualTo("Edit Municipality");
        var saved = this.<Municipality>grid().asSingleSelect().getValue();
        assertThat(saved.getNameFi()).isEmpty();
        assertThat(saved.getNameSv()).isEqualTo(name);
    }

    @Test
    void add_enteringOneLanguageAfterFailedSave_clearsTheErrorOnBothFields() {
        navigate(MunicipalityManagementView.class);
        add();
        save();

        test(editorTextField("Name in Swedish")).setValue(unique("Namn "));

        assertThat(editorTextField("Name in Finnish").isInvalid()).isFalse();
        assertThat(editorTextField("Name in Swedish").isInvalid()).isFalse();
    }

    @Test
    void add_createsMunicipalityAndKeepsEditingIt() {
        var name = unique("Added ");
        navigate(MunicipalityManagementView.class);

        add();
        assertThat(editorTitle()).isEqualTo("Add Municipality");
        assertThat(findEditorButton("Delete")).isEmpty();
        test(editorTextField("Name in Finnish")).setValue(name);
        save();

        assertThat(names()).contains(name);
        assertThat(editorTitle()).isEqualTo("Edit Municipality");
        assertThat(this.<Municipality>grid().asSingleSelect().getValue().getNameFi()).isEqualTo(name);
    }

    @Test
    void add_withEmptyName_showsValidationErrorNextToField() {
        navigate(MunicipalityManagementView.class);

        add();
        save();

        // Either language will do, so the error is on both fields and neither is marked as required
        for (var nameField : List.of(editorTextField("Name in Finnish"), editorTextField("Name in Swedish"))) {
            assertThat(nameField.isRequiredIndicatorVisible()).isFalse();
            assertThat(nameField.isInvalid()).isTrue();
            assertThat(nameField.getErrorMessage()).isEqualTo("Please enter a name in Finnish or Swedish");
        }
        assertThat(editorTitle()).isEqualTo("Add Municipality");
    }

    @Test
    void add_withDuplicateName_showsDataIntegrityWarning() {
        var name = unique("Duplicate ");
        testData.municipality(name);
        navigate(MunicipalityManagementView.class);

        add();
        test(editorTextField("Name in Finnish")).setValue(name);
        save();

        assertThat(isEditorOpen()).isTrue();
        assertThat(notificationTexts()).singleElement().asString()
                .startsWith("A data integrity violation has been detected.");
    }

    @Test
    void cancel_closesEditorWithoutSaving() {
        var name = unique("Cancelled ");
        navigate(MunicipalityManagementView.class);

        add();
        test(editorTextField("Name in Finnish")).setValue(name);
        test(editorButton("Cancel")).click();

        assertThat(isEditorOpen()).isFalse();
        assertThat(findConfirmDialog()).isEmpty();
        assertThat(names()).doesNotContain(name);
    }

    @Test
    void select_opensEditorAndSaveRenamesMunicipality() {
        var original = unique("Original ");
        var renamed = unique("Renamed ");
        testData.municipality(original);
        navigate(MunicipalityManagementView.class);

        select(Municipality::getNameFi, original);
        assertThat(editorTitle()).isEqualTo("Edit Municipality");
        var nameField = editorTextField("Name in Finnish");
        assertThat(nameField.getValue()).isEqualTo(original);
        test(nameField).setValue(renamed);
        save();

        assertThat(names()).contains(renamed).doesNotContain(original);
    }

    @Test
    void failedSave_leavesGridItemUnchanged() {
        var original = unique("Original ");
        var duplicate = unique("Taken ");
        testData.municipality(original);
        testData.municipality(duplicate);
        navigate(MunicipalityManagementView.class);

        select(Municipality::getNameFi, original);
        test(editorTextField("Name in Finnish")).setValue(duplicate);
        save();

        assertThat(names()).contains(original);
    }

    @Test
    void selectingAnotherRowWithUnsavedChanges_asksBeforeDiscardingThem() {
        var first = unique("First ");
        var second = unique("Second ");
        var edited = unique("Edited ");
        testData.municipality(first);
        testData.municipality(second);
        navigate(MunicipalityManagementView.class);

        select(Municipality::getNameFi, first);
        test(editorTextField("Name in Finnish")).setValue(edited);
        select(Municipality::getNameFi, second);

        var confirm = findConfirmDialog().orElseThrow();
        assertThat(test(confirm).getHeader()).isEqualTo("Discard changes?");
        test(confirm).cancel();
        assertThat(editorTextField("Name in Finnish").getValue()).isEqualTo(edited);
        assertThat(this.<Municipality>grid().asSingleSelect().getValue().getNameFi()).isEqualTo(first);

        select(Municipality::getNameFi, second);
        test(findConfirmDialog().orElseThrow()).confirm();
        assertThat(editorTextField("Name in Finnish").getValue()).isEqualTo(second);
        assertThat(names()).contains(first).doesNotContain(edited);
    }

    @Test
    void delete_hidesMunicipalityAndRestoreBringsItBack() {
        var name = unique("Lifecycle ");
        testData.municipality(name);
        navigate(MunicipalityManagementView.class);

        select(Municipality::getNameFi, name);
        test(editorButton("Delete")).click();
        assertThat(names()).doesNotContain(name);
        assertThat(isEditorOpen()).isFalse();

        includeDeleted();
        select(Municipality::getNameFi, name);
        assertThat(editorTextField("Name in Finnish").isReadOnly()).isTrue();
        assertThat(findEditorButton("Save")).isEmpty();
        assertThat(findEditorButton("Delete")).isEmpty();
        test(editorButton("Restore")).click();

        includeDeleted();
        assertThat(names()).contains(name);
    }

    private List<String> names() {
        return items(this.<Municipality>grid()).stream().map(Municipality::getNameFi).toList();
    }
}
