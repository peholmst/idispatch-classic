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
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get(Municipality.PROP_NAME_FI)), term.toLowerCase() + "%"),
                cb.like(cb.lower(root.get(Municipality.PROP_NAME_SV)), term.toLowerCase() + "%")
        );
    }
}
