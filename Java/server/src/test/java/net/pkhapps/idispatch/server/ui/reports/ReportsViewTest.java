package net.pkhapps.idispatch.server.ui.reports;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import net.pkhapps.idispatch.server.boundary.AssignmentService;
import net.pkhapps.idispatch.server.boundary.DispatchService;
import net.pkhapps.idispatch.server.boundary.ReportService;
import net.pkhapps.idispatch.server.boundary.ResourceStatusService;
import net.pkhapps.idispatch.server.entity.Assignment;
import net.pkhapps.idispatch.server.entity.AssignmentUrgency;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.ResourceState;
import net.pkhapps.idispatch.server.entity.ValidationFailedException;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.RootView;
import net.pkhapps.idispatch.server.util.UpdateResult;
import net.pkhapps.idispatch.server.ui.AbstractUITest;
import net.pkhapps.idispatch.server.ui.Formatters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.stream.Stream;

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
    @Autowired
    ReportService reportService;

    @Test
    void view_startsWithNoAssignmentSelected() {
        navigate(ReportsView.class);

        assertThat(find(Span.class).withText("No assignment selected").exists()).isTrue();
        assertThat(view().getDetail()).isNull();
        assertThat(find(AssignmentReportPanel.class).exists()).isFalse();
    }

    @Test
    void view_listsClosedAssignmentsOnly() {
        var closed = handledAssignment(unique("CS"));
        var open = assignmentService.findAssignment(assignmentService.openAssignment()).orElseThrow();
        navigate(ReportsView.class);

        assertThat(containsRow(assignments(), Assignment::getId, closed.getId())).isTrue();
        assertThat(containsRow(assignments(), Assignment::getId, open.getId())).isFalse();
        var row = rowOf(assignments(), Assignment::getId, closed.getId());
        assertThat(test(assignments()).getCellText(row, 0)).as("Number").isEqualTo(closed.getId().toString());
        assertThat(test(assignments()).getCellText(row, 1)).as("Closed").isEqualTo(Formatters.formatDateTime(closed.getClosed()));
        assertThat(test(assignments()).getCellText(row, 2)).as("Urgency").isEqualTo("B");
        assertThat(test(assignments()).getCellText(row, 3)).as("Type").isEqualTo(closed.getType().getFormattedDescription());
        assertThat(test(assignments()).getCellText(row, 4)).as("Municipality").isEqualTo(closed.getMunicipality().getName());
        assertThat(test(assignments()).getCellText(row, 5)).as("Address").isEqualTo("Report Road 7");
    }

    @Test
    void assignments_areSortedByClosedTimeNewestFirst() {
        var older = handledAssignment(unique("CS"));
        var newer = handledAssignment(unique("CS"));
        navigate(ReportsView.class);

        assertThat(rowOf(assignments(), Assignment::getId, newer.getId()))
                .isLessThan(rowOf(assignments(), Assignment::getId, older.getId()));
    }

    @Test
    @WithMockUser(username = "report-writer", roles = {"REPORT_WRITER", "DISPATCHER"})
    void closingTheReportOverlay_deselectsTheAssignment_butAsksFirstIfThereAreUnsavedChanges() {
        var closed = handledAssignment(unique("CS"));
        navigate(ReportsView.class);
        select(closed);

        ComponentUtil.fireEvent(view(), new MasterDetailLayout.DetailEscapePressEvent(view(), true));
        assertThat(selectedAssignment()).isNull();
        assertThat(view().getDetail()).isNull();

        select(closed);
        integerField("How many people were involved?").setValue(5);
        ComponentUtil.fireEvent(view(), new MasterDetailLayout.BackdropClickEvent(view(), true));
        assertThat(selectedAssignment()).isEqualTo(closed);
        assertThat(view().getDetail()).isSameAs(reportPanel());
        test(confirmDialog()).confirm();

        assertThat(selectedAssignment()).isNull();
        assertThat(view().getDetail()).isNull();
    }

    @Test
    void selectingAssignment_showsReportWithResourceTimeline() {
        var callSign = unique("CS");
        var closed = handledAssignment(callSign);
        navigate(ReportsView.class);

        select(closed);

        assertThat(view().getDetail()).isSameAs(reportPanel());
        assertThat(reportField("Number").getValue()).isEqualTo(closed.getId().toString());
        assertThat(reportField("Opened").getValue()).isEqualTo(Formatters.formatDateTime(closed.getOpened()));
        assertThat(reportField("Closed").getValue()).isEqualTo(Formatters.formatDateTime(closed.getClosed()));
        assertThat(reportField("Urgency").getValue()).isEqualTo("B");
        assertThat(reportField("Type").getValue()).isEqualTo(closed.getType().getFormattedDescription());
        assertThat(reportField("Municipality").getValue()).isEqualTo(closed.getMunicipality().getName());
        assertThat(reportField("Address").getValue()).isEqualTo("Report Road 7");
        assertThat(find(TextArea.class, reportItem("Description")).single().getValue()).isEqualTo("Smoke seen");
        assertThat(reportField("Number").isReadOnly()).isTrue();

        var resources = resources();
        assertThat(test(resources).size()).isEqualTo(1);
        assertThat(test(resources).getCellText(0, 0)).isEqualTo(callSign);
        assertThat(test(resources).getCellText(0, 1)).as("Dispatched").matches("\\d{2}:\\d{2}:\\d{2}");
        assertThat(test(resources).getCellText(0, 2)).as("En route").matches("\\d{2}:\\d{2}:\\d{2}");
        assertThat(test(resources).getCellText(0, 3)).as("On scene").matches("\\d{2}:\\d{2}:\\d{2}");
        assertThat(test(resources).getCellText(0, 4)).as("Available").matches("\\d{2}:\\d{2}:\\d{2}");
        assertThat(test(resources).getCellText(0, 5)).as("At station").isEmpty();
    }

    @Test
    @WithMockUser(username = "report-writer", roles = {"REPORT_WRITER", "DISPATCHER"})
    void writer_fillsInWhatHappenedOnScene() {
        var callSign = unique("CS");
        var closed = handledAssignment(callSign);
        navigate(ReportsView.class);
        select(closed);

        assertThat(find(Span.class, reportPanel()).withText("No units responded to this assignment").exists()).isFalse();
        textArea(reportPanel(), "How did the situation develop?").setValue("The fire spread to the attic");
        integerField("How many people were involved?").setValue(4);
        integerField("How many lightly injured?").setValue(2);
        integerField("How many seriously injured?").setValue(1);
        integerField("How many deceased?").setValue(0);
        unitDetails(callSign).setOpened(true);
        textArea(unitForm(callSign), "What was the situation like when you arrived?").setValue("Smoke from the roof");
        textArea(unitForm(callSign), "What actions did you take?").setValue("Interior attack");
        test(button("Save Report")).click();

        assertThat(notificationTexts()).contains("Report saved");

        test(refreshButton()).click();
        select(closed);
        unitDetails(callSign).setOpened(true);
        assertThat(textArea(reportPanel(), "How did the situation develop?").getValue()).isEqualTo("The fire spread to the attic");
        assertThat(integerField("How many people were involved?").getValue()).isEqualTo(4);
        assertThat(integerField("How many lightly injured?").getValue()).isEqualTo(2);
        assertThat(integerField("How many seriously injured?").getValue()).isEqualTo(1);
        assertThat(integerField("How many deceased?").getValue()).isEqualTo(0);
        assertThat(textArea(unitForm(callSign), "What was the situation like when you arrived?").getValue()).isEqualTo("Smoke from the roof");
        assertThat(textArea(unitForm(callSign), "What actions did you take?").getValue()).isEqualTo("Interior attack");

        // Saving twice must work, so the panel has to pick up the new version of the report
        textArea(unitForm(callSign), "What actions did you take?").setValue("Interior attack, then ventilation");
        test(button("Save Report")).click();
        assertThat(reportService.findReportByAssignmentId(closed.getId()).orElseThrow().getReport()
                .getUnitReport(unitForm(callSign).getResource()).getActionsTaken()).isEqualTo("Interior attack, then ventilation");
    }

    @Test
    @WithMockUser(username = "report-writer", roles = {"REPORT_WRITER", "DISPATCHER"})
    void writer_cannotReportMoreCasualtiesThanPeopleInvolved() {
        var closed = handledAssignment(unique("CS"));
        navigate(ReportsView.class);
        select(closed);

        integerField("How many people were involved?").setValue(2);
        integerField("How many lightly injured?").setValue(2);
        integerField("How many deceased?").setValue(1);
        test(button("Save Report")).click();

        assertThat(notificationTexts())
                .contains("The injured and deceased cannot add up to more than the number of people involved");
        assertThat(reportService.findReportByAssignmentId(closed.getId()).orElseThrow().getReport().isNew()).isTrue();
    }

    @Test
    @WithMockUser(username = "report-writer", roles = {"REPORT_WRITER", "DISPATCHER"})
    void writer_isToldWhenSomeoneElseSavedTheReportFirst() throws ValidationFailedException {
        var closed = handledAssignment(unique("CS"));
        navigate(ReportsView.class);
        select(closed);

        var otherReport = reportService.findReportByAssignmentId(closed.getId()).orElseThrow().getReport();
        otherReport.setSituationDevelopment("Written by someone else");
        assertThat(reportService.saveReport(otherReport)).isInstanceOf(UpdateResult.Success.class);

        textArea(reportPanel(), "How did the situation develop?").setValue("Written by me");
        test(button("Save Report")).click();

        assertThat(notificationTexts()).anyMatch(text -> text.startsWith("Someone else saved this report"));
        assertThat(textArea(reportPanel(), "How did the situation develop?").getValue()).isEqualTo("Written by someone else");
    }

    @Test
    @WithMockUser(username = "report-writer", roles = {"REPORT_WRITER", "DISPATCHER"})
    void selectingAnotherAssignment_withUnsavedChanges_asksFirst() {
        var first = handledAssignment(unique("CS"));
        var second = handledAssignment(unique("CS"));
        navigate(ReportsView.class);
        select(first);
        textArea(reportPanel(), "How did the situation develop?").setValue("Not saved yet");

        select(second);

        assertThat(test(confirmDialog()).getHeader()).isEqualTo("Unsaved changes");
        assertThat(selectedAssignment()).isEqualTo(first);
        test(confirmDialog()).cancel();
        assertThat(selectedAssignment()).isEqualTo(first);
        assertThat(reportField("Number").getValue()).isEqualTo(first.getId().toString());
        assertThat(textArea(reportPanel(), "How did the situation develop?").getValue()).isEqualTo("Not saved yet");

        select(second);
        test(confirmDialog()).confirm();

        assertThat(selectedAssignment()).isEqualTo(second);
        assertThat(reportField("Number").getValue()).isEqualTo(second.getId().toString());
        select(first);
        assertThat(find(ConfirmDialog.class).exists()).isFalse();
        assertThat(textArea(reportPanel(), "How did the situation develop?").getValue()).isEmpty();
    }

    @Test
    @WithMockUser(username = "report-writer", roles = {"REPORT_WRITER", "DISPATCHER"})
    void selectingAnotherAssignment_afterSaving_doesNotAsk() {
        var first = handledAssignment(unique("CS"));
        var second = handledAssignment(unique("CS"));
        navigate(ReportsView.class);
        select(first);
        integerField("How many people were involved?").setValue(3);
        test(button("Save Report")).click();

        select(second);

        assertThat(find(ConfirmDialog.class).exists()).isFalse();
        assertThat(selectedAssignment()).isEqualTo(second);
    }

    @Test
    @WithMockUser(username = "report-writer", roles = {"REPORT_WRITER", "DISPATCHER"})
    void refreshing_withUnsavedChanges_asksFirst() {
        var closed = handledAssignment(unique("CS"));
        navigate(ReportsView.class);
        select(closed);
        integerField("How many deceased?").setValue(0);

        test(refreshButton()).click();
        assertThat(selectedAssignment()).isEqualTo(closed);
        test(confirmDialog()).confirm();

        assertThat(selectedAssignment()).isNull();
        assertThat(view().getDetail()).isNull();
    }

    @Test
    @WithMockUser(username = "report-writer", roles = {"REPORT_WRITER", "DISPATCHER"})
    void leavingTheView_withUnsavedChanges_asksFirst() {
        var closed = handledAssignment(unique("CS"));
        navigate(ReportsView.class);
        select(closed);
        textArea(reportPanel(), "How did the situation develop?").setValue("Not saved yet");

        UI.getCurrent().navigate(RootView.class);
        assertThat(currentView()).isInstanceOf(ReportsView.class);
        test(confirmDialog()).confirm();

        assertThat(currentView()).isInstanceOf(RootView.class);
    }

    @Test
    void savingReport_recordsWhoLastEditedTheSceneAndEachUnit() {
        loginWithIdToken("subject-erik", "Erik", "Explorer");
        var callSign = unique("CS");
        var closed = handledAssignment(callSign);
        navigate(ReportsView.class);
        select(closed);
        assertThat(lastEditTexts(reportPanel())).contains("Not filled in yet");

        textArea(reportPanel(), "How did the situation develop?").setValue("The fire spread to the attic");
        test(button("Save Report")).click();

        var report = reportService.findReportByAssignmentId(closed.getId()).orElseThrow().getReport();
        assertThat(report.getLastEdit().getSubject()).isEqualTo("subject-erik");
        assertThat(report.getLastEdit().getFirstName()).isEqualTo("Erik");
        assertThat(report.getLastEdit().getLastName()).isEqualTo("Explorer");
        var resource = unitForm(callSign).getResource();
        assertThat(report.getUnitReport(resource).getLastEdit()).as("Unit report was not touched").isNull();
        assertThat(lastEditTexts(reportPanel())).anyMatch(text -> text.startsWith("Last edited by Erik Explorer on "));
        assertThat(lastEditTexts(unitDetails(callSign).getSummary())).containsExactly("Not filled in yet");

        loginWithIdToken("subject-ella", "Ella", "Explorer");
        navigate(ReportsView.class);
        select(closed);
        unitDetails(callSign).setOpened(true);
        textArea(unitForm(callSign), "What actions did you take?").setValue("Interior attack");
        test(button("Save Report")).click();

        report = reportService.findReportByAssignmentId(closed.getId()).orElseThrow().getReport();
        assertThat(report.getLastEdit().getSubject()).as("Scene was not touched").isEqualTo("subject-erik");
        var unitEdit = report.getUnitReport(resource).getLastEdit();
        assertThat(unitEdit.getSubject()).isEqualTo("subject-ella");
        assertThat(unitEdit.getFirstName()).isEqualTo("Ella");
        assertThat(unitEdit.getLastName()).isEqualTo("Explorer");
        assertThat(lastEditTexts(unitDetails(callSign).getSummary())).singleElement().asString().startsWith("Last edited by Ella Explorer on ");
    }

    @Test
    @WithMockUser(username = "report-writer", roles = {"REPORT_WRITER", "DISPATCHER"})
    void singleUnit_startsClosed() {
        var callSign = unique("CS");
        var closed = handledAssignment(callSign);
        navigate(ReportsView.class);
        select(closed);

        assertThat(unitDetails(callSign).isOpened()).isFalse();
    }

    @Test
    @WithMockUser(username = "report-writer", roles = {"REPORT_WRITER", "DISPATCHER"})
    void manyUnits_startClosed_andOpenedUnitsStayOpenAfterSaving() {
        var mine = unique("CS");
        var other = unique("CS");
        var closed = handledAssignment(mine, other);
        var another = handledAssignment(unique("CS"), unique("CS"));
        navigate(ReportsView.class);
        select(closed);

        assertThat(unitDetails(mine).isOpened()).isFalse();
        assertThat(unitDetails(other).isOpened()).isFalse();
        assertThat(((HorizontalLayout) unitDetails(mine).getSummary()).getComponentAt(0))
                .isInstanceOfSatisfying(Span.class, span -> assertThat(span.getText()).isEqualTo(mine));

        unitDetails(mine).setOpened(true);
        textArea(unitForm(mine), "What actions did you take?").setValue("Interior attack");
        test(button("Save Report")).click();

        assertThat(notificationTexts()).contains("Report saved");
        assertThat(unitDetails(mine).isOpened()).isTrue();
        assertThat(unitDetails(other).isOpened()).isFalse();

        select(another);
        select(closed);
        assertThat(unitDetails(mine).isOpened()).as("Another assignment in between starts over").isFalse();
    }

    @Test
    void reader_seesWhatHappenedOnSceneButCannotChangeIt() {
        var callSign = unique("CS");
        var closed = handledAssignment(callSign);
        navigate(ReportsView.class);
        select(closed);

        assertThat(textArea(reportPanel(), "How did the situation develop?").isReadOnly()).isTrue();
        assertThat(integerField("How many people were involved?").isReadOnly()).isTrue();
        unitDetails(callSign).setOpened(true);
        assertThat(textArea(unitForm(callSign), "What actions did you take?").isReadOnly()).isTrue();
        assertThat(find(Button.class).withText("Save Report").exists()).isFalse();
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
     * in, assigns and dispatches one resource per call sign, lets the resources progress to available, and closes the
     * assignment.
     */
    private Assignment handledAssignment(String... callSigns) {
        var type = testData.assignmentType(unique("T"), "Building fire");
        var municipality = testData.municipality(unique("Town "));
        List<Resource> resources = Stream.of(callSigns)
                .map(callSign -> testData.resource(callSign, ResourceState.AT_STATION))
                .toList();

        var assignment = assignmentService.findAssignment(assignmentService.openAssignment()).orElseThrow();
        assignment.setType(type);
        assignment.setMunicipality(municipality);
        assignment.setAddress("Report Road 7");
        assignment.setDescription("Smoke seen");
        assignment.setUrgency(AssignmentUrgency.B);
        assignment = assignmentService.updateAssignment(assignment).getData().orElseThrow();

        for (var resource : resources) {
            resourceStatusService.setResourceAssignment(resource, assignment);
        }
        try {
            dispatchService.dispatchAllResources(assignment);
        } catch (ValidationFailedException ex) {
            throw new AssertionError("Assignment could not be dispatched", ex);
        }
        for (var resource : resources) {
            resourceStatusService.setResourceState(resource, ResourceState.EN_ROUTE);
            resourceStatusService.setResourceState(resource, ResourceState.ON_SCENE);
            resourceStatusService.setResourceState(resource, ResourceState.AVAILABLE);
        }
        assertThat(assignmentService.closeAssignment(assignment)).isTrue();
        return assignmentService.findAssignment(assignment.getId()).orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private Grid<Assignment> assignments() {
        return (Grid<Assignment>) view().getMaster();
    }

    private ReportsView view() {
        return (ReportsView) currentView();
    }

    private Grid<?> resources() {
        return find(Grid.class, reportPanel()).single();
    }

    private AssignmentReportPanel reportPanel() {
        return find(AssignmentReportPanel.class).single();
    }

    private void loginWithIdToken(String subject, String givenName, String familyName) {
        var idToken = OidcIdToken.withTokenValue("id-token")
                .issuer("http://keycloak.invalid/realms/idispatch")
                .subject(subject)
                .claim("preferred_username", subject)
                .claim("given_name", givenName)
                .claim("family_name", familyName)
                .build();
        var user = new DefaultOidcUser(List.of(new SimpleGrantedAuthority(Roles.ROLE_REPORT_WRITER),
                new SimpleGrantedAuthority(Roles.ROLE_DISPATCHER)), idToken, "preferred_username");
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new OAuth2AuthenticationToken(user, user.getAuthorities(), "keycloak"));
        SecurityContextHolder.setContext(context);
    }

    private List<String> lastEditTexts(Component container) {
        return find(Span.class, container).all().stream()
                .map(Span::getText)
                .filter(text -> text.equals("Not filled in yet") || text.startsWith("Last edited by "))
                .toList();
    }

    private ConfirmDialog confirmDialog() {
        return find(ConfirmDialog.class).single();
    }

    private Assignment selectedAssignment() {
        return assignments().asSingleSelect().getValue();
    }

    private void select(Assignment assignment) {
        test(assignments()).select(rowOf(assignments(), Assignment::getId, assignment.getId()));
    }

    private FormLayout.FormItem reportItem(String label) {
        return find(FormLayout.FormItem.class, reportPanel()).all().stream()
                .filter(formItem -> formItem.getElement().getTextRecursively().equals(label))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No report field " + label));
    }

    private TextField reportField(String label) {
        return find(TextField.class, reportItem(label)).single();
    }

    private TextArea textArea(Component container, String label) {
        return find(TextArea.class, container).all().stream()
                .filter(textArea -> label.equals(textArea.getLabel()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No text area " + label));
    }

    private IntegerField integerField(String label) {
        return find(IntegerField.class, reportPanel()).all().stream()
                .filter(field -> label.equals(field.getLabel()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No number field " + label));
    }

    private Details unitDetails(String callSign) {
        var form = unitForm(callSign);
        return find(Details.class, reportPanel()).all().stream()
                .filter(details -> details.getContent().anyMatch(form::equals))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No expandable unit report for " + callSign));
    }

    private AssignmentReportPanel.UnitReportForm unitForm(String callSign) {
        return find(AssignmentReportPanel.UnitReportForm.class, reportPanel()).all().stream()
                .filter(form -> form.getResource().getCallSign().equals(callSign))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No unit report for " + callSign));
    }

    private Button refreshButton() {
        return find(Button.class).all().stream()
                .filter(b -> b.getIcon() != null && "vaadin:refresh".equals(b.getIcon().getElement().getAttribute("icon")))
                .findFirst()
                .orElseThrow();
    }
}
