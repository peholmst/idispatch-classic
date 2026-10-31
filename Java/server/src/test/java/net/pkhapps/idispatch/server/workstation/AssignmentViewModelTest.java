package net.pkhapps.idispatch.server.workstation;

import net.pkhapps.idispatch.server.assignment.Assignment;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.assignment.AssignmentService;
import net.pkhapps.idispatch.server.assignment.AssignmentUrgency;
import net.pkhapps.idispatch.server.assignment.MissingForDispatch;
import net.pkhapps.idispatch.server.dispatch.AssignmentNotReadyForDispatchException;
import net.pkhapps.idispatch.server.dispatch.DispatchService;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceQueries;
import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.resource.ResourceStatus;
import net.pkhapps.idispatch.server.resource.ResourceStatusService;
import net.pkhapps.idispatch.server.shared.UpdateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static net.pkhapps.idispatch.server.workstation.DispatchViewModelTest.NOW;
import static net.pkhapps.idispatch.server.workstation.DispatchViewModelTest.assignment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The rules of the assignment form, without the form: it saves every change right away, and dispatches the resources
 * on the assignment.
 */
class AssignmentViewModelTest {

    static final AssignmentId ID = new AssignmentId(1);
    static final ResourceId FIRST = new ResourceId(10);
    static final ResourceId SECOND = new ResourceId(11);

    AssignmentService assignmentService = mock(AssignmentService.class);
    ResourceStatusService resourceStatusService = mock(ResourceStatusService.class);
    ResourceQueries resourceQueries = mock(ResourceQueries.class);
    DispatchService dispatchService = mock(DispatchService.class);
    AssignmentViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new AssignmentViewModel(assignmentService, resourceStatusService, resourceQueries,
                dispatchService);
        when(assignmentService.findAssignment(ID)).thenReturn(Optional.of(assignment(1)));
        when(resourceQueries.findCallSigns(any())).thenReturn(Map.of(FIRST, "RVS101", SECOND, "RVS102"));
    }

    @Test
    void aChangeIsSavedRightAway() {
        viewModel.open(ID);
        var saved = assignment(1).withAddress("Main Street 1");
        when(assignmentService.updateAssignment(saved)).thenReturn(new UpdateResult.Success<>(withVersion(saved, 1)));

        viewModel.setAddress("Main Street 1");

        assertEquals("Main Street 1", viewModel.address().peek());
    }

    @Test
    void aChangeThatChangesNothingIsNotSaved() {
        viewModel.open(ID);

        viewModel.setAddress("Address 1");

        verify(assignmentService, never()).updateAssignment(any());
    }

    @Test
    void aChangeWithNoAssignmentOpenIsNotSaved() {
        viewModel.setAddress("Main Street 1");

        verify(assignmentService, never()).updateAssignment(any());
    }

    @Test
    void anotherDispatchersNewerVersionReplacesTheChange() {
        viewModel.open(ID);
        var theirs = withVersion(assignment(1).withAddress("Their Street"), 1);
        when(assignmentService.updateAssignment(any()))
                .thenReturn(new UpdateResult.Conflict<>(assignment(1).withAddress("Mine"), theirs));

        viewModel.setAddress("Mine");

        assertEquals("Their Street", viewModel.address().peek());
    }

    @Test
    void clearingTheUrgencyMakesTheAssignmentNotUrgent() {
        when(assignmentService.findAssignment(ID))
                .thenReturn(Optional.of(assignment(1).withUrgency(AssignmentUrgency.A)));
        viewModel.open(ID);
        var saved = assignment(1);
        when(assignmentService.updateAssignment(saved)).thenReturn(new UpdateResult.Success<>(withVersion(saved, 1)));

        viewModel.setUrgency(null);

        assertEquals(AssignmentUrgency.N, viewModel.urgency().peek());
    }

    @Test
    void dispatchingAnAssignmentThatIsNotReadyTellsWhatIsMissing() throws Exception {
        viewModel.open(ID);
        when(dispatchService.dispatchAllResources(any())).thenThrow(new AssignmentNotReadyForDispatchException(
                EnumSet.of(MissingForDispatch.TYPE, MissingForDispatch.MUNICIPALITY)));

        assertEquals(Set.of(MissingForDispatch.TYPE, MissingForDispatch.MUNICIPALITY), viewModel.dispatchAll());
    }

    @Test
    void dispatchingWithNoAssignmentOpenDoesNothing() {
        assertEquals(Set.of(), viewModel.dispatchAll());
        verifyNoInteractions(dispatchService);
    }

    @Test
    void dispatchingTheSelectedResourcesDispatchesOnlyThose() throws Exception {
        givenOnAssignment(FIRST, SECOND);
        viewModel.open(ID);
        selectResources(FIRST);

        assertTrue(viewModel.anyResourceSelected().peek());
        assertEquals(Set.of(), viewModel.dispatchSelected());
        verify(dispatchService).dispatchSelectedResources(any(), eq(Set.of(FIRST)));
    }

    @Test
    void aSelectedResourceThatLeavesTheAssignmentIsNoLongerSelected() {
        givenOnAssignment(FIRST, SECOND);
        viewModel.open(ID);
        selectResources(FIRST, SECOND);

        givenOnAssignment(SECOND);
        viewModel.resourcesChanged();

        assertEquals(Set.of(SECOND), selectedResourceIds());
    }

    @Test
    void theSelectionFollowsTheResourcesNewState() {
        givenOnAssignment(FIRST);
        viewModel.open(ID);
        selectResources(FIRST);

        when(resourceStatusService.getStatusOfResourcesAssignedToAssignment(ID))
                .thenReturn(List.of(status(FIRST, ResourceState.DISPATCHED)));
        viewModel.resourcesChanged();

        assertEquals(ResourceState.DISPATCHED,
                viewModel.selectedResources().peek().iterator().next().status().state());
    }

    @Test
    void openingAnotherAssignmentStartsWithNoResourcesSelected() {
        givenOnAssignment(FIRST);
        viewModel.open(ID);
        selectResources(FIRST);
        var other = new AssignmentId(2);
        when(assignmentService.findAssignment(other)).thenReturn(Optional.of(assignment(2)));
        when(resourceStatusService.getStatusOfResourcesAssignedToAssignment(other))
                .thenReturn(List.of(status(FIRST, ResourceState.RESERVED)));

        viewModel.open(other);

        assertFalse(viewModel.anyResourceSelected().peek());
    }

    private void givenOnAssignment(ResourceId... resources) {
        when(resourceStatusService.getStatusOfResourcesAssignedToAssignment(ID)).thenReturn(
                java.util.Arrays.stream(resources).map(id -> status(id, ResourceState.RESERVED)).toList());
    }

    private void selectResources(ResourceId... resources) {
        var ids = Set.of(resources);
        viewModel.selectResources(viewModel.assignedResources().peek().stream()
                .filter(row -> ids.contains(row.resourceId())).collect(java.util.stream.Collectors.toSet()));
    }

    private Set<ResourceId> selectedResourceIds() {
        return viewModel.selectedResources().peek().stream().map(ResourceRow::resourceId)
                .collect(java.util.stream.Collectors.toSet());
    }

    private static ResourceStatus status(ResourceId resource, ResourceState state) {
        return new ResourceStatus(resource, 0, state, ID, NOW);
    }

    private static Assignment withVersion(Assignment assignment, long version) {
        return new Assignment(assignment.id(), version, assignment.opened(), assignment.closed(), assignment.urgency(),
                assignment.typeId(), assignment.municipalityId(), assignment.address(), assignment.description());
    }
}
