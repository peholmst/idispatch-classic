package net.pkhapps.idispatch.server.boundary;

import net.pkhapps.idispatch.server.entity.AbstractEntity;
import net.pkhapps.idispatch.server.entity.ValidationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Validator;
import javax.validation.groups.Default;
import java.util.List;

abstract class AbstractManagementServiceBean<E extends AbstractEntity, R extends JpaRepository<E, Long>> implements ManagementService<E> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final Validator validator;

    AbstractManagementServiceBean(Validator validator) {
        this.validator = validator;
    }

    protected abstract R getRepository();

    protected Validator getValidator() {
        return validator;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public E save(E entity) throws ValidationFailedException {
        ValidationFailedException.throwIfNotEmpty(getValidator().validate(entity, Default.class));
        logger.debug("Saving entity {}", entity);
        return getRepository().saveAndFlush(entity);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<E> findAll() {
        logger.debug("Finding all entities");
        return getRepository().findAll();
    }
}
