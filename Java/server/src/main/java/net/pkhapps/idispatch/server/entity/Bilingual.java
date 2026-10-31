package net.pkhapps.idispatch.server.entity;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

import static com.google.common.base.Strings.nullToEmpty;

/**
 * Picks one of the Finnish and Swedish versions of a text that has both, such as the name of a municipality, when only
 * one of them can be shown.
 */
public final class Bilingual {

    public static final Locale FINNISH = Locale.of("fi");
    public static final Locale SWEDISH = Locale.of("sv");

    private Bilingual() {
    }

    /**
     * Returns the version that has a value if only one does. If both do, returns the Swedish version for a Swedish
     * locale and the Finnish version for any other locale, including {@code null}.
     */
    public static String select(@Nullable String finnish, @Nullable String swedish, @Nullable Locale locale) {
        var hasFinnish = hasText(finnish);
        var hasSwedish = hasText(swedish);
        if (hasFinnish && hasSwedish) {
            return isSwedish(locale) ? swedish : finnish;
        }
        return hasSwedish ? swedish : nullToEmpty(finnish);
    }

    /**
     * Returns whether at least one of the versions has a value.
     */
    public static boolean isGiven(@Nullable String finnish, @Nullable String swedish) {
        return hasText(finnish) || hasText(swedish);
    }

    /**
     * Returns whether the Swedish version is the one to prefer for the given locale.
     */
    public static boolean isSwedish(@Nullable Locale locale) {
        return locale != null && SWEDISH.getLanguage().equals(locale.getLanguage());
    }

    private static boolean hasText(@Nullable String text) {
        return text != null && !text.isBlank();
    }
}
