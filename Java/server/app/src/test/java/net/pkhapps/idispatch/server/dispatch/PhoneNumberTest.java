package net.pkhapps.idispatch.server.dispatch;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * How the phone numbers an administrator types for an SMS destination are read.
 */
class PhoneNumberTest {

    @Test
    void numbersAreSeparatedByCommasAndLineBreaks() {
        assertThat(PhoneNumber.parse("+3584001, +3584002\n+3584003"))
                .containsExactly(new PhoneNumber("+3584001"), new PhoneNumber("+3584002"), new PhoneNumber("+3584003"));
    }

    @Test
    void spacesAndDashesInANumberDoNotMatter() {
        assertThat(PhoneNumber.parse("+358 40-123 4567")).containsExactly(new PhoneNumber("+358401234567"));
    }

    @Test
    void emptyPartsAreLeftOut() {
        assertThat(PhoneNumber.parse(",\n+3584001,,")).containsExactly(new PhoneNumber("+3584001"));
        assertThat(PhoneNumber.parse("no numbers here")).isEmpty();
        assertThat(PhoneNumber.parse(null)).isEmpty();
    }

    @Test
    void phoneNumberIsDigitsAfterAnOptionalPlus() {
        assertThat(PhoneNumber.isValid("0401234567")).isTrue();
        assertThatThrownBy(() -> new PhoneNumber("+")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PhoneNumber("040 123")).isInstanceOf(IllegalArgumentException.class);
    }
}
