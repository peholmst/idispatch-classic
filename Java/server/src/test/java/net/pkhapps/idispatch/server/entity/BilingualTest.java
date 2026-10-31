package net.pkhapps.idispatch.server.entity;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class BilingualTest {

    @Test
    void onlyFinnish_isShownInEveryLocale() {
        for (var locale : new Locale[]{Bilingual.FINNISH, Bilingual.SWEDISH, Locale.ENGLISH, null}) {
            assertThat(Bilingual.select("Pargas", "", locale)).isEqualTo("Pargas");
        }
    }

    @Test
    void onlySwedish_isShownInEveryLocale() {
        for (var locale : new Locale[]{Bilingual.FINNISH, Bilingual.SWEDISH, Locale.ENGLISH, null}) {
            assertThat(Bilingual.select(" ", "Pargas", locale)).isEqualTo("Pargas");
        }
    }

    @Test
    void both_followTheLocaleAndDefaultToFinnish() {
        assertThat(Bilingual.select("Parainen", "Pargas", Bilingual.FINNISH)).isEqualTo("Parainen");
        assertThat(Bilingual.select("Parainen", "Pargas", Bilingual.SWEDISH)).isEqualTo("Pargas");
        assertThat(Bilingual.select("Parainen", "Pargas", Locale.forLanguageTag("sv-FI"))).isEqualTo("Pargas");
        assertThat(Bilingual.select("Parainen", "Pargas", Locale.ENGLISH)).isEqualTo("Parainen");
        assertThat(Bilingual.select("Parainen", "Pargas", null)).isEqualTo("Parainen");
    }

    @Test
    void neither_isEmptyAndNotGiven() {
        assertThat(Bilingual.select(null, "", Bilingual.SWEDISH)).isEmpty();
        assertThat(Bilingual.isGiven(null, " ")).isFalse();
        assertThat(Bilingual.isGiven("", "Pargas")).isTrue();
    }
}
