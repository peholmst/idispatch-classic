package net.pkhapps.idispatch.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import static com.google.common.base.Strings.nullToEmpty;

/**
 * Entity representing a municipality.
 */
@Entity
@Table(name = "municipalities")
public class Municipality extends AbstractLockableEntity implements Deactivatable {

    public static final String PROP_NAME = "name";

    @Column(name = "name", nullable = false, unique = true)
    @NotBlank(message = "Please enter a name")
    private String name = "";
    @Column(name = "active", nullable = false)
    private boolean active = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = nullToEmpty(name);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }
}
