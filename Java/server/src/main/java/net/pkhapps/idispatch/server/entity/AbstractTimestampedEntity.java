package net.pkhapps.idispatch.server.entity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import java.time.Instant;

/**
 * Base class for entities that are timestamped ("application events"). Normally, these events never change once they
 * have been persisted.
 */
@MappedSuperclass
public abstract class AbstractTimestampedEntity extends AbstractEntity {

    public static final String PROP_TIMESTAMP = "timestamp";

    @Column(name = "ts", nullable = false)
    private Instant timestamp;

    protected AbstractTimestampedEntity() {
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    protected void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Sets the timestamp to the current date and time, unless a timestamp has already been set.
     */
    @PrePersist
    protected void beforePersist() {
        if (getTimestamp() == null) {
            setTimestamp(Instant.now());
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Timestamped entities cannot be cloned");
    }
}
