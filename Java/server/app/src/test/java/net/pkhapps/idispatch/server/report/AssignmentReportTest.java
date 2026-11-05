package net.pkhapps.idispatch.server.report;

import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.resource.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The rules of a report that the writing and reviewing stories rely on: which parts an edit changes, which is what the
 * audit log records, and what the counts may be.
 */
class AssignmentReportTest {

    private static final ResourceId FIRST = new ResourceId(1);
    private static final ResourceId SECOND = new ResourceId(2);

    @Test
    void onlyThePartsWhoseContentDiffersAreChanged() {
        var report = AssignmentReport.empty(new AssignmentId(1)).withDraft(new ReportDraft(scene("Fire"),
                Map.of(FIRST, answers("Smoke"), SECOND, UnitAnswers.EMPTY)));

        var edited = report.withDraft(new ReportDraft(scene("Fire"),
                Map.of(FIRST, answers("Smoke"), SECOND, answers("Flames"))));

        assertThat(report.partsChangedFrom(AssignmentReport.empty(new AssignmentId(1))))
                .as("The second unit was shown but left empty")
                .containsExactly(ReportPart.SCENE, ReportPart.unit(FIRST));
        assertThat(edited.partsChangedFrom(report)).containsExactly(ReportPart.unit(SECOND));
        assertThat(edited.partsChangedFrom(edited)).isEmpty();
    }

    @Test
    void unitsNotInTheDraftAreKept() {
        var report = AssignmentReport.empty(new AssignmentId(1))
                .withDraft(new ReportDraft(SceneDetails.EMPTY, Map.of(FIRST, answers("Smoke"))));

        var edited = report.withDraft(new ReportDraft(scene("Fire"), Map.of()));

        assertThat(edited.unit(FIRST)).isEqualTo(answers("Smoke"));
        assertThat(edited.partsChangedFrom(report)).containsExactly(ReportPart.SCENE);
    }

    @Test
    void closingAndReopeningKeepTheContent() {
        var report = AssignmentReport.empty(new AssignmentId(1))
                .withDraft(new ReportDraft(scene("Fire"), Map.of()));

        var closed = report.closedByReviewer();

        assertThat(closed.isClosed()).isTrue();
        assertThat(closed.partsChangedFrom(report)).isEmpty();
        assertThat(closed.reopened().isClosed()).isFalse();
    }

    @Test
    void unitPartsNameTheirUnit() {
        assertThat(ReportPart.unitOf(ReportPart.unit(SECOND))).isEqualTo(SECOND);
        assertThat(ReportPart.unitOf(ReportPart.SCENE)).isNull();
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

    private static SceneDetails scene(String situationDevelopment) {
        return new SceneDetails(new ReportText(situationDevelopment), null, null, null, null);
    }

    private static UnitAnswers answers(String situationOnArrival) {
        return new UnitAnswers(new ReportText(situationOnArrival), ReportText.EMPTY);
    }
}
