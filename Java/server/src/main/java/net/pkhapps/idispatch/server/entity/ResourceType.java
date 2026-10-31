package net.pkhapps.idispatch.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Strings.nullToEmpty;

/**
 * Entity representing a resource type. The description is in Finnish and Swedish, and at least one of them is required.
 */
@Entity
@Table(name = "resource_types")
public class ResourceType extends AbstractLockableEntity implements Deactivatable {

    public static final String PROP_CODE = "code";
    public static final String PROP_DESCRIPTION_FI = "descriptionFi";
    public static final String PROP_DESCRIPTION_SV = "descriptionSv";

    @Column(name = "code", unique = true, nullable = false)
    @NotBlank(message = "{idispatch.resource-type.code.required}")
    private String code = "";
    @Column(name = "description_fi", nullable = false)
    private String descriptionFi = "";
    @Column(name = "description_sv", nullable = false)
    private String descriptionSv = "";
    @Column(name = "active", nullable = false)
    private boolean active = true;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = nullToEmpty(code);
    }

    public String getDescriptionFi() {
        return descriptionFi;
    }

    public void setDescriptionFi(String descriptionFi) {
        this.descriptionFi = nullToEmpty(descriptionFi);
    }

    public String getDescriptionSv() {
        return descriptionSv;
    }

    public void setDescriptionSv(String descriptionSv) {
        this.descriptionSv = nullToEmpty(descriptionSv);
    }

    /**
     * Returns the description to show to a user with the given locale, as described in {@link Bilingual#select}.
     */
    public String getDescription(@Nullable Locale locale) {
        return Bilingual.select(descriptionFi, descriptionSv, locale);
    }

    public String getFormattedDescription(@Nullable Locale locale) {
        return String.format("%s - %s", code, getDescription(locale));
    }

    @AssertTrue(message = "{idispatch.resource-type.description.required}")
    public boolean isDescriptionGiven() {
        return Bilingual.isGiven(descriptionFi, descriptionSv);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add(PROP_ID, getId())
                .add(PROP_VERSION, getVersion())
                .add(PROP_CODE, code)
                .toString();
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
