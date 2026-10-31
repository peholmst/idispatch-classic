package net.pkhapps.idispatch.server.resource;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * The call sign of a {@link Resource}, such as {@code PAR101}, by which dispatchers and the notifications name it.
 * Unique among the resources, which the database checks.
 */
record CallSign(String value) {

    public CallSign {
        requireNonNull(value);
        if (!isValid(value)) {
            throw new IllegalArgumentException("A call sign cannot be blank");
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
