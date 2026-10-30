package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarTester;
import com.vaadin.flow.component.textfield.TextField;
import net.pkhapps.idispatch.server.ui.AbstractUITest;

import java.util.Optional;
import java.util.function.Function;

/**
 * Helpers for the management views: a grid of entities, and an editor for the selected or new entity in the detail
 * area of a master-detail layout.
 */
abstract class AbstractManagementViewTest extends AbstractUITest {

    protected <T> Grid<T> grid() {
        return grid(currentView());
    }

    protected <T> void select(Function<T, ?> property, Object value) {
        Grid<T> grid = grid();
        test(grid).select(rowOf(grid, property, value));
    }

    protected void add() {
        test(button("Add...")).click();
    }

    protected boolean isEditorOpen() {
        return test(find(MasterDetailLayout.class).single()).getDetail() != null;
    }

    protected Component editor() {
        return Optional.ofNullable(test(find(MasterDetailLayout.class).single()).getDetail())
                .orElseThrow(() -> new AssertionError("The editor is not open"));
    }

    protected String editorTitle() {
        return find(H3.class, editor()).single().getText();
    }

    protected TextField editorTextField(String label) {
        return find(TextField.class, editor()).withLabel(label).single();
    }

    protected Button editorButton(String text) {
        return findEditorButton(text).orElseThrow(() -> new AssertionError("No button " + text + " in the editor"));
    }

    protected Optional<Button> findEditorButton(String text) {
        return find(Button.class, editor()).withText(text).all().stream().findFirst();
    }

    protected void save() {
        test(editorButton("Save")).click();
    }

    protected Optional<ConfirmDialog> findConfirmDialog() {
        return find(ConfirmDialog.class).all().stream().findFirst();
    }

    protected void includeDeleted() {
        var viewOptions = find(MenuBar.class).withClassName("view-options").single();
        test(MenuBarTester.class, viewOptions).clickItem(0, 0);
    }

    protected void search(String term) {
        test(find(TextField.class).withPropertyValue(TextField::getPlaceholder, "Search...").single()).setValue(term);
    }
}
