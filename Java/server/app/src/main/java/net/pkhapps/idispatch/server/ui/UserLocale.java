package net.pkhapps.idispatch.server.ui;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The language of the user interface. Users choose it in their Keycloak account, which puts it in the {@code locale}
 * claim of the ID token. The application reads the claim when the user opens it, so a user who changes their language
 * sees it after logging out and back in. Users without a supported language get {@link #DEFAULT}.
 */
final class UserLocale {

    public static final Locale ENGLISH = Locale.ENGLISH;
    public static final Locale SWEDISH = Locale.of("sv");
    public static final Locale FINNISH = Locale.of("fi");

    /**
     * The languages the user interface is translated to, with the default first.
     */
    public static final List<Locale> SUPPORTED = List.of(ENGLISH, SWEDISH, FINNISH);

    public static final Locale DEFAULT = ENGLISH;

    private UserLocale() {
    }

    /**
     * Returns the language of the current user.
     */
    public static Locale current() {
        return of(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Returns the language from the ID token of the given user, or {@link #DEFAULT} if the token has no supported
     * language, or the user did not log in through Keycloak.
     */
    public static Locale of(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getPrincipal)
                .filter(OidcUser.class::isInstance)
                .map(OidcUser.class::cast)
                .map(OidcUser::getLocale)
                .flatMap(UserLocale::supported)
                .orElse(DEFAULT);
    }

    /**
     * Returns the supported language that matches the given language tag, such as {@code fi} or {@code sv-FI}.
     */
    static Optional<Locale> supported(String languageTag) {
        if (languageTag == null || languageTag.isBlank()) {
            return Optional.empty();
        }
        var language = Locale.forLanguageTag(languageTag.trim().replace('_', '-')).getLanguage();
        return SUPPORTED.stream().filter(locale -> locale.getLanguage().equals(language)).findFirst();
    }
}
