package net.pkhapps.idispatch.server.entity.repository;

import net.pkhapps.idispatch.server.entity.ResourceType;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.domain.Specification;

public final class ResourceTypeSpecifications {

    private ResourceTypeSpecifications() {
    }

    public static Specification<ResourceType> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<ResourceType> searchTerm(@NonNull String term) {
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("code")), term.toLowerCase() + "%"),
                cb.like(cb.lower(root.get("description")), "%" + term.toLowerCase() + "%")
        );
    }
}
