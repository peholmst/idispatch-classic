package net.pkhapps.idispatch.server.entity.repository;

import net.pkhapps.idispatch.server.entity.Municipality;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository of {@link Municipality}-entities.
 */
public interface MunicipalityRepository extends JpaRepository<Municipality, Long> {

    List<Municipality> findByActiveTrueOrderByNameAsc();
}
