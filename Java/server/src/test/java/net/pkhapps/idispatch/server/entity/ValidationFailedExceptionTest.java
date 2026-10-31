package net.pkhapps.idispatch.server.entity;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationFailedExceptionTest {

    @Test
    void violationMessages_areInTheRequestedLanguage() {
        var violations = Validation.buildDefaultValidatorFactory().getValidator().validate(new Municipality());
        var exception = new ValidationFailedException(violations);

        assertThat(exception.getViolationMessages(Locale.ENGLISH)).containsExactly("Please enter a name in Finnish or Swedish");
        assertThat(exception.getViolationMessages(Locale.of("sv"))).containsExactly("Ange ett namn på finska eller svenska");
        assertThat(exception.getViolationMessages(Locale.of("fi"))).containsExactly("Syötä nimi suomeksi tai ruotsiksi");
    }

    @Test
    void englishMessages_doNotDependOnTheServerLanguage() {
        var defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.of("fi"));
        try {
            var violations = Validation.buildDefaultValidatorFactory().getValidator().validate(new Municipality());

            assertThat(new ValidationFailedException(violations).getViolationMessages(Locale.ENGLISH))
                    .containsExactly("Please enter a name in Finnish or Swedish");
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }
}
