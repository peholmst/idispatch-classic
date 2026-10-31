package net.pkhapps.idispatch.server.ui.reports;

import net.pkhapps.idispatch.server.ui.UseCaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static net.pkhapps.idispatch.server.ui.reports.ReportUser.ACTIONS_TAKEN;
import static net.pkhapps.idispatch.server.ui.reports.ReportUser.DECEASED;
import static net.pkhapps.idispatch.server.ui.reports.ReportUser.LIGHTLY_INJURED;
import static net.pkhapps.idispatch.server.ui.reports.ReportUser.PEOPLE_INVOLVED;
import static net.pkhapps.idispatch.server.ui.reports.ReportUser.SERIOUSLY_INJURED;
import static net.pkhapps.idispatch.server.ui.reports.ReportUser.SITUATION_DEVELOPMENT;
import static net.pkhapps.idispatch.server.ui.reports.ReportUser.SITUATION_ON_ARRIVAL;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * After an assignment is closed, the units that responded write down what happened on scene: how the situation
 * developed and who was hurt, and what each unit found and did. Writers work on the same report at the same time, so
 * nobody's changes may silently overwrite somebody else's, and nobody loses unsaved changes by accident.
 */
class ReportWritingTest extends UseCaseTest {

    @Autowired
    HandledAssignments handled;

    private ReportUser erik() {
        return new ReportUser(keycloakUser("erik", "Erik", "Explorer", "REPORT_WRITER"));
    }

    private ReportUser ella() {
        return new ReportUser(keycloakUser("ella", "Ella", "Explorer", "REPORT_WRITER"));
    }

    @Test
    void writerFillsInWhatHappenedOnScene() {
        var callSign = unique("CS");
        var closed = handled.closedAssignment(callSign);
        var erik = erik();
        erik.opensTheReportOf(closed);
        assertThat(erik.seesTextStartingWith("No units responded")).isFalse();

        erik.writesHowTheSituationDeveloped("The fire spread to the attic");
        erik.counts(PEOPLE_INVOLVED, 4);
        erik.counts(LIGHTLY_INJURED, 2);
        erik.counts(SERIOUSLY_INJURED, 1);
        erik.counts(DECEASED, 0);
        erik.answersForUnit(callSign, SITUATION_ON_ARRIVAL, "Smoke from the roof");
        erik.answersForUnit(callSign, ACTIONS_TAKEN, "Interior attack");
        erik.savesTheReport();

        assertThat(erik.wasToldThat("Report saved")).isTrue();

        erik.refreshes();
        erik.opensTheReportOf(closed);

        assertThat(erik.situationDevelopment()).isEqualTo("The fire spread to the attic");
        assertThat(erik.count(PEOPLE_INVOLVED)).isEqualTo(4);
        assertThat(erik.count(LIGHTLY_INJURED)).isEqualTo(2);
        assertThat(erik.count(SERIOUSLY_INJURED)).isEqualTo(1);
        assertThat(erik.count(DECEASED)).isEqualTo(0);
        assertThat(erik.answerOfUnit(callSign, SITUATION_ON_ARRIVAL)).isEqualTo("Smoke from the roof");
        assertThat(erik.answerOfUnit(callSign, ACTIONS_TAKEN)).isEqualTo("Interior attack");
    }

    @Test
    void writerSavesTheReportAgainAfterAddingToIt() {
        var callSign = unique("CS");
        var closed = handled.closedAssignment(callSign);
        var erik = erik();
        erik.opensTheReportOf(closed);
        erik.answersForUnit(callSign, ACTIONS_TAKEN, "Interior attack");
        erik.savesTheReport();

        erik.answersForUnit(callSign, ACTIONS_TAKEN, "Interior attack, then ventilation");
        erik.savesTheReport();

        assertThat(erik.wasToldThat("Someone else saved this report")).isFalse();
        var stored = handled.reportOf(closed);
        assertThat(stored.getUnitReports().values()).singleElement()
                .satisfies(unit -> assertThat(unit.getActionsTaken()).isEqualTo("Interior attack, then ventilation"));
    }

    @Test
    void moreCasualtiesThanPeopleInvolvedCannotBeSaved() {
        var closed = handled.closedAssignment(unique("CS"));
        var erik = erik();
        erik.opensTheReportOf(closed);

        erik.counts(PEOPLE_INVOLVED, 2);
        erik.counts(LIGHTLY_INJURED, 2);
        erik.counts(DECEASED, 1);
        erik.savesTheReport();

        assertThat(erik.wasToldThat(
                "The injured and deceased cannot add up to more than the number of people involved")).isTrue();
        assertThat(handled.reportOf(closed).isNew()).as("Nothing was saved").isTrue();
    }

    @Test
    void writerIsToldWhenSomeoneElseSavedTheReportFirst() {
        var closed = handled.closedAssignment(unique("CS"));
        var erik = erik();
        var ella = ella();
        erik.opensTheReportOf(closed);
        ella.opensTheReportOf(closed);

        ella.writesHowTheSituationDeveloped("Written by Ella");
        ella.savesTheReport();
        erik.writesHowTheSituationDeveloped("Written by Erik");
        erik.savesTheReport();

        assertThat(erik.wasToldThat("Someone else saved this report")).isTrue();
        assertThat(erik.situationDevelopment()).as("Ella's version is shown").isEqualTo("Written by Ella");
        assertThat(handled.reportOf(closed).getSituationDevelopment()).isEqualTo("Written by Ella");
    }

    @Test
    void writerIsAskedBeforeSwitchingAwayFromUnsavedChanges() {
        var first = handled.closedAssignment(unique("CS"));
        var second = handled.closedAssignment(unique("CS"));
        var erik = erik();
        erik.opensTheReportOf(first);
        erik.writesHowTheSituationDeveloped("Not saved yet");

        erik.opensTheReportOf(second);
        assertThat(erik.isAsked("Unsaved changes")).isTrue();
        erik.keepsEditing();

        assertThat(erik.hasSelected(first)).isTrue();
        assertThat(erik.reportField("Number")).isEqualTo(first.number());
        assertThat(erik.situationDevelopment()).isEqualTo("Not saved yet");

        erik.opensTheReportOf(second);
        erik.throwsAwayTheChanges();

        assertThat(erik.hasSelected(second)).isTrue();
        assertThat(erik.reportField("Number")).isEqualTo(second.number());
        erik.opensTheReportOf(first);
        assertThat(erik.isAskedAnything()).isFalse();
        assertThat(erik.situationDevelopment()).as("The changes are gone").isEmpty();
    }

    @Test
    void writerIsNotAskedAfterSaving() {
        var first = handled.closedAssignment(unique("CS"));
        var second = handled.closedAssignment(unique("CS"));
        var erik = erik();
        erik.opensTheReportOf(first);
        erik.counts(PEOPLE_INVOLVED, 3);
        erik.savesTheReport();

        erik.opensTheReportOf(second);

        assertThat(erik.isAskedAnything()).isFalse();
        assertThat(erik.hasSelected(second)).isTrue();
    }

    @Test
    void writerIsAskedBeforeRefreshingAwayUnsavedChanges() {
        var closed = handled.closedAssignment(unique("CS"));
        var erik = erik();
        erik.opensTheReportOf(closed);
        erik.counts(DECEASED, 0);

        erik.refreshes();
        assertThat(erik.isAsked("Unsaved changes")).isTrue();
        erik.throwsAwayTheChanges();

        assertThat(erik.hasSelectedNothing()).isTrue();
        assertThat(erik.seesAReport()).isFalse();
    }

    @Test
    void writerIsAskedBeforeClosingTheReportShownOnTopOfTheListWithUnsavedChanges() {
        var closed = handled.closedAssignment(unique("CS"));
        var erik = erik();
        erik.opensTheReportOf(closed);
        erik.counts(PEOPLE_INVOLVED, 5);

        erik.clicksBesideTheReport();
        assertThat(erik.isAsked("Unsaved changes")).isTrue();
        assertThat(erik.seesAReport()).as("The report stays until the writer answers").isTrue();
        erik.throwsAwayTheChanges();

        assertThat(erik.hasSelectedNothing()).isTrue();
        assertThat(erik.seesAReport()).isFalse();
    }

    @Test
    void writerIsAskedBeforeLeavingTheViewWithUnsavedChanges() {
        var closed = handled.closedAssignment(unique("CS"));
        var erik = erik();
        erik.opensTheReportOf(closed);
        erik.writesHowTheSituationDeveloped("Not saved yet");

        erik.goesHome();
        assertThat(erik.isAsked("Unsaved changes")).isTrue();
        assertThat(erik.viewTitle()).as("Still on the reports").isEqualTo("Reports");
        erik.throwsAwayTheChanges();

        assertThat(erik.viewTitle()).isEqualTo("Home");
    }

    @Test
    void theReportTellsWhoLastEditedTheSceneAndEachUnit() {
        var callSign = unique("CS");
        var closed = handled.closedAssignment(callSign);
        var erik = erik();
        erik.opensTheReportOf(closed);
        assertThat(erik.whoLastEditedTheScene()).isEqualTo("Not filled in yet");

        erik.writesHowTheSituationDeveloped("The fire spread to the attic");
        erik.savesTheReport();

        assertThat(erik.whoLastEditedTheScene()).startsWith("Last edited by Erik Explorer on ");
        assertThat(erik.whoLastEditedTheUnit(callSign)).as("The unit was not touched").isEqualTo("Not filled in yet");

        var ella = ella();
        ella.opensTheReportOf(closed);
        ella.answersForUnit(callSign, ACTIONS_TAKEN, "Interior attack");
        ella.savesTheReport();

        assertThat(ella.whoLastEditedTheScene()).as("The scene was not touched")
                .startsWith("Last edited by Erik Explorer on ");
        assertThat(ella.whoLastEditedTheUnit(callSign)).startsWith("Last edited by Ella Explorer on ");
        var stored = handled.reportOf(closed);
        assertThat(stored.getLastEdit().getSubject()).isEqualTo("subject-erik");
        assertThat(stored.getUnitReports().values()).singleElement()
                .satisfies(unit -> assertThat(unit.getLastEdit().getSubject()).isEqualTo("subject-ella"));
    }

    @Test
    void theOnlyUnitStartsClosed() {
        var callSign = unique("CS");
        var closed = handled.closedAssignment(callSign);
        var erik = erik();

        erik.opensTheReportOf(closed);

        assertThat(erik.isUnitOpen(callSign)).isFalse();
    }

    @Test
    void unitsStartClosedAndTheOpenedUnitStaysOpenAfterSaving() {
        var mine = unique("CS");
        var other = unique("CS");
        var closed = handled.closedAssignment(mine, other);
        var another = handled.closedAssignment(unique("CS"), unique("CS"));
        var erik = erik();
        erik.opensTheReportOf(closed);
        assertThat(erik.isUnitOpen(mine)).isFalse();
        assertThat(erik.isUnitOpen(other)).isFalse();

        erik.answersForUnit(mine, ACTIONS_TAKEN, "Interior attack");
        erik.savesTheReport();

        assertThat(erik.wasToldThat("Report saved")).isTrue();
        assertThat(erik.isUnitOpen(mine)).isTrue();
        assertThat(erik.isUnitOpen(other)).isFalse();

        erik.opensTheReportOf(another);
        erik.opensTheReportOf(closed);

        assertThat(erik.isUnitOpen(mine)).as("Another report in between starts over").isFalse();
    }

    @Test
    void unitBadgeFollowsWhatTheWriterHasFilledIn() {
        var callSign = unique("CS");
        var closed = handled.closedAssignment(callSign);
        var erik = erik();
        erik.opensTheReportOf(closed);

        assertThat(erik.unitBadge(callSign)).as("Shown while the unit is closed").isEqualTo("Not filled in");
        assertThat(erik.unitBadgeColor(callSign)).isEmpty();

        erik.answersForUnit(callSign, SITUATION_ON_ARRIVAL, "Smoke from the roof");
        assertThat(erik.unitBadge(callSign)).isEqualTo("Partly filled in");
        assertThat(erik.unitBadgeColor(callSign)).isEqualTo("warning");

        erik.answersForUnit(callSign, ACTIONS_TAKEN, "Interior attack");
        assertThat(erik.unitBadge(callSign)).isEqualTo("Filled in");
        assertThat(erik.unitBadgeColor(callSign)).isEqualTo("success");

        erik.answersForUnit(callSign, ACTIONS_TAKEN, "   ");
        assertThat(erik.unitBadge(callSign)).as("Blank doesn't count").isEqualTo("Partly filled in");
    }
}
