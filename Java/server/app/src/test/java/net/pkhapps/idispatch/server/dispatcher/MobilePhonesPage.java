package net.pkhapps.idispatch.server.dispatcher;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.flow.component.grid.Grid;
import net.pkhapps.idispatch.server.ui.AppShell;
import net.pkhapps.idispatch.server.ui.GridRows;

import java.util.List;

/**
 * Where things are on the mobile phones view: a grid of the phones with the unit each is on, whose unit button opens a
 * dialog for choosing the new unit.
 */
public class MobilePhonesPage extends AppShell {

    private static final String PHONE = "Phone";
    private static final String UNIT = "Unit";
    private static final String NEW_UNIT = "New unit";

    public MobilePhonesPage(BrowserlessUIContext window) {
        super(window);
    }

    public void open() {
        window.navigate(MobilePhonesView.class);
    }

    public void filterBy(String text) {
        window.findTextField().withAriaLabel("Filter phones by description or call sign").setValue(text);
    }

    /**
     * The phones as listed, by the Phone column, from top to bottom.
     */
    public List<String> phones() {
        return phonesGrid().column(PHONE);
    }

    public String unitOf(String phone) {
        return phonesGrid().rowWhere(PHONE, phone).get(UNIT);
    }

    public void clickUnitOf(String phone) {
        phonesGrid().clickCellWhere(PHONE, phone, UNIT);
    }

    public String emptyText() {
        return phonesGrid().emptyStateText();
    }

    // --- The dialog for changing the unit ---

    public String dialogTitle() {
        return window.findDialog().component().getHeaderTitle();
    }

    public List<String> unitChoices() {
        return window.findComboBox(Object.class).withLabel(NEW_UNIT).getSuggestions();
    }

    public void chooseUnit(String callSign) {
        window.findComboBox(Object.class).withLabel(NEW_UNIT).selectItem(callSign);
    }

    public void save() {
        window.findButton().withText("Save").click();
    }

    public void cancel() {
        window.findButton().withText("Cancel").click();
    }

    public boolean showsDialog() {
        return window.findDialog().components().stream().findAny().isPresent();
    }

    public void receiveUpdates() {
        window.roundTrip();
    }

    private GridRows phonesGrid() {
        return GridRows.of(window, window.find(Grid.class)
                .withCondition(g -> g.getAriaLabel().filter("Mobile phones"::equals).isPresent())
                .single());
    }
}
