package net.pkhapps.idispatch.server.dashboard;

import java.util.List;

/**
 * What the dashboard shows, read at one moment.
 *
 * @param activeAssignments the assignments that have not been closed
 * @param openReports       the closed assignments whose report a reviewer has not closed, including the ones nobody has
 *                          written anything in yet
 * @param units             every unit that has not been deleted, the most used first
 */
public record DashboardFigures(int activeAssignments, int openReports, List<UnitUtilization> units) {

    public DashboardFigures {
        units = List.copyOf(units);
    }
}
