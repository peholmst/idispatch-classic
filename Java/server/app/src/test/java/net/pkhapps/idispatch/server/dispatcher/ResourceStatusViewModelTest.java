package net.pkhapps.idispatch.server.dispatcher;

import net.pkhapps.idispatch.server.assignment.AssignmentService;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceQueries;
import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.resource.ResourceStatus;
import net.pkhapps.idispatch.server.resource.ResourceStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The rules of the resource status view, without the view: which resources it shows, and in what order.
 */
class ResourceStatusViewModelTest {

    static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");

    ResourceStatusService resourceStatusService = mock(ResourceStatusService.class);
    ResourceQueries resourceQueries = mock(ResourceQueries.class);
    AssignmentService assignmentService = mock(AssignmentService.class);
    ResourceStatusViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new ResourceStatusViewModel(resourceStatusService, resourceQueries, assignmentService);
        when(assignmentService.findOpenAssignments()).thenReturn(List.of());
        when(resourceQueries.findCallSigns(any())).thenReturn(Map.of(new ResourceId(10), "rvs102",
                new ResourceId(11), "RVS101", new ResourceId(12), "PAR201"));
    }

    @Test
    void resourcesAreInCallSignOrderIgnoringCase() {
        givenResources(status(10, ResourceState.AVAILABLE), status(11, ResourceState.AVAILABLE),
                status(12, ResourceState.AVAILABLE), status(13, ResourceState.AVAILABLE));

        viewModel.refresh();

        assertEquals(List.of(12L, 11L, 10L, 13L), resourceIds());
    }

    @Test
    void theCallSignFilterListsOnlyResourcesWhoseCallSignContainsItIgnoringCase() {
        givenResources(status(10, ResourceState.AVAILABLE), status(11, ResourceState.AVAILABLE),
                status(12, ResourceState.AVAILABLE));
        viewModel.refresh();

        viewModel.filterByCallSign(" RVS ");

        assertEquals(List.of(11L, 10L), resourceIds());
    }

    @Test
    void theStateFilterListsOnlyResourcesInTheChosenStates() {
        givenResources(status(10, ResourceState.AVAILABLE), status(11, ResourceState.EN_ROUTE),
                status(12, ResourceState.OUT_OF_SERVICE));
        viewModel.refresh();

        viewModel.filterByStates(Set.of(ResourceState.EN_ROUTE, ResourceState.OUT_OF_SERVICE));

        assertEquals(List.of(12L, 11L), resourceIds());

        viewModel.filterByStates(Set.of());

        assertEquals(List.of(12L, 11L, 10L), resourceIds());
    }

    @Test
    void bothFiltersApplyTogether() {
        givenResources(status(10, ResourceState.AVAILABLE), status(11, ResourceState.EN_ROUTE),
                status(12, ResourceState.EN_ROUTE));
        viewModel.refresh();

        viewModel.filterByCallSign("RVS");
        viewModel.filterByStates(Set.of(ResourceState.EN_ROUTE));

        assertEquals(List.of(11L), resourceIds());
    }

    @Test
    void theFiltersStayWhenTheResourcesChange() {
        givenResources(status(10, ResourceState.AVAILABLE));
        viewModel.refresh();
        viewModel.filterByStates(Set.of(ResourceState.EN_ROUTE));

        givenResources(status(10, ResourceState.EN_ROUTE), status(11, ResourceState.AVAILABLE));
        viewModel.resourcesChanged();

        assertEquals(List.of(10L), resourceIds());
    }

    private void givenResources(ResourceStatus... statuses) {
        when(resourceStatusService.getStatusOfAllResources()).thenReturn(List.of(statuses));
    }

    private static ResourceStatus status(long resourceId, ResourceState state) {
        return new ResourceStatus(new ResourceId(resourceId), 0, state, null, NOW);
    }

    private List<Long> resourceIds() {
        return viewModel.resources().peek().stream().map(row -> row.resourceId().value()).toList();
    }
}
