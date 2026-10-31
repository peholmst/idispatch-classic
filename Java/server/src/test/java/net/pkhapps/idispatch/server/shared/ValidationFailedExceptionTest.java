package net.pkhapps.idispatch.server.shared;

import jakarta.validation.Validation;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationFailedExceptionTest {

    @Test
    void violationMessages_areInTheRequestedLanguage() {
        var violations = Validation.buildDefaultValidatorFactory().getValidator()
                .validateProperty(new Counted(-1), "peopleInvolved");
        var exception = new ValidationFailedException(violations);

        assertThat(exception.getViolationMessages(Locale.ENGLISH)).containsExactly("The number of people involved cannot be negative");
        assertThat(exception.getViolationMessages(Locale.of("sv"))).containsExactly("Antalet inblandade personer kan inte vara negativt");
        assertThat(exception.getViolationMessages(Locale.of("fi"))).containsExactly("Osallisten ihmisten määrä ei voi olla negatiivinen");
    }

    @Test
    void englishMessages_doNotDependOnTheServerLanguage() {
        var defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.of("fi"));
        try {
            var violations = Validation.buildDefaultValidatorFactory().getValidator()
                    .validateProperty(new Counted(-1), "peopleInvolved");

            assertThat(new ValidationFailedException(violations).getViolationMessages(Locale.ENGLISH))
                    .containsExactly("The number of people involved cannot be negative");
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    /**
     * A bean with a constraint whose message is in {@code ValidationMessages}, now that no entity has one.
     */
    record Counted(@Min(value = 0, message = "{idispatch.report.people-involved.min}") int peopleInvolved) {
    }
}
