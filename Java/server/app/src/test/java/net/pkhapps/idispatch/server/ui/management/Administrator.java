package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.browserless.BrowserlessUserContext;
import com.vaadin.flow.component.Component;

import java.util.List;
import java.util.Map;

/**
 * Actor: an administrator keeping one kind of master data up to date in an administration view, in their own browser
 * window. The items are told apart by their key: the text in one column of the list, such as the name or the code.
 * Leaves where things are on the screen to {@link AdministrationPage}.
 */
public class Administrator {

    private final AdministrationPage page;
    private final String keyColumn;

    /**
     * Logs in as the given user and opens the administration view.
     *
     * @param keyColumn the header of the column whose text identifies an item
     */
    public Administrator(BrowserlessUserContext user, Class<? extends Component> view, String keyColumn) {
        this.page = new AdministrationPage(user.newWindow());
        this.keyColumn = keyColumn;
        page.open(view);
    }

    // --- What the administrator does ---

    public void startsAdding() {
        page.clickAdd();
    }

    /**
     * Starts adding an item of the given kind, in a view that adds more than one kind.
     */
    public void startsAdding(String kind) {
        page.clickAddMenuItem(kind);
    }

    /**
     * Selects the item in the list, which opens it in the editor.
     */
    public void opens(String key) {
        page.selectRowWhere(keyColumn, key);
    }

    public void opensTheFirstItem() {
        page.selectRow(0);
    }

    public void fillsIn(String field, String value) {
        page.enter(field, value);
    }

    public void writes(String textArea, String value) {
        page.enterInTextArea(textArea, value);
    }

    public void chooses(String field, String item) {
        page.choose(field, item);
    }

    public void picks(String field, String item) {
        page.chooseAlso(field, item);
    }

    public void saves() {
        page.clickEditorButton("Save");
    }

    public void cancels() {
        page.clickEditorButton("Cancel");
    }

    public void deletes() {
        page.clickEditorButton("Delete");
    }

    public void restores() {
        page.clickEditorButton("Restore");
    }

    public void searchesFor(String term) {
        page.search(term);
    }

    /**
     * Turns showing deleted items on, or off if it is on.
     */
    public void togglesShowingDeleted() {
        page.toggleIncludeDeleted();
    }

    public void discardsTheChanges() {
        page.answerConfirm();
    }

    public void keepsEditing() {
        page.answerCancel();
    }

    /**
     * Clicks a button in the editor other than saving, cancelling, deleting and restoring.
     */
    public void clicks(String button) {
        page.clickEditorButton(button);
    }

    public void confirms() {
        page.answerConfirm();
    }

    // --- What the administrator sees ---

    /**
     * The keys of the items in the list, from top to bottom.
     */
    public List<String> listed() {
        return page.column(keyColumn);
    }

    public List<String> listedIn(String column) {
        return page.column(column);
    }

    public Map<String, String> row(String key) {
        return page.rowWhere(keyColumn, key);
    }

    public boolean seesAsDeleted(String key) {
        return page.isMarkedAsDeleted(keyColumn, key);
    }

    public List<String> selected() {
        return page.selected(keyColumn);
    }

    public boolean isEditing() {
        return page.showsEditor();
    }

    public String editorTitle() {
        return page.editorTitle();
    }

    public String valueOf(String field) {
        return page.valueOf(field);
    }

    public String textIn(String textArea) {
        return page.textAreaValueOf(textArea);
    }

    public String chosen(String field) {
        return page.chosen(field);
    }

    public List<String> chosenItems(String field) {
        return page.chosenItems(field);
    }

    public List<String> choicesOf(String field) {
        return page.choicesOf(field);
    }

    public List<String> addChoices() {
        return page.addChoices();
    }

    /**
     * The error shown under the field, or an empty string if there is none.
     */
    public String errorOf(String field) {
        return page.errorOf(field);
    }

    public boolean seesItAsRequired(String field) {
        return page.isMarkedRequired(field);
    }

    /**
     * The text of the QR code with the given description, or empty if none is shown.
     */
    public java.util.Optional<String> qrCode(String description) {
        return page.qrCode(description);
    }

    public boolean seesField(String field) {
        return page.showsField(field);
    }

    public boolean canChange(String field) {
        return page.canEdit(field);
    }

    public boolean canDo(String action) {
        return page.showsEditorButton(action);
    }

    public boolean wasToldThat(String message) {
        return page.wasToldThat(message);
    }

    public boolean isAsked(String question) {
        return page.isAsked(question);
    }

    public boolean isAskedAnything() {
        return page.isAskedAnything();
    }

    public String viewTitle() {
        return page.viewTitle().orElse("");
    }
}
