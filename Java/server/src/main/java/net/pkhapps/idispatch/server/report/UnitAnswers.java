package net.pkhapps.idispatch.server.report;

import static java.util.Objects.requireNonNull;

/**
 * What one responding unit found and did on scene.
 */
public record UnitAnswers(ReportText situationOnArrival, ReportText actionsTaken) {

    public static final UnitAnswers EMPTY = new UnitAnswers(ReportText.EMPTY, ReportText.EMPTY);

    public UnitAnswers {
        requireNonNull(situationOnArrival);
        requireNonNull(actionsTaken);
    }
}
