package net.pkhapps.idispatch.server.workstation;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import net.pkhapps.idispatch.server.assignment.AssignmentFormatters;
import net.pkhapps.idispatch.server.resource.ResourceFormatters;
import net.pkhapps.idispatch.server.ui.Formatters;

import java.util.Comparator;

import static net.pkhapps.idispatch.server.ui.SignalBindings.bindItems;

/**
 * Every resource with its state and the assignment it is on, filtered by call sign and sortable by any column.
 */
class ResourceListPanel extends VerticalLayout {

    private final DispatchViewModel viewModel;

    ResourceListPanel(DispatchViewModel viewModel) {
        this.viewModel = viewModel;
        addClassName("resource-list");

        setSizeFull();
        setMargin(false);
        setPadding(false);
        setSpacing(false);

        var filterField = new TextField();
        filterField.setPlaceholder(getTranslation("dispatch.filter-resources"));
        filterField.setAriaLabel(getTranslation("dispatch.filter-resources-label"));
        filterField.setPrefixComponent(VaadinIcon.SEARCH.create());
        filterField.setClearButtonVisible(true);
        filterField.setValueChangeMode(ValueChangeMode.LAZY);
        filterField.setWidthFull();
        filterField.bindValue(viewModel.resourceFilter(), viewModel::filterResources);
        add(filterField);

        var grid = new Grid<ResourceRow>();
        grid.addThemeVariants(GridVariant.NO_BORDER, GridVariant.ROW_STRIPES);
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        grid.setAriaLabel(getTranslation("dispatch.all-resources"));
        addAndExpand(grid);

        // The rows are in memory, so the grid sorts them itself with the columns' comparators
        grid.addColumn(ResourceRow::callSign).setHeader(getTranslation("dispatch.resource"))
                .setComparator(Comparator.comparing(ResourceRow::callSign,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        // By the order of the states, from reserved to out of service, rather than by their names
        grid.addColumn(new ComponentRenderer<>(this::createStatusButton)).setHeader(getTranslation("dispatch.state"))
                .setComparator(Comparator.comparing(row -> row.status().state()));
        grid.addColumn(row -> Formatters.formatDateTime(row.status().timestamp()))
                .setHeader(getTranslation("dispatch.timestamp"))
                .setComparator(Comparator.comparing(row -> row.status().timestamp()));
        grid.addColumn(row -> AssignmentFormatters.formatAssignment(row.assignment()))
                .setHeader(getTranslation("dispatch.assignment"))
                .setComparator(Comparator.comparing(row -> AssignmentFormatters.formatAssignment(row.assignment()),
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        bindItems(grid, viewModel.resources()).setIdentifierProvider(ResourceRow::resourceId);
    }

    private Button createStatusButton(ResourceRow row) {
        var btn = new Button(ResourceFormatters.formatResourceState(row.status().state()));
        btn.addThemeVariants(ButtonVariant.SMALL);
        btn.addClassName(ResourceFormatters.resourceStateClassName(row.status().state()));
        btn.addClickListener(event -> new ChangeResourceStatusDialog(row,
                state -> viewModel.changeResourceState(row.resourceId(), state)).open());
        return btn;
    }
}
