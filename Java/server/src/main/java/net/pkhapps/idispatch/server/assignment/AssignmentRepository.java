package net.pkhapps.idispatch.server.assignment;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repository of {@link Assignment}s.
 */
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByClosedIsNull();

    List<Assignment> findByClosedIsNotNull(Pageable pageable);

    /**
     * Returns the IDs of the municipalities that at least one closed assignment is in, in no particular order.
     */
    @Query("select distinct a.municipalityId from Assignment a where a.closed is not null and a.municipalityId is not null")
    List<Long> findMunicipalityIdsOfClosedAssignments();

    /**
     * Returns the IDs of the types that at least one closed assignment has, in no particular order.
     */
    @Query("select distinct a.typeId from Assignment a where a.closed is not null and a.typeId is not null")
    List<Long> findTypeIdsOfClosedAssignments();
}
