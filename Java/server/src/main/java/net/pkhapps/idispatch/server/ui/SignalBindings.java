package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridSelectionColumn;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Signal bindings for component properties that Vaadin has no {@code bind…} method for.
 */
public final class SignalBindings {

    private SignalBindings() {
    }

    /**
     * Shows the items of a list signal in a grid. The items are set once and refreshed on every change, because setting
     * them again would clear the grid's selection, and a bound selection would pass that on as a deselection. Set an
     * identifier provider on the returned data view, so that a selected row stays selected when its item is replaced
     * by an equal one with newer data. Like the {@code bind…} methods of the components, the binding is active while
     * the grid is attached.
     * <p>
     * In multi-select mode, set the selection mode before binding, so that the select-all checkbox is kept right as
     * well: Grid works out its state only when the selection changes, and counts an empty grid as all selected.
     *
     * @return the grid's data view
     */
    public static <T> GridListDataView<T> bindItems(Grid<T> grid, Signal<List<T>> items) {
        var rows = new ArrayList<T>();
        var dataView = grid.setItems(rows);
        Signal.effect(grid, () -> {
            var current = items.get();
            rows.clear();
            rows.addAll(current);
            dataView.refreshAll();
            updateSelectAllCheckbox(grid, rows);
        });
        if (grid.getSelectionModel() instanceof GridMultiSelectionModel<T>) {
            grid.addSelectionListener(event -> updateSelectAllCheckbox(grid, rows));
        }
        return dataView;
    }

    /**
     * Sets the select-all checkbox from the rows and the selection once the round trip's changes are done, after Grid
     * has set it itself. Grid sets it only when the selection changes, so it goes stale when the rows change, and it
     * shows an empty grid with an empty selection as all selected.
     */
    private static <T> void updateSelectAllCheckbox(Grid<T> grid, List<T> rows) {
        grid.getUI().ifPresent(ui -> ui.beforeClientResponse(grid, context -> {
            if (!(grid.getSelectionModel() instanceof GridMultiSelectionModel<T> model)
                    || !model.isSelectAllCheckboxVisible()) {
                return;
            }
            var size = rows.size();
            var selected = rows.stream().filter(model::isSelected).count();
            grid.getChildren().filter(GridSelectionColumn.class::isInstance).map(GridSelectionColumn.class::cast)
                    .findFirst().ifPresent(column -> {
                        column.setSelectAllCheckboxState(size > 0 && selected == size);
                        column.setSelectAllCheckboxIndeterminateState(selected > 0 && selected < size);
                    });
        }));
    }

    /**
     * Binds the field's error to the signal: the field is invalid and shows the message while the signal holds one,
     * and is valid while it holds {@code null}. Puts the field in manual validation mode, so that its own constraints
     * do not reset the invalid state. Like the {@code bind…} methods of the components, the binding is active while the
     * field is attached.
     *
     * @return a registration that removes the binding
     */
    public static <F extends Component & HasValidation> Registration bindErrorMessage(F field,
                                                                                     Signal<@Nullable String> errorMessage) {
        field.setManualValidation(true);
        return Signal.effect(field, () -> {
            var message = errorMessage.get();
            field.setInvalid(message != null);
            field.setErrorMessage(message == null ? "" : message);
        });
    }
}
