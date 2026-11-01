package net.pkhapps.idispatch.server.dispatch;

import org.springframework.context.ApplicationEvent;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Event published when a dispatch notification has been received by a destination.
 */
class DispatchNotificationReceived extends ApplicationEvent {

    private final Receipt receipt;

    public DispatchNotificationReceived(Object source, Receipt receipt) {
        super(source);
        this.receipt = checkNotNull(receipt);
    }

    public Receipt getReceipt() {
        return receipt;
    }
}
