package net.pkhapps.idispatch.server.boundary;


import net.pkhapps.idispatch.server.entity.AbstractEntity;
import net.pkhapps.idispatch.server.entity.Deactivatable;
import net.pkhapps.idispatch.server.entity.ValidationFailedException;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface ManagementService<E extends AbstractEntity> {

    E save(E entity) throws ValidationFailedException;

    List<E> findAll(Filter filter);

    interface HardDeletable<E extends AbstractEntity> extends ManagementService<E> {
        boolean delete(E entity);
    }

    interface SoftDeletable<E extends AbstractEntity & Deactivatable> extends ManagementService<E> {

        void delete(E entity);

        void restore(E entity);
    }

    interface Filter {
    }

    interface SoftDeletableFilter extends Filter {
        boolean includesDeleted();

        default boolean includesActiveOnly() {
            return !includesDeleted();
        }
    }

    interface TextSearchFilter extends Filter {
        @Nullable
        String searchTerm();

        default void doWithSearchTerm(Consumer<String> consumer) {
            var st = Optional.ofNullable(searchTerm()).map(String::trim).orElse("");
            if (!st.isEmpty()) {
                consumer.accept(st);
            }
        }
    }
}
