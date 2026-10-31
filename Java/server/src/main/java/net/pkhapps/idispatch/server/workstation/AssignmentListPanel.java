package net.pkhapps.idispatch.server.workstation;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import net.pkhapps.idispatch.server.assignment.AssignmentEvent;
import net.pkhapps.idispatch.server.assignment.AssignmentFormatters;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.assignment.AssignmentListItem;
import net.pkhapps.idispatch.server.assignment.AssignmentService;
import net.pkhapps.idispatch.server.ui.Formatters;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;

import java.util.List;

class AssignmentListPanel extends VerticalLayout {

    private final AssignmentService assignmentService;
    private final ApplicationEventMulticaster eventMulticaster;
    private final Grid<AssignmentListItem> grid;
    private final ValueSignal<List<AssignmentListItem>> assignments = new ValueSignal<>(List.of());
    private final ValueSignal<@Nullable AssignmentId> selected;
    private OwnerUpdater ownerUpdater;
    private final ApplicationListener<AssignmentEvent> assignmentEventListener = event -> ownerUpdater.update(this::refresh);

    /**
     * @param selected the assignment selected in the list, which the user changes by selecting another one
     */
    public AssignmentListPanel(AssignmentService assignmentService,
                               ApplicationEventMulticaster eventMulticaster,
                               ValueSignal<@Nullable AssignmentId> selected) {
        this.assignmentService = assignmentService;
        this.eventMulticaster = eventMulticaster;
        this.selected = selected;
        addClassName("assignment-list");

        setSizeFull();
        setMargin(false);
        setPadding(false);
        setSpacing(false);

        grid = new Grid<>();
        grid.addThemeVariants(
                GridVariant.NO_BORDER
        );
        grid.setSizeFull();
        grid.setAriaLabel(getTranslation("dispatch.open-assignments"));

        grid.addColumn(assignment -> assignment.id().value()).setHeader(getTranslation("assignment.id"));
        grid.addColumn(assignment -> Formatters.formatDateTime(assignment.opened())).setHeader(getTranslation("assignment.date"));
        grid.addColumn(AssignmentFormatters::formatAssignmentType).setHeader(getTranslation("assignment.type"));
        grid.addComponentColumn(this::createUrgencyBadge).setHeader(getTranslation("assignment.urgency"));
        grid.addColumn(AssignmentFormatters::formatMunicipality).setHeader(getTranslation("assignment.municipality"));
        grid.addColumn(AssignmentListItem::address).setHeader(getTranslation("assignment.address"));

        grid.addSelectionListener(event -> {
            if (event.isFromClient()) {
                selected.set(event.getFirstSelectedItem().map(AssignmentListItem::id).orElse(null));
            }
        });
        // Setting the items clears the selection, so the row is selected again after
        Signal.effect(grid, () -> {
            grid.setItems(assignments.get());
            showSelection();
        });
        Signal.effect(grid, () -> {
            selected.get();
            showSelection();
        });

        add(grid);
    }

    private Badge createUrgencyBadge(AssignmentListItem assignment) {
        var badge = new Badge();
        badge.setText(assignment.urgency().name());
        badge.addClassName(AssignmentFormatters.assignmentUrgencyClassName(assignment.urgency()));
        return badge;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ownerUpdater = OwnerUpdater.forCurrentUser(attachEvent.getUI());
        eventMulticaster.addApplicationListener(assignmentEventListener);
        refresh();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        eventMulticaster.removeApplicationListener(assignmentEventListener);
    }

    /**
     * Lists the open assignments again. An assignment that has been closed since is no longer selected.
     */
    private void refresh() {
        var open = assignmentService.findOpenAssignments();
        assignments.set(open);
        var selectedId = selected.peek();
        if (selectedId != null && open.stream().noneMatch(assignment -> assignment.id().equals(selectedId))) {
            selected.set(null);
        }
    }

    private void showSelection() {
        var selectedId = selected.peek();
        grid.getListDataView().getItems().filter(assignment -> assignment.id().equals(selectedId)).findFirst()
                .ifPresentOrElse(grid::select, grid::deselectAll);
    }
}
