package net.pkhapps.idispatch.server.resource;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * The short code of a {@link ResourceType}, such as {@code 103}, shown in front of its description. Unique among the
 * resource types, which the database checks.
 */
public record ResourceTypeCode(String value) {

    public ResourceTypeCode {
        requireNonNull(value);
        if (!isValid(value)) {
            throw new IllegalArgumentException("A resource type code cannot be blank");
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
