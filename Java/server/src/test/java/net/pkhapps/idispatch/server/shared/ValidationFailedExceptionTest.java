package net.pkhapps.idispatch.server.shared;

import jakarta.validation.Validation;
import net.pkhapps.idispatch.server.dispatch.Destination;
import net.pkhapps.idispatch.server.dispatch.runboard.RunboardDestination;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationFailedExceptionTest {

    @Test
    void violationMessages_areInTheRequestedLanguage() {
        var violations = Validation.buildDefaultValidatorFactory().getValidator()
                .validateProperty(new RunboardDestination(), Destination.PROP_RESOURCES);
        var exception = new ValidationFailedException(violations);

        assertThat(exception.getViolationMessages(Locale.ENGLISH)).containsExactly("Please select at least one resource");
        assertThat(exception.getViolationMessages(Locale.of("sv"))).containsExactly("Välj minst en enhet");
        assertThat(exception.getViolationMessages(Locale.of("fi"))).containsExactly("Valitse vähintään yksi yksikkö");
    }

    @Test
    void englishMessages_doNotDependOnTheServerLanguage() {
        var defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.of("fi"));
        try {
            var violations = Validation.buildDefaultValidatorFactory().getValidator()
                    .validateProperty(new RunboardDestination(), Destination.PROP_RESOURCES);

            assertThat(new ValidationFailedException(violations).getViolationMessages(Locale.ENGLISH))
                    .containsExactly("Please select at least one resource");
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }
}
