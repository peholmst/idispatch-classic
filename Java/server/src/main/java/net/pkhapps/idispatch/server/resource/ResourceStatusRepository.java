package net.pkhapps.idispatch.server.resource;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repository of {@link ResourceStatus}es.
 */
public interface ResourceStatusRepository extends JpaRepository<ResourceStatus, Long> {

    ResourceStatus findByResource(Resource resource);

    List<ResourceStatus> findByAssignmentId(Long assignmentId);

    @Query("select rs from ResourceStatus rs where rs.resource.active = TRUE and rs.available = TRUE order by rs.resource.callSign")
    List<ResourceStatus> findByActiveTrueAndAvailableTrue();

    List<ResourceStatus> findByAssignmentIdAndAssignedTrue(Long assignmentId);

    @Query("select rs from ResourceStatus rs where rs.resource.active = TRUE order by rs.resource.callSign")
    List<ResourceStatus> findByActiveTrue();
}
