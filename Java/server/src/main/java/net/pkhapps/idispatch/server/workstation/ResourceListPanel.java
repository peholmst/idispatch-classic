package net.pkhapps.idispatch.server.workstation;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import net.pkhapps.idispatch.server.assignment.AssignmentFormatters;
import net.pkhapps.idispatch.server.resource.ResourceFormatters;
import net.pkhapps.idispatch.server.ui.Formatters;

import static net.pkhapps.idispatch.server.ui.SignalBindings.bindItems;

/**
 * Every resource with its state and the assignment it is on.
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

        var grid = new Grid<ResourceRow>();
        grid.addThemeVariants(GridVariant.NO_BORDER, GridVariant.ROW_STRIPES);
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        grid.setAriaLabel(getTranslation("dispatch.all-resources"));
        add(grid);

        grid.addColumn(ResourceRow::callSign).setHeader(getTranslation("dispatch.resource"));
        grid.addColumn(new ComponentRenderer<>(this::createStatusButton)).setHeader(getTranslation("dispatch.state"));
        grid.addColumn(row -> Formatters.formatDateTime(row.status().timestamp())).setHeader(getTranslation("dispatch.timestamp"));
        grid.addColumn(row -> AssignmentFormatters.formatAssignment(row.assignment()))
                .setHeader(getTranslation("dispatch.assignment"));
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
