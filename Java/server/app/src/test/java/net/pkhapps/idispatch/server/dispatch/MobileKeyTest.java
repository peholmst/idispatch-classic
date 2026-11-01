package net.pkhapps.idispatch.server.dispatch;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MobileKeyTest {

    @Test
    void randomKeysAreValidAndDiffer() {
        var first = MobileKey.random();
        var second = MobileKey.random();

        assertThat(MobileKey.isValid(first.value())).isTrue();
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void hashIsTheSha256OfTheKeyInLowercaseHex() {
        var key = new MobileKey("A".repeat(43));

        assertThat(key.hash())
                .isEqualTo(new MobileKeyHash("0f007385b6f9d4b7eeb2748605afe1a984a0a3bfa3f014d09e2a784ce9e5cd1a"));
    }

    @Test
    void keyIsNeverPrinted() {
        var key = MobileKey.random();

        assertThat(key.toString()).doesNotContain(key.value());
    }

    @Test
    void keysOfTheWrongFormatAreRefused() {
        assertThat(MobileKey.isValid("too-short")).isFalse();
        assertThat(MobileKey.isValid("A".repeat(42) + "=")).isFalse();
        assertThatThrownBy(() -> new MobileKey("")).isInstanceOf(IllegalArgumentException.class);
    }
}
