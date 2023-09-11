package net.pkhapps.idispatch.server.boundary;

import net.pkhapps.idispatch.server.entity.Assignment;
import net.pkhapps.idispatch.server.entity.AssignmentType;
import net.pkhapps.idispatch.server.entity.AssignmentUrgency;
import net.pkhapps.idispatch.server.entity.Municipality;
import net.pkhapps.idispatch.server.events.AssignmentClosed;
import net.pkhapps.idispatch.server.events.AssignmentOpened;
import net.pkhapps.idispatch.server.events.AssignmentUpdated;
import net.pkhapps.idispatch.server.util.UpdateResult;

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
     * Finds all closed assignments.
     */
    List<Assignment> findClosedAssignments();

    List<Municipality> findApplicableMunicipalities();

    List<AssignmentType> findApplicableAssignmentTypes();

    List<AssignmentUrgency> findApplicableUrgencies();
}
