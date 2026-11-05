package net.pkhapps.idispatch.server.report;

import net.pkhapps.idispatch.server.resource.ResourceId;
import org.jspecify.annotations.Nullable;

/**
 * The parts of an {@link AssignmentReport} that the audit log tells apart when a writer edits the report: the scene,
 * with the situation and the counts, and each unit's answers.
 */
final class ReportPart {

    static final String SCENE = "SCENE";
    private static final String UNIT_PREFIX = "UNIT:";

    private ReportPart() {
    }

    static String unit(ResourceId resource) {
        return UNIT_PREFIX + resource.value();
    }

    /**
     * Returns the unit whose answers the part is, or {@code null} if it is not a unit's part.
     */
    static @Nullable ResourceId unitOf(@Nullable String part) {
        if (part == null || !part.startsWith(UNIT_PREFIX)) {
            return null;
        }
        try {
            return new ResourceId(Long.parseLong(part.substring(UNIT_PREFIX.length())));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
