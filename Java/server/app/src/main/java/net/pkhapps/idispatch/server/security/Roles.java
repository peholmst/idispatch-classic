package net.pkhapps.idispatch.server.security;

public final class Roles {

    public static final String ROLE_DISPATCHER = "ROLE_DISPATCHER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_REPORT_READER = "ROLE_REPORT_READER";
    public static final String ROLE_REPORT_WRITER = "ROLE_REPORT_WRITER";
    public static final String ROLE_REPORT_REVIEWER = "ROLE_REPORT_REVIEWER";

    /**
     * The prefix of the authority that a unit's mobile app has for its own unit: the app of resource 42 has
     * {@code UNIT_42}. The mobile API grants it to a request with the key of the unit's mobile destination; Keycloak
     * never does.
     */
    public static final String UNIT_PREFIX = "UNIT_";

    /*
     * @PreAuthorize expressions for the roles above.
     */

    public static final String HAS_DISPATCHER = "hasAuthority('" + ROLE_DISPATCHER + "')";
    /**
     * A dispatcher, or the mobile app of the resource in the method's {@code resource} parameter.
     */
    public static final String HAS_DISPATCHER_OR_IS_UNIT = "hasAuthority('" + ROLE_DISPATCHER + "') or hasAuthority('"
            + UNIT_PREFIX + "' + #resource.value())";
    public static final String HAS_ADMIN = "hasAuthority('" + ROLE_ADMIN + "')";
    public static final String HAS_REPORT_WRITER = "hasAuthority('" + ROLE_REPORT_WRITER + "')";
    public static final String HAS_REPORT_REVIEWER = "hasAuthority('" + ROLE_REPORT_REVIEWER + "')";
    public static final String HAS_ANY_REPORT_ROLE = "hasAnyAuthority('" + ROLE_REPORT_READER + "', '"
            + ROLE_REPORT_WRITER + "', '" + ROLE_REPORT_REVIEWER + "')";

    private Roles() {
    }
}
