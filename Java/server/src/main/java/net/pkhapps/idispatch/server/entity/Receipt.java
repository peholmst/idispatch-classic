package net.pkhapps.idispatch.server.entity;

import jakarta.persistence.*;

/**
 * TODO Document me!
 */
@Entity
@Table(name = "receipts", uniqueConstraints = @UniqueConstraint(columnNames = {"destination_id", "notification_id"}))
public class Receipt extends AbstractTimestampedEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;

    @ManyToOne(optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private DispatchNotification notification;

    protected Receipt() {
    }

    public Receipt(Destination destination, DispatchNotification notification) {
        this.destination = destination;
        this.notification = notification;
    }

    public Destination getDestination() {
        return destination;
    }

    public DispatchNotification getNotification() {
        return notification;
    }
}
