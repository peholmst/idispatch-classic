package net.pkhapps.idispatch.server.assignmenttype;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * Repository of {@link AssignmentType}s.
 */
public interface AssignmentTypeRepository extends JpaRepository<AssignmentType, Long>, JpaSpecificationExecutor<AssignmentType> {

    List<AssignmentType> findByActiveTrueOrderByCodeAsc();
}
