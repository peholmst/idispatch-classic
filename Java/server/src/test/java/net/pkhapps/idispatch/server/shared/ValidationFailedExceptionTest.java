package net.pkhapps.idispatch.server.shared;

import jakarta.validation.Validation;
import net.pkhapps.idispatch.server.resource.Resource;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationFailedExceptionTest {

    @Test
    void violationMessages_areInTheRequestedLanguage() {
        var violations = Validation.buildDefaultValidatorFactory().getValidator()
                .validateProperty(new Resource(), Resource.PROP_CALL_SIGN);
        var exception = new ValidationFailedException(violations);

        assertThat(exception.getViolationMessages(Locale.ENGLISH)).containsExactly("Please enter a call sign");
        assertThat(exception.getViolationMessages(Locale.of("sv"))).containsExactly("Ange ett anropsnamn");
        assertThat(exception.getViolationMessages(Locale.of("fi"))).containsExactly("Syötä tunnus");
    }

    @Test
    void englishMessages_doNotDependOnTheServerLanguage() {
        var defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.of("fi"));
        try {
            var violations = Validation.buildDefaultValidatorFactory().getValidator()
                    .validateProperty(new Resource(), Resource.PROP_CALL_SIGN);

            assertThat(new ValidationFailedException(violations).getViolationMessages(Locale.ENGLISH))
                    .containsExactly("Please enter a call sign");
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }
}
