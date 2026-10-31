package net.pkhapps.idispatch.server.report;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Free text in a report, such as how the situation developed. It may be empty, but not longer than the database
 * column.
 */
public record ReportText(String value) {

    public static final int MAX_LENGTH = 4000;
    public static final ReportText EMPTY = new ReportText("");

    public ReportText {
        requireNonNull(value);
        if (!isValid(value)) {
            throw new IllegalArgumentException("A report text can be at most " + MAX_LENGTH + " characters");
        }
    }

    public static boolean isValid(@Nullable String value) {
        return value != null && value.length() <= MAX_LENGTH;
    }

    public boolean isBlank() {
        return value.isBlank();
    }

    @Override
    public String toString() {
        return value;
    }
}
