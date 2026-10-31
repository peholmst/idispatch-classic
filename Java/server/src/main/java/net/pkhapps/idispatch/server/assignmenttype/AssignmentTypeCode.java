package net.pkhapps.idispatch.server.assignmenttype;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * The short code of an {@link AssignmentType}, such as {@code 401}, shown in front of its description and in the
 * dispatch notifications. Unique among the assignment types, which the database checks.
 */
record AssignmentTypeCode(String value) {

    public AssignmentTypeCode {
        requireNonNull(value);
        if (!isValid(value)) {
            throw new IllegalArgumentException("An assignment type code cannot be blank");
        }
    }

    public static boolean isValid(@Nullable String value) {
        return value != null && !value.isBlank();
    }

    @Override
    public String toString() {
        return value;
    }
}
