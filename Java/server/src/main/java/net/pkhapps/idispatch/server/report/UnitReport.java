package net.pkhapps.idispatch.server.report;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * One responding unit's part of an {@link AssignmentReport}: its answers, and who last changed them.
 *
 * @param lastEdit who last changed the answers, or {@code null} if nobody has
 */
public record UnitReport(UnitAnswers answers, @Nullable UserStamp lastEdit) {

    public static final UnitReport EMPTY = new UnitReport(UnitAnswers.EMPTY, null);

    public UnitReport {
        requireNonNull(answers);
    }
}
