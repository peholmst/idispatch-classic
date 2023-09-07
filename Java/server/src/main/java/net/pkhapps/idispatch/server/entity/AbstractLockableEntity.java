package net.pkhapps.idispatch.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

/**
 * Base class for persistable entities that use optimistic locking to prevent accidental overwrites.
 */
@MappedSuperclass
public abstract class AbstractLockableEntity extends AbstractEntity {

    public static final String PROP_VERSION = "version";

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected AbstractLockableEntity() {
    }

    public Long getVersion() {
        return version;
    }

    protected void setVersion(Long version) {
        this.version = version;
    }
}
