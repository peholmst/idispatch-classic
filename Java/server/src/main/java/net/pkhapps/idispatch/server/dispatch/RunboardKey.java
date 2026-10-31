package net.pkhapps.idispatch.server.dispatch;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * The key that a runboard connects with, which tells which {@link RunboardDestination} it is. Unique among the
 * runboards, which the database checks.
 */
public record RunboardKey(String value) {

    public RunboardKey {
        requireNonNull(value);
        if (!isValid(value)) {
            throw new IllegalArgumentException("A runboard key cannot be blank");
        }
    }

    public static boolean isValid(@Nullable String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Returns a new key that no other runboard has.
     */
    public static RunboardKey random() {
        return new RunboardKey(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
