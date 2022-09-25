package net.pkhapps.idispatch.server.boundary;


import net.pkhapps.idispatch.server.entity.Assignment;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.ResourceState;
import net.pkhapps.idispatch.server.entity.ResourceStatus;
import net.pkhapps.idispatch.server.events.ResourceStatusChanged;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for resource status operations.
 */
public interface ResourceStatusService {

    /**
     * Finds the current status of the specified resource. If the resource is {@code null}, not active, or it does not
     * exist, an empty {@code Optional} is returned.
     *
     * @see Resource#isActive()
     */
    Optional<ResourceStatus> findCurrentStatus(Resource resource);

    /**
     * Attempts to set the state of the specified resource. If the requested state transition is not valid or the
     * resource is not active, nothing happens. Otherwise, the new state is saved and a {@link ResourceStatusChanged}
     * event is fired.
     *
     * @see ResourceStatus#getAllValidNextStates()
     * @see Resource#isActive()
     */
    void setResourceState(Resource resource, ResourceState resourceState);

    /**
     * Attempts to assign the resource to the specified assignment, settings its state to
     * {@link ResourceState#RESERVED}. If the resource is not active or not available, nothing happens. Otherwise, the
     * new state is saved and a {@link ResourceStatusChanged} event is fired.
     *
     * @see ResourceStatus#isAvailable()
     * @see Resource#isActive()
     */
    void setResourceAssignment(Resource resource, Assignment assignment);

    /**
     * Attempts to clear the assignment from the specified resource, and stores the state and fires a
     * {@link ResourceStatusChanged} event if successful. This can only happen if the resource is active and is in any
     * of the following states:
     * <ul>
     * <li>{@link ResourceState#RESERVED}, in which case the state of the resource will change to whatever it was before the resource was reserved)</li>
     * <li>{@link ResourceState#AVAILABLE}</li>
     * <li>{@link ResourceState#AT_STATION}</li>
     * <li>{@link ResourceState#OUT_OF_SERVICE}</li>
     * </ul>
     * Otherwise, nothing happens.
     *
     * @see Resource#isActive()
     */
    void clearResourceAssignment(Resource resource);

    /**
     * Same as {@link #clearResourceAssignment(Resource)} , but for all resources associated with the specified
     * assignment.
     */
    void clearAssignmentFromAllResources(Assignment assignment);

    /**
     * Finds all resources that are currently assigned to the specified assignment.
     *
     * @see ResourceStatus#isAssigned()
     */
    List<Resource> getResourcesAssignedToAssignment(Assignment assignment);

    /**
     * Finds the current status of all resources that are currently assigned to the specified assignment.
     *
     * @see ResourceStatus#isAssigned()
     */
    List<ResourceStatus> getStatusOfResourcesAssignedToAssignment(Assignment assignment);

    /**
     * Finds all active and available resources.
     *
     * @see Resource#isActive()
     * @see ResourceStatus#isAvailable()
     */
    List<Resource> getAvailableResources();

    /**
     * Finds the current status of all active and available resources.
     *
     * @see Resource#isActive()
     * @see ResourceStatus#isAvailable()
     */
    List<ResourceStatus> getStatusOfAvailableResources();

    /**
     * Finds all active resources.
     *
     * @see Resource#isActive()
     */
    List<Resource> getAllResources();

    /**
     * Finds the current status of all active resources.
     *
     * @see Resource#isActive()
     */
    List<ResourceStatus> getStatusOfAllResources();
}
