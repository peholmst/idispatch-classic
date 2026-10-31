package net.pkhapps.idispatch.server.assignment;

import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeId;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeListItem;
import net.pkhapps.idispatch.server.municipality.MunicipalityId;
import net.pkhapps.idispatch.server.municipality.MunicipalityListItem;
import net.pkhapps.idispatch.server.shared.UpdateResult;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for assignment operations.
 */
public interface AssignmentService {

    /**
     * Finds the assignment with the specified ID. If the ID is {@code null}, or the assignment does not exist, an empty
     * {@code Optional} is returned.
     */
    Optional<Assignment> findAssignment(Long id);

    /**
     * Creates a new assignment, stores it, publishes an {@link AssignmentOpened} event and returns the assignment ID.
     */
    Long openAssignment();

    /**
     * Saves the existing assignment, publishes an {@link AssignmentUpdated} event and returns:
     * <ul>
     * <li>{@link UpdateResult.Success} if the operation was successful.</li>
     * <li>{@link UpdateResult.Conflict} if the assignment had been modified by another user.</li>
     * <li>{@link UpdateResult.NoChange} if the assignment did not exist or was closed.</li>
     * </ui>
     */
    UpdateResult<Assignment> updateAssignment(Assignment assignment);

    /**
     * Closes the assignment and fires an {@link AssignmentClosed} event. If the assignment is already closed, or there
     * are still resources assigned to the assignment, nothing happens and false is returned.
     */
    boolean closeAssignment(Assignment assignment);

    /**
     * Finds all currently open assignments.
     */
    List<Assignment> findOpenAssignments();

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
