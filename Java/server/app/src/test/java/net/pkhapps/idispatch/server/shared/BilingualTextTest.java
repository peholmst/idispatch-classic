package net.pkhapps.idispatch.server.shared;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BilingualTextTest {

    @Test
    void eitherVersionIsEnough() {
        assertThat(new BilingualText("Pelastusyksikkö", null).swedish()).isEmpty();
        assertThat(new BilingualText(null, "Räddningsenhet").finnish()).isEmpty();
    }

    @Test
    void textWithNeitherVersionCannotBeMade() {
        assertThat(BilingualText.isValid(" ", "")).isFalse();
        assertThatThrownBy(() -> new BilingualText(" ", "")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void selectsTheVersionForTheLocale() {
        var text = new BilingualText("Pelastusyksikkö", "Räddningsenhet");

        assertThat(text.select(Bilingual.SWEDISH)).isEqualTo("Räddningsenhet");
        assertThat(text.select(Bilingual.FINNISH)).isEqualTo("Pelastusyksikkö");
    }
}
