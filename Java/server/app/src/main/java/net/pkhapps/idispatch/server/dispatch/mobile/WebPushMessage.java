package net.pkhapps.idispatch.server.dispatch.mobile;

import net.pkhapps.idispatch.server.dispatch.PushSubscription;
import org.jspecify.annotations.Nullable;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

/**
 * A Web Push message, ready to be posted to the push service: its payload encrypted for the subscription with
 * {@code aes128gcm} (RFC 8291, RFC 8188), and signed with the server's VAPID key (RFC 8292).
 *
 * @param endpoint the push service's endpoint of the subscription
 * @param headers  the headers to post it with
 * @param body     the encrypted payload
 */
record WebPushMessage(URI endpoint, Map<String, String> headers, byte[] body) {

    /**
     * The largest payload that fits a push message: push services take at most 4096 bytes of body, of which the
     * {@code aes128gcm} header takes 86 bytes, the authentication tag 16 and the padding delimiter 1.
     */
    static final int MAX_PAYLOAD = 4096 - 86 - 16 - 1;

    /**
     * How long a VAPID token is valid; at most 24 hours (RFC 8292).
     */
    private static final Duration TOKEN_VALIDITY = Duration.ofHours(12);

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Returns the message that delivers the payload to the subscription with high urgency, which wakes the device.
     *
     * @param subject    the contact of the server's operator, a {@code mailto:} or {@code https:} URL, which the push
     *                   service may use if the server misbehaves; {@code null} for none
     * @param timeToLive how long the push service keeps trying to deliver it
     */
    static WebPushMessage of(PushSubscription subscription, byte[] payload, VapidKeys vapid, @Nullable String subject,
                             Duration timeToLive, Instant now) {
        var salt = new byte[16];
        RANDOM.nextBytes(salt);
        var body = encrypt(payload, subscription.publicKey(), subscription.authSecret(), VapidKeys.generateKeyPair(),
                salt);
        return new WebPushMessage(subscription.endpoint(), Map.of(
                "TTL", String.valueOf(timeToLive.toSeconds()),
                "Urgency", "high",
                "Content-Encoding", "aes128gcm",
                "Content-Type", "application/octet-stream",
                "Authorization", "vapid t=" + token(subscription.endpoint(), vapid, subject, now) + ", k="
                                 + vapid.publicKeyBase64()), body);
    }

    /**
     * Encrypts the payload for the app's public key and secret (RFC 8291) as one {@code aes128gcm} record.
     *
     * @param serverKeys a key pair of the server's own, used only for this message
     * @param salt       16 random bytes
     */
    static byte[] encrypt(byte[] payload, byte[] appPublicKey, byte[] authSecret, KeyPair serverKeys, byte[] salt) {
        if (payload.length > MAX_PAYLOAD) {
            throw new IllegalArgumentException("A push payload is at most " + MAX_PAYLOAD + " bytes");
        }
        try {
            var serverPublicKey = VapidKeys.rawPublicKey((ECPublicKey) serverKeys.getPublic());
            var agreement = KeyAgreement.getInstance("ECDH");
            agreement.init(serverKeys.getPrivate());
            agreement.doPhase(VapidKeys.publicKeyOf(appPublicKey), true);
            var sharedSecret = agreement.generateSecret();

            var keyInfo = concat(ascii("WebPush: info\0"), appPublicKey, serverPublicKey);
            var ikm = hkdf(authSecret, sharedSecret, keyInfo, 32);
            var contentKey = hkdf(salt, ikm, ascii("Content-Encoding: aes128gcm\0"), 16);
            var nonce = hkdf(salt, ikm, ascii("Content-Encoding: nonce\0"), 12);

            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(contentKey, "AES"), new GCMParameterSpec(128, nonce));
            // The delimiter 2 marks the last record, and no padding follows it
            var ciphertext = cipher.doFinal(concat(payload, new byte[]{2}));

            var header = ByteBuffer.allocate(16 + 4 + 1 + serverPublicKey.length)
                    .put(salt)
                    .putInt(4096)
                    .put((byte) serverPublicKey.length)
                    .put(serverPublicKey)
                    .array();
            return concat(header, ciphertext);
        } catch (GeneralSecurityException ex) {
            throw new IllegalArgumentException("Could not encrypt for the push subscription", ex);
        }
    }

    /**
     * The VAPID token for the push service of the endpoint: a JWT signed with ES256.
     */
    static String token(URI endpoint, VapidKeys vapid, @Nullable String subject, Instant now) {
        var audience = endpoint.getScheme() + "://" + endpoint.getRawAuthority();
        var claims = "{\"aud\":\"" + audience + "\",\"exp\":" + now.plus(TOKEN_VALIDITY).getEpochSecond()
                     + (subject == null ? "" : ",\"sub\":\"" + subject.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                     + "}";
        var unsigned = VapidKeys.encode(ascii("{\"typ\":\"JWT\",\"alg\":\"ES256\"}")) + "."
                       + VapidKeys.encode(claims.getBytes(StandardCharsets.UTF_8));
        try {
            // ES256 signatures in a JWT are r and s side by side, not DER
            var signature = Signature.getInstance("SHA256withECDSAinP1363Format");
            signature.initSign(vapid.privateKey());
            signature.update(ascii(unsigned));
            return unsigned + "." + VapidKeys.encode(signature.sign());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Could not sign the VAPID token", ex);
        }
    }

    /**
     * HKDF with SHA-256 (RFC 5869), for at most 32 bytes of output.
     */
    private static byte[] hkdf(byte[] salt, byte[] ikm, byte[] info, int length) throws GeneralSecurityException {
        var prk = hmac(salt, ikm);
        return Arrays.copyOf(hmac(prk, concat(info, new byte[]{1})), length);
    }

    private static byte[] hmac(byte[] key, byte[] data) throws GeneralSecurityException {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static byte[] ascii(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] concat(byte[]... parts) {
        var out = new ByteArrayOutputStream();
        for (var part : parts) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }
}
