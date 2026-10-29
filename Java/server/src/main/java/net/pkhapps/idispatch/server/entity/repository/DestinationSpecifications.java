package net.pkhapps.idispatch.server.entity.repository;

import jakarta.persistence.criteria.JoinType;
import net.pkhapps.idispatch.server.entity.Destination;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.domain.Specification;

public final class DestinationSpecifications {

    private DestinationSpecifications() {
    }

    public static Specification<Destination> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Destination> searchTerm(@NonNull String term) {
        return (root, query, cb) -> {
            query.distinct(true);
            var pattern = "%" + term.toLowerCase() + "%";
            var resources = root.join(Destination.PROP_RESOURCES, JoinType.LEFT);
            return cb.or(cb.like(cb.lower(resources.get("callSign")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern));
        };
    }
}
