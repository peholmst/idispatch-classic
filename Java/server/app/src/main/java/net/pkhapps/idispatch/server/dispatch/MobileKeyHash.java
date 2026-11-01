package net.pkhapps.idispatch.server.dispatch;

import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * The SHA-256 hash of a {@link MobileKey}, in lowercase hex, as stored. Unique among the mobile destinations, which the
 * database checks.
 */
public record MobileKeyHash(String value) {

    private static final Pattern FORMAT = Pattern.compile("[0-9a-f]{64}");

    public MobileKeyHash {
        requireNonNull(value);
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("A mobile key hash is 64 lowercase hex digits");
        }
    }
}
