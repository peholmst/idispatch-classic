package net.pkhapps.idispatch.server.entity.repository;

import net.pkhapps.idispatch.server.entity.User;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> searchTerm(@NonNull String term) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("username")), "%" + term.toLowerCase() + "%");
    }
}
