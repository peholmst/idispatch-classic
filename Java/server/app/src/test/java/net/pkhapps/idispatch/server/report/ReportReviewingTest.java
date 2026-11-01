package net.pkhapps.idispatch.server.report;

import net.pkhapps.idispatch.server.ui.UseCaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A reviewer reads the reports that writers have filled in and closes them, after which nobody can change them. If
 * something must be fixed, the reviewer re-opens the report. Writers and reviewers work at the same time, so a review
 * is only of the version the reviewer actually read.
 */
class ReportReviewingTest extends UseCaseTest {

    @Autowired
    HandledAssignments handled;

    private ReportUser erik() {
        return new ReportUser(keycloakUser("erik", "Erik", "Explorer", "REPORT_WRITER"));
    }

    private ReportUser rita() {
        return new ReportUser(keycloakUser("rita", "Rita", "Reviewer", "REPORT_REVIEWER"));
    }

    @Test
    void reviewerWhoAlsoWritesSavesBeforeClosing() {
        var closed = handled.closedAssignment(unique("CS"));
        var wilma = new ReportUser(keycloakUser("wilma", "Wilma", "Writer", "REPORT_WRITER", "REPORT_REVIEWER"));
        wilma.opensTheReportOf(closed);
        wilma.writesHowTheSituationDeveloped("Written while reviewing");

        wilma.closesTheReport();

        assertThat(wilma.wasToldThat("Save or throw away your changes before closing the report.")).isTrue();
        assertThat(wilma.reportStatus()).isEqualTo("Open");
        assertThat(handled.reportOf(closed).closed()).isFalse();

        wilma.savesTheReport();
        wilma.closesTheReport();

        assertThat(wilma.reportStatus()).isEqualTo("Closed");
        assertThat(handled.reportOf(closed).situationDevelopment()).isEqualTo("Written while reviewing");
    }

    @Test
    void reviewerClosesTheReportAndThenNobodyCanChangeIt() {
        var callSign = unique("CS");
        var closed = handled.closedAssignment(callSign);
        var erik = erik();
        erik.opensTheReportOf(closed);
        erik.writesHowTheSituationDeveloped("The fire spread to the attic");
        erik.savesTheReport();
        var rita = rita();

        rita.opensTheReportOf(closed);
        assertThat(rita.reportStatus()).isEqualTo("Open");
        assertThat(rita.canWriteTheReport(callSign)).as("Reviewers only read").isFalse();
        assertThat(rita.canReopenTheReport()).isFalse();
        rita.closesTheReport();

        assertThat(rita.wasToldThat("Report closed")).isTrue();
        assertThat(rita.reportStatus()).isEqualTo("Closed");
        assertThat(rita.seesReportAsDone()).isTrue();
        assertThat(rita.seesTextStartingWith("Closed by Rita Reviewer on ")).isTrue();
        assertThat(rita.canCloseTheReport()).isFalse();
        assertThat(rita.canReopenTheReport()).isTrue();
        var stored = handled.reportOf(closed);
        assertThat(stored.closed()).isTrue();
        assertThat(stored.closedBy()).isEqualTo("subject-rita");
        assertThat(stored.lastEditedBy()).as("Closing isn't editing").isEqualTo("subject-erik");

        erik.refreshes();
        erik.opensTheReportOf(closed);

        assertThat(erik.reportStatus()).isEqualTo("Closed");
        assertThat(erik.canWriteTheReport(callSign)).isFalse();
    }

    @Test
    void writerCannotSaveAReportThatWasClosedWhileTheyWereWriting() {
        var closed = handled.closedAssignment(unique("CS"));
        handled.writerHasWrittenInTheReportOf(closed, "Before closing");
        var erik = erik();
        var rita = rita();
        erik.opensTheReportOf(closed);
        erik.writesHowTheSituationDeveloped("After closing");

        rita.opensTheReportOf(closed);
        rita.closesTheReport();
        erik.savesTheReport();

        assertThat(erik.wasToldThat("A reviewer closed this report")).isTrue();
        assertThat(erik.situationDevelopment()).isEqualTo("Before closing");
        assertThat(erik.canSave()).isFalse();
        assertThat(handled.reportOf(closed).situationDevelopment()).isEqualTo("Before closing");
    }

    @Test
    void reviewerReopensTheReportAndWritersCanChangeItAgain() {
        var closed = handled.closedAssignment(unique("CS"));
        handled.writerHasWrittenInTheReportOf(closed, "First version");
        handled.reviewerHasClosedTheReportOf(closed);
        var rita = rita();

        rita.opensTheReportOf(closed);
        rita.reopensTheReport();

        assertThat(rita.wasToldThat("Report re-opened")).isTrue();
        assertThat(rita.reportStatus()).isEqualTo("Open");
        assertThat(handled.reportOf(closed).closed()).isFalse();

        var erik = erik();
        erik.opensTheReportOf(closed);
        erik.writesHowTheSituationDeveloped("Second version");
        erik.savesTheReport();

        assertThat(handled.reportOf(closed).situationDevelopment()).isEqualTo("Second version");
    }

    @Test
    void reviewerCannotCloseAReportThatChangedSinceTheyReadIt() {
        var closed = handled.closedAssignment(unique("CS"));
        handled.writerHasWrittenInTheReportOf(closed, "What the reviewer read");
        var rita = rita();
        var erik = erik();
        rita.opensTheReportOf(closed);
        erik.opensTheReportOf(closed);
        erik.writesHowTheSituationDeveloped("Written after the reviewer read it");
        erik.savesTheReport();

        rita.closesTheReport();

        assertThat(rita.wasToldThat("This report has changed since you opened it")).isTrue();
        assertThat(rita.situationDevelopment()).as("The latest version is shown")
                .isEqualTo("Written after the reviewer read it");
        assertThat(rita.reportStatus()).isEqualTo("Open");
        assertThat(handled.reportOf(closed).closed()).isFalse();
    }

    @Test
    void reviewerCanCloseAReportThatNobodyHasWrittenIn() {
        var closed = handled.closedAssignment(unique("CS"));
        var rita = rita();

        rita.opensTheReportOf(closed);
        rita.closesTheReport();

        assertThat(rita.reportStatus()).isEqualTo("Closed");
        assertThat(handled.reportOf(closed).closed()).isTrue();
    }

    @Test
    void closingAndReopeningTheReportUpdatesTheListRightAway() {
        var closed = handled.closedAssignment(unique("CS"));
        var rita = rita();
        rita.filtersBy("Report", "Open");
        rita.opensTheReportOf(closed);

        rita.closesTheReport();

        assertThat(rita.seesInTheList(closed)).as("No longer open").isFalse();
        assertThat(rita.seesAReport()).as("The report stays open while the reviewer reads it").isTrue();
        rita.stopsFilteringBy("Report");
        assertThat(rita.rowOf(closed)).containsEntry("Report", "Closed");

        rita.reopensTheReport();

        assertThat(rita.rowOf(closed)).containsEntry("Report", "Open");
    }
}
