package net.pkhapps.idispatch.server.resource;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * Repository of {@link ResourceType}s.
 */
public interface ResourceTypeRepository extends JpaRepository<ResourceType, Long>, JpaSpecificationExecutor<ResourceType> {

    List<ResourceType> findByActiveTrueOrderByCodeAsc();
}
