package net.pkhapps.idispatch.server.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.metadata.ConstraintDescriptor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class ValidationFailedException extends Exception {

    private final Set<ConstraintViolation<?>> violations;

    public ValidationFailedException(Set<? extends ConstraintViolation<?>> violations) {
        this.violations = Collections.unmodifiableSet(new HashSet<>(violations));
    }

    public static void throwIfNotEmpty(Set<? extends ConstraintViolation<?>> violations) throws ValidationFailedException {
        if (violations.size() > 0) {
            throw new ValidationFailedException(violations);
        }
    }

    public Set<ConstraintViolation<?>> getViolations() {
        return violations;
    }

    /**
     * Returns the messages of the violations in the given language. The messages are in
     * {@code ValidationMessages*.properties}, and are looked up again here, since the validator used the server's
     * language.
     */
    public Set<String> getViolationMessages(Locale locale) {
        return getViolations().stream()
                .map(violation -> Interpolator.INSTANCE.interpolate(violation.getMessageTemplate(),
                        new ViolationContext(violation), locale))
                .collect(Collectors.toSet());
    }

    private static final class Interpolator {
        private static final MessageInterpolator INSTANCE = Validation.byDefaultProvider().configure()
                .getDefaultMessageInterpolator();
    }

    private record ViolationContext(ConstraintViolation<?> violation) implements MessageInterpolator.Context {

        @Override
        public ConstraintDescriptor<?> getConstraintDescriptor() {
            return violation.getConstraintDescriptor();
        }

        @Override
        public Object getValidatedValue() {
            return violation.getInvalidValue();
        }

        @Override
        public <T> T unwrap(Class<T> type) {
            throw new ValidationException("Cannot unwrap to " + type.getName());
        }
    }
}
