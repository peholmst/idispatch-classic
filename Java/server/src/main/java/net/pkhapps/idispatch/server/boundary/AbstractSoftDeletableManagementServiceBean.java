package net.pkhapps.idispatch.server.boundary;

import jakarta.validation.Validator;
import net.pkhapps.idispatch.server.entity.AbstractEntity;
import net.pkhapps.idispatch.server.entity.Deactivatable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

abstract class AbstractSoftDeletableManagementServiceBean<E extends AbstractEntity & Deactivatable, R extends JpaRepository<E, Long>> extends AbstractManagementServiceBean<E, R> implements ManagementService.SoftDeletable<E> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    AbstractSoftDeletableManagementServiceBean(Validator validator) {
        super(validator);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restore(E entity) {
        logger.debug("Restoring entity {}", entity);
        setActive(entity, true);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void delete(E entity) {
        logger.debug("Softly deleting entity {}", entity);
        setActive(entity, false);
    }

    private void setActive(E entity, boolean active) {
        entity.getIdOptional().flatMap(getRepository()::findById).ifPresent(existing -> {
            existing.setActive(active);
            getRepository().saveAndFlush(existing);
        });
    }
}
