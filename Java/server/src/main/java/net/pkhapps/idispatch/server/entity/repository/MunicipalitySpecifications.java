package net.pkhapps.idispatch.server.entity.repository;

import net.pkhapps.idispatch.server.entity.Municipality;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.domain.Specification;

public final class MunicipalitySpecifications {

    private MunicipalitySpecifications() {
    }

    public static Specification<Municipality> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Municipality> searchTerm(@NonNull String term) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), term.toLowerCase() + "%");
    }
}
