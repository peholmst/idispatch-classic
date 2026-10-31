package net.pkhapps.idispatch.server.dispatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

public abstract class AbstractDispatcher implements ApplicationListener<DispatchNotificationSent> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected ReceiptRepository receiptRepository;

    @Autowired
    protected ApplicationContext applicationContext;

    protected void createReceipt(Destination destination, DispatchNotification notification) {
        logger.debug("Storing receipt for destination {} and notification {}", destination, notification);
        final Receipt receipt = receiptRepository.saveAndFlush(new Receipt(destination, notification));
        applicationContext.publishEvent(new DispatchNotificationReceived(this, receipt));
    }

}
