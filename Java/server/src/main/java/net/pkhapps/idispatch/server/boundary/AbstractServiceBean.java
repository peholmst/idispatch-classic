package net.pkhapps.idispatch.server.boundary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

abstract class AbstractServiceBean {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final ApplicationContext applicationContext;

    private final TransactionTemplate txTemplate;

    AbstractServiceBean(ApplicationContext applicationContext, PlatformTransactionManager txManager) {
        this.applicationContext = applicationContext;
        txTemplate = new TransactionTemplate(txManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    protected TransactionTemplate getTxTemplate() {
        return txTemplate;
    }

    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
