package net.pkhapps.idispatch.server.report;

import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * What happened on scene as a whole: how the situation developed and how many people were involved, injured and
 * killed. A count is {@code null} until someone fills it in.
 */
record SceneDetails(ReportText situationDevelopment, @Nullable Integer peopleInvolved,
                           @Nullable Integer lightlyInjured, @Nullable Integer seriouslyInjured,
                           @Nullable Integer deceased) {

    public static final SceneDetails EMPTY = new SceneDetails(ReportText.EMPTY, null, null, null, null);

    public SceneDetails {
        requireNonNull(situationDevelopment);
        if (!Stream.of(peopleInvolved, lightlyInjured, seriouslyInjured, deceased).allMatch(SceneDetails::isCountValid)) {
            throw new IllegalArgumentException("A count cannot be negative");
        }
        if (!casualtiesWithinPeopleInvolved(peopleInvolved, lightlyInjured, seriouslyInjured, deceased)) {
            throw new IllegalArgumentException("More casualties than people involved");
        }
    }

    /**
     * Whether the value can be a count: empty, or not negative.
     */
    public static boolean isCountValid(@Nullable Integer count) {
        return count == null || count >= 0;
    }

    /**
     * Whether the injured and deceased add up to no more than the people involved. Holds while the people involved
     * have not been counted.
     */
    public static boolean casualtiesWithinPeopleInvolved(@Nullable Integer peopleInvolved,
                                                         @Nullable Integer lightlyInjured,
                                                         @Nullable Integer seriouslyInjured,
                                                         @Nullable Integer deceased) {
        return peopleInvolved == null
                || zeroIfNull(lightlyInjured) + zeroIfNull(seriouslyInjured) + zeroIfNull(deceased) <= peopleInvolved;
    }

    private static int zeroIfNull(@Nullable Integer value) {
        return value == null ? 0 : value;
    }
}
