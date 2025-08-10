package net.pkhapps.idispatch.server.entity.repository;

import net.pkhapps.idispatch.server.entity.AssignmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * Repository of {@link AssignmentType}s.
 */
public interface AssignmentTypeRepository extends JpaRepository<AssignmentType, Long>, JpaSpecificationExecutor<AssignmentType> {

    List<AssignmentType> findByActiveTrueOrderByCodeAsc();
}
