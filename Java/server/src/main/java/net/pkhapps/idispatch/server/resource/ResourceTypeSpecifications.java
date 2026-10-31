package net.pkhapps.idispatch.server.resource;

import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.domain.Specification;

final class ResourceTypeSpecifications {

    private ResourceTypeSpecifications() {
    }

    public static Specification<ResourceType> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<ResourceType> searchTerm(@NonNull String term) {
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("code")), term.toLowerCase() + "%"),
                cb.like(cb.lower(root.get(ResourceType.PROP_DESCRIPTION_FI)), "%" + term.toLowerCase() + "%"),
                cb.like(cb.lower(root.get(ResourceType.PROP_DESCRIPTION_SV)), "%" + term.toLowerCase() + "%")
        );
    }
}
