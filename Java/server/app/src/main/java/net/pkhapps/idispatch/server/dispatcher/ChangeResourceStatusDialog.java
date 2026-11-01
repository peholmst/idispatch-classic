package net.pkhapps.idispatch.server.dispatcher;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.SerializableConsumer;
import net.pkhapps.idispatch.server.resource.ResourceFormatters;
import net.pkhapps.idispatch.server.resource.ResourceState;

/**
 * Asks which state to put a resource in, offering the states it can be put in by hand.
 */
class ChangeResourceStatusDialog extends Dialog {

    private final SerializableConsumer<ResourceState> changeState;
    private final VerticalLayout buttons;

    /**
     * @param changeState changes the resource to the chosen state
     */
    ChangeResourceStatusDialog(ResourceRow resource, SerializableConsumer<ResourceState> changeState) {
        this.changeState = changeState;
        addClassName("change-resource-status-dialog");
        buttons = new VerticalLayout();
        add(buttons);

        setHeaderTitle(getTranslation("dispatch.change-state.title", resource.callSign()));
        resource.status().manualValidNextStates().stream().sorted().forEach(this::createResourceStateButton);
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);
    }

    private void createResourceStateButton(ResourceState resourceState) {
        var button = new Button(ResourceFormatters.formatResourceState(resourceState));
        button.addClassName(ResourceFormatters.resourceStateClassName(resourceState));
        button.addClickListener(event -> {
            changeState.accept(resourceState);
            close();
        });
        button.setWidthFull();
        buttons.add(button);
    }
}
