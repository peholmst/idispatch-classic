package net.pkhapps.idispatch.server.control;

import net.pkhapps.idispatch.server.entity.Destination;
import net.pkhapps.idispatch.server.entity.DispatchNotification;
import net.pkhapps.idispatch.server.entity.Receipt;
import net.pkhapps.idispatch.server.entity.repository.ReceiptRepository;
import net.pkhapps.idispatch.server.events.DispatchNotificationReceived;
import net.pkhapps.idispatch.server.events.DispatchNotificationSent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

abstract class AbstractDispatcher implements ApplicationListener<DispatchNotificationSent> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    ReceiptRepository receiptRepository;

    @Autowired
    ApplicationContext applicationContext;

    protected void createReceipt(Destination destination, DispatchNotification notification) {
        logger.debug("Storing receipt for destination {} and notification {}", destination, notification);
        final Receipt receipt = receiptRepository.saveAndFlush(new Receipt(destination, notification));
        applicationContext.publishEvent(new DispatchNotificationReceived(this, receipt));
    }

}
