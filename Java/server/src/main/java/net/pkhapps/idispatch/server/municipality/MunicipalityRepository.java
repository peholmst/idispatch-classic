package net.pkhapps.idispatch.server.municipality;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * Repository of {@link Municipality}-entities.
 */
public interface MunicipalityRepository extends JpaRepository<Municipality, Long>, JpaSpecificationExecutor<Municipality> {

    List<Municipality> findByActiveTrue();
}
