package net.pkhapps.idispatch.server.entity.repository;

import net.pkhapps.idispatch.server.entity.Municipality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * Repository of {@link Municipality}-entities.
 */
public interface MunicipalityRepository extends JpaRepository<Municipality, Long>, JpaSpecificationExecutor<Municipality> {

    List<Municipality> findByActiveTrueOrderByNameAsc();
}
