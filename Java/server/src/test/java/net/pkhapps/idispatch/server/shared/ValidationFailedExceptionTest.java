package net.pkhapps.idispatch.server.shared;

import jakarta.validation.Validation;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.report.AssignmentReport;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationFailedExceptionTest {

    @Test
    void violationMessages_areInTheRequestedLanguage() {
        var violations = Validation.buildDefaultValidatorFactory().getValidator()
                .validateProperty(reportWithNegativePeopleInvolved(), "peopleInvolved");
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
                    .validateProperty(reportWithNegativePeopleInvolved(), "peopleInvolved");

            assertThat(new ValidationFailedException(violations).getViolationMessages(Locale.ENGLISH))
                    .containsExactly("The number of people involved cannot be negative");
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    private static AssignmentReport reportWithNegativePeopleInvolved() {
        var report = new AssignmentReport(new AssignmentId(1));
        report.setPeopleInvolved(-1);
        return report;
    }
}
