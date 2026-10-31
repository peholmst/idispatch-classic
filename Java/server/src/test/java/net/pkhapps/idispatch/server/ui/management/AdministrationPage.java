package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.textfield.TextField;
import net.pkhapps.idispatch.server.ui.AppShell;
import net.pkhapps.idispatch.server.ui.GridRows;

import java.util.List;
import java.util.Map;

/**
 * Where things are on the administration views: a list of everything of one kind with a search field and view options
 * above it, and an editor for the selected or new item beside it.
 */
public class AdministrationPage extends AppShell {

    public AdministrationPage(BrowserlessUIContext window) {
        super(window);
    }

    public void open(Class<? extends Component> view) {
        window.navigate(view);
    }

    // --- The toolbar ---

    public void search(String term) {
        window.findTextField().withAriaLabel("Search").setValue(term);
    }

    /**
     * Ticks "Include deleted" in the view options menu, or unticks it if it is ticked. The menu shows only a filter
     * icon, so its items are found by their position.
     */
    public void toggleIncludeDeleted() {
        window.findMenuBar().withCondition(menuBar -> menuBar.hasClassName("view-options"))
                .clickItem(0, 0);
    }

    public void clickRefresh() {
        window.findButton().withAriaLabel("Refresh").click();
    }

    public void clickAdd() {
        window.findButton().withText("Add...").click();
    }

    /**
     * The choices in the Add menu, for views that add more than one kind of item.
     */
    public List<String> addChoices() {
        return addMenu().getItemTexts("Add...");
    }

    public void clickAddMenuItem(String kind) {
        addMenu().clickItem("Add...", kind);
    }

    private com.vaadin.flow.component.menubar.MenuBarLocator addMenu() {
        return window.findMenuBar().withCondition(menuBar -> !menuBar.hasClassName("view-options"));
    }

    // --- The list ---

    public List<Map<String, String>> rows() {
        return list().all();
    }

    public List<String> column(String header) {
        return list().column(header);
    }

    public Map<String, String> rowWhere(String header, String text) {
        return list().rowWhere(header, text);
    }

    public void selectRowWhere(String header, String text) {
        list().selectRowWhere(header, text);
    }

    public void selectRow(int index) {
        list().selectRow(index);
    }

    public List<String> selected(String header) {
        return list().selected(header);
    }

    /**
     * Whether the row is marked as deleted, which the theme shows by striking it through.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean isMarkedAsDeleted(String header, String text) {
        Grid grid = window.findGrid(Object.class).component();
        var row = list().indexOfRowWhere(header, text);
        var item = window.use(grid).getRow(row);
        return "deleted".equals(grid.getPartNameGenerator().apply(item));
    }

    private GridRows list() {
        return GridRows.of(window, window.findGrid(Object.class).component());
    }

    // --- The editor ---

    public boolean showsEditor() {
        return !window.findMasterDetailLayout().isDetailPlaceholderVisible()
                && window.findH3().components().stream().findAny().isPresent();
    }

    public String editorTitle() {
        return window.findH3().component().getText();
    }

    public void enter(String label, String value) {
        window.findTextField().withLabel(label).setValue(value);
    }

    public void enterInTextArea(String label, String value) {
        window.findTextArea().withLabel(label).setValue(value);
    }

    public String valueOf(String label) {
        return field(label).getValue();
    }

    public String textAreaValueOf(String label) {
        return window.findTextArea().withLabel(label).component().getValue();
    }

    public void choose(String label, String item) {
        window.findComboBox(Object.class).withLabel(label).selectItem(item);
    }

    public String chosen(String label) {
        var comboBox = window.findComboBox(Object.class).withLabel(label).component();
        var value = comboBox.getValue();
        return value == null ? "" : comboBox.getItemLabelGenerator().apply(value);
    }

    public List<String> choicesOf(String label) {
        return window.findComboBox(Object.class).withLabel(label).getSuggestions();
    }

    public void chooseAlso(String label, String item) {
        window.findMultiSelectComboBox(Object.class).withLabel(label).selectItem(item);
    }

    @SuppressWarnings("unchecked")
    public List<String> chosenItems(String label) {
        var comboBox = window.findMultiSelectComboBox(Object.class).withLabel(label).component();
        return comboBox.getValue().stream().map(item -> comboBox.getItemLabelGenerator().apply(item)).sorted().toList();
    }

    /**
     * The error shown under the field, or nothing if the field is valid.
     */
    public String errorOf(String label) {
        var field = window.find(Component.class)
                .withCondition(c -> c instanceof com.vaadin.flow.component.HasValidation
                        && c instanceof com.vaadin.flow.component.HasLabel hasLabel
                        && label.equals(hasLabel.getLabel()))
                .single();
        var validation = (com.vaadin.flow.component.HasValidation) field;
        return validation.isInvalid() ? validation.getErrorMessage() : "";
    }

    public boolean isMarkedRequired(String label) {
        return field(label).isRequiredIndicatorVisible();
    }

    public boolean canEdit(String label) {
        var field = window.find(Component.class)
                .withCondition(c -> c instanceof com.vaadin.flow.component.HasValue<?, ?>
                        && c instanceof com.vaadin.flow.component.HasLabel hasLabel
                        && label.equals(hasLabel.getLabel()))
                .single();
        return !((com.vaadin.flow.component.HasValue<?, ?>) field).isReadOnly();
    }

    public boolean showsEditorButton(String text) {
        return window.findButton().withText(text).exists();
    }

    public void clickEditorButton(String text) {
        window.findButton().withText(text).click();
    }

    private TextField field(String label) {
        return window.findTextField().withLabel(label).component();
    }
}
