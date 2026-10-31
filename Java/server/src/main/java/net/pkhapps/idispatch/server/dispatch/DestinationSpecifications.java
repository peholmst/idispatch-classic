package net.pkhapps.idispatch.server.dispatch;

import jakarta.persistence.criteria.JoinType;
import net.pkhapps.idispatch.server.resource.Resource;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.domain.Specification;

final class DestinationSpecifications {

    private DestinationSpecifications() {
    }

    public static Specification<Destination> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Destination> searchTerm(@NonNull String term) {
        return (root, query, cb) -> {
            query.distinct(true);
            var pattern = "%" + term.toLowerCase() + "%";
            var resourceIds = root.join(Destination.PROP_RESOURCES, JoinType.LEFT);
            var matchingResources = query.subquery(Long.class);
            var resource = matchingResources.from(Resource.class);
            matchingResources.select(resource.get(Resource.PROP_ID))
                    .where(cb.like(cb.lower(resource.get(Resource.PROP_CALL_SIGN)), pattern));
            return cb.or(resourceIds.in(matchingResources),
                    cb.like(cb.lower(root.get("description")), pattern));
        };
    }
}
