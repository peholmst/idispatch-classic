package net.pkhapps.idispatch.server.dispatch.mobile;

import org.jspecify.annotations.Nullable;

/**
 * The unit the mobile app is paired with, as the app gets it.
 *
 * @param locale the language of the destination, {@code fi} or {@code sv}, which the app speaks
 * @param status the unit's current status, or {@code null} if it has none, as when the unit has been deleted
 */
record UnitResponse(String callSign, String locale, @Nullable Status status) {

    /**
     * @param state the {@link net.pkhapps.idispatch.server.resource.ResourceState}
     * @param label the name of the state in the destination's language
     */
    record Status(String state, String label) {
    }
}
