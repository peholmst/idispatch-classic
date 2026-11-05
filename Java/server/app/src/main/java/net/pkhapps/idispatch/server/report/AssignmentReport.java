package net.pkhapps.idispatch.server.report;

import net.pkhapps.idispatch.server.assignment.Assignment;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.resource.ResourceId;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.SequencedSet;

import static java.util.Objects.requireNonNull;

/**
 * What happened on scene of a closed {@link Assignment}, as told by the people who were there: the scene as a whole,
 * and the answers of each responding unit. Who changed which part, and when, is kept in the audit log, by the
 * {@link ReportPart}s.
 * <p>
 * Once a reviewer has checked the report, they close it, and nobody can change it until it is re-opened.
 *
 * @param version incremented on every change, so that a change based on an older version can be refused; {@code null}
 *                until the report is first saved
 * @param units   the answers of the units that have reported, or have been shown a form to report in
 */
record AssignmentReport(AssignmentId assignmentId, @Nullable Long version, SceneDetails scene, boolean closed,
                        Map<ResourceId, UnitAnswers> units) {

    public AssignmentReport {
        requireNonNull(assignmentId);
        requireNonNull(scene);
        units = Map.copyOf(units);
    }

    /**
     * Returns the report of an assignment that nobody has written anything in yet.
     */
    public static AssignmentReport empty(AssignmentId assignment) {
        return new AssignmentReport(assignment, null, SceneDetails.EMPTY, false, Map.of());
    }

    public boolean isNew() {
        return version == null;
    }

    public boolean isClosed() {
        return closed;
    }

    /**
     * Returns the answers of the given responding unit, which are empty if the unit has not reported yet.
     */
    public UnitAnswers unit(ResourceId resource) {
        return units.getOrDefault(resource, UnitAnswers.EMPTY);
    }

    /**
     * Returns this report with what the writer filled in.
     */
    public AssignmentReport withDraft(ReportDraft draft) {
        var units = new HashMap<>(this.units);
        units.putAll(draft.units());
        return new AssignmentReport(assignmentId, version, draft.scene(), closed, units);
    }

    /**
     * Returns the parts whose content differs between the given report and this one: the scene first, then the units.
     * A unit that is in only one of them counts as empty in the other, so a unit shown but left empty is no change.
     */
    public SequencedSet<String> partsChangedFrom(AssignmentReport before) {
        var parts = new LinkedHashSet<String>();
        if (!scene.equals(before.scene())) {
            parts.add(ReportPart.SCENE);
        }
        var resources = new LinkedHashSet<>(before.units().keySet());
        resources.addAll(units.keySet());
        resources.stream()
                .filter(resource -> !unit(resource).equals(before.unit(resource)))
                .sorted((a, b) -> Long.compare(a.value(), b.value()))
                .forEach(resource -> parts.add(ReportPart.unit(resource)));
        return parts;
    }

    /**
     * Returns this report closed by a reviewer.
     */
    public AssignmentReport closedByReviewer() {
        return new AssignmentReport(assignmentId, version, scene, true, units);
    }

    public AssignmentReport reopened() {
        return new AssignmentReport(assignmentId, version, scene, false, units);
    }
}
