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

        assertThat(exception.getViolationMessages(Locale.ENGLISH)).containsExactly("Please enter a name");
        assertThat(exception.getViolationMessages(Locale.of("sv"))).containsExactly("Ange ett namn");
        assertThat(exception.getViolationMessages(Locale.of("fi"))).containsExactly("Syötä nimi");
    }

    @Test
    void englishMessages_doNotDependOnTheServerLanguage() {
        var defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.of("fi"));
        try {
            var violations = Validation.buildDefaultValidatorFactory().getValidator().validate(new Municipality());

            assertThat(new ValidationFailedException(violations).getViolationMessages(Locale.ENGLISH))
                    .containsExactly("Please enter a name");
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }
}
