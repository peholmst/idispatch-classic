package net.pkhapps.idispatch.server.entity;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.util.Optional;

/**
 * Base class for persistable entities.
 */
@MappedSuperclass
public abstract class AbstractEntity implements Persistable<Long>, Serializable, Cloneable {

    public static final String PROP_ID = "id";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO) // TODO Replace with sequences, one for each entity.
    @Column(name = "id", nullable = false)
    private Long id;

    protected AbstractEntity() {
    }

    @Override
    public Long getId() {
        return id;
    }

    public long nullSafeId() {
        return getIdOptional().orElseThrow();
    }

    public Optional<Long> getIdOptional() {
        return Optional.ofNullable(id);
    }

    protected void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AbstractEntity that = (AbstractEntity) o;

        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
