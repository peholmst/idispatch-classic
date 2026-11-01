package net.pkhapps.idispatch.server.security;

public final class Roles {

    public static final String ROLE_DISPATCHER = "ROLE_DISPATCHER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_REPORT_READER = "ROLE_REPORT_READER";
    public static final String ROLE_REPORT_WRITER = "ROLE_REPORT_WRITER";
    public static final String ROLE_REPORT_REVIEWER = "ROLE_REPORT_REVIEWER";

    /*
     * @PreAuthorize expressions for the roles above.
     */

    public static final String HAS_DISPATCHER = "hasAuthority('" + ROLE_DISPATCHER + "')";
    public static final String HAS_ADMIN = "hasAuthority('" + ROLE_ADMIN + "')";
    public static final String HAS_REPORT_WRITER = "hasAuthority('" + ROLE_REPORT_WRITER + "')";
    public static final String HAS_REPORT_REVIEWER = "hasAuthority('" + ROLE_REPORT_REVIEWER + "')";
    public static final String HAS_ANY_REPORT_ROLE = "hasAnyAuthority('" + ROLE_REPORT_READER + "', '"
            + ROLE_REPORT_WRITER + "', '" + ROLE_REPORT_REVIEWER + "')";

    private Roles() {
    }
}
