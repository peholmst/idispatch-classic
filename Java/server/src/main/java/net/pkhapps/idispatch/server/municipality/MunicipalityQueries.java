package net.pkhapps.idispatch.server.municipality;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Looks up municipalities for the features that show them, such as the dispatcher's assignment form, the reports and
 * the dispatch notifications. Read-only and not secured, as it also serves dispatching in the background; the views
 * and services that use it decide who may see what.
 */
@Service
@Transactional(readOnly = true)
public class MunicipalityQueries {

    private final MunicipalityRepository repository;

    MunicipalityQueries(MunicipalityRepository repository) {
        this.repository = repository;
    }

    /**
     * Finds the municipalities that an assignment can be in, which are the ones not deleted, in no particular order.
     */
    public List<MunicipalityListItem> findActive() {
        return repository.findListItems(false, null);
    }

    /**
     * Finds the municipality with the given ID, whether deleted or not.
     */
    public Optional<MunicipalityListItem> findById(MunicipalityId id) {
        return repository.findListItem(id);
    }

    /**
     * Finds the municipalities with the given IDs, whether deleted or not, in no particular order.
     */
    public List<MunicipalityListItem> findByIds(Collection<MunicipalityId> ids) {
        return ids.isEmpty() ? List.of() : repository.findListItems(ids);
    }
}
