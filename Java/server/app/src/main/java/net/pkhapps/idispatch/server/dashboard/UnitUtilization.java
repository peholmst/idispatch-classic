package net.pkhapps.idispatch.server.dashboard;

/**
 * A unit on the dashboard: its call sign and how many assignments it has been on during the window the dashboard
 * looks back on.
 */
public record UnitUtilization(long resourceId, String callSign, int assignments) {
}
