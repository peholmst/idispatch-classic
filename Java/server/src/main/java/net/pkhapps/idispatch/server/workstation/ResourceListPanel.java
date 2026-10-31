package net.pkhapps.idispatch.server.workstation;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import net.pkhapps.idispatch.server.assignment.AssignmentFormatters;
import net.pkhapps.idispatch.server.resource.ResourceFormatters;
import net.pkhapps.idispatch.server.resource.ResourceStatus;
import net.pkhapps.idispatch.server.resource.ResourceStatusChanged;
import net.pkhapps.idispatch.server.resource.ResourceStatusService;
import net.pkhapps.idispatch.server.ui.Formatters;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;

public class ResourceListPanel extends VerticalLayout {

    private final ResourceStatusService resourceStatusService;
    private final ApplicationEventMulticaster eventMulticaster;
    private final Grid<ResourceStatus> grid;
    private OwnerUpdater ownerUpdater;
    private final ApplicationListener<ResourceStatusChanged> resourceStatusChangedListener = event -> ownerUpdater.update(this::refresh);

    public ResourceListPanel(ResourceStatusService resourceStatusService,
                             ApplicationEventMulticaster eventMulticaster) {
        this.resourceStatusService = resourceStatusService;
        this.eventMulticaster = eventMulticaster;
        addClassName("resource-list");

        setSizeFull();
        setMargin(false);
        setPadding(false);
        setSpacing(false);

        grid = new Grid<>();
        grid.addThemeVariants(
                GridVariant.NO_BORDER,
                GridVariant.ROW_STRIPES
        );
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        grid.setAriaLabel(getTranslation("dispatch.all-resources"));
        add(grid);

        grid.addColumn(rs -> ResourceFormatters.formatResource(rs.getResource())).setHeader(getTranslation("dispatch.resource"));
        grid.addColumn(new ComponentRenderer<>(this::createStatusButton)).setHeader(getTranslation("dispatch.state"));
        grid.addColumn(rs -> Formatters.formatDateTime(rs.getTimestamp())).setHeader(getTranslation("dispatch.timestamp"));
        grid.addColumn(rs -> AssignmentFormatters.formatAssignment(rs.getAssignment())).setHeader(getTranslation("dispatch.assignment"));
    }

    private Button createStatusButton(ResourceStatus resourceStatus) {
        var btn = new Button(ResourceFormatters.formatResourceState(resourceStatus.getState()));
        btn.addThemeVariants(ButtonVariant.SMALL);
        btn.addClassName(ResourceFormatters.resourceStateClassName(resourceStatus.getState()));
        btn.addClickListener(event -> new ChangeResourceStatusDialog(resourceStatusService, resourceStatus).open());
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
        grid.setItems(resourceStatusService.getStatusOfAllResources());
    }
}
