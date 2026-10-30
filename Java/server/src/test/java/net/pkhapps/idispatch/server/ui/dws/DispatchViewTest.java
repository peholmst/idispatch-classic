package net.pkhapps.idispatch.server.ui.dws;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import net.pkhapps.idispatch.server.entity.Assignment;
import net.pkhapps.idispatch.server.entity.AssignmentType;
import net.pkhapps.idispatch.server.entity.Municipality;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.ResourceState;
import net.pkhapps.idispatch.server.entity.ResourceStatus;
import net.pkhapps.idispatch.server.ui.AbstractUITest;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.stream.IntStream;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the dispatcher workstation: opening and editing assignments, assigning and dispatching resources,
 * changing resource states and closing assignments.
 */
@WithMockUser(username = "dispatcher", roles = "DISPATCHER")
class DispatchViewTest extends AbstractUITest {

    private AssignmentType assignmentType;
    private Municipality municipality;

    @Test
    void view_startsWithNoAssignmentSelected() {
        navigate(DispatchView.class);

        assertThat(isNoAssignmentSelectedShown()).isTrue();
        assertThat(isAssignmentFormShown()).isFalse();
        assertThat(test(button("New Assignment")).isUsable()).isTrue();
    }

    @Test
    void newAssignment_opensSelectsAndShowsIt() {
        navigate(DispatchView.class);

        var assignment = openNewAssignment();

        assertThat(test(assignmentList()).getSelected()).extracting(Assignment::getId).containsExactly(assignment.getId());
        assertThat(isNoAssignmentSelectedShown()).isFalse();
        assertThat(isAssignmentFormShown()).isTrue();
        assertThat(urgencyField().getValue()).hasToString("N");
    }

    @Test
    void editingForm_savesAssignmentImmediately() {
        navigateWithAssignmentOptions();
        var assignment = openNewAssignment();
        var type = assignmentType;

        test(typeField()).selectItem(type.getFormattedDescription());
        test(urgencyField()).selectItem("A");
        test(municipalityField()).selectItem(municipality.getName());
        test(addressField()).setValue("Main Street 1");

        var row = rowOf(assignmentList(), Assignment::getId, assignment.getId());
        assertThat(test(assignmentList()).getCellText(row, 2)).isEqualTo(type.getFormattedDescription());
        var urgencyBadge = (Badge) test(assignmentList()).getCellComponent(row, 3);
        assertThat(urgencyBadge.getText()).isEqualTo("A");
        assertThat(urgencyBadge.getClassNames()).contains("urgency-a");
        assertThat(test(assignmentList()).getCellText(row, 4)).isEqualTo(municipality.getName());
        assertThat(test(assignmentList()).getCellText(row, 5)).isEqualTo("Main Street 1");
    }

    @Test
    void optionsAddedAfterOpeningView_areOfferedForNextAssignment() {
        navigate(DispatchView.class);
        openNewAssignment();
        var type = testData.assignmentType(unique("T"), "Added later");
        var town = testData.municipality(unique("Town "));

        openNewAssignment();

        assertThat(test(typeField()).getSuggestions()).contains(type.getFormattedDescription());
        assertThat(test(municipalityField()).getSuggestions()).contains(town.getName());
    }

    @Test
    void selectingAssignmentInList_showsItInForm() {
        navigate(DispatchView.class);
        var first = openNewAssignment();
        test(addressField()).setValue("First Street");
        openNewAssignment();
        test(addressField()).setValue("Second Street");

        test(assignmentList()).select(rowOf(assignmentList(), Assignment::getId, first.getId()));

        assertThat(addressField().getValue()).isEqualTo("First Street");
    }

    @Test
    void addResource_reservesItForAssignment() {
        var resource = testData.resource(unique("CS"), ResourceState.AVAILABLE);
        navigate(DispatchView.class);
        var assignment = openNewAssignment();

        assign(resource);

        assertThat(assignedCallSigns()).containsExactly(resource.getCallSign());
        assertThat(testData.statusOf(resource).getState()).isEqualTo(ResourceState.RESERVED);
        var row = resourceRow(resource);
        assertThat(statusButton(resourceList(), row).getText()).isEqualTo("Reserved");
        assertThat(test(resourceList()).getCellText(row, 3)).endsWith("(" + assignment.getId() + ")");
        assertThat(test(availableResourceField()).getSuggestions()).doesNotContain(resource.getCallSign());
    }

    @Test
    void unavailableResources_cannotBeAssigned() {
        var resource = testData.resource(unique("CS"), ResourceState.OUT_OF_SERVICE);
        navigate(DispatchView.class);
        openNewAssignment();

        assertThat(test(availableResourceField()).getSuggestions()).doesNotContain(resource.getCallSign());
    }

    @Test
    void removeResource_releasesItFromAssignment() {
        var resource = testData.resource(unique("CS"), ResourceState.AT_STATION);
        navigate(DispatchView.class);
        openNewAssignment();
        assign(resource);
        assertThat(iconButton("vaadin:minus-circle").isEnabled()).isFalse();

        test(assignedResources()).select(0);
        test(iconButton("vaadin:minus-circle")).click();

        assertThat(assignedCallSigns()).isEmpty();
        assertThat(testData.statusOf(resource).getState()).isEqualTo(ResourceState.AT_STATION);
        assertThat(testData.statusOf(resource).getAssignment()).isNull();
    }

    @Test
    void dispatchAll_withIncompleteAssignment_showsValidationErrors() {
        var resource = testData.resource(unique("CS"), ResourceState.AVAILABLE);
        navigate(DispatchView.class);
        openNewAssignment();
        assign(resource);

        test(button("Dispatch All")).click();

        var notification = find(Notification.class).single();
        assertThat(find(ListItem.class, notification).all().stream().map(ListItem::getText))
                .containsExactlyInAnyOrder("Please select an assignment type", "Please select a municipality",
                        "Please enter an address");
        assertThat(testData.statusOf(resource).getState()).isEqualTo(ResourceState.RESERVED);
    }

    @Test
    void dispatchAll_dispatchesAllAssignedResources() {
        var first = testData.resource(unique("CS"), ResourceState.AVAILABLE);
        var second = testData.resource(unique("CS"), ResourceState.AVAILABLE);
        navigateWithAssignmentOptions();
        openCompleteAssignment();
        assign(first);
        assign(second);

        test(button("Dispatch All")).click();

        assertThat(find(Notification.class).all()).isEmpty();
        assertThat(testData.statusOf(first).getState()).isEqualTo(ResourceState.DISPATCHED);
        assertThat(testData.statusOf(second).getState()).isEqualTo(ResourceState.DISPATCHED);
        assertThat(assignedStates()).containsOnly("Dispatched");
    }

    @Test
    void dispatchSelected_dispatchesOnlySelectedResources() {
        var selected = testData.resource(unique("CS"), ResourceState.AVAILABLE);
        var other = testData.resource(unique("CS"), ResourceState.AVAILABLE);
        navigateWithAssignmentOptions();
        openCompleteAssignment();
        assign(selected);
        assign(other);
        assertThat(button("Dispatch Selected").isEnabled()).isFalse();

        test(assignedResources()).select(rowOf(assignedResources(), rs -> rs.getResource().getCallSign(),
                selected.getCallSign()));
        test(button("Dispatch Selected")).click();

        assertThat(testData.statusOf(selected).getState()).isEqualTo(ResourceState.DISPATCHED);
        assertThat(testData.statusOf(other).getState()).isEqualTo(ResourceState.RESERVED);
    }

    @Test
    void dispatchReserved_dispatchesOnlyReservedResources() {
        var reserved = testData.resource(unique("CS"), ResourceState.AVAILABLE);
        var enRoute = testData.resource(unique("CS"), ResourceState.AVAILABLE);
        navigateWithAssignmentOptions();
        openCompleteAssignment();
        assign(reserved);
        assign(enRoute);
        changeStateInAssignment(enRoute, "En route");

        test(button("Dispatch Reserved")).click();

        assertThat(testData.statusOf(reserved).getState()).isEqualTo(ResourceState.DISPATCHED);
        assertThat(testData.statusOf(enRoute).getState()).isEqualTo(ResourceState.EN_ROUTE);
    }

    @Test
    void changeResourceState_fromResourceList_offersManualStatesOnly() {
        var resource = testData.resource(unique("CS"), ResourceState.OUT_OF_SERVICE);
        navigate(DispatchView.class);
        var row = resourceRow(resource);
        assertThat(statusButton(resourceList(), row).getText()).isEqualTo("Out of service");

        test(statusButton(resourceList(), row)).click();

        assertThat(openDialog().getHeaderTitle()).isEqualTo("Change state of " + resource.getCallSign());
        assertThat(find(Button.class, openDialog()).all().stream().map(Button::getText))
                .containsExactlyInAnyOrder("Available", "At station");

        test(buttonInDialog("Available")).click();

        assertThat(findOpenDialog()).isEmpty();
        assertThat(testData.statusOf(resource).getState()).isEqualTo(ResourceState.AVAILABLE);
        assertThat(statusButton(resourceList(), resourceRow(resource)).getText()).isEqualTo("Available");
    }

    @Test
    void closeAssignment_withAssignedResources_keepsItOpen() {
        var resource = testData.resource(unique("CS"), ResourceState.AVAILABLE);
        navigate(DispatchView.class);
        var assignment = openNewAssignment();
        assign(resource);

        test(button("Close Assignment")).click();

        assertThat(containsRow(assignmentList(), Assignment::getId, assignment.getId())).isTrue();
        assertThat(isAssignmentFormShown()).isTrue();
    }

    @Test
    void closeAssignment_withoutResources_removesItFromList() {
        navigate(DispatchView.class);
        var assignment = openNewAssignment();

        test(button("Close Assignment")).click();

        assertThat(containsRow(assignmentList(), Assignment::getId, assignment.getId())).isFalse();
        assertThat(isNoAssignmentSelectedShown()).isTrue();
        assertThat(isAssignmentFormShown()).isFalse();
    }

    @Test
    void closeAssignment_afterResourcesAreAvailableAgain_releasesThem() {
        var resource = testData.resource(unique("CS"), ResourceState.AVAILABLE);
        navigateWithAssignmentOptions();
        var assignment = openCompleteAssignment();
        assign(resource);
        test(button("Dispatch All")).click();
        changeStateInAssignment(resource, "Available");

        test(button("Close Assignment")).click();

        assertThat(containsRow(assignmentList(), Assignment::getId, assignment.getId())).isFalse();
        assertThat(testData.statusOf(resource).getState()).isEqualTo(ResourceState.AVAILABLE);
        assertThat(testData.statusOf(resource).getAssignment()).isNull();
    }

    // --- Helpers ---

    private Assignment openNewAssignment() {
        var before = items(assignmentList()).stream().map(Assignment::getId).toList();
        test(button("New Assignment")).click();
        return items(assignmentList()).stream()
                .filter(a -> !before.contains(a.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No new assignment in list"));
    }

    /**
     * Creates an assignment type and a municipality to fill in assignments with, and opens the dispatcher view.
     */
    private void navigateWithAssignmentOptions() {
        assignmentType = testData.assignmentType(unique("T"), "Fire");
        municipality = testData.municipality(unique("Town "));
        navigate(DispatchView.class);
    }

    private Assignment openCompleteAssignment() {
        var assignment = openNewAssignment();
        test(typeField()).selectItem(assignmentType.getFormattedDescription());
        test(municipalityField()).selectItem(municipality.getName());
        test(addressField()).setValue("Fire Street 1");
        return assignment;
    }

    private void assign(Resource resource) {
        test(availableResourceField()).selectItem(resource.getCallSign());
        test(iconButton("vaadin:plus-circle")).click();
    }

    private void changeStateInAssignment(Resource resource, String newState) {
        var row = rowOf(assignedResources(), rs -> rs.getResource().getCallSign(), resource.getCallSign());
        test(statusButton(assignedResources(), row)).click();
        test(buttonInDialog(newState)).click();
    }

    // Queries only find visible components
    private boolean isNoAssignmentSelectedShown() {
        return find(Span.class).withText("No assignment selected").exists();
    }

    private boolean isAssignmentFormShown() {
        return find(TextField.class).withLabel("Address").exists();
    }

    private Grid<Assignment> assignmentList() {
        return grid(find(AssignmentListPanel.class).single());
    }

    private Grid<ResourceStatus> resourceList() {
        return grid(find(ResourceListPanel.class).single());
    }

    private Grid<ResourceStatus> assignedResources() {
        return grid(find(AssignmentPanel.class).single());
    }

    private int resourceRow(Resource resource) {
        return rowOf(resourceList(), rs -> rs.getResource().getCallSign(), resource.getCallSign());
    }

    private Button statusButton(Grid<ResourceStatus> grid, int row) {
        return (Button) test(grid).getCellComponent(row, 1);
    }

    private List<String> assignedCallSigns() {
        return items(assignedResources()).stream().map(rs -> rs.getResource().getCallSign()).toList();
    }

    private List<String> assignedStates() {
        var grid = assignedResources();
        return IntStream.range(0, test(grid).size())
                .mapToObj(row -> statusButton(grid, row).getText())
                .toList();
    }

    private Button iconButton(String icon) {
        return find(Button.class, find(AssignmentPanel.class).single()).all().stream()
                .filter(b -> b.getIcon() != null && icon.equals(b.getIcon().getElement().getAttribute("icon")))
                .findFirst()
                .orElseThrow();
    }

    private TextField addressField() {
        return find(TextField.class).withLabel("Address").single();
    }

    @SuppressWarnings("unchecked")
    private ComboBox<Object> typeField() {
        return find(ComboBox.class).withLabel("Type").single();
    }

    @SuppressWarnings("unchecked")
    private ComboBox<Object> urgencyField() {
        return find(ComboBox.class).withLabel("Urgency").single();
    }

    @SuppressWarnings("unchecked")
    private ComboBox<Object> municipalityField() {
        return find(ComboBox.class).withLabel("Municipality").single();
    }

    @SuppressWarnings("unchecked")
    private ComboBox<Resource> availableResourceField() {
        return find(ComboBox.class, find(AssignmentPanel.class).single()).all().stream()
                .filter(c -> c.getLabel() == null)
                .findFirst()
                .orElseThrow();
    }
}
