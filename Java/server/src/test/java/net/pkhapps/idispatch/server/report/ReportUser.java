package net.pkhapps.idispatch.server.report;

import com.vaadin.browserless.BrowserlessUserContext;
import com.vaadin.flow.data.provider.SortDirection;
import net.pkhapps.idispatch.server.report.HandledAssignments.Handled;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Actor: someone who works with the reports of closed assignments, in their own browser window. A reader reads them,
 * a writer fills in what happened on scene, and a reviewer closes and re-opens them; what this actor may do depends on
 * the roles of its user. Leaves where things are on the screen to {@link ReportsPage}.
 */
public class ReportUser {

    static final String SITUATION_DEVELOPMENT = "How did the situation develop?";
    static final String PEOPLE_INVOLVED = "How many people were involved?";
    static final String LIGHTLY_INJURED = "How many lightly injured?";
    static final String SERIOUSLY_INJURED = "How many seriously injured?";
    static final String DECEASED = "How many deceased?";
    static final String SITUATION_ON_ARRIVAL = "What was the situation like when you arrived?";
    static final String ACTIONS_TAKEN = "What actions did you take?";

    private final ReportsPage page;

    /**
     * Logs in as the given user and opens the reports view.
     */
    public ReportUser(BrowserlessUserContext user) {
        page = new ReportsPage(user.newWindow());
        page.open();
    }

    // --- Finding the report ---

    public void filtersBy(String filter, String item) {
        page.filter(filter, item);
    }

    public void stopsFilteringBy(String filter) {
        page.clearFilter(filter);
    }

    public void filtersClosedFrom(LocalDate date) {
        page.filterClosedFrom(date);
    }

    public void filtersClosedTo(LocalDate date) {
        page.filterClosedTo(date);
    }

    public void stopsFilteringClosedFrom() {
        page.clearClosedFrom();
    }

    public void stopsFilteringClosedTo() {
        page.clearClosedTo();
    }

    public void clearsTheFilters() {
        page.clickClearFilters();
    }

    public void refreshes() {
        page.clickRefresh();
    }

    public void sortsBy(String column, SortDirection direction) {
        page.sortBy(column, direction);
    }

    /**
     * Selects the assignment in the list, which shows its report.
     */
    public void opensTheReportOf(Handled assignment) {
        page.select(assignment.number());
    }

    public void closesTheReportWithEscape() {
        page.pressEscapeOnReport();
    }

    public void clicksBesideTheReport() {
        page.clickBesideReport();
    }

    public void goesHome() {
        page.goTo("Home");
    }

    // --- Writing the report ---

    public void writesHowTheSituationDeveloped(String text) {
        page.enterSituationDevelopment(text);
    }

    public void counts(String question, Integer count) {
        page.enterCount(question, count);
    }

    public void opensTheUnit(String callSign) {
        page.openUnit(callSign);
    }

    /**
     * Answers one of a unit's questions, opening the unit first if it is closed.
     */
    public void answersForUnit(String callSign, String question, String answer) {
        if (!page.isUnitOpen(callSign)) {
            page.openUnit(callSign);
        }
        page.enterForUnit(callSign, question, answer);
    }

    public void savesTheReport() {
        page.click("Save Report");
    }

    public void closesTheReport() {
        page.click("Close Report");
    }

    public void reopensTheReport() {
        page.click("Re-open Report");
    }

    public void throwsAwayTheChanges() {
        page.answerConfirm();
    }

    public void keepsEditing() {
        page.answerCancel();
    }

    // --- What the user sees ---

    public boolean seesInTheList(Handled assignment) {
        return page.listedNumbers().contains(assignment.number());
    }

    public boolean seesInTheList(String number) {
        return page.listedNumbers().contains(number);
    }

    /**
     * Whether the first assignment is listed above the second.
     */
    public boolean seesAbove(Handled first, Handled second) {
        var numbers = page.listedNumbers();
        return numbers.indexOf(first.number()) < numbers.indexOf(second.number());
    }

    public Map<String, String> rowOf(Handled assignment) {
        return page.rowOf(assignment.number());
    }

    public boolean seesReportAsDoneInTheList(Handled assignment) {
        return page.listShowsReportAsDone(assignment.number());
    }

    public String explanationOfTheEmptyList() {
        return page.emptyListText();
    }

    public List<String> choicesOf(String filter) {
        return page.filterChoices(filter);
    }

    public String filter(String filter) {
        return page.filterValue(filter);
    }

    public LocalDate closedFrom() {
        return page.closedFrom();
    }

    public boolean hasSelected(Handled assignment) {
        return page.selectedNumbers().equals(List.of(assignment.number()));
    }

    public boolean hasSelectedNothing() {
        return page.selectedNumbers().isEmpty();
    }

    public boolean seesAReport() {
        return page.showsReport();
    }

    public boolean isToldNoAssignmentIsSelected() {
        return page.showsNoAssignmentSelected();
    }

    public String reportField(String label) {
        return page.field(label);
    }

    public boolean canChangeReportField(String label) {
        return page.canChangeField(label);
    }

    public String description() {
        return page.description();
    }

    public List<Map<String, String>> timeline() {
        return page.timeline();
    }

    public String reportStatus() {
        return page.status();
    }

    public boolean seesReportAsDone() {
        return page.showsStatusAsDone();
    }

    public boolean seesTextStartingWith(String start) {
        return page.showsTextStartingWith(start);
    }

    public String whoLastEditedTheScene() {
        return page.sceneLastEdit();
    }

    public String whoLastEditedTheUnit(String callSign) {
        return page.unitLastEdit(callSign);
    }

    public String situationDevelopment() {
        return page.situationDevelopment();
    }

    public Integer count(String question) {
        return page.count(question);
    }

    public String answerOfUnit(String callSign, String question) {
        if (!page.isUnitOpen(callSign)) {
            page.openUnit(callSign);
        }
        return page.answerOfUnit(callSign, question);
    }

    public boolean isUnitOpen(String callSign) {
        return page.isUnitOpen(callSign);
    }

    public String unitBadge(String callSign) {
        return page.unitBadge(callSign);
    }

    public String unitBadgeColor(String callSign) {
        return page.unitBadgeColor(callSign);
    }

    /**
     * Whether the user can change anything in what happened on scene, including the answers of the given unit.
     */
    public boolean canWriteTheReport(String callSign) {
        var canWrite = page.canChangeSituationDevelopment() || page.canChangeCount(PEOPLE_INVOLVED)
                || page.showsButton("Save Report");
        if (!page.isUnitOpen(callSign)) {
            page.openUnit(callSign);
        }
        return canWrite || page.canAnswerForUnit(callSign, ACTIONS_TAKEN);
    }

    public boolean canSave() {
        return page.showsButton("Save Report");
    }

    public boolean canCloseTheReport() {
        return page.showsButton("Close Report");
    }

    public boolean canReopenTheReport() {
        return page.showsButton("Re-open Report");
    }

    public boolean wasToldThat(String message) {
        return page.wasToldThat(message);
    }

    public boolean isAsked(String question) {
        return page.isAsked(question);
    }

    public boolean isAskedAnything() {
        return page.isAskedAnything();
    }

    public String viewTitle() {
        return page.viewTitle().orElse("");
    }
}
