package net.pkhapps.idispatch.server.ui;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridLocator;
import com.vaadin.flow.component.grid.GridTester;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Reads and works a grid the way the user sees it: rows of cell texts under the column headers. Page objects use it so
 * that the stories never depend on the type of the items behind the grid.
 */
public final class GridRows {

    private final BrowserlessUIContext window;
    private final GridLocator<Object> grid;

    private GridRows(BrowserlessUIContext window, GridLocator<Object> grid) {
        this.window = window;
        this.grid = grid;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static GridRows of(BrowserlessUIContext window, Grid<?> grid) {
        return new GridRows(window, window.use((Grid) grid));
    }

    /**
     * Returns every row as a map from column header to cell text.
     */
    public List<Map<String, String>> all() {
        return IntStream.range(0, grid.size()).mapToObj(this::row).toList();
    }

    /**
     * Returns the texts in the column with the given header, from top to bottom.
     */
    public List<String> column(String header) {
        var index = columnIndex(header);
        return IntStream.range(0, grid.size()).mapToObj(row -> text(grid.getCellText(row, index))).toList();
    }

    /**
     * Returns the row whose cell under the given header has the given text.
     */
    public Map<String, String> rowWhere(String header, String text) {
        return row(indexOfRowWhere(header, text));
    }

    public boolean hasRowWhere(String header, String text) {
        return column(header).contains(text);
    }

    /**
     * Returns the position of the row, from 0, whose cell under the given header has the given text.
     */
    public int indexOfRowWhere(String header, String text) {
        var index = column(header).indexOf(text);
        if (index < 0) {
            throw new AssertionError("No row with " + header + " " + text + " in " + all());
        }
        return index;
    }

    public void selectRowWhere(String header, String text) {
        grid.select(indexOfRowWhere(header, text));
    }

    public void deselectRowWhere(String header, String text) {
        grid.deselect(indexOfRowWhere(header, text));
    }

    public void selectRow(int index) {
        grid.select(index);
    }

    /**
     * Returns the cell texts under the given header of the selected rows.
     */
    public List<String> selected(String header) {
        var selected = grid.getSelected();
        var index = columnIndex(header);
        return IntStream.range(0, grid.size())
                .filter(row -> selected.contains(grid.getRow(row)))
                .mapToObj(row -> text(grid.getCellText(row, index)))
                .toList();
    }

    /**
     * Clicks the first button in the cell under the given header, such as a button the cell renders.
     */
    public void clickCellWhere(String header, String text, String clickedColumnHeader) {
        clickButtonInCellWhere(header, text, clickedColumnHeader, button -> true,
                "The " + clickedColumnHeader + " cell has no button");
    }

    /**
     * Clicks the button with the given accessible name in the cell under the given header, such as an icon-only button
     * next to another one.
     */
    public void clickButtonInCellWhere(String header, String text, String clickedColumnHeader, String ariaLabel) {
        clickButtonInCellWhere(header, text, clickedColumnHeader,
                button -> button.getAriaLabel().filter(ariaLabel::equals).isPresent(),
                "The " + clickedColumnHeader + " cell has no button " + ariaLabel);
    }

    private void clickButtonInCellWhere(String header, String text, String clickedColumnHeader,
                                        Predicate<Button> which, String notFound) {
        var row = indexOfRowWhere(header, text);
        var cell = new GridTester<>(grid.getComponent()).getCellComponent(row, columnIndex(clickedColumnHeader));
        var button = Stream.of(cell).flatMap(GridRows::withDescendants)
                .filter(Button.class::isInstance).map(Button.class::cast)
                .filter(which)
                .findFirst()
                .orElseThrow(() -> new AssertionError(notFound));
        // A component that a renderer makes is not in the component tree that locators search
        window.test(button).click();
    }

    private static Stream<Component> withDescendants(Component component) {
        return Stream.concat(Stream.of(component), component.getChildren().flatMap(GridRows::withDescendants));
    }

    public void sortBy(String header, com.vaadin.flow.data.provider.SortDirection direction) {
        grid.sortByColumn(columnIndex(header), direction);
    }

    public String emptyStateText() {
        return grid.getComponent().getEmptyStateText();
    }

    public int size() {
        return grid.size();
    }

    private Map<String, String> row(int index) {
        var row = new LinkedHashMap<String, String>();
        var headers = headers();
        for (int column = 0; column < headers.size(); column++) {
            row.put(headers.get(column), text(grid.getCellText(index, column)));
        }
        return row;
    }

    private List<String> headers() {
        var headers = new ArrayList<String>();
        var columns = grid.getComponent().getColumns().stream().filter(c -> c.isVisible()).count();
        for (int column = 0; column < columns; column++) {
            headers.add(Objects.requireNonNullElse(grid.getHeaderCell(column), ""));
        }
        return headers;
    }

    private int columnIndex(String header) {
        var index = headers().indexOf(header);
        if (index < 0) {
            throw new AssertionError("No column " + header + " in " + headers());
        }
        return index;
    }

    private static String text(String cellText) {
        return cellText == null ? "" : cellText;
    }
}
