package net.pkhapps.idispatch.server.resource;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.pkhapps.idispatch.server.shared.AbstractLockableEntity;
import net.pkhapps.idispatch.server.shared.Bilingual;
import net.pkhapps.idispatch.server.shared.Deactivatable;
import org.hibernate.annotations.Formula;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Strings.nullToEmpty;

/**
 * Entity representing a resource.
 */
@Entity
@Table(name = "resources")
public class Resource extends AbstractLockableEntity implements Comparable<Resource>, Deactivatable {

    public static final String PROP_CALL_SIGN = "callSign";
    public static final String PROP_TYPE_ID = "typeId";

    @Column(name = "call_sign", unique = true, nullable = false)
    @NotBlank(message = "{idispatch.resource.call-sign.required}")
    private String callSign = "";
    @Column(name = "type_id", nullable = false)
    @NotNull(message = "{idispatch.resource.type.required}")
    private Long typeId;
    // Read-only, for showing the type in the list until resources are stored with jOOQ too
    @Formula("(select t.description_fi from resource_types t where t.id = type_id)")
    private String typeDescriptionFi;
    @Formula("(select t.description_sv from resource_types t where t.id = type_id)")
    private String typeDescriptionSv;
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * Returns the ID of a resource that has been saved.
     */
    public ResourceId getResourceId() {
        return new ResourceId(getId());
    }

    public String getCallSign() {
        return callSign;
    }

    public void setCallSign(String callSign) {
        this.callSign = nullToEmpty(callSign);
    }

    /**
     * Returns the ID of the resource's {@link ResourceType}, or {@code null} if no type has been chosen yet.
     */
    public @Nullable Long getTypeId() {
        return typeId;
    }

    public void setTypeId(@Nullable Long typeId) {
        this.typeId = typeId;
    }

    /**
     * Returns the description of the resource's type to show to a user with the given locale, as it was when the
     * resource was loaded.
     */
    public String getTypeDescription(@Nullable Locale locale) {
        return Bilingual.select(typeDescriptionFi, typeDescriptionSv, locale);
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
    public String toString() {
        return toStringHelper(this)
                .add(PROP_ID, getId())
                .add(PROP_VERSION, getVersion())
                .add(PROP_CALL_SIGN, callSign)
                .toString();
    }

    @Override
    public int compareTo(Resource o) {
        return callSign.compareTo(o.callSign);
    }
}
