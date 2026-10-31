package net.pkhapps.idispatch.server.ui.dws;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
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
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import net.pkhapps.idispatch.server.boundary.AssignmentService;
import net.pkhapps.idispatch.server.boundary.DispatchService;
import net.pkhapps.idispatch.server.boundary.ResourceStatusService;
import net.pkhapps.idispatch.server.entity.*;
import net.pkhapps.idispatch.server.events.ResourceStatusChanged;
import net.pkhapps.idispatch.server.ui.Formatters;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class AssignmentPanel extends VerticalLayout {

    private final AssignmentService assignmentService;
    private final ResourceStatusService resourceStatusService;
    private final DispatchService dispatchService;
    private final ApplicationEventMulticaster eventMulticaster;
    private final Binder<Assignment> binder;
    private final Component noAssignmentSelected;
    private final Component assignmentForm;
    private final ComboBox<AssignmentType> assignmentType;
    private final ComboBox<Municipality> municipality;
    private final ComboBox<Resource> availableResource;

    private final AtomicBoolean initializingForm = new AtomicBoolean(false);
    private final Grid<ResourceStatus> assignedResources;
    private OwnerUpdater ownerUpdater;
    private final ApplicationListener<ResourceStatusChanged> resourceStatusChangedListener = event -> ownerUpdater.update(this::updateResources);
    private Long selectedAssignmentId;

    public AssignmentPanel(AssignmentService assignmentService, ResourceStatusService resourceStatusService,
                           DispatchService dispatchService, ApplicationEventMulticaster eventMulticaster) {
        this.assignmentService = assignmentService;
        this.resourceStatusService = resourceStatusService;
        this.dispatchService = dispatchService;
        this.eventMulticaster = eventMulticaster;

        addClassName("assignment-panel");
        binder = new Binder<>();

        setSizeFull();
        setMargin(false);
        setSpacing(false);
        setPadding(false);

        noAssignmentSelected = createNoAssignmentSelectedComponent();

        var assignmentLayout = new VerticalLayout();
        var assignmentScroller = new Scroller(assignmentLayout);
        assignmentForm = assignmentScroller;

        assignmentScroller.setSizeFull();
        assignmentScroller.addClassName("content");

        assignmentLayout.setSizeUndefined();
        assignmentLayout.setWidthFull();
        assignmentLayout.setMargin(false);
        assignmentLayout.setSpacing(true);
        assignmentLayout.setPadding(true);

        assignmentType = new ComboBox<>(getTranslation("assignment.type"));
        assignmentType.setItemLabelGenerator(Formatters::formatAssignmentType);
        assignmentType.setWidthFull();
        binder.bind(assignmentType, Assignment::getType, Assignment::setType);

        var urgency = new ComboBox<AssignmentUrgency>(getTranslation("assignment.urgency"));
        urgency.setItems(assignmentService.findApplicableUrgencies());
        urgency.setItemLabelGenerator(AssignmentUrgency::name);
        binder.bind(urgency, Assignment::getUrgency, Assignment::setUrgency);

        var typeAndUrgency = new HorizontalLayout(assignmentType, urgency);
        typeAndUrgency.setWidthFull();
        assignmentLayout.add(typeAndUrgency);

        municipality = new ComboBox<>(getTranslation("assignment.municipality"));
        municipality.setItemLabelGenerator(Formatters::formatMunicipality);
        municipality.setWidthFull();
        assignmentLayout.add(municipality);
        binder.bind(municipality, Assignment::getMunicipality, Assignment::setMunicipality);

        var address = new TextField(getTranslation("assignment.address"));
        address.setWidthFull();
        assignmentLayout.add(address);
        binder.bind(address, Assignment::getAddress, Assignment::setAddress);

        var description = new TextArea(getTranslation("assignment.description"));
        description.setWidthFull();
        assignmentLayout.add(description);
        binder.bind(description, Assignment::getDescription, Assignment::setDescription);

        binder.addStatusChangeListener(event -> {
            if (initializingForm.get()) {
                return;
            }
            if (binder.getBean() != null) {
                var assignment = binder.getBean();
                var result = assignmentService.updateAssignment(assignment);
                if (result.isSuccessful()) {
                    initializingForm.set(true);
                    try {
                        binder.setBean(result.getData().orElseThrow());
                    } finally {
                        initializingForm.set(false);
                    }
                } else {
                    setAssignmentId(assignment.getId());
                }
            }
        });

        assignmentLayout.add(new H4(getTranslation("dispatch.resources")));
        availableResource = new ComboBox<>();
        availableResource.setWidthFull();
        availableResource.setItemLabelGenerator(Resource::getCallSign);
        availableResource.setAriaLabel(getTranslation("dispatch.available-resource"));
        var addToAssignment = new Button(VaadinIcon.PLUS_CIRCLE.create(), event -> addToAssignment());
        var removeFromAssignment = new Button(VaadinIcon.MINUS_CIRCLE.create(), event -> removeFromAssignment());
        addToAssignment.setAriaLabel(getTranslation("dispatch.add-resource"));
        removeFromAssignment.setAriaLabel(getTranslation("dispatch.remove-resources"));
        removeFromAssignment.setEnabled(false);
        var assignmentActions = new HorizontalLayout(availableResource, addToAssignment, removeFromAssignment);
        assignmentActions.setFlexGrow(1.0, availableResource);
        assignmentActions.setWidthFull();
        assignmentLayout.add(assignmentActions);

        assignedResources = new Grid<>();
        assignedResources.addThemeVariants(GridVariant.ROW_STRIPES);
        assignedResources.setWidthFull();
        assignedResources.setHeight(200, Unit.PIXELS);
        assignedResources.addColumn(rs -> Formatters.formatResource(rs.getResource())).setHeader(getTranslation("dispatch.resource"));
        assignedResources.addColumn(new ComponentRenderer<>(this::createStatusButton)).setHeader(getTranslation("dispatch.state"));
        assignedResources.setSelectionMode(Grid.SelectionMode.MULTI);
        assignedResources.setAriaLabel(getTranslation("dispatch.assigned-resources"));
        assignmentLayout.add(assignedResources);

        var dispatchAll = new Button(getTranslation("dispatch.dispatch-all"), event -> dispatchAll());
        dispatchAll.addThemeVariants(ButtonVariant.ERROR);
        var dispatchSelected = new Button(getTranslation("dispatch.dispatch-selected"), event -> dispatchSelected());
        dispatchSelected.setEnabled(false);
        dispatchSelected.addThemeVariants(ButtonVariant.ERROR);
        var dispatchReserved = new Button(getTranslation("dispatch.dispatch-reserved"), event -> dispatchReserved());
        dispatchReserved.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);

        var dispatchActions = new HorizontalLayout(dispatchAll, dispatchSelected, dispatchReserved);
        assignmentLayout.add(dispatchActions);

        var closeAssignment = new Button(getTranslation("dispatch.close-assignment"), event -> closeAssignment());
        closeAssignment.addThemeVariants(ButtonVariant.TERTIARY);
        assignmentLayout.add(closeAssignment);

        assignedResources.addSelectionListener(event -> {
            removeFromAssignment.setEnabled(event.getAllSelectedItems().size() > 0);
            dispatchSelected.setEnabled(event.getAllSelectedItems().size() > 0);
        });

        add(noAssignmentSelected);
        add(assignmentForm);
        hideForm();
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

    private Button createStatusButton(ResourceStatus resourceStatus) {
        var btn = new Button(Formatters.formatResourceState(resourceStatus.getState()));
        btn.addThemeVariants(ButtonVariant.SMALL);
        btn.addClassName(Formatters.resourceStateClassName(resourceStatus.getState()));
        btn.addClickListener(event -> new ChangeResourceStatusDialog(resourceStatusService, resourceStatus).open());
        return btn;
    }

    private void updateResources() {
        availableResource.setItems(resourceStatusService.getAvailableResources());
        if (binder.getBean() != null) {
            assignedResources.setItems(resourceStatusService.getStatusOfResourcesAssignedToAssignment(binder.getBean()));
        } else {
            assignedResources.setItems(Collections.emptyList());
        }
    }

    private void addToAssignment() {
        var resource = availableResource.getValue();
        var assignment = binder.getBean();
        if (resource != null && assignment != null) {
            resourceStatusService.setResourceAssignment(resource, assignment);
        }
    }

    private void removeFromAssignment() {
        var resources = assignedResources.getSelectedItems().stream().map(ResourceStatus::getResource).collect(Collectors.toSet());
        resources.forEach(resourceStatusService::clearResourceAssignment);
    }

    private void dispatchAll() {
        var assignment = binder.getBean();
        if (assignment != null) {
            try {
                dispatchService.dispatchAllResources(assignment);
            } catch (ValidationFailedException ex) {
                showValidationFailedException(ex);
            }
        }
    }

    private void dispatchSelected() {
        var resources = assignedResources.getSelectedItems().stream().map(ResourceStatus::getResource).collect(Collectors.toSet());
        var assignment = binder.getBean();
        if (resources.size() > 0 && assignment != null) {
            try {
                dispatchService.dispatchSelectedResources(assignment, resources);
            } catch (ValidationFailedException ex) {
                showValidationFailedException(ex);
            }
        }
    }

    private void dispatchReserved() {
        var assignment = binder.getBean();
        if (assignment != null) {
            try {
                dispatchService.dispatchAllReservedResources(assignment);
            } catch (ValidationFailedException ex) {
                showValidationFailedException(ex);
            }
        }
    }

    private void showValidationFailedException(ValidationFailedException ex) {
        var errors = new UnorderedList();
        ex.getViolationMessages(getLocale()).forEach(error -> errors.add(new ListItem(error)));
        var notification = new Notification(errors);
        notification.setPosition(Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.ERROR);
        notification.setDuration(2000);
        notification.open();
    }

    private void closeAssignment() {
        var assignment = binder.getBean();
        if (assignment != null) {
            assignmentService.closeAssignment(assignment);
        }
    }

    public void setAssignmentId(@Nullable Long id) {
        if (Objects.equals(id, selectedAssignmentId)) {
            return;
        }
        Optional.ofNullable(id)
                .flatMap(assignmentService::findAssignment)
                .ifPresentOrElse(this::showForm, this::hideForm);
    }

    private void showForm(Assignment assignment) {
        initializingForm.set(true);
        try {
            selectedAssignmentId = assignment.getId();
            // Reload the options every time, so that changes made in the management console show up
            assignmentType.setItems(assignmentService.findApplicableAssignmentTypes());
            municipality.setItems(Formatters.sortedByLabel(assignmentService.findApplicableMunicipalities(),
                    Formatters::formatMunicipality));
            binder.setBean(assignment);
            updateResources();
            noAssignmentSelected.setVisible(false);
            assignmentForm.setVisible(true);
        } finally {
            initializingForm.set(false);
        }
    }

    private void hideForm() {
        initializingForm.set(true);
        try {
            selectedAssignmentId = null;
            binder.setBean(null);
            noAssignmentSelected.setVisible(true);
            assignmentForm.setVisible(false);
        } finally {
            initializingForm.set(false);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ownerUpdater = OwnerUpdater.forCurrentUser(attachEvent.getUI());
        eventMulticaster.addApplicationListener(resourceStatusChangedListener);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        eventMulticaster.removeApplicationListener(resourceStatusChangedListener);
    }
}
