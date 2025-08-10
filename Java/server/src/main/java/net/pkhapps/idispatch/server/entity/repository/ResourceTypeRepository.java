package net.pkhapps.idispatch.server.entity.repository;

import net.pkhapps.idispatch.server.entity.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * Repository of {@link ResourceType}s.
 */
public interface ResourceTypeRepository extends JpaRepository<ResourceType, Long>, JpaSpecificationExecutor<ResourceType> {

    List<ResourceType> findByActiveTrueOrderByCodeAsc();
}
