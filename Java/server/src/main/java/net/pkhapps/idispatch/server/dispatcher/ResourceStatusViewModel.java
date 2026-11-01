package net.pkhapps.idispatch.server.dispatcher;

import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import net.pkhapps.idispatch.server.assignment.AssignmentService;
import net.pkhapps.idispatch.server.resource.ResourceQueries;
import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.resource.ResourceStatusService;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * What the resource status view knows and can do: every resource with its state and the assignment it is on, in call
 * sign order, filtered by call sign and by state.
 * <p>
 * The list does not follow the services by itself. The view tells this ViewModel when resources or assignments have
 * changed, through {@link #resourcesChanged()}, as the services' events arrive.
 */
class ResourceStatusViewModel {

    private static final Comparator<ResourceRow> BY_CALL_SIGN = Comparator.comparing(ResourceRow::callSign,
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    private final ResourceStatusService resourceStatusService;
    private final ResourceQueries resourceQueries;
    private final AssignmentService assignmentService;

    private final ValueSignal<List<ResourceRow>> allResources = new ValueSignal<>(List.of());
    private final ValueSignal<String> callSignFilter = new ValueSignal<>("");
    private final ValueSignal<Set<ResourceState>> stateFilter = new ValueSignal<>(Set.of());

    private final Signal<List<ResourceRow>> resources = Signal.computed(() -> {
        var callSign = callSignFilter.get().strip().toLowerCase(Locale.ROOT);
        var states = stateFilter.get();
        return allResources.get().stream()
                .filter(row -> callSign.isEmpty()
                        || row.callSign() != null && row.callSign().toLowerCase(Locale.ROOT).contains(callSign))
                .filter(row -> states.isEmpty() || states.contains(row.status().state()))
                .sorted(BY_CALL_SIGN)
                .toList();
    });

    ResourceStatusViewModel(ResourceStatusService resourceStatusService, ResourceQueries resourceQueries,
                            AssignmentService assignmentService) {
        this.resourceStatusService = resourceStatusService;
        this.resourceQueries = resourceQueries;
        this.assignmentService = assignmentService;
    }

    // --- State ---

    /**
     * The resources that pass both filters, in call sign order.
     */
    Signal<List<ResourceRow>> resources() {
        return resources;
    }

    /**
     * The text the listed resources' call signs must contain, ignoring case; empty lists every resource.
     */
    Signal<String> callSignFilter() {
        return callSignFilter.asReadonly();
    }

    /**
     * The states the listed resources must be in; empty lists resources in every state.
     */
    Signal<Set<ResourceState>> stateFilter() {
        return stateFilter.asReadonly();
    }

    // --- Commands ---

    /**
     * Loads the resources.
     */
    void refresh() {
        allResources.set(ResourceRows.load(resourceStatusService, resourceQueries, assignmentService));
    }

    /**
     * Loads the resources again, keeping the filters.
     */
    void resourcesChanged() {
        refresh();
    }

    /**
     * Lists only the resources whose call sign contains the text, ignoring case.
     */
    void filterByCallSign(String text) {
        callSignFilter.set(text);
    }

    /**
     * Lists only the resources in one of the states, or in any state when there are none.
     */
    void filterByStates(Set<ResourceState> states) {
        stateFilter.set(Set.copyOf(states));
    }
}
