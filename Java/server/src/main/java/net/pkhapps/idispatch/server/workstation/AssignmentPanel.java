package net.pkhapps.idispatch.server.workstation;

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
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import net.pkhapps.idispatch.server.assignment.Assignment;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.assignment.AssignmentService;
import net.pkhapps.idispatch.server.assignment.AssignmentUrgency;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeFormatters;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeId;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeListItem;
import net.pkhapps.idispatch.server.dispatch.AssignmentNotReadyForDispatchException;
import net.pkhapps.idispatch.server.dispatch.DispatchService;
import net.pkhapps.idispatch.server.municipality.MunicipalityFormatters;
import net.pkhapps.idispatch.server.municipality.MunicipalityId;
import net.pkhapps.idispatch.server.municipality.MunicipalityListItem;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceListItem;
import net.pkhapps.idispatch.server.resource.ResourceQueries;
import net.pkhapps.idispatch.server.resource.ResourceFormatters;
import net.pkhapps.idispatch.server.resource.ResourceStatus;
import net.pkhapps.idispatch.server.resource.ResourceStatusChanged;
import net.pkhapps.idispatch.server.resource.ResourceStatusService;
import net.pkhapps.idispatch.server.shared.UpdateResult;
import net.pkhapps.idispatch.server.ui.Formatters;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AssignmentPanel extends VerticalLayout {

    private final AssignmentService assignmentService;
    private final ResourceStatusService resourceStatusService;
    private final ResourceQueries resourceQueries;
    // The call signs of the assigned resources, loaded with them
    private final Map<ResourceId, String> assignedCallSigns = new HashMap<>();
    private final DispatchService dispatchService;
    private final ApplicationEventMulticaster eventMulticaster;
    // The assignment in the form, as last saved, or null when none is selected. The fields show it, and every change a
    // dispatcher makes is saved right away and replaces it.
    private final ValueSignal<@Nullable Assignment> assignment = new ValueSignal<>(null);
    // The choices, with the type and municipality that the assignment has even if they have been deleted since
    private final ValueSignal<Map<AssignmentTypeId, AssignmentTypeListItem>> types = new ValueSignal<>(Map.of());
    private final ValueSignal<Map<MunicipalityId, MunicipalityListItem>> municipalities = new ValueSignal<>(Map.of());
    // The ID of the assignment in the form, for refreshing its resources when another session changes them, which
    // must not read this session's signals
    private @Nullable AssignmentId shownAssignmentId;
    private final Component noAssignmentSelected;
    private final Component assignmentForm;
    private final ComboBox<AssignmentTypeListItem> assignmentType;
    private final ComboBox<MunicipalityListItem> municipality;
    private final ComboBox<ResourceListItem> availableResource;

    private final Grid<ResourceStatus> assignedResources;
    private OwnerUpdater ownerUpdater;
    private final ApplicationListener<ResourceStatusChanged> resourceStatusChangedListener = event -> ownerUpdater.update(this::updateResources);

    public AssignmentPanel(AssignmentService assignmentService, ResourceStatusService resourceStatusService,
                           ResourceQueries resourceQueries, DispatchService dispatchService,
                           ApplicationEventMulticaster eventMulticaster) {
        this.assignmentService = assignmentService;
        this.resourceStatusService = resourceStatusService;
        this.resourceQueries = resourceQueries;
        this.dispatchService = dispatchService;
        this.eventMulticaster = eventMulticaster;

        addClassName("assignment-panel");

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
        assignmentType.setItemLabelGenerator(AssignmentTypeFormatters::formatAssignmentType);
        assignmentType.setWidthFull();
        assignmentType.bindValue(Signal.computed(() -> {
            var current = assignment.get();
            return current == null || current.typeId() == null ? null : types.get().get(current.typeId());
        }), item -> save(current -> current.withTypeId(item == null ? null : item.id())));

        var urgency = new ComboBox<AssignmentUrgency>(getTranslation("assignment.urgency"));
        urgency.setItems(assignmentService.findApplicableUrgencies());
        urgency.setItemLabelGenerator(AssignmentUrgency::name);
        // An assignment always has an urgency, so clearing the field makes it not urgent
        urgency.bindValue(assignment.map(current -> current == null ? null : current.urgency()),
                value -> save(current -> current.withUrgency(value == null ? AssignmentUrgency.N : value)));

        var typeAndUrgency = new HorizontalLayout(assignmentType, urgency);
        typeAndUrgency.setWidthFull();
        assignmentLayout.add(typeAndUrgency);

        municipality = new ComboBox<>(getTranslation("assignment.municipality"));
        municipality.setItemLabelGenerator(MunicipalityFormatters::formatMunicipality);
        municipality.setWidthFull();
        assignmentLayout.add(municipality);
        municipality.bindValue(Signal.computed(() -> {
            var current = assignment.get();
            return current == null || current.municipalityId() == null ? null
                    : municipalities.get().get(current.municipalityId());
        }), item -> save(current -> current.withMunicipalityId(item == null ? null : item.id())));

        var address = new TextField(getTranslation("assignment.address"));
        address.setWidthFull();
        assignmentLayout.add(address);
        address.bindValue(assignment.map(current -> current == null ? "" : current.address()),
                value -> save(current -> current.withAddress(value)));

        var description = new TextArea(getTranslation("assignment.description"));
        description.setWidthFull();
        assignmentLayout.add(description);
        description.bindValue(assignment.map(current -> current == null ? "" : current.description()),
                value -> save(current -> current.withDescription(value)));

        assignmentLayout.add(new H4(getTranslation("dispatch.resources")));
        availableResource = new ComboBox<>();
        availableResource.setWidthFull();
        availableResource.setItemLabelGenerator(ResourceListItem::callSign);
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
        assignedResources.addColumn(rs -> assignedCallSigns.get(rs.getResourceId())).setHeader(getTranslation("dispatch.resource"));
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
        var btn = new Button(ResourceFormatters.formatResourceState(resourceStatus.getState()));
        btn.addThemeVariants(ButtonVariant.SMALL);
        btn.addClassName(ResourceFormatters.resourceStateClassName(resourceStatus.getState()));
        btn.addClickListener(event -> new ChangeResourceStatusDialog(resourceStatusService, resourceStatus,
                assignedCallSigns.get(resourceStatus.getResourceId())).open());
        return btn;
    }

    private void updateResources() {
        availableResource.setItems(resourceStatusService.getAvailableResources());
        var statuses = shownAssignmentId == null ? List.<ResourceStatus>of()
                : resourceStatusService.getStatusOfResourcesAssignedToAssignment(shownAssignmentId);
        assignedCallSigns.clear();
        assignedCallSigns.putAll(resourceQueries.findCallSigns(statuses.stream().map(ResourceStatus::getResourceId).toList()));
        assignedResources.setItems(statuses);
    }

    private void addToAssignment() {
        var resource = availableResource.getValue();
        var current = assignment.peek();
        if (resource != null && current != null) {
            resourceStatusService.setResourceAssignment(resource.id(), current.id());
        }
    }

    private void removeFromAssignment() {
        var resources = assignedResources.getSelectedItems().stream().map(ResourceStatus::getResourceId).collect(Collectors.toSet());
        resources.forEach(resourceStatusService::clearResourceAssignment);
    }

    private void dispatchAll() {
        var assignment = this.assignment.peek();
        if (assignment != null) {
            try {
                dispatchService.dispatchAllResources(assignment);
            } catch (AssignmentNotReadyForDispatchException ex) {
                showWhatIsMissing(ex);
            }
        }
    }

    private void dispatchSelected() {
        var resources = assignedResources.getSelectedItems().stream().map(ResourceStatus::getResourceId).collect(Collectors.toSet());
        var assignment = this.assignment.peek();
        if (resources.size() > 0 && assignment != null) {
            try {
                dispatchService.dispatchSelectedResources(assignment, resources);
            } catch (AssignmentNotReadyForDispatchException ex) {
                showWhatIsMissing(ex);
            }
        }
    }

    private void dispatchReserved() {
        var assignment = this.assignment.peek();
        if (assignment != null) {
            try {
                dispatchService.dispatchAllReservedResources(assignment);
            } catch (AssignmentNotReadyForDispatchException ex) {
                showWhatIsMissing(ex);
            }
        }
    }

    private void showWhatIsMissing(AssignmentNotReadyForDispatchException ex) {
        var errors = new UnorderedList();
        ex.getMissing().stream().sorted()
                .forEach(missing -> errors.add(new ListItem(getTranslation("dispatch.missing." + missing.name()))));
        var notification = new Notification(errors);
        notification.setPosition(Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.ERROR);
        notification.setDuration(2000);
        notification.open();
    }

    private void closeAssignment() {
        var assignment = this.assignment.peek();
        if (assignment != null) {
            assignmentService.closeAssignment(assignment.id());
        }
    }

    public void setAssignmentId(@Nullable AssignmentId id) {
        var current = assignment.peek();
        if (Objects.equals(id, current == null ? null : current.id())) {
            return;
        }
        Optional.ofNullable(id)
                .flatMap(assignmentService::findAssignment)
                .ifPresentOrElse(this::showForm, this::hideForm);
    }

    /**
     * Saves the change to the assignment in the form. If another dispatcher has changed it in the meantime, shows their
     * version instead.
     */
    private void save(UnaryOperator<Assignment> change) {
        var current = assignment.peek();
        if (current == null) {
            return;
        }
        var changed = change.apply(current);
        if (changed.equals(current)) {
            return;
        }
        var result = assignmentService.updateAssignment(changed);
        if (result instanceof UpdateResult.Conflict<Assignment> conflict) {
            Optional.ofNullable(conflict.getConflictingData()).ifPresentOrElse(this::showForm, this::hideForm);
        } else if (result instanceof UpdateResult.Success<Assignment> success) {
            assignment.set(success.getData().orElseThrow());
        }
    }

    private void showForm(Assignment assignment) {
        // Empty the form first: setting the choices clears the combo boxes, which must not be saved as a change
        this.assignment.set(null);
        // Reload the choices every time, so that changes made in the management console show up
        var typeChoices = assignmentService.findApplicableAssignmentTypes();
        var municipalityChoices = Formatters.sortedByLabel(assignmentService.findApplicableMunicipalities(),
                MunicipalityFormatters::formatMunicipality);
        var typesById = new HashMap<AssignmentTypeId, AssignmentTypeListItem>();
        typeChoices.forEach(type -> typesById.put(type.id(), type));
        if (assignment.typeId() != null && !typesById.containsKey(assignment.typeId())) {
            assignmentService.findAssignmentType(assignment.typeId()).ifPresent(type -> typesById.put(type.id(), type));
        }
        var municipalitiesById = new HashMap<MunicipalityId, MunicipalityListItem>();
        municipalityChoices.forEach(municipality -> municipalitiesById.put(municipality.id(), municipality));
        if (assignment.municipalityId() != null && !municipalitiesById.containsKey(assignment.municipalityId())) {
            assignmentService.findMunicipality(assignment.municipalityId())
                    .ifPresent(municipality -> municipalitiesById.put(municipality.id(), municipality));
        }
        assignmentType.setItems(typeChoices);
        municipality.setItems(municipalityChoices);
        types.set(Map.copyOf(typesById));
        municipalities.set(Map.copyOf(municipalitiesById));
        this.assignment.set(assignment);
        shownAssignmentId = assignment.id();
        updateResources();
        noAssignmentSelected.setVisible(false);
        assignmentForm.setVisible(true);
    }

    private void hideForm() {
        assignment.set(null);
        shownAssignmentId = null;
        noAssignmentSelected.setVisible(true);
        assignmentForm.setVisible(false);
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
