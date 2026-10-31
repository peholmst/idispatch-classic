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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The assignment selected in the dispatcher view, in a form that saves every change right away, with the resources
 * on it and the actions for dispatching them.
 * <p>
 * The form follows signals: the selected assignment's ID, which the view shares with the list, and the assignment as
 * last saved, which every change replaces. The choices, the resources and the resources the dispatcher has selected
 * are signals too, and the components are bound to them.
 */
class AssignmentPanel extends VerticalLayout {

    /**
     * The resources on the assignment, with their call signs, loaded together.
     */
    private record AssignedResources(List<ResourceStatus> statuses, Map<ResourceId, String> callSigns) {

        static final AssignedResources NONE = new AssignedResources(List.of(), Map.of());
    }

    private final AssignmentService assignmentService;
    private final ResourceStatusService resourceStatusService;
    private final ResourceQueries resourceQueries;
    private final DispatchService dispatchService;
    private final ApplicationEventMulticaster eventMulticaster;
    // The assignment in the form, as last saved, or null when none is selected. The fields show it, and every change a
    // dispatcher makes is saved right away and replaces it.
    private final ValueSignal<@Nullable Assignment> assignment = new ValueSignal<>(null);
    // The choices, and by ID also the type and municipality that the assignment has even if they have been deleted
    // since
    private final ValueSignal<List<AssignmentTypeListItem>> typeChoices = new ValueSignal<>(List.of());
    private final ValueSignal<List<MunicipalityListItem>> municipalityChoices = new ValueSignal<>(List.of());
    private final ValueSignal<Map<AssignmentTypeId, AssignmentTypeListItem>> types = new ValueSignal<>(Map.of());
    private final ValueSignal<Map<MunicipalityId, MunicipalityListItem>> municipalities = new ValueSignal<>(Map.of());
    private final ValueSignal<List<ResourceListItem>> availableResources = new ValueSignal<>(List.of());
    private final ValueSignal<@Nullable ResourceListItem> resourceToAdd = new ValueSignal<>(null);
    private final ValueSignal<AssignedResources> assignedResources = new ValueSignal<>(AssignedResources.NONE);
    private final ValueSignal<Set<ResourceStatus>> selectedResources = new ValueSignal<>(Set.of());
    private OwnerUpdater ownerUpdater;
    private final ApplicationListener<ResourceStatusChanged> resourceStatusChangedListener = event -> ownerUpdater.update(this::updateResources);

    /**
     * @param selected the assignment to show, or {@code null} for none
     */
    public AssignmentPanel(AssignmentService assignmentService, ResourceStatusService resourceStatusService,
                           ResourceQueries resourceQueries, DispatchService dispatchService,
                           ApplicationEventMulticaster eventMulticaster, Signal<@Nullable AssignmentId> selected) {
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

        var noAssignmentSelected = createNoAssignmentSelectedComponent();
        noAssignmentSelected.bindVisible(assignment.map(Objects::isNull));

        var assignmentLayout = new VerticalLayout();
        var assignmentForm = new Scroller(assignmentLayout);
        assignmentForm.setSizeFull();
        assignmentForm.addClassName("content");
        assignmentForm.bindVisible(assignment.map(Objects::nonNull));

        assignmentLayout.setSizeUndefined();
        assignmentLayout.setWidthFull();
        assignmentLayout.setMargin(false);
        assignmentLayout.setSpacing(true);
        assignmentLayout.setPadding(true);

        var assignmentType = new ComboBox<AssignmentTypeListItem>(getTranslation("assignment.type"));
        assignmentType.setItemLabelGenerator(AssignmentTypeFormatters::formatAssignmentType);
        assignmentType.setWidthFull();
        Signal.effect(assignmentType, () -> assignmentType.setItems(typeChoices.get()));
        assignmentType.bindValue(Signal.computed(() -> {
            var current = assignment.get();
            var byId = types.get();
            return current == null || current.typeId() == null ? null : byId.get(current.typeId());
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

        var municipality = new ComboBox<MunicipalityListItem>(getTranslation("assignment.municipality"));
        municipality.setItemLabelGenerator(MunicipalityFormatters::formatMunicipality);
        municipality.setWidthFull();
        assignmentLayout.add(municipality);
        Signal.effect(municipality, () -> municipality.setItems(municipalityChoices.get()));
        municipality.bindValue(Signal.computed(() -> {
            var current = assignment.get();
            var byId = municipalities.get();
            return current == null || current.municipalityId() == null ? null : byId.get(current.municipalityId());
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
        var availableResource = new ComboBox<ResourceListItem>();
        availableResource.setWidthFull();
        availableResource.setItemLabelGenerator(ResourceListItem::callSign);
        availableResource.setAriaLabel(getTranslation("dispatch.available-resource"));
        Signal.effect(availableResource, () -> availableResource.setItems(availableResources.get()));
        availableResource.bindValue(resourceToAdd, resourceToAdd::set);
        var anySelected = selectedResources.map(resources -> !resources.isEmpty());
        var addToAssignment = new Button(VaadinIcon.PLUS_CIRCLE.create(), event -> addToAssignment());
        var removeFromAssignment = new Button(VaadinIcon.MINUS_CIRCLE.create(), event -> removeFromAssignment());
        addToAssignment.setAriaLabel(getTranslation("dispatch.add-resource"));
        removeFromAssignment.setAriaLabel(getTranslation("dispatch.remove-resources"));
        removeFromAssignment.bindEnabled(anySelected);
        var assignmentActions = new HorizontalLayout(availableResource, addToAssignment, removeFromAssignment);
        assignmentActions.setFlexGrow(1.0, availableResource);
        assignmentActions.setWidthFull();
        assignmentLayout.add(assignmentActions);

        var assignedResourcesGrid = new Grid<ResourceStatus>();
        assignedResourcesGrid.addThemeVariants(GridVariant.ROW_STRIPES);
        assignedResourcesGrid.setWidthFull();
        assignedResourcesGrid.setHeight(200, Unit.PIXELS);
        assignedResourcesGrid.addColumn(rs -> assignedResources.peek().callSigns().get(rs.resourceId()))
                .setHeader(getTranslation("dispatch.resource"));
        assignedResourcesGrid.addColumn(new ComponentRenderer<>(this::createStatusButton)).setHeader(getTranslation("dispatch.state"));
        assignedResourcesGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        assignedResourcesGrid.setAriaLabel(getTranslation("dispatch.assigned-resources"));
        Signal.effect(assignedResourcesGrid, () -> assignedResourcesGrid.setItems(assignedResources.get().statuses()));
        assignedResourcesGrid.asMultiSelect().bindValue(selectedResources, selectedResources::set);
        assignmentLayout.add(assignedResourcesGrid);

        var dispatchAll = new Button(getTranslation("dispatch.dispatch-all"), event -> dispatchAll());
        dispatchAll.addThemeVariants(ButtonVariant.ERROR);
        var dispatchSelected = new Button(getTranslation("dispatch.dispatch-selected"), event -> dispatchSelected());
        dispatchSelected.bindEnabled(anySelected);
        dispatchSelected.addThemeVariants(ButtonVariant.ERROR);
        var dispatchReserved = new Button(getTranslation("dispatch.dispatch-reserved"), event -> dispatchReserved());
        dispatchReserved.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);

        var dispatchActions = new HorizontalLayout(dispatchAll, dispatchSelected, dispatchReserved);
        assignmentLayout.add(dispatchActions);

        var closeAssignment = new Button(getTranslation("dispatch.close-assignment"), event -> closeAssignment());
        closeAssignment.addThemeVariants(ButtonVariant.TERTIARY);
        assignmentLayout.add(closeAssignment);

        add(noAssignmentSelected);
        add(assignmentForm);

        Signal.effect(this, () -> open(selected.get()));
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
        var btn = new Button(ResourceFormatters.formatResourceState(resourceStatus.state()));
        btn.addThemeVariants(ButtonVariant.SMALL);
        btn.addClassName(ResourceFormatters.resourceStateClassName(resourceStatus.state()));
        btn.addClickListener(event -> new ChangeResourceStatusDialog(resourceStatusService, resourceStatus,
                assignedResources.peek().callSigns().get(resourceStatus.resourceId())).open());
        return btn;
    }

    /**
     * Loads the resources that are available and the ones on the assignment in the form.
     */
    private void updateResources() {
        availableResources.set(resourceStatusService.getAvailableResources());
        var current = assignment.peek();
        var statuses = current == null ? List.<ResourceStatus>of()
                : resourceStatusService.getStatusOfResourcesAssignedToAssignment(current.id());
        assignedResources.set(new AssignedResources(statuses,
                resourceQueries.findCallSigns(statuses.stream().map(ResourceStatus::resourceId).toList())));
    }

    private void addToAssignment() {
        var resource = resourceToAdd.peek();
        var current = assignment.peek();
        if (resource != null && current != null) {
            resourceStatusService.setResourceAssignment(resource.id(), current.id());
        }
    }

    private void removeFromAssignment() {
        selectedResources.peek().stream().map(ResourceStatus::resourceId).collect(Collectors.toSet())
                .forEach(resourceStatusService::clearResourceAssignment);
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
        var resources = selectedResources.peek().stream().map(ResourceStatus::resourceId).collect(Collectors.toSet());
        var assignment = this.assignment.peek();
        if (!resources.isEmpty() && assignment != null) {
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

    /**
     * Shows the assignment with the given ID in the form, or empties the form for {@code null}.
     */
    private void open(@Nullable AssignmentId id) {
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
        this.typeChoices.set(typeChoices);
        this.municipalityChoices.set(municipalityChoices);
        types.set(Map.copyOf(typesById));
        municipalities.set(Map.copyOf(municipalitiesById));
        this.assignment.set(assignment);
        updateResources();
    }

    private void hideForm() {
        assignment.set(null);
        assignedResources.set(AssignedResources.NONE);
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
