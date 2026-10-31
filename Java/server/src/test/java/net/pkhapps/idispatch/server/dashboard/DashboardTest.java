package net.pkhapps.idispatch.server.dashboard;

import net.pkhapps.idispatch.server.report.HandledAssignments;
import net.pkhapps.idispatch.server.ui.UseCaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static net.pkhapps.idispatch.server.resource.ResourceState.AT_STATION;
import static net.pkhapps.idispatch.server.resource.ResourceState.AVAILABLE;
import static net.pkhapps.idispatch.server.resource.ResourceState.DISPATCHED;
import static net.pkhapps.idispatch.server.resource.ResourceState.ON_SCENE;
import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every signed-in user starts on the dashboard, which shows how busy things are: the active assignments, the reports
 * waiting for a reviewer, and the units by how many assignments they have been on during the past 24 hours.
 */
class DashboardTest extends UseCaseTest {

    @Autowired
    HandledAssignments handled;

    @Test
    void userSeesTheUnitsByHowManyAssignmentsTheyHaveBeenOnDuringThePastDay() {
        var now = OffsetDateTime.now();
        var municipality = given.municipality(unique("Town "));
        var type = given.assignmentType(unique("T"), "Building fire");
        var yesterdaysFire = given.openAssignment(municipality, type, "Old Road 1");
        var morningFire = given.openAssignment(municipality, type, "Morning Road 2");
        var longFire = given.openAssignment(municipality, type, "Long Road 3");
        var marker = unique("");

        // Back at the station before the past day began
        var idle = given.resource("D" + marker, AT_STATION);
        given.pastStatus(idle, DISPATCHED, yesterdaysFire, now.minusHours(30));
        given.pastStatus(idle, AVAILABLE, null, now.minusHours(26));
        // Still on yesterday's fire when the past day began, then on another one
        var busy = given.resource("A" + marker, AVAILABLE);
        given.pastStatus(busy, DISPATCHED, yesterdaysFire, now.minusHours(30));
        given.pastStatus(busy, ON_SCENE, yesterdaysFire, now.minusHours(20));
        given.pastStatus(busy, AVAILABLE, null, now.minusHours(18));
        given.pastStatus(busy, DISPATCHED, morningFire, now.minusHours(10));
        given.pastStatus(busy, ON_SCENE, morningFire, now.minusHours(9));
        // On the same assignment for longer than a day
        var steady = given.resource("C" + marker, AT_STATION);
        given.currentStatus(steady, ON_SCENE, longFire, now.minusHours(40));
        // Never on an assignment
        var unused = given.resource("B" + marker, AT_STATION);

        var user = new DashboardPage(user(unique("user")).newWindow());
        user.openHome();

        assertThat(user.units().stream().filter(row -> row.get("Call sign").endsWith(marker)))
                .map(row -> Map.entry(row.get("Call sign"), row.get("Assignments (24 h)")))
                .containsExactly(
                        Map.entry(busy.callSign(), "2"),
                        Map.entry(steady.callSign(), "1"),
                        Map.entry(unused.callSign(), "0"),
                        Map.entry(idle.callSign(), "0"));
    }

    @Test
    void deletedUnitsAreNotShown() {
        var unit = given.resource(unique("DEL"), AT_STATION);
        given.deleteResource(unit);

        var user = new DashboardPage(user(unique("user")).newWindow());
        user.openHome();

        assertThat(user.units()).extracting(row -> row.get("Call sign")).doesNotContain(unit.callSign());
    }

    @Test
    void userSeesHowManyAssignmentsAreActive() {
        var user = new DashboardPage(user(unique("user")).newWindow());
        user.openHome();
        var before = user.activeAssignments();

        handled.openAssignment();
        user.openHome();

        assertThat(user.activeAssignments()).isEqualTo(before + 1);
    }

    @Test
    void userSeesHowManyReportsWaitForAReviewer() {
        var user = new DashboardPage(user(unique("user")).newWindow());
        user.openHome();
        var before = user.openReports();

        var assignment = handled.closedAssignment(unique("R"));
        user.openHome();
        var afterClosing = user.openReports();
        handled.reviewerHasClosedTheReportOf(assignment);
        user.openHome();

        assertThat(List.of(afterClosing, user.openReports())).containsExactly(before + 1, before);
    }
}
