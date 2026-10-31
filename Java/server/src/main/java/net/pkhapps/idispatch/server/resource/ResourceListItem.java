package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.shared.Bilingual;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * A resource as it is listed in grids and offered in combo boxes, flattened to what they show. The type's description
 * is the one it has now, in both languages.
 */
public record ResourceListItem(ResourceId id, String callSign, String typeDescriptionFi, String typeDescriptionSv,
                               boolean active) {

    /**
     * Returns the description of the type to show to a user with the given locale, as described in
     * {@link Bilingual#select}.
     */
    public String typeDescription(@Nullable Locale locale) {
        return Bilingual.select(typeDescriptionFi, typeDescriptionSv, locale);
    }
}
