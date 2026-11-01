package net.pkhapps.idispatch.server.municipality;

import net.pkhapps.idispatch.server.shared.Bilingual;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * A municipality as it is listed in grids and offered in combo boxes, flattened to what they show.
 */
public record MunicipalityListItem(MunicipalityId id, String nameFi, String nameSv, boolean active) {

    /**
     * Returns the name to show to a user with the given locale, as described in {@link Bilingual#select}.
     */
    public String name(@Nullable Locale locale) {
        return Bilingual.select(nameFi, nameSv, locale);
    }
}
