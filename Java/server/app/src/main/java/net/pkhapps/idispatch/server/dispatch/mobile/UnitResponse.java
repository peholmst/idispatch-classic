package net.pkhapps.idispatch.server.dispatch.mobile;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The unit the mobile app is paired with, as the app gets it.
 *
 * @param locale     the language of the destination, {@code fi} or {@code sv}, which the app speaks
 * @param status     the unit's current state, or {@code null} if it has none, as when the unit has been deleted
 * @param nextStates the states the unit may set next, in the order the dispatcher's buttons show them; empty without a
 *                   status
 */
record UnitResponse(String callSign, String locale, @Nullable State status, List<State> nextStates) {

    /**
     * @param state the {@link net.pkhapps.idispatch.server.resource.ResourceState}
     * @param label the name of the state in the destination's language
     */
    record State(String state, String label) {
    }
}
