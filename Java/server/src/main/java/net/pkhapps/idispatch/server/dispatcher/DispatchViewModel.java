package net.pkhapps.idispatch.server.dispatcher;

import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.assignment.AssignmentListItem;
import net.pkhapps.idispatch.server.assignment.AssignmentService;
import net.pkhapps.idispatch.server.dispatch.DispatchService;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceQueries;
import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.resource.ResourceStatus;
import net.pkhapps.idispatch.server.resource.ResourceStatusService;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * What the dispatcher view knows and can do: the open assignments, the one the dispatcher has selected, which the
 * {@linkplain #assignment() assignment form} shows, and all resources.
 * <p>
 * The lists do not follow the services by themselves. The view tells this ViewModel when assignments or resources
 * have changed, through {@link #assignmentsChanged()} and {@link #resourcesChanged()}, as the services' events
 * arrive.
 */
class DispatchViewModel {

    private final AssignmentService assignmentService;
    private final ResourceStatusService resourceStatusService;
    private final ResourceQueries resourceQueries;
    private final AssignmentViewModel assignment;

    private final ValueSignal<List<AssignmentListItem>> openAssignments = new ValueSignal<>(List.of());
    // The assignment open in the form and selected in the list, or null for none
    private final ValueSignal<@Nullable AssignmentId> selected = new ValueSignal<>(null);
    private final ValueSignal<List<ResourceRow>> allResources = new ValueSignal<>(List.of());
    private final ValueSignal<String> resourceFilter = new ValueSignal<>("");

    private final Signal<List<ResourceRow>> resources = Signal.computed(() -> {
        var filter = resourceFilter.get().strip().toLowerCase(Locale.ROOT);
        var all = allResources.get();
        if (filter.isEmpty()) {
            return all;
        }
        return all.stream()
                .filter(row -> row.callSign() != null && row.callSign().toLowerCase(Locale.ROOT).contains(filter))
                .toList();
    });

    private final Signal<@Nullable AssignmentListItem> selectedAssignment = Signal.computed(() -> {
        var id = selected.get();
        return openAssignments.get().stream().filter(item -> item.id().equals(id)).findFirst().orElse(null);
    });

    DispatchViewModel(AssignmentService assignmentService, ResourceStatusService resourceStatusService,
                      ResourceQueries resourceQueries, DispatchService dispatchService) {
        this.assignmentService = assignmentService;
        this.resourceStatusService = resourceStatusService;
        this.resourceQueries = resourceQueries;
        this.assignment = new AssignmentViewModel(assignmentService, resourceStatusService, resourceQueries,
                dispatchService);
    }

    // --- State ---

    /**
     * The form of the selected assignment.
     */
    AssignmentViewModel assignment() {
        return assignment;
    }

    Signal<List<AssignmentListItem>> openAssignments() {
        return openAssignments.asReadonly();
    }

    /**
     * The selected assignment as the list shows it, or {@code null} when none is selected.
     */
    Signal<@Nullable AssignmentListItem> selectedAssignment() {
        return selectedAssignment;
    }

    /**
     * Every resource whose call sign contains the {@linkplain #resourceFilter() filter}, with the assignment it is on.
     */
    Signal<List<ResourceRow>> resources() {
        return resources;
    }

    /**
     * The text the listed resources' call signs must contain, ignoring case; empty lists every resource.
     */
    Signal<String> resourceFilter() {
        return resourceFilter.asReadonly();
    }

    // --- Commands ---

    /**
     * Loads the open assignments and the resources.
     */
    void refresh() {
        loadOpenAssignments();
        loadResources();
    }

    /**
     * Lists the open assignments again. An assignment that has been closed since is no longer selected.
     */
    void assignmentsChanged() {
        loadOpenAssignments();
    }

    /**
     * Loads the resources again, in the list and on the assignment in the form.
     */
    void resourcesChanged() {
        loadResources();
        assignment.resourcesChanged();
    }

    /**
     * Opens a new assignment and selects it. Opening it publishes an event that lists it, so the list has it by the
     * time it is selected.
     */
    void newAssignment() {
        select(assignmentService.openAssignment());
    }

    /**
     * Selects the assignment and shows it in the form, or empties the form for {@code null}.
     */
    void select(@Nullable AssignmentId id) {
        selected.set(id);
        assignment.open(id);
    }

    /**
     * Lists only the resources whose call sign contains the text, ignoring case.
     */
    void filterResources(String text) {
        resourceFilter.set(text);
    }

    void changeResourceState(ResourceId resource, ResourceState state) {
        resourceStatusService.setResourceState(resource, state);
    }

    private void loadOpenAssignments() {
        var open = assignmentService.findOpenAssignments();
        openAssignments.set(List.copyOf(open));
        var selectedId = selected.peek();
        if (selectedId != null && open.stream().noneMatch(item -> item.id().equals(selectedId))) {
            select(null);
        }
    }

    private void loadResources() {
        var statuses = resourceStatusService.getStatusOfAllResources();
        var callSigns = resourceQueries.findCallSigns(statuses.stream().map(ResourceStatus::resourceId).toList());
        var assignments = new HashMap<AssignmentId, AssignmentListItem>();
        assignmentService.findOpenAssignments().forEach(item -> assignments.put(item.id(), item));
        // Resources can still refer to an assignment that has been closed
        var others = statuses.stream().map(ResourceStatus::assignment).filter(Objects::nonNull)
                .filter(id -> !assignments.containsKey(id)).collect(Collectors.toSet());
        assignmentService.findAssignments(others).forEach(item -> assignments.put(item.id(), item));
        allResources.set(statuses.stream()
                .map(status -> new ResourceRow(status, callSigns.get(status.resourceId()),
                        status.assignment() == null ? null : assignments.get(status.assignment())))
                .toList());
    }
}
