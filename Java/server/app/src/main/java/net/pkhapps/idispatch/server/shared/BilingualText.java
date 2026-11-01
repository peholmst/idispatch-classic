package net.pkhapps.idispatch.server.shared;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

import static com.google.common.base.Strings.nullToEmpty;

/**
 * A text in Finnish and Swedish, such as a description, of which at least one version is given. A version that is not
 * given is empty.
 */
public record BilingualText(String finnish, String swedish) {

    public BilingualText {
        finnish = nullToEmpty(finnish);
        swedish = nullToEmpty(swedish);
        if (!isValid(finnish, swedish)) {
            throw new IllegalArgumentException("Neither the Finnish nor the Swedish version is given");
        }
    }

    /**
     * Returns whether the versions make a bilingual text, which is when at least one of them is given.
     */
    public static boolean isValid(@Nullable String finnish, @Nullable String swedish) {
        return Bilingual.isGiven(finnish, swedish);
    }

    /**
     * Returns the version to show to a user with the given locale, as described in {@link Bilingual#select}.
     */
    public String select(@Nullable Locale locale) {
        return Bilingual.select(finnish, swedish, locale);
    }
}
