package net.pkhapps.idispatch.server.ui;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserLocaleTest {

    @Test
    void supportedLanguageInIdToken_isUsed() {
        assertThat(UserLocale.of(withIdTokenClaims(Map.of("locale", "fi")))).isEqualTo(UserLocale.FINNISH);
        assertThat(UserLocale.of(withIdTokenClaims(Map.of("locale", "sv")))).isEqualTo(UserLocale.SWEDISH);
        assertThat(UserLocale.of(withIdTokenClaims(Map.of("locale", "en")))).isEqualTo(UserLocale.ENGLISH);
    }

    @Test
    void regionalVariant_usesItsLanguage() {
        assertThat(UserLocale.of(withIdTokenClaims(Map.of("locale", "sv-FI")))).isEqualTo(UserLocale.SWEDISH);
        assertThat(UserLocale.of(withIdTokenClaims(Map.of("locale", "fi_FI")))).isEqualTo(UserLocale.FINNISH);
    }

    @Test
    void unsupportedOrMissingLanguage_fallsBackToEnglish() {
        assertThat(UserLocale.of(withIdTokenClaims(Map.of("locale", "de")))).isEqualTo(UserLocale.ENGLISH);
        assertThat(UserLocale.of(withIdTokenClaims(Map.of()))).isEqualTo(UserLocale.ENGLISH);
    }

    @Test
    void userWithoutIdToken_getsEnglish() {
        assertThat(UserLocale.of(null)).isEqualTo(UserLocale.ENGLISH);
        assertThat(UserLocale.of(new UsernamePasswordAuthenticationToken("user", "password", List.of())))
                .isEqualTo(UserLocale.ENGLISH);
    }

    private static OAuth2AuthenticationToken withIdTokenClaims(Map<String, Object> claims) {
        var idToken = OidcIdToken.withTokenValue("id-token")
                .issuer("http://keycloak.invalid/realms/idispatch")
                .subject("f2b4c1d0")
                .claim("preferred_username", "oidc-user")
                .claims(c -> c.putAll(claims))
                .build();
        var user = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), idToken, "preferred_username");
        return new OAuth2AuthenticationToken(user, user.getAuthorities(), "keycloak");
    }
}
