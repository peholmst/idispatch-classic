package net.pkhapps.idispatch.server.dispatch;

import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.Base64;

import static java.util.Objects.requireNonNull;

/**
 * Where to send a Web Push message to the mobile app of a {@link MobileDestination}, as the browser's push service gave
 * it to the app: the endpoint of the push service, and the app's public key ({@code p256dh}, an uncompressed P-256
 * point) and authentication secret ({@code auth}, 16 bytes), base64url-encoded without padding, that the message is
 * encrypted for.
 */
public record PushSubscription(URI endpoint, String p256dh, String auth) {

    /**
     * The longest endpoint stored. Those of the push services are a few hundred characters.
     */
    public static final int MAX_ENDPOINT_LENGTH = 2048;

    private static final int PUBLIC_KEY_LENGTH = 65;
    private static final int AUTH_LENGTH = 16;

    public PushSubscription {
        requireNonNull(endpoint);
        requireNonNull(p256dh);
        requireNonNull(auth);
        if (!isValid(endpoint.toString(), p256dh, auth)) {
            throw new IllegalArgumentException("Not a valid push subscription");
        }
    }

    /**
     * Returns a subscription from what the app sends, with the keys normalized to base64url without padding.
     *
     * @throws IllegalArgumentException if it is not a valid subscription
     */
    public static PushSubscription of(@Nullable String endpoint, @Nullable String p256dh, @Nullable String auth) {
        if (!isValid(endpoint, p256dh, auth)) {
            throw new IllegalArgumentException("Not a valid push subscription");
        }
        return new PushSubscription(URI.create(endpoint), encode(decode(p256dh)), encode(decode(auth)));
    }

    /**
     * Whether these make a subscription: an absolute HTTPS endpoint, as every push service has, a P-256 public key and
     * a 16-byte secret.
     */
    public static boolean isValid(@Nullable String endpoint, @Nullable String p256dh, @Nullable String auth) {
        if (endpoint == null || p256dh == null || auth == null || endpoint.length() > MAX_ENDPOINT_LENGTH) {
            return false;
        }
        try {
            var uri = new URI(endpoint);
            var publicKey = decode(p256dh);
            return "https".equals(uri.getScheme()) && uri.getHost() != null
                   && publicKey.length == PUBLIC_KEY_LENGTH && publicKey[0] == 0x04
                   && decode(auth).length == AUTH_LENGTH;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * The app's public key, an uncompressed P-256 point.
     */
    public byte[] publicKey() {
        return decode(p256dh);
    }

    /**
     * The app's authentication secret.
     */
    public byte[] authSecret() {
        return decode(auth);
    }

    private static byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private static String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
