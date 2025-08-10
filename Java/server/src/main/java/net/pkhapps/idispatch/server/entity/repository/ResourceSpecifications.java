package net.pkhapps.idispatch.server.entity.repository;

import net.pkhapps.idispatch.server.entity.Resource;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.domain.Specification;

public final class ResourceSpecifications {

    private ResourceSpecifications() {
    }

    public static Specification<Resource> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Resource> searchTerm(@NonNull String term) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("callSign")), "%" + term.toLowerCase() + "%");
    }
}
