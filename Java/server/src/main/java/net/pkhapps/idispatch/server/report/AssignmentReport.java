package net.pkhapps.idispatch.server.report;

import net.pkhapps.idispatch.server.assignment.Assignment;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.resource.ResourceId;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * What happened on scene of a closed {@link Assignment}, as told by the people who were there. The report as a whole
 * and each {@link UnitReport} remember who last changed them; the report's last edit covers the scene only.
 * <p>
 * Once a reviewer has checked the report, they close it, and nobody can change it until it is re-opened.
 *
 * @param version  incremented on every change, so that a change based on an older version can be refused; {@code null}
 *                 until the report is first saved
 * @param lastEdit who last changed the scene, or {@code null} if nobody has
 * @param closed   who closed the report, or {@code null} while it is open
 * @param units    the units that have reported, or have been shown a form to report in
 */
public record AssignmentReport(AssignmentId assignmentId, @Nullable Long version, SceneDetails scene,
                               @Nullable UserStamp lastEdit, @Nullable UserStamp closed,
                               Map<ResourceId, UnitReport> units) {

    public AssignmentReport {
        requireNonNull(assignmentId);
        requireNonNull(scene);
        units = Map.copyOf(units);
    }

    /**
     * Returns the report of an assignment that nobody has written anything in yet.
     */
    public static AssignmentReport empty(AssignmentId assignment) {
        return new AssignmentReport(assignment, null, SceneDetails.EMPTY, null, null, Map.of());
    }

    public boolean isNew() {
        return version == null;
    }

    public boolean isClosed() {
        return closed != null;
    }

    /**
     * Returns the report of the given responding unit, which is empty if the unit has not reported yet.
     */
    public UnitReport unit(ResourceId resource) {
        return units.getOrDefault(resource, UnitReport.EMPTY);
    }

    /**
     * Returns this report with what the writer filled in. The edit is recorded on the scene and on each unit whose
     * content differs from this report's, so that saving doesn't claim the parts the writer didn't touch. A part that
     * has not been saved before counts as changed if it has any content.
     */
    public AssignmentReport withDraft(ReportDraft draft, UserStamp edit) {
        var units = new HashMap<>(this.units);
        draft.units().forEach((resource, answers) -> {
            var before = unit(resource);
            units.put(resource, before.answers().equals(answers) ? before : new UnitReport(answers, edit));
        });
        var sceneChanged = !scene.equals(draft.scene());
        return new AssignmentReport(assignmentId, version, draft.scene(), sceneChanged ? edit : lastEdit, closed,
                units);
    }

    /**
     * Returns this report closed by the reviewer.
     */
    public AssignmentReport closedBy(UserStamp reviewer) {
        return new AssignmentReport(assignmentId, version, scene, lastEdit, requireNonNull(reviewer), units);
    }

    public AssignmentReport reopened() {
        return new AssignmentReport(assignmentId, version, scene, lastEdit, null, units);
    }
}
