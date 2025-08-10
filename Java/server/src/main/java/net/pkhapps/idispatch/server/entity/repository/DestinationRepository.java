package net.pkhapps.idispatch.server.entity.repository;

import net.pkhapps.idispatch.server.entity.Destination;
import net.pkhapps.idispatch.server.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repository of {@link Destination}s.
 */
public interface DestinationRepository extends JpaRepository<Destination, Long>, JpaSpecificationExecutor<Destination> {

    @Query("select d from Destination d join d.resources r where d.active = TRUE and r = ?1")
    List<Destination> findDestinationsForResource(Resource resource);
}
