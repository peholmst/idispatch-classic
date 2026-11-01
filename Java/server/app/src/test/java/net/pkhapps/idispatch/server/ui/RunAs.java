package net.pkhapps.idispatch.server.ui;

import net.pkhapps.idispatch.server.security.Roles;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.function.Supplier;

/**
 * Calls the secured services from a fixture, as a user with the given role in another session would, and restores the
 * security context of whoever is active in the test afterwards.
 */
public final class RunAs {

    private RunAs() {
    }

    /**
     * Runs the action as a user with the given role, who may also read reports.
     */
    public static <T> T role(String role, Supplier<T> action) {
        var previous = SecurityContextHolder.getContext();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("fixture", null,
                AuthorityUtils.createAuthorityList(role, Roles.ROLE_REPORT_READER)));
        SecurityContextHolder.setContext(context);
        try {
            return action.get();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }

    public static <T> T dispatcher(Supplier<T> action) {
        return role(Roles.ROLE_DISPATCHER, action);
    }

    /**
     * Runs the action as the mobile app of the resource with the given ID, which may change only that resource.
     */
    public static <T> T unit(long resourceId, Supplier<T> action) {
        var previous = SecurityContextHolder.getContext();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("unit", null,
                AuthorityUtils.createAuthorityList(Roles.UNIT_PREFIX + resourceId)));
        SecurityContextHolder.setContext(context);
        try {
            return action.get();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }
}
