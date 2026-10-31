package net.pkhapps.idispatch.server.dispatch;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Free text that tells destinations apart in the management console. It is never sent to the destination. A
 * destination without one has none, not an empty one.
 */
public record DestinationDescription(String value) {

    public static final int MAX_LENGTH = 255;

    public DestinationDescription {
        requireNonNull(value);
        if (!isValid(value)) {
            throw new IllegalArgumentException("A description must be at most " + MAX_LENGTH + " characters");
        }
    }

    /**
     * Whether the text can be a description, or be left out. A blank text means no description.
     */
    public static boolean isValid(@Nullable String value) {
        return value == null || value.length() <= MAX_LENGTH;
    }

    /**
     * Returns the description of the text, or {@code null} if the text is blank.
     */
    public static @Nullable DestinationDescription of(@Nullable String value) {
        return value == null || value.isBlank() ? null : new DestinationDescription(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
