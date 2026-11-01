package net.pkhapps.idispatch.server.dispatcher;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxTester;
import com.vaadin.flow.component.html.Span;
import net.pkhapps.idispatch.server.ui.AppShell;

import java.util.List;

/**
 * Where things are on the resource status view: a card per resource with its call sign as the title, its state as the
 * subtitle and the assignment it is on below, and the filters in the toolbar.
 */
public class ResourceStatusPage extends AppShell {

    public ResourceStatusPage(BrowserlessUIContext window) {
        super(window);
    }

    public void open() {
        window.navigate(ResourceStatusView.class);
    }

    /**
     * The call signs on the cards, in the order they are shown.
     */
    public List<String> callSigns() {
        return cards().stream().map(Card::getTitleAsText).toList();
    }

    public String stateOf(String callSign) {
        return ((Span) card(callSign).getSubtitle()).getText();
    }

    /**
     * The assignment on the card, or empty if the card shows none.
     */
    public String assignmentOf(String callSign) {
        return card(callSign).getChildren().filter(Span.class::isInstance).map(Span.class::cast)
                .map(Span::getText).findFirst().orElse("");
    }

    public void filterByCallSign(String text) {
        window.findTextField().withAriaLabel("Filter resources by call sign").setValue(text);
    }

    public void filterByStates(String... states) {
        new MultiSelectComboBoxTester<>(stateFilter()).selectItem(states);
    }

    public void clearStateFilter() {
        new MultiSelectComboBoxTester<>(stateFilter()).clickClearButton();
    }

    public boolean showsNoResources() {
        return showsText("No resources match the filters");
    }

    public void receiveUpdates() {
        window.roundTrip();
    }

    @SuppressWarnings("unchecked")
    private MultiSelectComboBox<Object> stateFilter() {
        return window.find(MultiSelectComboBox.class).all().stream()
                .map(box -> (MultiSelectComboBox<Object>) box)
                .filter(box -> box.getAriaLabel().filter("Filter resources by state"::equals).isPresent())
                .findFirst()
                .orElseThrow(() -> new AssertionError("No state filter"));
    }

    private List<Card> cards() {
        return window.find(Card.class).all();
    }

    private Card card(String callSign) {
        return cards().stream().filter(card -> callSign.equals(card.getTitleAsText())).findFirst()
                .orElseThrow(() -> new AssertionError("No card for " + callSign));
    }
}
