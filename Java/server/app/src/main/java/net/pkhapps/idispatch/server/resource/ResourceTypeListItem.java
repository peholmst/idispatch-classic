package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.shared.Bilingual;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * A resource type as it is listed in grids and offered in combo boxes, flattened to what they show.
 */
record ResourceTypeListItem(ResourceTypeId id, String code, String descriptionFi, String descriptionSv,
                                   boolean active) {

    /**
     * Returns the description to show to a user with the given locale, as described in {@link Bilingual#select}.
     */
    public String description(@Nullable Locale locale) {
        return Bilingual.select(descriptionFi, descriptionSv, locale);
    }
}
