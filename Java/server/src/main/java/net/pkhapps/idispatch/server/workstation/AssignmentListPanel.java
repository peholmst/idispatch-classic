package net.pkhapps.idispatch.server.workstation;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import net.pkhapps.idispatch.server.assignment.Assignment;
import net.pkhapps.idispatch.server.assignment.AssignmentEvent;
import net.pkhapps.idispatch.server.assignment.AssignmentFormatters;
import net.pkhapps.idispatch.server.assignment.AssignmentService;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeFormatters;
import net.pkhapps.idispatch.server.municipality.MunicipalityFormatters;
import net.pkhapps.idispatch.server.ui.Formatters;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.lang.Nullable;

import java.util.function.Consumer;

public class AssignmentListPanel extends VerticalLayout {

    private final AssignmentService assignmentService;
    private final ApplicationEventMulticaster eventMulticaster;
    private final Grid<Assignment> grid;
    private OwnerUpdater ownerUpdater;
    private final ApplicationListener<AssignmentEvent> assignmentEventListener = event -> ownerUpdater.update(this::refresh);

    public AssignmentListPanel(AssignmentService assignmentService,
                               ApplicationEventMulticaster eventMulticaster,
                               Consumer<Long> onAssignmentSelected) {
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

        grid.addColumn(Assignment::getId).setHeader(getTranslation("assignment.id"));
        grid.addColumn(assignment -> Formatters.formatDateTime(assignment.getOpened())).setHeader(getTranslation("assignment.date"));
        grid.addColumn(assignment -> AssignmentTypeFormatters.formatAssignmentType(assignment.getType())).setHeader(getTranslation("assignment.type"));
        grid.addComponentColumn(this::createUrgencyBadge).setHeader(getTranslation("assignment.urgency"));
        grid.addColumn(assignment -> MunicipalityFormatters.formatMunicipality(assignment.getMunicipality())).setHeader(getTranslation("assignment.municipality"));
        grid.addColumn(Assignment::getAddress).setHeader(getTranslation("assignment.address"));

        if (onAssignmentSelected != null) {
            grid.addSelectionListener(event -> onAssignmentSelected.accept(
                            event.getFirstSelectedItem().map(Assignment::getId).orElse(null)
                    )
            );
        }

        add(grid);
    }

    private Badge createUrgencyBadge(Assignment assignment) {
        var badge = new Badge();
        if (assignment.getUrgency() != null) {
            badge.setText(assignment.getUrgency().name());
            badge.addClassName(AssignmentFormatters.assignmentUrgencyClassName(assignment.getUrgency()));
        }
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
        var selectedId = grid.getSelectionModel().getFirstSelectedItem().map(Assignment::getId).orElse(null);
        grid.setItems(assignmentService.findOpenAssignments());
        selectAssignmentById(selectedId);
    }

    public void selectAssignmentById(@Nullable Long id) {
        if (id == null) {
            grid.deselectAll();
        } else {
            grid.getListDataView().getItems().filter(assignment -> id.equals(assignment.getId())).findFirst().ifPresent(grid::select);
        }
    }
}
