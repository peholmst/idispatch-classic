package net.pkhapps.idispatch.server.ui.dws;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import net.pkhapps.idispatch.server.boundary.ResourceStatusService;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.ResourceState;
import net.pkhapps.idispatch.server.entity.ResourceStatus;
import net.pkhapps.idispatch.server.ui.Formatters;


@CssImport("./styles/dispatch-view-styles.css")
public class ChangeResourceStatusDialog extends Dialog {

    private final ResourceStatusService resourceStatusService;
    private final Resource resource;
    private final VerticalLayout buttons;

    public ChangeResourceStatusDialog(ResourceStatusService resourceStatusService, ResourceStatus resourceStatus) {
        this.resourceStatusService = resourceStatusService;
        this.resource = resourceStatus.getResource();
        addClassName("change-resource-status-dialog");
        buttons = new VerticalLayout();
        add(buttons);

        setHeaderTitle("Change state of " + resourceStatus.getResource().getCallSign());
        resourceStatus.getManualValidNextStates().stream().sorted().forEach(this::createResourceStateButton);
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);
    }

    private void createResourceStateButton(ResourceState resourceState) {
        var button = new Button(Formatters.formatResourceState(resourceState));
        button.addClassName(Formatters.resourceStateClassName(resourceState));
        button.addClickListener(event -> {
            resourceStatusService.setResourceState(resource, resourceState);
            close();
        });
        button.setWidthFull();
        buttons.add(button);
    }
}
