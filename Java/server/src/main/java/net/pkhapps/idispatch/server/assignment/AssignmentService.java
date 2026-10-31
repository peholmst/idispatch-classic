package net.pkhapps.idispatch.server.assignment;

import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeId;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeListItem;
import net.pkhapps.idispatch.server.municipality.MunicipalityId;
import net.pkhapps.idispatch.server.municipality.MunicipalityListItem;
import net.pkhapps.idispatch.server.shared.UpdateResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Lets dispatchers open, fill in and close assignments.
 */
public interface AssignmentService {

    Optional<Assignment> findAssignment(AssignmentId id);

    /**
     * Opens a new, empty assignment, publishes an {@link AssignmentOpened} event and returns the assignment's ID.
     */
    AssignmentId openAssignment();

    /**
     * Saves what a dispatcher has filled in, publishes an {@link AssignmentUpdated} event and returns:
     * <ul>
     * <li>{@link UpdateResult.Success} with the assignment as saved, with its new version.</li>
     * <li>{@link UpdateResult.Conflict} with the assignment as it now is, if it had been changed by another user.</li>
     * <li>{@link UpdateResult.NoChange} if the assignment is closed.</li>
     * </ul>
     */
    UpdateResult<Assignment> updateAssignment(Assignment assignment);

    /**
     * Closes the assignment and publishes an {@link AssignmentClosed} event. If the assignment is already closed, or
     * resources are still assigned to it, nothing happens and false is returned.
     */
    boolean closeAssignment(AssignmentId id);

    /**
     * Finds the open assignments, oldest first.
     */
    List<AssignmentListItem> findOpenAssignments();

    /**
     * Finds the assignments with the given IDs, open or closed, oldest first.
     */
    List<AssignmentListItem> findAssignments(Collection<AssignmentId> ids);

    /**
     * Finds the municipalities that an assignment can be in, which are the ones not deleted, in no particular order.
     */
    List<MunicipalityListItem> findApplicableMunicipalities();

    /**
     * Finds the municipality with the given ID, whether deleted or not, so that an assignment keeps showing the
     * municipality it is in.
     */
    Optional<MunicipalityListItem> findMunicipality(MunicipalityId id);

    /**
     * Finds the types that an assignment can be given, which are the ones not deleted, ordered by code.
     */
    List<AssignmentTypeListItem> findApplicableAssignmentTypes();

    /**
     * Finds the type with the given ID, whether deleted or not, so that an assignment keeps showing the type it has.
     */
    Optional<AssignmentTypeListItem> findAssignmentType(AssignmentTypeId id);

    List<AssignmentUrgency> findApplicableUrgencies();
}
