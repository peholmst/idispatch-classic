package net.pkhapps.idispatch.server.report;

import net.pkhapps.idispatch.server.resource.ResourceId;

import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * What a writer has filled in for a report: the scene, and the answers of each responding unit shown to them.
 */
public record ReportDraft(SceneDetails scene, Map<ResourceId, UnitAnswers> units) {

    public ReportDraft {
        requireNonNull(scene);
        units = Map.copyOf(units);
    }
}
