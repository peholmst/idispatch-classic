package net.pkhapps.idispatch.server.entity.repository;

import net.pkhapps.idispatch.server.entity.Assignment;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.ResourceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repository of {@link ResourceStatus}es.
 */
public interface ResourceStatusRepository extends JpaRepository<ResourceStatus, Long> {

    ResourceStatus findByResource(Resource resource);

    List<ResourceStatus> findByAssignment(Assignment assignment);

    @Query("select rs from ResourceStatus rs where rs.resource.active = TRUE and rs.available = TRUE order by rs.resource.callSign")
    List<ResourceStatus> findByActiveTrueAndAvailableTrue();

    List<ResourceStatus> findByAssignmentAndAssignedTrue(Assignment assignment);

    @Query("select rs from ResourceStatus rs where rs.resource.active = TRUE order by rs.resource.callSign")
    List<ResourceStatus> findByActiveTrue();
}
