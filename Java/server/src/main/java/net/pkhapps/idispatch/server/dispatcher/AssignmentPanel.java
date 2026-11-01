package net.pkhapps.idispatch.server.dispatcher;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.signals.Signal;
import net.pkhapps.idispatch.server.assignment.AssignmentUrgency;
import net.pkhapps.idispatch.server.assignment.MissingForDispatch;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeFormatters;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeListItem;
import net.pkhapps.idispatch.server.municipality.MunicipalityFormatters;
import net.pkhapps.idispatch.server.municipality.MunicipalityListItem;
import net.pkhapps.idispatch.server.resource.ResourceFormatters;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceListItem;
import net.pkhapps.idispatch.server.resource.ResourceState;

import java.util.Set;

import static net.pkhapps.idispatch.server.ui.SignalBindings.bindItems;

/**
 * The selected assignment in a form that saves every change right away, with the resources on it and the actions for
 * dispatching them. The form binds to an {@link AssignmentViewModel}.
 */
class AssignmentPanel extends VerticalLayout {

    private final SerializableBiConsumer<ResourceId, ResourceState> changeResourceState;

    /**
     * @param changeResourceState changes the state of a resource, as chosen in its state dialog
     */
    AssignmentPanel(AssignmentViewModel viewModel,
                    SerializableBiConsumer<ResourceId, ResourceState> changeResourceState) {
        this.changeResourceState = changeResourceState;
        addClassName("assignment-panel");

        setSizeFull();
        setMargin(false);
        setSpacing(false);
        setPadding(false);

        var noAssignmentSelected = createNoAssignmentSelectedComponent();
        noAssignmentSelected.bindVisible(viewModel.showing().map(showing -> !showing));

        var assignmentLayout = new VerticalLayout();
        var assignmentForm = new Scroller(assignmentLayout);
        assignmentForm.setSizeFull();
        assignmentForm.addClassName("content");
        assignmentForm.bindVisible(viewModel.showing());

        assignmentLayout.setSizeFull();
        assignmentLayout.setMargin(false);
        assignmentLayout.setSpacing(true);
        assignmentLayout.setPadding(true);

        // Setting a combo box's items clears its value. The ViewModel empties the form before it changes the choices,
        // so the clearing is not saved.
        var assignmentType = new ComboBox<AssignmentTypeListItem>(getTranslation("assignment.type"));
        assignmentType.setItemLabelGenerator(AssignmentTypeFormatters::formatAssignmentType);
        assignmentType.setWidthFull();
        Signal.effect(assignmentType, () -> assignmentType.setItems(viewModel.typeChoices().get()));
        assignmentType.bindValue(viewModel.type(), viewModel::setType);

        var urgency = new ComboBox<AssignmentUrgency>(getTranslation("assignment.urgency"));
        urgency.setItems(viewModel.urgencyChoices());
        urgency.setItemLabelGenerator(AssignmentUrgency::name);
        urgency.bindValue(viewModel.urgency(), viewModel::setUrgency);

        var typeAndUrgency = new HorizontalLayout(assignmentType, urgency);
        typeAndUrgency.setWidthFull();
        assignmentLayout.add(typeAndUrgency);

        var municipality = new ComboBox<MunicipalityListItem>(getTranslation("assignment.municipality"));
        municipality.setItemLabelGenerator(MunicipalityFormatters::formatMunicipality);
        municipality.setWidthFull();
        assignmentLayout.add(municipality);
        Signal.effect(municipality, () -> municipality.setItems(viewModel.municipalityChoices().get()));
        municipality.bindValue(viewModel.municipality(), viewModel::setMunicipality);

        var address = new TextField(getTranslation("assignment.address"));
        address.setWidthFull();
        assignmentLayout.add(address);
        address.bindValue(viewModel.address(), viewModel::setAddress);

        var description = new TextArea(getTranslation("assignment.description"));
        description.setWidthFull();
        assignmentLayout.add(description);
        description.bindValue(viewModel.description(), viewModel::setDescription);

        assignmentLayout.add(new H4(getTranslation("dispatch.resources")));
        var availableResource = new ComboBox<ResourceListItem>();
        availableResource.setWidthFull();
        availableResource.setItemLabelGenerator(ResourceListItem::callSign);
        availableResource.setAriaLabel(getTranslation("dispatch.available-resource"));
        Signal.effect(availableResource, () -> availableResource.setItems(viewModel.availableResources().get()));
        availableResource.bindValue(viewModel.resourceToAdd(), viewModel::chooseResourceToAdd);
        var addToAssignment = new Button(VaadinIcon.PLUS_CIRCLE.create(), event -> viewModel.addResource());
        var removeFromAssignment = new Button(VaadinIcon.MINUS_CIRCLE.create(),
                event -> viewModel.removeSelectedResources());
        addToAssignment.setAriaLabel(getTranslation("dispatch.add-resource"));
        removeFromAssignment.setAriaLabel(getTranslation("dispatch.remove-resources"));
        removeFromAssignment.bindEnabled(viewModel.anyResourceSelected());
        var assignmentActions = new HorizontalLayout(availableResource, addToAssignment, removeFromAssignment);
        assignmentActions.setFlexGrow(1.0, availableResource);
        assignmentActions.setWidthFull();
        assignmentLayout.add(assignmentActions);

        var assignedResourcesGrid = new Grid<ResourceRow>();
        assignedResourcesGrid.addThemeVariants(GridVariant.ROW_STRIPES);
        assignedResourcesGrid.setSizeFull();
        assignedResourcesGrid.addColumn(ResourceRow::callSign).setHeader(getTranslation("dispatch.resource"));
        assignedResourcesGrid.addColumn(new ComponentRenderer<>(this::createStatusButton))
                .setHeader(getTranslation("dispatch.state"));
        assignedResourcesGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        assignedResourcesGrid.setAriaLabel(getTranslation("dispatch.assigned-resources"));
        bindItems(assignedResourcesGrid, viewModel.assignedResources()).setIdentifierProvider(ResourceRow::resourceId);
        assignedResourcesGrid.asMultiSelect().bindValue(viewModel.selectedResources(), viewModel::selectResources);
        assignmentLayout.add(assignedResourcesGrid);

        var dispatchAll = new Button(getTranslation("dispatch.dispatch-all"),
                event -> showWhatIsMissing(viewModel.dispatchAll()));
        dispatchAll.addThemeVariants(ButtonVariant.ERROR);
        var dispatchSelected = new Button(getTranslation("dispatch.dispatch-selected"),
                event -> showWhatIsMissing(viewModel.dispatchSelected()));
        dispatchSelected.bindEnabled(viewModel.anyResourceSelected());
        dispatchSelected.addThemeVariants(ButtonVariant.ERROR);
        var dispatchReserved = new Button(getTranslation("dispatch.dispatch-reserved"),
                event -> showWhatIsMissing(viewModel.dispatchReserved()));
        dispatchReserved.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);
        var closeAssignment = new Button(getTranslation("dispatch.close-assignment"),
                event -> viewModel.closeAssignment());
        closeAssignment.addThemeVariants(ButtonVariant.TERTIARY);

        var dispatchActions = new HorizontalLayout(dispatchAll, dispatchSelected, dispatchReserved);
        dispatchActions.addToEnd(closeAssignment);
        dispatchActions.setWidthFull();
        assignmentLayout.add(dispatchActions);

        add(noAssignmentSelected);
        add(assignmentForm);
    }

    private Component createNoAssignmentSelectedComponent() {
        var noAssignmentSelected = new VerticalLayout(new Span(getTranslation("assignment.none-selected")));
        noAssignmentSelected.setSizeFull();
        noAssignmentSelected.addClassName("content");
        noAssignmentSelected.setMargin(false);
        noAssignmentSelected.setSpacing(false);
        noAssignmentSelected.setPadding(true);
        return noAssignmentSelected;
    }

    private Button createStatusButton(ResourceRow row) {
        var btn = new Button(ResourceFormatters.formatResourceState(row.status().state()));
        btn.addThemeVariants(ButtonVariant.SMALL);
        btn.addClassName(ResourceFormatters.resourceStateClassName(row.status().state()));
        btn.addClickListener(event -> new ChangeResourceStatusDialog(row,
                state -> changeResourceState.accept(row.resourceId(), state)).open());
        return btn;
    }

    /**
     * Tells what the assignment is missing for dispatching, if anything.
     */
    private void showWhatIsMissing(Set<MissingForDispatch> missing) {
        if (missing.isEmpty()) {
            return;
        }
        var errors = new UnorderedList();
        missing.stream().sorted()
                .forEach(item -> errors.add(new ListItem(getTranslation("dispatch.missing." + item.name()))));
        var notification = new Notification(errors);
        notification.setPosition(Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.ERROR);
        notification.setDuration(2000);
        notification.open();
    }
}
