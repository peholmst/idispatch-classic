package net.pkhapps.idispatch.server.assignmenttype;

import net.pkhapps.idispatch.server.shared.Bilingual;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * An assignment type as it is listed in grids and offered in combo boxes, flattened to what they show.
 */
public record AssignmentTypeListItem(AssignmentTypeId id, String code, String descriptionFi, String descriptionSv,
                                   boolean active) {

    /**
     * Returns the description to show to a user with the given locale, as described in {@link Bilingual#select}.
     */
    public String description(@Nullable Locale locale) {
        return Bilingual.select(descriptionFi, descriptionSv, locale);
    }
}
