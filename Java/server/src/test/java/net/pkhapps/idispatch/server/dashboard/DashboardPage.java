package net.pkhapps.idispatch.server.dashboard;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import net.pkhapps.idispatch.server.ui.AppShell;
import net.pkhapps.idispatch.server.ui.GridRows;

import java.util.List;
import java.util.Map;

/**
 * Where things are on the home view: a card for each figure and the list of units.
 */
public class DashboardPage extends AppShell {

    public DashboardPage(BrowserlessUIContext window) {
        super(window);
    }

    public int activeAssignments() {
        return figure("Active assignments");
    }

    public int openReports() {
        return figure("Open reports");
    }

    /**
     * The rows of the list of units, as cell texts by column header, top to bottom.
     */
    public List<Map<String, String>> units() {
        return GridRows.of(window, window.find(Grid.class).single()).all();
    }

    private int figure(String title) {
        var card = window.find(Card.class).withCondition(c -> title.equals(c.getTitleAsText())).single();
        var value = card.getChildren().filter(Span.class::isInstance).map(Span.class::cast).findFirst().orElseThrow();
        return Integer.parseInt(value.getText());
    }
}
