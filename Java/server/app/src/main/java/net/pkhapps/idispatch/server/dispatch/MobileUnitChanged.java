package net.pkhapps.idispatch.server.dispatch;

import org.springframework.context.ApplicationEvent;

import static java.util.Objects.requireNonNull;

/**
 * Published when a dispatcher has moved a mobile destination to another unit, after the change is saved.
 */
public class MobileUnitChanged extends ApplicationEvent {

    private final MobileDestination destination;

    public MobileUnitChanged(Object source, MobileDestination destination) {
        super(source);
        this.destination = requireNonNull(destination);
    }

    /**
     * The destination as saved, with its new unit.
     */
    public MobileDestination getDestination() {
        return destination;
    }
}
