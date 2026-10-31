package net.pkhapps.idispatch.server.workstation;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import net.pkhapps.idispatch.server.assignment.AssignmentFormatters;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.assignment.AssignmentListItem;
import net.pkhapps.idispatch.server.assignment.AssignmentService;
import net.pkhapps.idispatch.server.resource.ResourceFormatters;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceQueries;
import net.pkhapps.idispatch.server.resource.ResourceStatus;
import net.pkhapps.idispatch.server.resource.ResourceStatusChanged;
import net.pkhapps.idispatch.server.resource.ResourceStatusService;
import net.pkhapps.idispatch.server.ui.Formatters;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

class ResourceListPanel extends VerticalLayout {

    /**
     * The status of every resource, with the call signs of the resources and the assignments they are on, loaded
     * together.
     */
    private record Resources(List<ResourceStatus> statuses, Map<ResourceId, String> callSigns,
                             Map<AssignmentId, AssignmentListItem> assignments) {

        static final Resources NONE = new Resources(List.of(), Map.of(), Map.of());
    }

    private final ResourceStatusService resourceStatusService;
    private final AssignmentService assignmentService;
    private final ResourceQueries resourceQueries;
    private final ValueSignal<Resources> resources = new ValueSignal<>(Resources.NONE);
    private final ApplicationEventMulticaster eventMulticaster;
    private OwnerUpdater ownerUpdater;
    private final ApplicationListener<ResourceStatusChanged> resourceStatusChangedListener = event -> ownerUpdater.update(this::refresh);

    public ResourceListPanel(ResourceStatusService resourceStatusService, AssignmentService assignmentService,
                             ResourceQueries resourceQueries, ApplicationEventMulticaster eventMulticaster) {
        this.resourceStatusService = resourceStatusService;
        this.assignmentService = assignmentService;
        this.resourceQueries = resourceQueries;
        this.eventMulticaster = eventMulticaster;
        addClassName("resource-list");

        setSizeFull();
        setMargin(false);
        setPadding(false);
        setSpacing(false);

        var grid = new Grid<ResourceStatus>();
        grid.addThemeVariants(
                GridVariant.NO_BORDER,
                GridVariant.ROW_STRIPES
        );
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        grid.setAriaLabel(getTranslation("dispatch.all-resources"));
        add(grid);

        grid.addColumn(rs -> resources.peek().callSigns().get(rs.resourceId())).setHeader(getTranslation("dispatch.resource"));
        grid.addColumn(new ComponentRenderer<>(this::createStatusButton)).setHeader(getTranslation("dispatch.state"));
        grid.addColumn(rs -> Formatters.formatDateTime(rs.timestamp())).setHeader(getTranslation("dispatch.timestamp"));
        grid.addColumn(rs -> rs.assignment() == null ? null
                : AssignmentFormatters.formatAssignment(resources.peek().assignments().get(rs.assignment())))
                .setHeader(getTranslation("dispatch.assignment"));
        Signal.effect(grid, () -> grid.setItems(resources.get().statuses()));
    }

    private Button createStatusButton(ResourceStatus resourceStatus) {
        var btn = new Button(ResourceFormatters.formatResourceState(resourceStatus.state()));
        btn.addThemeVariants(ButtonVariant.SMALL);
        btn.addClassName(ResourceFormatters.resourceStateClassName(resourceStatus.state()));
        btn.addClickListener(event -> new ChangeResourceStatusDialog(resourceStatusService, resourceStatus,
                resources.peek().callSigns().get(resourceStatus.resourceId())).open());
        return btn;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ownerUpdater = OwnerUpdater.forCurrentUser(attachEvent.getUI());
        eventMulticaster.addApplicationListener(resourceStatusChangedListener);
        refresh();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        eventMulticaster.removeApplicationListener(resourceStatusChangedListener);
    }

    private void refresh() {
        var statuses = resourceStatusService.getStatusOfAllResources();
        var callSigns = resourceQueries.findCallSigns(statuses.stream().map(ResourceStatus::resourceId).toList());
        var assignments = new HashMap<AssignmentId, AssignmentListItem>();
        assignmentService.findOpenAssignments().forEach(assignment -> assignments.put(assignment.id(), assignment));
        // Resources can still refer to an assignment that has been closed
        var others = statuses.stream().map(ResourceStatus::assignment).filter(Objects::nonNull)
                .filter(id -> !assignments.containsKey(id)).collect(Collectors.toSet());
        assignmentService.findAssignments(others).forEach(assignment -> assignments.put(assignment.id(), assignment));
        resources.set(new Resources(statuses, Map.copyOf(callSigns), Map.copyOf(assignments)));
    }
}
