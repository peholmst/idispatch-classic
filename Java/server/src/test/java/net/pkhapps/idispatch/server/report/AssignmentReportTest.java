package net.pkhapps.idispatch.server.report;

import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.resource.ResourceId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The rules of a report that the writing and reviewing stories rely on: which parts an edit is recorded on, and what
 * the counts may be.
 */
class AssignmentReportTest {

    private static final ResourceId FIRST = new ResourceId(1);
    private static final ResourceId SECOND = new ResourceId(2);
    private static final UserStamp ERIK = new UserStamp("subject-erik", "Erik", "Explorer", Instant.parse("2026-10-31T10:00:00Z"));
    private static final UserStamp ELLA = new UserStamp("subject-ella", "Ella", null, Instant.parse("2026-10-31T11:00:00Z"));

    @Test
    void editIsRecordedOnlyOnTheChangedParts() {
        var report = AssignmentReport.empty(new AssignmentId(1)).withDraft(new ReportDraft(scene("Fire"),
                Map.of(FIRST, answers("Smoke"), SECOND, UnitAnswers.EMPTY)), ERIK);

        var edited = report.withDraft(new ReportDraft(scene("Fire"),
                Map.of(FIRST, answers("Smoke"), SECOND, answers("Flames"))), ELLA);

        assertThat(report.lastEdit()).isEqualTo(ERIK);
        assertThat(report.unit(SECOND).lastEdit()).as("Shown but left empty").isNull();
        assertThat(edited.lastEdit()).as("The scene was not touched").isEqualTo(ERIK);
        assertThat(edited.unit(FIRST).lastEdit()).isEqualTo(ERIK);
        assertThat(edited.unit(SECOND).lastEdit()).isEqualTo(ELLA);
    }

    @Test
    void unitsNotInTheDraftAreKept() {
        var report = AssignmentReport.empty(new AssignmentId(1))
                .withDraft(new ReportDraft(SceneDetails.EMPTY, Map.of(FIRST, answers("Smoke"))), ERIK);

        var edited = report.withDraft(new ReportDraft(scene("Fire"), Map.of()), ELLA);

        assertThat(edited.unit(FIRST).answers()).isEqualTo(answers("Smoke"));
    }

    @Test
    void closingAndReopeningKeepTheEdits() {
        var report = AssignmentReport.empty(new AssignmentId(1))
                .withDraft(new ReportDraft(scene("Fire"), Map.of()), ERIK);

        var closed = report.closedBy(ELLA);

        assertThat(closed.isClosed()).isTrue();
        assertThat(closed.lastEdit()).isEqualTo(ERIK);
        assertThat(closed.reopened().isClosed()).isFalse();
    }

    @Test
    void countsCannotBeNegative_norCasualtiesMoreThanPeopleInvolved() {
        assertThatThrownBy(() -> new SceneDetails(ReportText.EMPTY, -1, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SceneDetails(ReportText.EMPTY, 2, 1, 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(SceneDetails.casualtiesWithinPeopleInvolved(null, 5, 5, 5)).as("Not counted yet").isTrue();
        assertThat(SceneDetails.casualtiesWithinPeopleInvolved(3, 1, null, 2)).isTrue();
    }

    @Test
    void displayNameFallsBackOnTheSubject() {
        assertThat(ELLA.displayName()).isEqualTo("Ella");
        assertThat(new UserStamp("subject-x", " ", null, Instant.EPOCH).displayName()).isEqualTo("subject-x");
    }

    private static SceneDetails scene(String situationDevelopment) {
        return new SceneDetails(new ReportText(situationDevelopment), null, null, null, null);
    }

    private static UnitAnswers answers(String situationOnArrival) {
        return new UnitAnswers(new ReportText(situationOnArrival), ReportText.EMPTY);
    }
}
