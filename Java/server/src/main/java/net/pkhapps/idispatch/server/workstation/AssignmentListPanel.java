package net.pkhapps.idispatch.server.workstation;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import net.pkhapps.idispatch.server.assignment.AssignmentFormatters;
import net.pkhapps.idispatch.server.assignment.AssignmentListItem;
import net.pkhapps.idispatch.server.ui.Formatters;

import static net.pkhapps.idispatch.server.ui.SignalBindings.bindItems;

/**
 * The open assignments, where the dispatcher selects the one to show in the form.
 */
class AssignmentListPanel extends VerticalLayout {

    AssignmentListPanel(DispatchViewModel viewModel) {
        addClassName("assignment-list");

        setSizeFull();
        setMargin(false);
        setPadding(false);
        setSpacing(false);

        var grid = new Grid<AssignmentListItem>();
        grid.addThemeVariants(GridVariant.NO_BORDER);
        grid.setSizeFull();
        grid.setAriaLabel(getTranslation("dispatch.open-assignments"));

        grid.addColumn(assignment -> assignment.id().value()).setHeader(getTranslation("assignment.id"));
        grid.addColumn(assignment -> Formatters.formatDateTime(assignment.opened())).setHeader(getTranslation("assignment.date"));
        grid.addColumn(AssignmentFormatters::formatAssignmentType).setHeader(getTranslation("assignment.type"));
        grid.addComponentColumn(this::createUrgencyBadge).setHeader(getTranslation("assignment.urgency"));
        grid.addColumn(AssignmentFormatters::formatMunicipality).setHeader(getTranslation("assignment.municipality"));
        grid.addColumn(AssignmentListItem::address).setHeader(getTranslation("assignment.address"));

        bindItems(grid, viewModel.openAssignments()).setIdentifierProvider(AssignmentListItem::id);
        grid.asSingleSelect().bindValue(viewModel.selectedAssignment(),
                item -> viewModel.select(item == null ? null : item.id()));

        add(grid);
    }

    private Badge createUrgencyBadge(AssignmentListItem assignment) {
        var badge = new Badge();
        badge.setText(assignment.urgency().name());
        badge.addClassName(AssignmentFormatters.assignmentUrgencyClassName(assignment.urgency()));
        return badge;
    }
}
