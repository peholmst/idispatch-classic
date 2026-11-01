package net.pkhapps.idispatch.server.dispatch;

import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * The key that the mobile app of a {@link MobileDestination} authenticates with: 256 random bits, base64url-encoded
 * without padding. Only its {@link #hash() hash} is stored, so the key itself is known only when it is generated. Never
 * logged, which is why {@link #toString()} hides it.
 */
public record MobileKey(String value) {

    private static final Pattern FORMAT = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final SecureRandom RANDOM = new SecureRandom();

    public MobileKey {
        requireNonNull(value);
        if (!isValid(value)) {
            throw new IllegalArgumentException("A mobile key is 43 base64url characters");
        }
    }

    public static boolean isValid(@Nullable String value) {
        return value != null && FORMAT.matcher(value).matches();
    }

    /**
     * Returns a new random key.
     */
    public static MobileKey random() {
        var bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return new MobileKey(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }

    /**
     * Returns the hash that is stored for this key. The key is random and long, so a plain SHA-256 is enough, and lets
     * the destination be looked up by the hash of the key an app presents.
     */
    public MobileKeyHash hash() {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII));
            return new MobileKeyHash(HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Every Java platform has SHA-256", ex);
        }
    }

    @Override
    public String toString() {
        return "MobileKey[hidden]";
    }
}
