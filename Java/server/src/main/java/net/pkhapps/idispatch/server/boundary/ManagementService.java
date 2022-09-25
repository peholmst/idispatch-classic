package net.pkhapps.idispatch.server.boundary;


import net.pkhapps.idispatch.server.entity.AbstractEntity;
import net.pkhapps.idispatch.server.entity.Deactivatable;
import net.pkhapps.idispatch.server.entity.ValidationFailedException;

import java.util.List;

public interface ManagementService<E extends AbstractEntity> {

    E save(E entity) throws ValidationFailedException;

    List<E> findAll();

    interface HardDeletable<E extends AbstractEntity> extends ManagementService<E> {
        boolean delete(E entity);
    }

    interface SoftDeletable<E extends AbstractEntity & Deactivatable> extends ManagementService<E> {

        void delete(E entity);

        void restore(E entity);
    }
}
