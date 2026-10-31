package net.pkhapps.idispatch.server.assignment;

import net.pkhapps.idispatch.server.shared.Bilingual;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Locale;

/**
 * An assignment as it is listed, flattened to what the lists show. The type and the municipality are those it has now,
 * in both languages, and are {@code null} when it has none.
 */
public record AssignmentListItem(AssignmentId id, Instant opened, @Nullable Instant closed, AssignmentUrgency urgency,
                                 @Nullable String typeCode, @Nullable String typeDescriptionFi,
                                 @Nullable String typeDescriptionSv, @Nullable String municipalityNameFi,
                                 @Nullable String municipalityNameSv, String address) {

    /**
     * Returns the description of the type to show to a user with the given locale, or {@code null} if it has no type.
     */
    public @Nullable String typeDescription(@Nullable Locale locale) {
        return typeCode == null ? null : Bilingual.select(typeDescriptionFi, typeDescriptionSv, locale);
    }

    /**
     * Returns the name of the municipality to show to a user with the given locale, or {@code null} if it has none.
     */
    public @Nullable String municipalityName(@Nullable Locale locale) {
        return municipalityNameFi == null && municipalityNameSv == null ? null
                : Bilingual.select(municipalityNameFi, municipalityNameSv, locale);
    }
}
