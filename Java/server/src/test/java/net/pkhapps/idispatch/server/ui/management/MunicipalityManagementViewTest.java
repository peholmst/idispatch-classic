package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.tabs.Tab;
import net.pkhapps.idispatch.server.entity.Municipality;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the municipality management view, and through it the shared behavior of {@link AbstractManagementView}
 * and {@link AbstractManagementDialog}: adding, editing, validation, soft deletion, restoring and searching.
 */
@WithMockUser(username = "mgmt-admin", roles = "ADMIN")
class MunicipalityManagementViewTest extends AbstractManagementViewTest {

    @Test
    void view_showsManagementConsoleWithAllSections() {
        navigate(MunicipalityManagementView.class);

        assertThat(find(H2.class).single().getText()).isEqualTo("Management Console");
        assertThat(find(Tab.class).all().stream().map(tab -> tab.getElement().getTextRecursively()))
                .contains("Municipalities", "Assignment Types", "Resources", "Resource Types", "Destinations", "Users");
    }

    @Test
    void view_listsActiveMunicipalitiesOnly() {
        var active = unique("Active ");
        var deleted = unique("Deleted ");
        testData.municipality(active);
        testData.deletedMunicipality(deleted);

        navigate(MunicipalityManagementView.class);

        assertThat(names()).contains(active).doesNotContain(deleted);
    }

    @Test
    void includeDeleted_showsDeletedMunicipalities() {
        var deleted = unique("Deleted ");
        testData.deletedMunicipality(deleted);
        navigate(MunicipalityManagementView.class);

        test(find(Checkbox.class).withLabel("Include deleted").single()).click();

        assertThat(names()).contains(deleted);
        var row = rowOf(this.<Municipality>grid(), Municipality::getName, deleted);
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
    void buttons_areEnabledAccordingToSelection() {
        var name = unique("Buttons ");
        testData.municipality(name);
        navigate(MunicipalityManagementView.class);

        assertThat(editButton().isEnabled()).isFalse();
        assertThat(deleteButton().isEnabled()).isFalse();
        assertThat(restoreButton().isEnabled()).isFalse();

        test(grid()).select(rowOf(this.<Municipality>grid(), Municipality::getName, name));

        assertThat(editButton().isEnabled()).isTrue();
        assertThat(deleteButton().isEnabled()).isTrue();
        assertThat(restoreButton().isEnabled()).isFalse();
    }

    @Test
    void add_createsMunicipality() {
        var name = unique("Added ");
        navigate(MunicipalityManagementView.class);

        test(button("Add...")).click();
        assertThat(openDialog().getHeaderTitle()).isEqualTo("Add Municipality");
        test(dialogTextField("Municipality name")).setValue(name);
        test(buttonInDialog("Save")).click();

        assertThat(findOpenDialog()).isEmpty();
        assertThat(names()).contains(name);
    }

    @Test
    void add_withEmptyName_showsValidationErrorAndKeepsDialogOpen() {
        navigate(MunicipalityManagementView.class);

        test(button("Add...")).click();
        test(buttonInDialog("Save")).click();

        assertThat(findOpenDialog()).isPresent();
        assertThat(find(ListItem.class, openDialog()).all().stream().map(ListItem::getText))
                .containsExactly("Please enter a name");
    }

    @Test
    void add_withDuplicateName_showsDataIntegrityWarning() {
        var name = unique("Duplicate ");
        testData.municipality(name);
        navigate(MunicipalityManagementView.class);

        test(button("Add...")).click();
        test(dialogTextField("Municipality name")).setValue(name);
        test(buttonInDialog("Save")).click();

        assertThat(findOpenDialog()).isPresent();
        assertThat(notificationTexts()).singleElement().asString()
                .startsWith("A data integrity violation has been detected.");
    }

    @Test
    void cancel_closesDialogWithoutSaving() {
        var name = unique("Cancelled ");
        navigate(MunicipalityManagementView.class);

        test(button("Add...")).click();
        test(dialogTextField("Municipality name")).setValue(name);
        test(buttonInDialog("Cancel")).click();

        assertThat(findOpenDialog()).isEmpty();
        assertThat(names()).doesNotContain(name);
    }

    @Test
    void edit_renamesMunicipality() {
        var original = unique("Original ");
        var renamed = unique("Renamed ");
        testData.municipality(original);
        navigate(MunicipalityManagementView.class);

        test(grid()).select(rowOf(this.<Municipality>grid(), Municipality::getName, original));
        test(editButton()).click();
        assertThat(openDialog().getHeaderTitle()).isEqualTo("Edit Municipality");
        var nameField = dialogTextField("Municipality name");
        assertThat(nameField.getValue()).isEqualTo(original);
        test(nameField).setValue(renamed);
        test(buttonInDialog("Save")).click();

        assertThat(names()).contains(renamed).doesNotContain(original);
    }

    @Test
    void delete_hidesMunicipalityAndRestoreBringsItBack() {
        var name = unique("Lifecycle ");
        testData.municipality(name);
        navigate(MunicipalityManagementView.class);

        test(grid()).select(rowOf(this.<Municipality>grid(), Municipality::getName, name));
        test(deleteButton()).click();
        assertThat(names()).doesNotContain(name);

        test(find(Checkbox.class).withLabel("Include deleted").single()).click();
        test(grid()).select(rowOf(this.<Municipality>grid(), Municipality::getName, name));
        assertThat(editButton().isEnabled()).isFalse();
        assertThat(deleteButton().isEnabled()).isFalse();
        assertThat(restoreButton().isEnabled()).isTrue();
        test(restoreButton()).click();

        test(find(Checkbox.class).withLabel("Include deleted").single()).click();
        assertThat(names()).contains(name);
    }

    private List<String> names() {
        return items(this.<Municipality>grid()).stream().map(Municipality::getName).toList();
    }
}
