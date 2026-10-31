package net.pkhapps.idispatch.server.municipality;

import net.pkhapps.idispatch.server.shared.management.DefaultFilter;

import java.util.List;
import java.util.Optional;

/**
 * Lets administrators keep the municipalities up to date. A name that another municipality already has in the same
 * language is refused with a {@link org.springframework.dao.DataIntegrityViolationException}, and a change based on an
 * outdated version with an {@link org.springframework.dao.OptimisticLockingFailureException}.
 */
public interface MunicipalityManagementService {

    List<MunicipalityListItem> findAll(DefaultFilter filter);

    Optional<Municipality> findById(MunicipalityId id);

    Municipality create(MunicipalityDetails details);

    /**
     * Saves the changed details of the municipality.
     *
     * @return the municipality as saved, with its new version
     */
    Municipality update(Municipality municipality);

    void delete(MunicipalityId id);

    void restore(MunicipalityId id);
}
