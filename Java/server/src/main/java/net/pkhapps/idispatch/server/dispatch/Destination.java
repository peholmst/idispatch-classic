package net.pkhapps.idispatch.server.dispatch;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.shared.AbstractLockableEntity;
import net.pkhapps.idispatch.server.shared.Bilingual;
import net.pkhapps.idispatch.server.shared.Deactivatable;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base class for a destination that receives dispatch notifications.
 */
@Entity
@Table(name = "destinations")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Destination extends AbstractLockableEntity implements Deactivatable {

    public static final String PROP_DESCRIPTION = "description";
    public static final String PROP_LOCALE = "locale";
    public static final String PROP_RESOURCES = "resources";

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "description")
    @Size(max = 255, message = "{idispatch.destination.description.size}")
    private String description;

    @Column(name = "locale", nullable = false)
    @NotNull(message = "{idispatch.destination.locale.required}")
    private Locale locale = Bilingual.FINNISH;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "destination_resources", joinColumns = @JoinColumn(name = "destination_id"))
    @Column(name = "resource_id", nullable = false)
    @NotEmpty(message = "{idispatch.destination.resources.required}")
    private Set<Long> resources = new HashSet<>();

    /**
     * Free-text description shown only in the management console, never sent to the destination.
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * The language the destination receives notifications in. It picks the language of the municipality names and
     * assignment type descriptions, as described in {@link Bilingual#select}.
     */
    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * Returns the resources whose dispatch notifications the destination receives.
     */
    public Set<ResourceId> getResources() {
        return resources.stream().map(ResourceId::new).collect(Collectors.toSet());
    }

    public void setResources(@Nullable Set<ResourceId> resources) {
        this.resources = resources == null ? new HashSet<>()
                : resources.stream().map(ResourceId::value).collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        final Destination clone = (Destination) super.clone();
        clone.resources = new HashSet<>(resources);
        return clone;
    }

    public abstract String getType();
}
