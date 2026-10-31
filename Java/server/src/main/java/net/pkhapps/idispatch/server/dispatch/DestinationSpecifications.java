package net.pkhapps.idispatch.server.dispatch;

import jakarta.persistence.criteria.JoinType;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

final class DestinationSpecifications {

    private DestinationSpecifications() {
    }

    public static Specification<Destination> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    /**
     * Matches the destinations whose description contains the term, or that have any of the given resources, whose
     * call signs contain it.
     */
    public static Specification<Destination> searchTerm(@NonNull String term, Collection<Long> matchingResourceIds) {
        return (root, query, cb) -> {
            query.distinct(true);
            var pattern = "%" + term.toLowerCase() + "%";
            var description = cb.like(cb.lower(root.get("description")), pattern);
            if (matchingResourceIds.isEmpty()) {
                return description;
            }
            var resourceIds = root.join(Destination.PROP_RESOURCES, JoinType.LEFT);
            return cb.or(resourceIds.in(matchingResourceIds), description);
        };
    }
}
