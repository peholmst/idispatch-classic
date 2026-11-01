package net.pkhapps.idispatch.server.dashboard;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Gives every signed-in user an overview of the work: the active assignments, the reports waiting for a reviewer, and
 * how many assignments each unit has been on lately.
 */
@Service
@PreAuthorize("isAuthenticated()")
@Transactional(readOnly = true)
public class DashboardService {

    /**
     * How far back the dashboard counts each unit's assignments.
     */
    public static final Duration UTILIZATION_WINDOW = Duration.ofHours(24);

    private final DashboardQuery dashboardQuery;

    DashboardService(DashboardQuery dashboardQuery) {
        this.dashboardQuery = dashboardQuery;
    }

    public DashboardFigures getFigures() {
        return new DashboardFigures(dashboardQuery.countActiveAssignments(), dashboardQuery.countOpenReports(),
                dashboardQuery.findUnitUtilization(Instant.now().minus(UTILIZATION_WINDOW)));
    }
}
