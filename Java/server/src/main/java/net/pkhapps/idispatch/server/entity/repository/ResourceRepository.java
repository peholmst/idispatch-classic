package net.pkhapps.idispatch.server.entity.repository;

import net.pkhapps.idispatch.server.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * Repository of {@link Resource}s.
 */
public interface ResourceRepository extends JpaRepository<Resource, Long>, JpaSpecificationExecutor<Resource> {

    /**
     * Finds and returns the resource with the specified call sign, or {@code null} if not found.
     */
    Resource findByCallSign(String callSign);

    /**
     * Finds and returns all active resources.
     */
    List<Resource> findByActiveTrueOrderByCallSignAsc();
}
