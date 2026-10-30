package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;
import net.pkhapps.idispatch.server.ui.AbstractUITest;

import java.util.Optional;

/**
 * Helpers for the management views, whose edit, delete, restore and refresh buttons only have an icon.
 */
abstract class AbstractManagementViewTest extends AbstractUITest {

    protected Button editButton() {
        return iconButton("vaadin:edit").orElseThrow();
    }

    protected Button deleteButton() {
        return iconButton("vaadin:close-circle").orElseThrow();
    }

    protected Optional<Button> findDeleteButton() {
        return iconButton("vaadin:close-circle");
    }

    protected Button restoreButton() {
        return iconButton("vaadin:reply").orElseThrow();
    }

    protected Optional<Button> findRestoreButton() {
        return iconButton("vaadin:reply");
    }

    protected Button refreshButton() {
        return iconButton("vaadin:refresh").orElseThrow();
    }

    protected <T> Grid<T> grid() {
        return grid(currentView());
    }

    protected void includeDeleted() {
        test(find(Checkbox.class).withLabel("Include deleted").single()).click();
    }

    protected void search(String term) {
        test(find(TextField.class).withPropertyValue(TextField::getPlaceholder, "Search...").single()).setValue(term);
    }

    protected TextField dialogTextField(String label) {
        return find(TextField.class, openDialog()).withLabel(label).single();
    }

    private Optional<Button> iconButton(String icon) {
        return find(Button.class, currentView()).all().stream()
                .filter(button -> button.getIcon() != null
                        && icon.equals(button.getIcon().getElement().getAttribute("icon")))
                .findFirst();
    }
}
