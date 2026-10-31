package net.pkhapps.idispatch.server.workstation;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import net.pkhapps.idispatch.server.assignment.AssignmentEvent;
import net.pkhapps.idispatch.server.assignment.AssignmentFormatters;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.assignment.AssignmentListItem;
import net.pkhapps.idispatch.server.assignment.AssignmentService;
import net.pkhapps.idispatch.server.ui.Formatters;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.lang.Nullable;

import java.util.function.Consumer;

public class AssignmentListPanel extends VerticalLayout {

    private final AssignmentService assignmentService;
    private final ApplicationEventMulticaster eventMulticaster;
    private final Grid<AssignmentListItem> grid;
    private OwnerUpdater ownerUpdater;
    private final ApplicationListener<AssignmentEvent> assignmentEventListener = event -> ownerUpdater.update(this::refresh);

    public AssignmentListPanel(AssignmentService assignmentService,
                               ApplicationEventMulticaster eventMulticaster,
                               Consumer<AssignmentId> onAssignmentSelected) {
        this.assignmentService = assignmentService;
        this.eventMulticaster = eventMulticaster;
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

        if (onAssignmentSelected != null) {
            grid.addSelectionListener(event -> onAssignmentSelected.accept(
                            event.getFirstSelectedItem().map(AssignmentListItem::id).orElse(null)
                    )
            );
        }

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

    private void refresh() {
        var selectedId = grid.getSelectionModel().getFirstSelectedItem().map(AssignmentListItem::id).orElse(null);
        grid.setItems(assignmentService.findOpenAssignments());
        selectAssignmentById(selectedId);
    }

    public void selectAssignmentById(@Nullable AssignmentId id) {
        if (id == null) {
            grid.deselectAll();
        } else {
            grid.getListDataView().getItems().filter(assignment -> id.equals(assignment.id())).findFirst().ifPresent(grid::select);
        }
    }
}
