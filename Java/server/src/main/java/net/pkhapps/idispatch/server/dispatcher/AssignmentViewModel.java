package net.pkhapps.idispatch.server.dispatcher;

import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import net.pkhapps.idispatch.server.assignment.Assignment;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.assignment.AssignmentService;
import net.pkhapps.idispatch.server.assignment.AssignmentUrgency;
import net.pkhapps.idispatch.server.assignment.MissingForDispatch;
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
import net.pkhapps.idispatch.server.resource.ResourceStatus;
import net.pkhapps.idispatch.server.resource.ResourceStatusService;
import net.pkhapps.idispatch.server.shared.UpdateResult;
import net.pkhapps.idispatch.server.ui.Formatters;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * The assignment form of the dispatcher view, which saves every change right away, with the resources on the
 * assignment and the actions for dispatching them.
 * <p>
 * The assignment is held as last saved; each field shows its part, and a change saves {@code assignment.with…(…)} and
 * replaces it with the saved version, or with another dispatcher's newer one.
 */
class AssignmentViewModel {

    private final AssignmentService assignmentService;
    private final ResourceStatusService resourceStatusService;
    private final ResourceQueries resourceQueries;
    private final DispatchService dispatchService;
    private final List<AssignmentUrgency> urgencyChoices;

    // The assignment in the form, as last saved, or null when none is open
    private final ValueSignal<@Nullable Assignment> assignment = new ValueSignal<>(null);
    // The choices, and by ID also the type and municipality that the assignment has even if they have been deleted
    // since
    private final ValueSignal<List<AssignmentTypeListItem>> typeChoices = new ValueSignal<>(List.of());
    private final ValueSignal<List<MunicipalityListItem>> municipalityChoices = new ValueSignal<>(List.of());
    private final ValueSignal<Map<AssignmentTypeId, AssignmentTypeListItem>> types = new ValueSignal<>(Map.of());
    private final ValueSignal<Map<MunicipalityId, MunicipalityListItem>> municipalities = new ValueSignal<>(Map.of());
    private final ValueSignal<List<ResourceListItem>> availableResources = new ValueSignal<>(List.of());
    private final ValueSignal<@Nullable ResourceListItem> resourceToAdd = new ValueSignal<>(null);
    private final ValueSignal<List<ResourceRow>> assignedResources = new ValueSignal<>(List.of());
    private final ValueSignal<Set<ResourceId>> selectedResourceIds = new ValueSignal<>(Set.of());

    private final Signal<@Nullable AssignmentTypeListItem> type = Signal.computed(() -> {
        var current = assignment.get();
        var byId = types.get();
        return current == null || current.typeId() == null ? null : byId.get(current.typeId());
    });
    private final Signal<@Nullable MunicipalityListItem> municipality = Signal.computed(() -> {
        var current = assignment.get();
        var byId = municipalities.get();
        return current == null || current.municipalityId() == null ? null : byId.get(current.municipalityId());
    });
    // The selected resources that are still on the assignment, with their latest status
    private final Signal<Set<ResourceRow>> selectedResources = Signal.computed(() -> {
        var ids = selectedResourceIds.get();
        return assignedResources.get().stream().filter(row -> ids.contains(row.resourceId()))
                .collect(Collectors.toUnmodifiableSet());
    });

    AssignmentViewModel(AssignmentService assignmentService, ResourceStatusService resourceStatusService,
                        ResourceQueries resourceQueries, DispatchService dispatchService) {
        this.assignmentService = assignmentService;
        this.resourceStatusService = resourceStatusService;
        this.resourceQueries = resourceQueries;
        this.dispatchService = dispatchService;
        this.urgencyChoices = List.copyOf(assignmentService.findApplicableUrgencies());
    }

    // --- State ---

    /**
     * Whether an assignment is open in the form.
     */
    Signal<Boolean> showing() {
        return assignment.map(Objects::nonNull);
    }

    Signal<List<AssignmentTypeListItem>> typeChoices() {
        return typeChoices.asReadonly();
    }

    List<AssignmentUrgency> urgencyChoices() {
        return urgencyChoices;
    }

    Signal<List<MunicipalityListItem>> municipalityChoices() {
        return municipalityChoices.asReadonly();
    }

    Signal<@Nullable AssignmentTypeListItem> type() {
        return type;
    }

    Signal<@Nullable AssignmentUrgency> urgency() {
        return assignment.map(current -> current == null ? null : current.urgency());
    }

    Signal<@Nullable MunicipalityListItem> municipality() {
        return municipality;
    }

    Signal<String> address() {
        return assignment.map(current -> current == null ? "" : current.address());
    }

    Signal<String> description() {
        return assignment.map(current -> current == null ? "" : current.description());
    }

    /**
     * The resources that can be added to the assignment.
     */
    Signal<List<ResourceListItem>> availableResources() {
        return availableResources.asReadonly();
    }

    Signal<@Nullable ResourceListItem> resourceToAdd() {
        return resourceToAdd.asReadonly();
    }

    Signal<List<ResourceRow>> assignedResources() {
        return assignedResources.asReadonly();
    }

    Signal<Set<ResourceRow>> selectedResources() {
        return selectedResources;
    }

    Signal<Boolean> anyResourceSelected() {
        return selectedResources.map(rows -> !rows.isEmpty());
    }

    // --- Commands ---

    void setType(@Nullable AssignmentTypeListItem type) {
        save(current -> current.withTypeId(type == null ? null : type.id()));
    }

    /**
     * An assignment always has an urgency, so clearing it makes the assignment not urgent.
     */
    void setUrgency(@Nullable AssignmentUrgency urgency) {
        save(current -> current.withUrgency(urgency == null ? AssignmentUrgency.N : urgency));
    }

    void setMunicipality(@Nullable MunicipalityListItem municipality) {
        save(current -> current.withMunicipalityId(municipality == null ? null : municipality.id()));
    }

    void setAddress(String address) {
        save(current -> current.withAddress(address));
    }

    void setDescription(String description) {
        save(current -> current.withDescription(description));
    }

    void chooseResourceToAdd(@Nullable ResourceListItem resource) {
        resourceToAdd.set(resource);
    }

    void selectResources(Set<ResourceRow> rows) {
        selectedResourceIds.set(rows.stream().map(ResourceRow::resourceId).collect(Collectors.toUnmodifiableSet()));
    }

    /**
     * Puts the chosen resource on the assignment.
     */
    void addResource() {
        var resource = resourceToAdd.peek();
        var current = assignment.peek();
        if (resource != null && current != null) {
            resourceStatusService.setResourceAssignment(resource.id(), current.id());
        }
    }

    /**
     * Takes the resource off the assignment.
     */
    void removeResource(ResourceId resource) {
        resourceStatusService.clearResourceAssignment(resource);
    }

    /**
     * Dispatches every resource on the assignment.
     *
     * @return what the assignment is missing for dispatching, empty when it was dispatched or none is open
     */
    Set<MissingForDispatch> dispatchAll() {
        return dispatch(current -> dispatchService.dispatchAllResources(current));
    }

    /**
     * Dispatches the selected resources.
     *
     * @return what the assignment is missing for dispatching, empty when it was dispatched or nothing was to dispatch
     */
    Set<MissingForDispatch> dispatchSelected() {
        var resources = selectedResourceIds();
        if (resources.isEmpty()) {
            return Set.of();
        }
        return dispatch(current -> dispatchService.dispatchSelectedResources(current, resources));
    }

    /**
     * Dispatches the resources that are reserved for the assignment.
     *
     * @return what the assignment is missing for dispatching, empty when it was dispatched or none is open
     */
    Set<MissingForDispatch> dispatchReserved() {
        return dispatch(current -> dispatchService.dispatchAllReservedResources(current));
    }

    void closeAssignment() {
        var current = assignment.peek();
        if (current != null) {
            assignmentService.closeAssignment(current.id());
        }
    }

    /**
     * Shows the assignment with the given ID in the form, or empties the form for {@code null}.
     */
    void open(@Nullable AssignmentId id) {
        var current = assignment.peek();
        if (Objects.equals(id, current == null ? null : current.id())) {
            return;
        }
        Optional.ofNullable(id)
                .flatMap(assignmentService::findAssignment)
                .ifPresentOrElse(this::showForm, this::hideForm);
    }

    /**
     * Loads the resources that are available and the ones on the assignment in the form again.
     */
    void resourcesChanged() {
        loadResources();
    }

    private Set<ResourceId> selectedResourceIds() {
        return selectedResources.peek().stream().map(ResourceRow::resourceId)
                .collect(Collectors.toUnmodifiableSet());
    }

    @FunctionalInterface
    private interface Dispatch {
        void dispatch(Assignment assignment) throws AssignmentNotReadyForDispatchException;
    }

    private Set<MissingForDispatch> dispatch(Dispatch dispatch) {
        var current = assignment.peek();
        if (current == null) {
            return Set.of();
        }
        try {
            dispatch.dispatch(current);
            return Set.of();
        } catch (AssignmentNotReadyForDispatchException ex) {
            return Set.copyOf(ex.getMissing());
        }
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
        selectedResourceIds.set(Set.of());
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
        this.typeChoices.set(List.copyOf(typeChoices));
        this.municipalityChoices.set(List.copyOf(municipalityChoices));
        types.set(Map.copyOf(typesById));
        municipalities.set(Map.copyOf(municipalitiesById));
        this.assignment.set(assignment);
        loadResources();
    }

    private void hideForm() {
        assignment.set(null);
        assignedResources.set(List.of());
        selectedResourceIds.set(Set.of());
    }

    private void loadResources() {
        availableResources.set(List.copyOf(resourceStatusService.getAvailableResources()));
        var current = assignment.peek();
        var statuses = current == null ? List.<ResourceStatus>of()
                : resourceStatusService.getStatusOfResourcesAssignedToAssignment(current.id());
        var callSigns = resourceQueries.findCallSigns(statuses.stream().map(ResourceStatus::resourceId).toList());
        assignedResources.set(statuses.stream()
                .map(status -> new ResourceRow(status, callSigns.get(status.resourceId()), null))
                .toList());
    }
}
