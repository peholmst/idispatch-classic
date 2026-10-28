package net.pkhapps.idispatch.server.ui.reports;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import net.pkhapps.idispatch.server.boundary.AssignmentService;
import net.pkhapps.idispatch.server.boundary.DispatchService;
import net.pkhapps.idispatch.server.boundary.ResourceStatusService;
import net.pkhapps.idispatch.server.entity.Assignment;
import net.pkhapps.idispatch.server.entity.AssignmentUrgency;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.ResourceState;
import net.pkhapps.idispatch.server.entity.ValidationFailedException;
import net.pkhapps.idispatch.server.ui.AbstractUITest;
import net.pkhapps.idispatch.server.ui.Formatters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

@WithMockUser(username = "report-reader", roles = {"REPORT_READER", "DISPATCHER"})
class ReportsViewTest extends AbstractUITest {

    @Autowired
    AssignmentService assignmentService;
    @Autowired
    ResourceStatusService resourceStatusService;
    @Autowired
    DispatchService dispatchService;

    @Test
    void view_startsWithNoAssignmentSelected() {
        navigate(ReportsView.class);

        assertThat(find(Span.class).withText("No assignment selected").exists()).isTrue();
        assertThat(find(FormLayout.class, reportPanel()).exists()).isFalse();
    }

    @Test
    void view_listsClosedAssignmentsOnly() {
        var closed = handledAssignment(unique("CS"));
        var open = assignmentService.findAssignment(assignmentService.openAssignment()).orElseThrow();
        navigate(ReportsView.class);

        assertThat(containsRow(assignments(), Assignment::getId, closed.getId())).isTrue();
        assertThat(containsRow(assignments(), Assignment::getId, open.getId())).isFalse();
        var row = rowOf(assignments(), Assignment::getId, closed.getId());
        assertThat(test(assignments()).getCellText(row, 0)).isEqualTo(Formatters.formatAssignment(closed));
    }

    @Test
    void selectingAssignment_showsReportWithResourceTimeline() {
        var callSign = unique("CS");
        var closed = handledAssignment(callSign);
        navigate(ReportsView.class);

        test(assignments()).select(rowOf(assignments(), Assignment::getId, closed.getId()));

        assertThat(find(Span.class).withText("No assignment selected").exists()).isFalse();
        assertThat(reportField("Number").getValue()).isEqualTo(closed.getId().toString());
        assertThat(reportField("Opened").getValue()).isEqualTo(Formatters.formatDateTime(closed.getOpened()));
        assertThat(reportField("Closed").getValue()).isEqualTo(Formatters.formatDateTime(closed.getClosed()));
        assertThat(reportField("Urgency").getValue()).isEqualTo("B");
        assertThat(reportField("Type").getValue()).isEqualTo(closed.getType().getFormattedDescription());
        assertThat(reportField("Municipality").getValue()).isEqualTo(closed.getMunicipality().getName());
        assertThat(reportField("Address").getValue()).isEqualTo("Report Road 7");
        assertThat(find(TextArea.class, reportPanel()).single().getValue()).isEqualTo("Smoke seen");
        assertThat(reportField("Number").isReadOnly()).isTrue();

        var resources = resources();
        assertThat(test(resources).size()).isEqualTo(1);
        assertThat(test(resources).getCellText(0, 0)).isEqualTo(callSign);
        assertThat(test(resources).getCellText(0, 1)).as("Dispatched").isNotBlank();
        assertThat(test(resources).getCellText(0, 2)).as("En route").isNotBlank();
        assertThat(test(resources).getCellText(0, 3)).as("On scene").isNotBlank();
        assertThat(test(resources).getCellText(0, 4)).as("Available").isNotBlank();
        assertThat(test(resources).getCellText(0, 5)).as("At station").isEmpty();
    }

    @Test
    void refresh_showsNewlyClosedAssignments() {
        navigate(ReportsView.class);
        var closed = handledAssignment(unique("CS"));
        assertThat(containsRow(assignments(), Assignment::getId, closed.getId())).isFalse();

        test(refreshButton()).click();

        assertThat(containsRow(assignments(), Assignment::getId, closed.getId())).isTrue();
    }

    /**
     * Runs an assignment from start to finish through the services, like a dispatcher would: opens it, fills it
     * in, assigns and dispatches a resource, lets the resource progress to available, and closes the assignment.
     */
    private Assignment handledAssignment(String callSign) {
        var type = testData.assignmentType(unique("T"), "Building fire");
        var municipality = testData.municipality(unique("Town "));
        Resource resource = testData.resource(callSign, ResourceState.AT_STATION);

        var assignment = assignmentService.findAssignment(assignmentService.openAssignment()).orElseThrow();
        assignment.setType(type);
        assignment.setMunicipality(municipality);
        assignment.setAddress("Report Road 7");
        assignment.setDescription("Smoke seen");
        assignment.setUrgency(AssignmentUrgency.B);
        assignment = assignmentService.updateAssignment(assignment).getData().orElseThrow();

        resourceStatusService.setResourceAssignment(resource, assignment);
        try {
            dispatchService.dispatchAllResources(assignment);
        } catch (ValidationFailedException ex) {
            throw new AssertionError("Assignment could not be dispatched", ex);
        }
        resourceStatusService.setResourceState(resource, ResourceState.EN_ROUTE);
        resourceStatusService.setResourceState(resource, ResourceState.ON_SCENE);
        resourceStatusService.setResourceState(resource, ResourceState.AVAILABLE);
        assertThat(assignmentService.closeAssignment(assignment)).isTrue();
        return assignmentService.findAssignment(assignment.getId()).orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private Grid<Assignment> assignments() {
        return find(Grid.class, currentView()).all().stream()
                .filter(grid -> grid.getParent().filter(reportPanel()::equals).isEmpty())
                .findFirst()
                .orElseThrow();
    }

    private Grid<?> resources() {
        return find(Grid.class, reportPanel()).single();
    }

    private AssignmentReportPanel reportPanel() {
        return find(AssignmentReportPanel.class).single();
    }

    private TextField reportField(String label) {
        var item = find(FormLayout.FormItem.class, reportPanel()).all().stream()
                .filter(formItem -> formItem.getElement().getTextRecursively().equals(label))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No report field " + label));
        return find(TextField.class, item).single();
    }

    private Button refreshButton() {
        return find(Button.class).all().stream()
                .filter(b -> b.getIcon() != null && "vaadin:refresh".equals(b.getIcon().getElement().getAttribute("icon")))
                .findFirst()
                .orElseThrow();
    }
}
