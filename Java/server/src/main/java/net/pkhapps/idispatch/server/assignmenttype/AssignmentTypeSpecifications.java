package net.pkhapps.idispatch.server.assignmenttype;

import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.domain.Specification;

public final class AssignmentTypeSpecifications {

    private AssignmentTypeSpecifications() {
    }

    public static Specification<AssignmentType> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<AssignmentType> searchTerm(@NonNull String term) {
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("code")), term.toLowerCase() + "%"),
                cb.like(cb.lower(root.get(AssignmentType.PROP_DESCRIPTION_FI)), "%" + term.toLowerCase() + "%"),
                cb.like(cb.lower(root.get(AssignmentType.PROP_DESCRIPTION_SV)), "%" + term.toLowerCase() + "%")
        );
    }
}
