package net.pkhapps.idispatch.server.report;

import com.vaadin.flow.data.provider.SortDirection;
import net.pkhapps.idispatch.server.ui.UseCaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Map;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static net.pkhapps.idispatch.server.report.ReportUser.ACTIONS_TAKEN;
import static net.pkhapps.idispatch.server.report.ReportUser.SITUATION_ON_ARRIVAL;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Someone who reads the reports finds closed assignments in the list, narrows it down with the filters, and reads what
 * happened. Readers cannot change anything.
 */
class ReportBrowsingTest extends UseCaseTest {

    @Autowired
    HandledAssignments handled;

    private ReportUser readerAtWork() {
        return new ReportUser(user("report-reader", "REPORT_READER"));
    }

    @Test
    void readerStartsWithNoAssignmentSelected() {
        var reader = readerAtWork();

        assertThat(reader.viewTitle()).isEqualTo("Reports");
        assertThat(reader.isToldNoAssignmentIsSelected()).isTrue();
        assertThat(reader.seesAReport()).isFalse();
    }

    @Test
    void onlyClosedAssignmentsAreListed() {
        var closed = handled.closedAssignment(unique("CS"));
        var open = handled.openAssignment();

        var reader = readerAtWork();

        assertThat(reader.seesInTheList(open)).isFalse();
        assertThat(reader.rowOf(closed)).isEqualTo(Map.of(
                "Number", closed.number(),
                "Report", "Open",
                "Closed", closed.closed(),
                "Urgency", "B",
                "Type", closed.type(),
                "Municipality", closed.municipality(),
                "Address", "Report Road 7"));
    }

    @Test
    void newestClosedAssignmentsAreListedFirst() {
        var older = handled.closedAssignment(unique("CS"));
        var newer = handled.closedAssignment(unique("CS"));

        var reader = readerAtWork();

        assertThat(reader.seesAbove(newer, older)).isTrue();
    }

    @Test
    void readerSortsOpenReportsFirstOrLast() {
        var reviewed = handled.closedAssignment(unique("CS"));
        handled.reviewerHasClosedTheReportOf(reviewed);
        var open = handled.closedAssignment(unique("CS"));
        var reader = readerAtWork();

        reader.sortsBy("Report", SortDirection.ASCENDING);
        assertThat(reader.seesAbove(open, reviewed)).isTrue();

        reader.sortsBy("Report", SortDirection.DESCENDING);
        assertThat(reader.seesAbove(reviewed, open)).isTrue();
    }

    @Test
    void readerSortsByNumber() {
        var first = handled.closedAssignment(unique("CS"));
        var second = handled.closedAssignment(unique("CS"));
        var reader = readerAtWork();

        reader.sortsBy("Number", SortDirection.ASCENDING);
        assertThat(reader.seesAbove(first, second)).isTrue();

        reader.sortsBy("Number", SortDirection.DESCENDING);
        assertThat(reader.seesAbove(second, first)).isTrue();
    }

    @Test
    void readerSortsByType() {
        var one = handled.closedAssignment(unique("CS"));
        var other = handled.closedAssignment(unique("CS"));
        var lower = one.type().compareTo(other.type()) < 0 ? one : other;
        var higher = lower == one ? other : one;
        var reader = readerAtWork();

        reader.sortsBy("Type", SortDirection.ASCENDING);
        assertThat(reader.seesAbove(lower, higher)).isTrue();

        reader.sortsBy("Type", SortDirection.DESCENDING);
        assertThat(reader.seesAbove(higher, lower)).isTrue();
    }

    @Test
    void readerSortsByMunicipality() {
        var one = handled.closedAssignment(unique("CS"));
        var other = handled.closedAssignment(unique("CS"));
        var lower = one.municipality().compareTo(other.municipality()) < 0 ? one : other;
        var higher = lower == one ? other : one;
        var reader = readerAtWork();

        reader.sortsBy("Municipality", SortDirection.ASCENDING);
        assertThat(reader.seesAbove(lower, higher)).isTrue();

        reader.sortsBy("Municipality", SortDirection.DESCENDING);
        assertThat(reader.seesAbove(higher, lower)).isTrue();
    }

    @Test
    void readerFiltersByMunicipality() {
        var first = handled.closedAssignment(unique("CS"));
        var second = handled.closedAssignment(unique("CS"));
        var reader = readerAtWork();
        assertThat(reader.choicesOf("Municipality")).contains(first.municipality(), second.municipality());

        reader.filtersBy("Municipality", first.municipality());

        assertThat(reader.seesInTheList(first)).isTrue();
        assertThat(reader.seesInTheList(second)).isFalse();
    }

    @Test
    void readerFiltersByType() {
        var first = handled.closedAssignment(unique("CS"));
        var second = handled.closedAssignment(unique("CS"));
        var reader = readerAtWork();
        assertThat(reader.choicesOf("Type")).contains(first.type(), second.type());

        reader.filtersBy("Type", second.type());

        assertThat(reader.seesInTheList(first)).isFalse();
        assertThat(reader.seesInTheList(second)).isTrue();
    }

    @Test
    void filteringByTheClosingDateIncludesBothEndsOfTheRange() {
        var closed = handled.closedAssignment(unique("CS"));
        var reader = readerAtWork();

        reader.filtersClosedFrom(closed.closedOn());
        reader.filtersClosedTo(closed.closedOn());
        assertThat(reader.seesInTheList(closed)).as("Closed on both ends").isTrue();

        reader.stopsFilteringClosedTo();
        reader.filtersClosedFrom(closed.closedOn().plusDays(1));
        assertThat(reader.seesInTheList(closed)).as("Closed before the range").isFalse();

        reader.stopsFilteringClosedFrom();
        reader.filtersClosedTo(closed.closedOn().minusDays(1));
        assertThat(reader.seesInTheList(closed)).as("Closed after the range").isFalse();
    }

    @Test
    void readerSeparatesOpenAndClosedReports() {
        var open = handled.closedAssignment(unique("CS"));
        var reviewed = handled.closedAssignment(unique("CS"));
        handled.reviewerHasClosedTheReportOf(reviewed);
        var reader = readerAtWork();
        assertThat(reader.rowOf(open)).containsEntry("Report", "Open");
        assertThat(reader.rowOf(reviewed)).containsEntry("Report", "Closed");
        assertThat(reader.seesReportAsDoneInTheList(reviewed)).isTrue();
        assertThat(reader.seesReportAsDoneInTheList(open)).isFalse();

        reader.filtersBy("Report", "Open");
        assertThat(reader.seesInTheList(open)).isTrue();
        assertThat(reader.seesInTheList(reviewed)).isFalse();

        reader.filtersBy("Report", "Closed");
        assertThat(reader.seesInTheList(open)).isFalse();
        assertThat(reader.seesInTheList(reviewed)).isTrue();
    }

    @Test
    void filtersThatMatchNothingExplainTheEmptyList() {
        var reader = readerAtWork();

        reader.filtersClosedFrom(LocalDate.now().plusDays(1));

        assertThat(reader.explanationOfTheEmptyList())
                .isEqualTo("No reports match the filters, or there are no reports yet.");
    }

    @Test
    void clearingTheFiltersShowsEverythingAgain() {
        var first = handled.closedAssignment(unique("CS"));
        var second = handled.closedAssignment(unique("CS"));
        var reader = readerAtWork();
        reader.filtersBy("Municipality", first.municipality());
        reader.filtersBy("Type", first.type());
        reader.filtersBy("Report", "Open");
        reader.filtersClosedFrom(first.closedOn().plusDays(1));
        assertThat(reader.seesInTheList(first)).isFalse();

        reader.clearsTheFilters();

        assertThat(reader.filter("Municipality")).isEmpty();
        assertThat(reader.filter("Type")).isEmpty();
        assertThat(reader.filter("Report")).isEmpty();
        assertThat(reader.closedFrom()).isNull();
        assertThat(reader.seesInTheList(first)).isTrue();
        assertThat(reader.seesInTheList(second)).isTrue();
    }

    @Test
    void refreshingKeepsTheFiltersAndOffersNewChoices() {
        var first = handled.closedAssignment(unique("CS"));
        var reader = readerAtWork();
        reader.filtersBy("Municipality", first.municipality());
        var later = handled.closedAssignment(unique("CS"));

        reader.refreshes();

        assertThat(reader.filter("Municipality")).isEqualTo(first.municipality());
        assertThat(reader.choicesOf("Municipality")).contains(later.municipality());
        assertThat(reader.seesInTheList(first)).isTrue();
        assertThat(reader.seesInTheList(later)).isFalse();
    }

    @Test
    void refreshingShowsAssignmentsClosedSinceTheViewWasOpened() {
        var reader = readerAtWork();
        var closed = handled.closedAssignment(unique("CS"));

        reader.refreshes();

        assertThat(reader.seesInTheList(closed)).isTrue();
    }

    @Test
    void readerOpensTheReportAndSeesWhenEachResourceGotWhere() {
        var callSign = unique("CS");
        var closed = handled.closedAssignment(callSign);
        var reader = readerAtWork();

        reader.opensTheReportOf(closed);

        assertThat(reader.seesAReport()).isTrue();
        assertThat(reader.reportField("Number")).isEqualTo(closed.number());
        assertThat(reader.reportField("Opened")).isEqualTo(closed.opened());
        assertThat(reader.reportField("Closed")).isEqualTo(closed.closed());
        assertThat(reader.reportField("Urgency")).isEqualTo("B");
        assertThat(reader.reportField("Type")).isEqualTo(closed.type());
        assertThat(reader.reportField("Municipality")).isEqualTo(closed.municipality());
        assertThat(reader.reportField("Address")).isEqualTo("Report Road 7");
        assertThat(reader.description()).isEqualTo("Smoke seen");
        assertThat(reader.canChangeReportField("Number")).isFalse();

        assertThat(reader.timeline()).singleElement().satisfies(row -> {
            assertThat(row).containsEntry("Resource", callSign);
            assertThat(row.get("Dispatched")).matches("\\d{2}:\\d{2}:\\d{2}");
            assertThat(row.get("En route")).matches("\\d{2}:\\d{2}:\\d{2}");
            assertThat(row.get("On scene")).matches("\\d{2}:\\d{2}:\\d{2}");
            assertThat(row.get("Available")).matches("\\d{2}:\\d{2}:\\d{2}");
            assertThat(row.get("At station")).as("Never got back to the station").isEmpty();
        });
    }

    @Test
    void readerCannotChangeTheReport() {
        var callSign = unique("CS");
        var closed = handled.closedAssignment(callSign);
        var reader = readerAtWork();

        reader.opensTheReportOf(closed);

        assertThat(reader.reportStatus()).isEqualTo("Open");
        assertThat(reader.canWriteTheReport(callSign)).isFalse();
        assertThat(reader.canCloseTheReport()).isFalse();
        assertThat(reader.canReopenTheReport()).isFalse();
    }

    @Test
    void readerSeesWhichUnitsHaveFilledInTheirPart() {
        var filledIn = unique("CS");
        var empty = unique("CS");
        var closed = handled.closedAssignment(filledIn, empty);
        var erik = new ReportUser(keycloakUser("erik", "Erik", "Explorer", "REPORT_WRITER"));
        erik.opensTheReportOf(closed);
        erik.answersForUnit(filledIn, SITUATION_ON_ARRIVAL, "Smoke from the roof");
        erik.answersForUnit(filledIn, ACTIONS_TAKEN, "Interior attack");
        erik.savesTheReport();
        var reader = readerAtWork();

        reader.opensTheReportOf(closed);

        assertThat(reader.unitBadge(filledIn)).isEqualTo("Filled in");
        assertThat(reader.unitBadge(empty)).isEqualTo("Not filled in");
    }

    @Test
    void readerClosesTheReportShownOnTopOfTheListWithEscape() {
        var closed = handled.closedAssignment(unique("CS"));
        var reader = readerAtWork();
        reader.opensTheReportOf(closed);

        reader.closesTheReportWithEscape();

        assertThat(reader.hasSelectedNothing()).isTrue();
        assertThat(reader.seesAReport()).isFalse();
    }
}
