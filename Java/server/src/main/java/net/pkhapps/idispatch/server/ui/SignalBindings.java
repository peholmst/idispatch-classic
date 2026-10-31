package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.grid.Grid;
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
        });
        return dataView;
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
