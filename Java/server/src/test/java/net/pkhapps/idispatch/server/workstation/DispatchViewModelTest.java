package net.pkhapps.idispatch.server.workstation;

import net.pkhapps.idispatch.server.assignment.Assignment;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.assignment.AssignmentListItem;
import net.pkhapps.idispatch.server.assignment.AssignmentService;
import net.pkhapps.idispatch.server.assignment.AssignmentUrgency;
import net.pkhapps.idispatch.server.dispatch.DispatchService;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The rules of the dispatcher view, without the view: which assignment is selected and what the lists hold.
 */
class DispatchViewModelTest {

    static final Instant NOW = Instant.parse("2026-10-31T12:00:00Z");

    AssignmentService assignmentService = mock(AssignmentService.class);
    ResourceStatusService resourceStatusService = mock(ResourceStatusService.class);
    ResourceQueries resourceQueries = mock(ResourceQueries.class);
    DispatchViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new DispatchViewModel(assignmentService, resourceStatusService, resourceQueries,
                mock(DispatchService.class));
    }

    @Test
    void selectingAnAssignmentOpensItInTheForm() {
        givenOpenAssignments(1, 2);
        viewModel.refresh();

        viewModel.select(new AssignmentId(2));

        assertEquals(new AssignmentId(2), viewModel.selectedAssignment().peek().id());
        assertTrue(viewModel.assignment().showing().peek());
        assertEquals("Address 2", viewModel.assignment().address().peek());
    }

    @Test
    void deselectingEmptiesTheForm() {
        givenOpenAssignments(1);
        viewModel.refresh();
        viewModel.select(new AssignmentId(1));

        viewModel.select(null);

        assertNull(viewModel.selectedAssignment().peek());
        assertFalse(viewModel.assignment().showing().peek());
    }

    @Test
    void anAssignmentClosedElsewhereIsNoLongerSelected() {
        givenOpenAssignments(1, 2);
        viewModel.refresh();
        viewModel.select(new AssignmentId(1));

        givenOpenAssignments(2);
        viewModel.assignmentsChanged();

        assertNull(viewModel.selectedAssignment().peek());
        assertFalse(viewModel.assignment().showing().peek());
        assertEquals(List.of(new AssignmentId(2)), openAssignmentIds());
    }

    @Test
    void theSelectionStaysWhenOtherAssignmentsChange() {
        givenOpenAssignments(1);
        viewModel.refresh();
        viewModel.select(new AssignmentId(1));

        givenOpenAssignments(1, 2);
        viewModel.assignmentsChanged();

        assertEquals(new AssignmentId(1), viewModel.selectedAssignment().peek().id());
        assertTrue(viewModel.assignment().showing().peek());
    }

    @Test
    void aNewAssignmentIsSelected() {
        givenOpenAssignments(1);
        viewModel.refresh();
        when(assignmentService.openAssignment()).thenAnswer(invocation -> {
            // Opening publishes an event, upon which the view lists the assignments again
            givenOpenAssignments(1, 3);
            viewModel.assignmentsChanged();
            return new AssignmentId(3);
        });

        viewModel.newAssignment();

        assertEquals(new AssignmentId(3), viewModel.selectedAssignment().peek().id());
        assertTrue(viewModel.assignment().showing().peek());
    }

    @Test
    void resourcesAreListedWithTheirCallSignsAndAssignments() {
        givenOpenAssignments(1);
        var onAssignment = new ResourceStatus(new ResourceId(10), 0, ResourceState.DISPATCHED, new AssignmentId(1), NOW);
        var free = new ResourceStatus(new ResourceId(11), 0, ResourceState.AVAILABLE, null, NOW);
        when(resourceStatusService.getStatusOfAllResources()).thenReturn(List.of(onAssignment, free));
        when(resourceQueries.findCallSigns(any())).thenReturn(Map.of(new ResourceId(10), "RVS101",
                new ResourceId(11), "RVS102"));

        viewModel.refresh();

        var rows = viewModel.resources().peek();
        assertEquals(List.of("RVS101", "RVS102"), rows.stream().map(ResourceRow::callSign).toList());
        assertEquals(new AssignmentId(1), rows.get(0).assignment().id());
        assertNull(rows.get(1).assignment());
    }

    @Test
    void theFilterListsOnlyResourcesWhoseCallSignContainsItIgnoringCase() {
        givenOpenAssignments();
        var first = new ResourceStatus(new ResourceId(10), 0, ResourceState.AVAILABLE, null, NOW);
        var second = new ResourceStatus(new ResourceId(11), 0, ResourceState.AVAILABLE, null, NOW);
        var unnamed = new ResourceStatus(new ResourceId(12), 0, ResourceState.AVAILABLE, null, NOW);
        when(resourceStatusService.getStatusOfAllResources()).thenReturn(List.of(first, second, unnamed));
        when(resourceQueries.findCallSigns(any())).thenReturn(Map.of(new ResourceId(10), "RVS101",
                new ResourceId(11), "PAR201"));
        viewModel.refresh();

        viewModel.filterResources(" vs1 ");

        assertEquals(List.of(new ResourceId(10)), resourceIds());

        viewModel.filterResources("");

        assertEquals(List.of(new ResourceId(10), new ResourceId(11), new ResourceId(12)), resourceIds());
    }

    @Test
    void theFilterStaysWhenTheResourcesChange() {
        givenOpenAssignments();
        when(resourceQueries.findCallSigns(any())).thenReturn(Map.of(new ResourceId(10), "RVS101",
                new ResourceId(11), "PAR201"));
        when(resourceStatusService.getStatusOfAllResources()).thenReturn(
                List.of(new ResourceStatus(new ResourceId(10), 0, ResourceState.AVAILABLE, null, NOW)));
        viewModel.refresh();
        viewModel.filterResources("PAR");
        when(resourceStatusService.getStatusOfAllResources()).thenReturn(List.of(
                new ResourceStatus(new ResourceId(10), 0, ResourceState.AVAILABLE, null, NOW),
                new ResourceStatus(new ResourceId(11), 0, ResourceState.AVAILABLE, null, NOW)));

        viewModel.resourcesChanged();

        assertEquals(List.of(new ResourceId(11)), resourceIds());
    }

    private List<ResourceId> resourceIds() {
        return viewModel.resources().peek().stream().map(ResourceRow::resourceId).toList();
    }

    private void givenOpenAssignments(long... ids) {
        var items = java.util.Arrays.stream(ids).mapToObj(DispatchViewModelTest::listItem).toList();
        when(assignmentService.findOpenAssignments()).thenReturn(items);
        for (var id : ids) {
            when(assignmentService.findAssignment(new AssignmentId(id))).thenReturn(Optional.of(assignment(id)));
        }
    }

    private List<AssignmentId> openAssignmentIds() {
        return viewModel.openAssignments().peek().stream().map(AssignmentListItem::id).toList();
    }

    static AssignmentListItem listItem(long id) {
        return new AssignmentListItem(new AssignmentId(id), NOW, null, AssignmentUrgency.N, null, null, null, null,
                null, "Address " + id);
    }

    static Assignment assignment(long id) {
        return new Assignment(new AssignmentId(id), 0, NOW, null, AssignmentUrgency.N, null, null, "Address " + id, "");
    }
}
