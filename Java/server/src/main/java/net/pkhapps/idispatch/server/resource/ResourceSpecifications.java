package net.pkhapps.idispatch.server.resource;

import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.domain.Specification;

final class ResourceSpecifications {

    private ResourceSpecifications() {
    }

    public static Specification<Resource> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Resource> searchTerm(@NonNull String term) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("callSign")), "%" + term.toLowerCase() + "%");
    }
}
