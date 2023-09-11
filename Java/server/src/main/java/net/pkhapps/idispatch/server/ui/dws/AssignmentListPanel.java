package net.pkhapps.idispatch.server.ui.dws;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import net.pkhapps.idispatch.server.boundary.AssignmentService;
import net.pkhapps.idispatch.server.entity.Assignment;
import net.pkhapps.idispatch.server.events.AssignmentEvent;
import net.pkhapps.idispatch.server.ui.Formatters;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.lang.Nullable;

import java.util.function.Consumer;

public class AssignmentListPanel extends VerticalLayout {

    private final AssignmentService assignmentService;
    private final ApplicationEventMulticaster eventMulticaster;
    private final Grid<Assignment> grid;
    private final ApplicationListener<AssignmentEvent> assignmentEventListener = event -> refresh();

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
                GridVariant.LUMO_NO_BORDER,
                GridVariant.LUMO_COMPACT
        );
        grid.setSizeFull();

        grid.addColumn(Assignment::getId).setHeader("ID");
        grid.addColumn(assignment -> Formatters.formatDateTime(assignment.getOpened())).setHeader("Date");
        grid.addColumn(assignment -> Formatters.formatAssignmentType(assignment.getType())).setHeader("Type");
        grid.addColumn(Assignment::getUrgency).setHeader("Urgency").setClassNameGenerator((assignment -> Formatters.assignmentUrgencyClassName(assignment.getUrgency())));
        grid.addColumn(assignment -> Formatters.formatMunicipality(assignment.getMunicipality())).setHeader("Municipality");
        grid.addColumn(Assignment::getAddress).setHeader("Address");

        if (onAssignmentSelected != null) {
            grid.addSelectionListener(event -> onAssignmentSelected.accept(
                            event.getFirstSelectedItem().map(Assignment::getId).orElse(null)
                    )
            );
        }

        add(grid);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
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
