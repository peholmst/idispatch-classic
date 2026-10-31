package net.pkhapps.idispatch.server.resource;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repository of {@link ResourceStatus}es.
 */
public interface ResourceStatusRepository extends JpaRepository<ResourceStatus, Long> {

    @Query("select rs from ResourceStatus rs join Resource r on r.id = rs.resourceId where r.active = TRUE and rs.resourceId = ?1")
    Optional<ResourceStatus> findByActiveResourceId(Long resourceId);

    List<ResourceStatus> findByAssignmentId(Long assignmentId);

    @Query("select rs from ResourceStatus rs join Resource r on r.id = rs.resourceId where r.active = TRUE and rs.available = TRUE order by r.callSign")
    List<ResourceStatus> findByActiveTrueAndAvailableTrue();

    List<ResourceStatus> findByAssignmentIdAndAssignedTrue(Long assignmentId);

    @Query("select rs from ResourceStatus rs join Resource r on r.id = rs.resourceId where r.active = TRUE order by r.callSign")
    List<ResourceStatus> findByActiveTrue();
}
