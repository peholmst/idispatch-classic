package net.pkhapps.idispatch.server.entity.repository;

import net.pkhapps.idispatch.server.entity.Assignment;
import net.pkhapps.idispatch.server.entity.AssignmentType;
import net.pkhapps.idispatch.server.entity.Municipality;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repository of {@link Assignment}s.
 */
public interface AssignmentRepository extends JpaRepository<Assignment, Long>, JpaSpecificationExecutor<Assignment> {

    List<Assignment> findByClosedIsNull();

    List<Assignment> findByClosedIsNotNull(Pageable pageable);

    /**
     * Returns the municipalities that at least one closed assignment is in, in no particular order.
     */
    @Query("select distinct m from Assignment a join a.municipality m where a.closed is not null")
    List<Municipality> findMunicipalitiesOfClosedAssignments();

    /**
     * Returns the types that at least one closed assignment has, by code.
     */
    @Query("select distinct t from Assignment a join a.type t where a.closed is not null order by t.code")
    List<AssignmentType> findTypesOfClosedAssignments();
}
