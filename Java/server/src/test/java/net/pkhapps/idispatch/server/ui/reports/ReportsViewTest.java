package net.pkhapps.idispatch.server.ui.reports;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import net.pkhapps.idispatch.server.boundary.AssignmentService;
import net.pkhapps.idispatch.server.boundary.ClosedAssignmentDTO;
import net.pkhapps.idispatch.server.boundary.DispatchService;
import net.pkhapps.idispatch.server.boundary.ReportService;
import net.pkhapps.idispatch.server.boundary.ResourceStatusService;
import net.pkhapps.idispatch.server.entity.Assignment;
import net.pkhapps.idispatch.server.entity.AssignmentReport;
import net.pkhapps.idispatch.server.entity.AssignmentType;
import net.pkhapps.idispatch.server.entity.AssignmentUrgency;
import net.pkhapps.idispatch.server.entity.Municipality;
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

import java.time.LocalDate;
import java.time.ZoneId;
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

        assertThat(containsRow(assignments(), ReportsViewTest::assignmentId, closed.getId())).isTrue();
        assertThat(containsRow(assignments(), ReportsViewTest::assignmentId, open.getId())).isFalse();
        var row = rowOf(assignments(), ReportsViewTest::assignmentId, closed.getId());
        assertThat(test(assignments()).getCellText(row, 0)).as("Number").isEqualTo(closed.getId().toString());
        assertThat(reportStatusCell(closed).getText()).as("Report").isEqualTo("Open");
        assertThat(test(assignments()).getCellText(row, 2)).as("Closed").isEqualTo(Formatters.formatDateTime(closed.getClosed()));
        assertThat(test(assignments()).getCellText(row, 3)).as("Urgency").isEqualTo("B");
        assertThat(test(assignments()).getCellText(row, 4)).as("Type").isEqualTo(closed.getType().getFormattedDescription());
        assertThat(test(assignments()).getCellText(row, 5)).as("Municipality").isEqualTo(closed.getMunicipality().getName());
        assertThat(test(assignments()).getCellText(row, 6)).as("Address").isEqualTo("Report Road 7");
    }

    @Test
    void assignments_areSortedByClosedTimeNewestFirst() {
        var older = handledAssignment(unique("CS"));
        var newer = handledAssignment(unique("CS"));
        navigate(ReportsView.class);

        assertThat(rowOf(assignments(), ReportsViewTest::assignmentId, newer.getId()))
                .isLessThan(rowOf(assignments(), ReportsViewTest::assignmentId, older.getId()));
    }

    @Test
    void sortingByReport_putsOpenReportsFirstOrLast() {
        var reviewed = handledAssignment(unique("CS"));
        asReviewer(() -> reportService.closeReport(storedReport(reviewed)));
        var open = handledAssignment(unique("CS"));
        navigate(ReportsView.class);
        var reportColumn = assignments().getColumns().get(1);

        assignments().sort(GridSortOrder.asc(reportColumn).build());
        assertThat(rowOf(assignments(), ReportsViewTest::assignmentId, open.getId()))
                .isLessThan(rowOf(assignments(), ReportsViewTest::assignmentId, reviewed.getId()));

        assignments().sort(GridSortOrder.desc(reportColumn).build());
        assertThat(rowOf(assignments(), ReportsViewTest::assignmentId, reviewed.getId()))
                .isLessThan(rowOf(assignments(), ReportsViewTest::assignmentId, open.getId()));
    }

    @Test
    void filteringByMunicipality_showsOnlyAssignmentsInThatMunicipality() {
        var first = handledAssignment(unique("CS"));
        var second = handledAssignment(unique("CS"));
        navigate(ReportsView.class);
        ComboBox<Municipality> municipality = comboBox("Municipality");
        assertThat(municipality.getListDataView().getItems())
                .contains(first.getMunicipality(), second.getMunicipality());

        municipality.setValue(first.getMunicipality());

        assertThat(containsRow(assignments(), ReportsViewTest::assignmentId, first.getId())).isTrue();
        assertThat(containsRow(assignments(), ReportsViewTest::assignmentId, second.getId())).isFalse();
    }

    @Test
    void filteringByType_showsOnlyAssignmentsOfThatType() {
        var first = handledAssignment(unique("CS"));
        var second = handledAssignment(unique("CS"));
        navigate(ReportsView.class);
        ComboBox<AssignmentType> type = comboBox("Type");
        assertThat(type.getListDataView().getItems()).contains(first.getType(), second.getType());

        type.setValue(second.getType());

        assertThat(containsRow(assignments(), ReportsViewTest::assignmentId, first.getId())).isFalse();
        assertThat(containsRow(assignments(), ReportsViewTest::assignmentId, second.getId())).isTrue();
    }

    @Test
    void filteringByClosedDate_includesBothEndsOfTheRange() {
        var closed = handledAssignment(unique("CS"));
        var closedOn = LocalDate.ofInstant(closed.getClosed(), ZoneId.systemDefault());
        navigate(ReportsView.class);

        datePicker("Closed from").setValue(closedOn);
        datePicker("Closed to").setValue(closedOn);
        assertThat(containsRow(assignments(), ReportsViewTest::assignmentId, closed.getId())).isTrue();

        datePicker("Closed to").clear();
        datePicker("Closed from").setValue(closedOn.plusDays(1));
        assertThat(containsRow(assignments(), ReportsViewTest::assignmentId, closed.getId())).isFalse();

        datePicker("Closed from").clear();
        datePicker("Closed to").setValue(closedOn.minusDays(1));
        assertThat(containsRow(assignments(), ReportsViewTest::assignmentId, closed.getId())).isFalse();
    }

    @Test
    void filteringByReportStatus_separatesOpenAndClosedReports() {
        var open = handledAssignment(unique("CS"));
        var reviewed = handledAssignment(unique("CS"));
        asReviewer(() -> reportService.closeReport(storedReport(reviewed)));
        navigate(ReportsView.class);
        assertThat(reportStatusCell(open).getText()).isEqualTo("Open");
        assertThat(reportStatusCell(reviewed).getText()).isEqualTo("Closed");
        assertThat(reportStatusCell(reviewed).getThemeNames()).contains("success");
        ComboBox<ReportStatus> status = comboBox("Report");

        status.setValue(ReportStatus.OPEN);
        assertThat(containsRow(assignments(), ReportsViewTest::assignmentId, open.getId())).isTrue();
        assertThat(containsRow(assignments(), ReportsViewTest::assignmentId, reviewed.getId())).isFalse();

        status.setValue(ReportStatus.CLOSED);
        assertThat(containsRow(assignments(), ReportsViewTest::assignmentId, open.getId())).isFalse();
        assertThat(containsRow(assignments(), ReportsViewTest::assignmentId, reviewed.getId())).isTrue();

        test(button("Clear")).click();
        assertThat(status.getValue()).isNull();
    }

    @Test
    void closingAndReopeningInTheReport_updatesTheListRightAway() {
        loginAsWriter();
        var closed = handledAssignment(unique("CS"));
        openReportsViewAs(this::loginAsReviewer);
        this.<ReportStatus>comboBox("Report").setValue(ReportStatus.OPEN);
        select(closed);

        test(button("Close Report")).click();

        assertThat(containsRow(assignments(), ReportsViewTest::assignmentId, closed.getId())).as("No longer open").isFalse();
        this.<ReportStatus>comboBox("Report").clear();
        assertThat(reportStatusCell(closed).getText()).isEqualTo("Closed");
        assertThat(view().getDetail()).as("The report stays open while the reviewer reads it").isSameAs(reportPanel());

        test(button("Re-open Report")).click();

        assertThat(reportStatusCell(closed).getText()).isEqualTo("Open");
    }

    @Test
    void clearingFilters_showsEverythingAgain() {
        var first = handledAssignment(unique("CS"));
        var second = handledAssignment(unique("CS"));
        navigate(ReportsView.class);
        this.<Municipality>comboBox("Municipality").setValue(first.getMunicipality());
        this.<AssignmentType>comboBox("Type").setValue(first.getType());
        datePicker("Closed from").setValue(LocalDate.ofInstant(first.getClosed(), ZoneId.systemDefault()).plusDays(1));
        assertThat(containsRow(assignments(), ReportsViewTest::assignmentId, first.getId())).isFalse();

        test(button("Clear")).click();

        assertThat(comboBox("Municipality").getValue()).isNull();
        assertThat(comboBox("Type").getValue()).isNull();
        assertThat(datePicker("Closed from").getValue()).isNull();
        assertThat(containsRow(assignments(), ReportsViewTest::assignmentId, first.getId())).isTrue();
        assertThat(containsRow(assignments(), ReportsViewTest::assignmentId, second.getId())).isTrue();
    }

    @Test
    void refreshing_keepsTheFiltersAndOffersNewChoices() {
        var first = handledAssignment(unique("CS"));
        navigate(ReportsView.class);
        ComboBox<Municipality> municipality = comboBox("Municipality");
        municipality.setValue(first.getMunicipality());
        var later = handledAssignment(unique("CS"));

        test(refreshButton()).click();

        assertThat(municipality.getValue()).isEqualTo(first.getMunicipality());
        assertThat(municipality.getListDataView().getItems()).contains(later.getMunicipality());
        assertThat(containsRow(assignments(), ReportsViewTest::assignmentId, first.getId())).isTrue();
        assertThat(containsRow(assignments(), ReportsViewTest::assignmentId, later.getId())).isFalse();
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
    @WithMockUser(username = "report-writer", roles = {"REPORT_WRITER", "DISPATCHER"})
    void unitBadge_followsWhatIsInTheFormWhileEditing() {
        var callSign = unique("CS");
        var closed = handledAssignment(callSign);
        navigate(ReportsView.class);
        select(closed);

        assertThat(unitDetails(callSign).isOpened()).isFalse();
        assertThat(unitBadge(callSign).getText()).as("Shown while collapsed").isEqualTo("Not filled in");
        assertThat(unitBadge(callSign).getThemeNames()).doesNotContain("success", "warning");

        unitDetails(callSign).setOpened(true);
        textArea(unitForm(callSign), "What was the situation like when you arrived?").setValue("Smoke from the roof");
        assertThat(unitBadge(callSign).getText()).isEqualTo("Partly filled in");
        assertThat(unitBadge(callSign).getThemeNames()).contains("warning").doesNotContain("success");

        textArea(unitForm(callSign), "What actions did you take?").setValue("Interior attack");
        assertThat(unitBadge(callSign).getText()).as("Shown while expanded").isEqualTo("Filled in");
        assertThat(unitBadge(callSign).getThemeNames()).contains("success").doesNotContain("warning");

        textArea(unitForm(callSign), "What actions did you take?").setValue("   ");
        assertThat(unitBadge(callSign).getText()).as("Blank doesn't count").isEqualTo("Partly filled in");
    }

    @Test
    void unitBadge_showsSavedStateToReaders() throws ValidationFailedException {
        var filledIn = unique("CS");
        var empty = unique("CS");
        var closed = handledAssignment(filledIn, empty);
        var report = reportService.findReportByAssignmentId(closed.getId()).orElseThrow();
        var resource = report.getRespondingResources().stream()
                .filter(r -> r.getCallSign().equals(filledIn)).findFirst().orElseThrow();
        // Saving needs a writer; the view itself is opened as the reader that the class is annotated with
        var reader = SecurityContextHolder.getContext();
        loginWithIdToken("subject-erik", "Erik", "Explorer");
        report.getReport().getUnitReport(resource).setSituationOnArrival("Smoke from the roof");
        report.getReport().getUnitReport(resource).setActionsTaken("Interior attack");
        reportService.saveReport(report.getReport());
        SecurityContextHolder.setContext(reader);

        navigate(ReportsView.class);
        select(closed);

        assertThat(unitBadge(filledIn).getText()).isEqualTo("Filled in");
        assertThat(unitBadge(empty).getText()).isEqualTo("Not filled in");
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
        assertThat(reportStatus().getText()).isEqualTo("Open");
        assertThat(find(Button.class).withText("Close Report").exists()).isFalse();
        assertThat(find(Button.class).withText("Re-open Report").exists()).isFalse();
    }

    @Test
    void reviewer_closesTheReport_andThenNobodyCanChangeIt() {
        var callSign = unique("CS");
        loginAsWriter();
        var closed = handledAssignment(callSign);
        writeReport(closed, "The fire spread to the attic");

        openReportsViewAs(this::loginAsReviewer);
        select(closed);
        assertThat(reportStatus().getText()).isEqualTo("Open");
        assertThat(textArea(reportPanel(), "How did the situation develop?").isReadOnly()).as("Reviewers only read").isTrue();
        assertThat(find(Button.class).withText("Save Report").exists()).isFalse();
        assertThat(find(Button.class).withText("Re-open Report").exists()).isFalse();

        test(button("Close Report")).click();

        assertThat(notificationTexts()).contains("Report closed");
        assertThat(reportStatus().getText()).isEqualTo("Closed");
        assertThat(reportStatus().getThemeNames()).contains("success");
        assertThat(find(Span.class, reportPanel()).withCondition(span -> span.getText().startsWith("Closed by Rita Reviewer on ")).exists()).isTrue();
        assertThat(find(Button.class).withText("Close Report").exists()).isFalse();
        assertThat(find(Button.class).withText("Re-open Report").exists()).isTrue();
        var stored = storedReport(closed);
        assertThat(stored.isClosed()).isTrue();
        assertThat(stored.getClosed().getSubject()).isEqualTo("subject-rita");
        assertThat(stored.getLastEdit().getSubject()).as("Closing isn't editing").isEqualTo("subject-erik");

        openReportsViewAs(this::loginAsWriter);
        select(closed);
        assertThat(reportStatus().getText()).isEqualTo("Closed");
        assertThat(textArea(reportPanel(), "How did the situation develop?").isReadOnly()).isTrue();
        unitDetails(callSign).setOpened(true);
        assertThat(textArea(unitForm(callSign), "What actions did you take?").isReadOnly()).isTrue();
        assertThat(find(Button.class).withText("Save Report").exists()).isFalse();
    }

    @Test
    void writer_cannotSaveAReportThatWasClosedWhileTheyWereWriting() {
        loginAsWriter();
        var closed = handledAssignment(unique("CS"));
        writeReport(closed, "Before closing");
        navigate(ReportsView.class);
        select(closed);
        textArea(reportPanel(), "How did the situation develop?").setValue("After closing");

        asReviewer(() -> reportService.closeReport(storedReport(closed)));
        test(button("Save Report")).click();

        assertThat(notificationTexts()).anyMatch(text -> text.startsWith("A reviewer closed this report"));
        assertThat(storedReport(closed).getSituationDevelopment()).isEqualTo("Before closing");
        assertThat(textArea(reportPanel(), "How did the situation develop?").getValue()).isEqualTo("Before closing");
        assertThat(textArea(reportPanel(), "How did the situation develop?").isReadOnly()).isTrue();
        assertThat(find(Button.class).withText("Save Report").exists()).isFalse();
    }

    @Test
    void reviewer_reopensTheReport_andWritersCanChangeItAgain() {
        loginAsWriter();
        var closed = handledAssignment(unique("CS"));
        writeReport(closed, "First version");
        asReviewer(() -> reportService.closeReport(storedReport(closed)));

        openReportsViewAs(this::loginAsReviewer);
        select(closed);
        test(button("Re-open Report")).click();

        assertThat(notificationTexts()).contains("Report re-opened");
        assertThat(reportStatus().getText()).isEqualTo("Open");
        assertThat(storedReport(closed).isClosed()).isFalse();

        openReportsViewAs(this::loginAsWriter);
        select(closed);
        textArea(reportPanel(), "How did the situation develop?").setValue("Second version");
        test(button("Save Report")).click();
        assertThat(storedReport(closed).getSituationDevelopment()).isEqualTo("Second version");
    }

    @Test
    void reviewer_cannotCloseAReportThatChangedSinceTheyReadIt() {
        loginAsWriter();
        var closed = handledAssignment(unique("CS"));
        writeReport(closed, "What the reviewer read");

        openReportsViewAs(this::loginAsReviewer);
        select(closed);
        asWriter(() -> writeReport(closed, "Written after the reviewer read it"));

        test(button("Close Report")).click();

        assertThat(notificationTexts()).anyMatch(text -> text.startsWith("This report has changed since you opened it"));
        assertThat(storedReport(closed).isClosed()).isFalse();
        assertThat(textArea(reportPanel(), "How did the situation develop?").getValue()).isEqualTo("Written after the reviewer read it");
        assertThat(reportStatus().getText()).isEqualTo("Open");
    }

    @Test
    void reviewer_canCloseAReportThatNobodyHasWrittenIn() {
        loginAsWriter();
        var closed = handledAssignment(unique("CS"));

        openReportsViewAs(this::loginAsReviewer);
        select(closed);
        test(button("Close Report")).click();

        assertThat(storedReport(closed).isClosed()).isTrue();
        assertThat(reportStatus().getText()).isEqualTo("Closed");
    }

    @Test
    void refresh_showsNewlyClosedAssignments() {
        navigate(ReportsView.class);
        var closed = handledAssignment(unique("CS"));

        test(refreshButton()).click();

        assertThat(containsRow(assignments(), ReportsViewTest::assignmentId, closed.getId())).isTrue();
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
    private Grid<ClosedAssignmentDTO> assignments() {
        return find(Grid.class, view().getMaster()).single();
    }

    private static Long assignmentId(ClosedAssignmentDTO item) {
        return item.assignment().getId();
    }

    private DatePicker datePicker(String label) {
        return find(DatePicker.class, view().getMaster()).withCondition(picker -> label.equals(picker.getLabel())).single();
    }

    @SuppressWarnings("unchecked")
    private <T> ComboBox<T> comboBox(String label) {
        return find(ComboBox.class, view().getMaster()).withCondition(comboBox -> label.equals(comboBox.getLabel())).single();
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

    /**
     * Logs in a report writer, who may also dispatch, so that tests can create their assignments.
     */
    private void loginWithIdToken(String subject, String givenName, String familyName) {
        loginWithIdToken(subject, givenName, familyName, Roles.ROLE_REPORT_WRITER, Roles.ROLE_DISPATCHER);
    }

    private void loginWithIdToken(String subject, String givenName, String familyName, String... roles) {
        var idToken = OidcIdToken.withTokenValue("id-token")
                .issuer("http://keycloak.invalid/realms/idispatch")
                .subject(subject)
                .claim("preferred_username", subject)
                .claim("given_name", givenName)
                .claim("family_name", familyName)
                .build();
        var user = new DefaultOidcUser(Stream.of(roles).map(SimpleGrantedAuthority::new).toList(), idToken,
                "preferred_username");
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new OAuth2AuthenticationToken(user, user.getAuthorities(), "keycloak"));
        SecurityContextHolder.setContext(context);
    }

    /**
     * Logs in as another user and opens a new reports view, like logging in does. Navigating from the reports view to
     * itself would keep the view of the previous user, with that user's permissions.
     */
    private void openReportsViewAs(Runnable login) {
        navigate(RootView.class);
        login.run();
        navigate(ReportsView.class);
    }

    private void loginAsWriter() {
        loginWithIdToken("subject-erik", "Erik", "Explorer");
    }

    private void loginAsReviewer() {
        loginWithIdToken("subject-rita", "Rita", "Reviewer", Roles.ROLE_REPORT_REVIEWER);
    }

    private void asWriter(Runnable action) {
        runAs(this::loginAsWriter, action);
    }

    private void asReviewer(Runnable action) {
        runAs(this::loginAsReviewer, action);
    }

    private void runAs(Runnable login, Runnable action) {
        var previous = SecurityContextHolder.getContext();
        login.run();
        try {
            action.run();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }

    /**
     * Saves the situation through the service, as a writer in another session would.
     */
    private void writeReport(Assignment assignment, String situationDevelopment) {
        var report = storedReport(assignment);
        report.setSituationDevelopment(situationDevelopment);
        try {
            assertThat(reportService.saveReport(report)).isInstanceOf(UpdateResult.Success.class);
        } catch (ValidationFailedException ex) {
            throw new AssertionError(ex);
        }
    }

    private AssignmentReport storedReport(Assignment assignment) {
        return reportService.findReportByAssignmentId(assignment.getId()).orElseThrow().getReport();
    }

    private Badge reportStatusCell(Assignment assignment) {
        return (Badge) test(assignments()).getCellComponent(rowOf(assignments(), ReportsViewTest::assignmentId, assignment.getId()), 1);
    }

    private Badge reportStatus() {
        return find(Badge.class, reportPanel()).withCondition(badge -> List.of("Open", "Closed").contains(badge.getText())).single();
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
        var selected = assignments().asSingleSelect().getValue();
        return selected == null ? null : selected.assignment();
    }

    private void select(Assignment assignment) {
        test(assignments()).select(rowOf(assignments(), ReportsViewTest::assignmentId, assignment.getId()));
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

    private Badge unitBadge(String callSign) {
        return find(Badge.class, unitDetails(callSign).getSummary()).single();
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
