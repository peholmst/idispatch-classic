package net.pkhapps.idispatch.server.ui.dws;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import net.pkhapps.idispatch.server.boundary.ResourceStatusService;
import net.pkhapps.idispatch.server.entity.ResourceStatus;
import net.pkhapps.idispatch.server.events.ResourceStatusChanged;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;

public class ResourceListPanel extends VerticalLayout {

    private final ResourceStatusService resourceStatusService;
    private final ApplicationEventMulticaster eventMulticaster;
    private final Grid<ResourceStatus> grid;
    private final ApplicationListener<ResourceStatusChanged> resourceStatusChangedListener = event -> refresh();

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
                GridVariant.LUMO_NO_BORDER,
                GridVariant.LUMO_COMPACT,
                GridVariant.LUMO_ROW_STRIPES
        );
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        add(grid);

        grid.addColumn(rs -> Formatters.formatResource(rs.getResource())).setHeader("Resource");
        grid.addColumn(new ComponentRenderer<>(this::createStatusButton)).setHeader("State");
        grid.addColumn(rs -> Formatters.formatDateTime(rs.getTimestamp())).setHeader("Timestamp");
        grid.addColumn(rs -> Formatters.formatAssignment(rs.getAssignment())).setHeader("Assignment");
    }

    private Button createStatusButton(ResourceStatus resourceStatus) {
        var btn = new Button(Formatters.formatResourceState(resourceStatus.getState()));
        btn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        btn.addClassName(Formatters.resourceStateClassName(resourceStatus.getState()));
        btn.addClickListener(event -> new ChangeResourceStatusDialog(resourceStatusService, resourceStatus).open());
        return btn;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
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
