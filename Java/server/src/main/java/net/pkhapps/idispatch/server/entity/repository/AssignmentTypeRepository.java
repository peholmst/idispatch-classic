package net.pkhapps.idispatch.server.entity.repository;

import net.pkhapps.idispatch.server.entity.AssignmentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository of {@link AssignmentType}s.
 */
public interface AssignmentTypeRepository extends JpaRepository<AssignmentType, Long> {

    List<AssignmentType> findByActiveTrueOrderByCodeAsc();
}
