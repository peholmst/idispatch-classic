package net.pkhapps.idispatch.server.workstation;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceFormatters;
import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.resource.ResourceStatus;
import net.pkhapps.idispatch.server.resource.ResourceStatusService;


public class ChangeResourceStatusDialog extends Dialog {

    private final ResourceStatusService resourceStatusService;
    private final ResourceId resource;
    private final VerticalLayout buttons;

    public ChangeResourceStatusDialog(ResourceStatusService resourceStatusService, ResourceStatus resourceStatus,
                                      String callSign) {
        this.resourceStatusService = resourceStatusService;
        this.resource = resourceStatus.getResourceId();
        addClassName("change-resource-status-dialog");
        buttons = new VerticalLayout();
        add(buttons);

        setHeaderTitle(getTranslation("dispatch.change-state.title", callSign));
        resourceStatus.getManualValidNextStates().stream().sorted().forEach(this::createResourceStateButton);
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);
    }

    private void createResourceStateButton(ResourceState resourceState) {
        var button = new Button(ResourceFormatters.formatResourceState(resourceState));
        button.addClassName(ResourceFormatters.resourceStateClassName(resourceState));
        button.addClickListener(event -> {
            resourceStatusService.setResourceState(resource, resourceState);
            close();
        });
        button.setWidthFull();
        buttons.add(button);
    }
}
