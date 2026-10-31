package net.pkhapps.idispatch.server.municipality;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Looks up municipalities for the features that show them, such as the dispatcher's assignment form, the reports and
 * the dispatch notifications. Read-only and not secured, as it also serves dispatching in the background; the views
 * and services that use it decide who may see what.
 */
public interface MunicipalityQueries {

    /**
     * Finds the municipalities that an assignment can be in, which are the ones not deleted, in no particular order.
     */
    List<MunicipalityListItem> findActive();

    /**
     * Finds the municipality with the given ID, whether deleted or not.
     */
    Optional<MunicipalityListItem> findById(MunicipalityId id);

    /**
     * Finds the municipalities with the given IDs, whether deleted or not, in no particular order.
     */
    List<MunicipalityListItem> findByIds(Collection<MunicipalityId> ids);
}
