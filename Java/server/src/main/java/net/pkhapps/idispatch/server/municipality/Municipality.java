package net.pkhapps.idispatch.server.municipality;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import net.pkhapps.idispatch.server.shared.AbstractLockableEntity;
import net.pkhapps.idispatch.server.shared.Bilingual;
import net.pkhapps.idispatch.server.shared.Deactivatable;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

import static com.google.common.base.Strings.nullToEmpty;

/**
 * Entity representing a municipality. The name is in Finnish and Swedish, and at least one of them is required.
 */
@Entity
@Table(name = "municipalities")
public class Municipality extends AbstractLockableEntity implements Deactivatable {

    public static final String PROP_NAME_FI = "nameFi";
    public static final String PROP_NAME_SV = "nameSv";

    @Column(name = "name_fi", nullable = false)
    private String nameFi = "";
    @Column(name = "name_sv", nullable = false)
    private String nameSv = "";
    @Column(name = "active", nullable = false)
    private boolean active = true;

    public String getNameFi() {
        return nameFi;
    }

    public void setNameFi(String nameFi) {
        this.nameFi = nullToEmpty(nameFi);
    }

    public String getNameSv() {
        return nameSv;
    }

    public void setNameSv(String nameSv) {
        this.nameSv = nullToEmpty(nameSv);
    }

    /**
     * Returns the name to show to a user with the given locale, as described in {@link Bilingual#select}.
     */
    public String getName(@Nullable Locale locale) {
        return Bilingual.select(nameFi, nameSv, locale);
    }

    @AssertTrue(message = "{idispatch.municipality.name.required}")
    public boolean isNameGiven() {
        return Bilingual.isGiven(nameFi, nameSv);
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
