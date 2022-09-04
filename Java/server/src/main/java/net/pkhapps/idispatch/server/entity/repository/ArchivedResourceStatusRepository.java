package net.pkhapps.idispatch.server.entity.repository;

import net.pkhapps.idispatch.server.entity.ArchivedResourceStatus;
import net.pkhapps.idispatch.server.entity.Assignment;
import net.pkhapps.idispatch.server.entity.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository of {@link ArchivedResourceStatus}es.
 */
public interface ArchivedResourceStatusRepository extends JpaRepository<ArchivedResourceStatus, Long> {

    List<ArchivedResourceStatus> findByAssignment(Assignment assignment, Pageable pageable);

    List<ArchivedResourceStatus> findByAssignment(Assignment assignment);

    List<ArchivedResourceStatus> findByResource(Resource resource, Pageable pageable);

}
