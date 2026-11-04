package net.pkhapps.idispatch.server.dispatch.mobile;

import net.pkhapps.idispatch.server.dispatch.PushSubscription;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import static net.pkhapps.idispatch.server.dispatch.mobile.VapidKeys.decode;
import static net.pkhapps.idispatch.server.dispatch.mobile.VapidKeys.encode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A push message is encrypted the way browsers decrypt it (RFC 8291) and signed the way push services check it
 * (RFC 8292).
 */
class WebPushMessageTest {

    /**
     * The example of RFC 8291, appendix A.
     */
    @Test
    void encryptsTheExampleOfTheStandard() throws Exception {
        var serverKeys = new KeyPair(
                VapidKeys.publicKeyOf(decode("BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8")),
                VapidKeys.privateKeyOf(decode("yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw")));

        var body = WebPushMessage.encrypt("When I grow up, I want to be a watermelon".getBytes(StandardCharsets.UTF_8),
                decode("BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4"),
                decode("BTBZMqHH6r4Tts7J_aSIgg"), serverKeys, decode("DGv6ra1nlYgDCS1FRnbzlw"));

        assertThat(encode(body)).isEqualTo("DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPTpK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN");
    }

    @Test
    void appDecryptsTheMessage_whichIsSentWithHighUrgency() throws Exception {
        var app = App.create();
        var vapid = VapidKeys.generate();

        var message = WebPushMessage.of(app.subscription(), "{\"id\":1}".getBytes(StandardCharsets.UTF_8), vapid,
                "mailto:admin@example.com", Duration.ofHours(1), Instant.now());

        assertThat(message.endpoint()).isEqualTo(app.subscription().endpoint());
        assertThat(new String(app.decrypt(message.body()), StandardCharsets.UTF_8)).isEqualTo("{\"id\":1}");
        assertThat(message.headers())
                .containsEntry("Urgency", "high")
                .containsEntry("TTL", "3600")
                .containsEntry("Content-Encoding", "aes128gcm");
    }

    @Test
    void pushServiceCanCheckTheSignature_forItsOwnOrigin() throws Exception {
        var vapid = VapidKeys.generate();
        var now = Instant.parse("2026-11-04T12:00:00Z");

        var token = WebPushMessage.token(URI.create("https://fcm.googleapis.com/fcm/send/abc"), vapid,
                "mailto:admin@example.com", now);

        var parts = token.split("\\.");
        assertThat(new String(decode(parts[0]), StandardCharsets.UTF_8)).contains("\"alg\":\"ES256\"");
        assertThat(new String(decode(parts[1]), StandardCharsets.UTF_8)).isEqualTo(
                "{\"aud\":\"https://fcm.googleapis.com\",\"exp\":" + now.plus(Duration.ofHours(12)).getEpochSecond()
                + ",\"sub\":\"mailto:admin@example.com\"}");
        var verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
        verifier.initVerify(vapid.publicKey());
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        assertThat(verifier.verify(decode(parts[2]))).isTrue();
    }

    @Test
    void authorizationNamesTheServersPublicKey() throws Exception {
        var vapid = VapidKeys.generate();

        var message = WebPushMessage.of(App.create().subscription(), new byte[1], vapid, null, Duration.ofHours(1),
                Instant.now());

        assertThat(message.headers().get("Authorization"))
                .startsWith("vapid t=")
                .endsWith(", k=" + vapid.publicKeyBase64());
    }

    @Test
    void payloadThatDoesNotFitIsRefused() throws Exception {
        var app = App.create();

        assertThatThrownBy(() -> WebPushMessage.of(app.subscription(), new byte[WebPushMessage.MAX_PAYLOAD + 1],
                VapidKeys.generate(), null, Duration.ofHours(1), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(WebPushMessage.of(app.subscription(), new byte[WebPushMessage.MAX_PAYLOAD], VapidKeys.generate(),
                null, Duration.ofHours(1), Instant.now()).body()).hasSize(4096);
    }

    @Test
    void keysWrittenByOpenSslAreRead_asTheDeploymentDocsTellToMakeThem() {
        var keys = VapidKeys.of("BMqJtpGzQVU3IkBBkjYR6ph-Ef6c_z0Ivdhvp21wQSJd2-YLW4SWghn-GSebt5a_0t9M7dKd1zU2fJy8oWobvqE",
                "6YHQ81O_4YW2T0_K4aqq3RoqJ9KcsjP0Z652m52QvFA");

        assertThat(keys.publicKeyBase64())
                .isEqualTo("BMqJtpGzQVU3IkBBkjYR6ph-Ef6c_z0Ivdhvp21wQSJd2-YLW4SWghn-GSebt5a_0t9M7dKd1zU2fJy8oWobvqE");
        assertThat(keys.privateKeyBase64()).isEqualTo("6YHQ81O_4YW2T0_K4aqq3RoqJ9KcsjP0Z652m52QvFA");
    }

    @Test
    void keysThatAreNotAPairAreRefused() {
        var one = VapidKeys.generate();
        var other = VapidKeys.generate();

        assertThatThrownBy(() -> VapidKeys.of(one.publicKeyBase64(), other.privateKeyBase64()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VapidKeys.of("not a key", one.privateKeyBase64()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * The receiving end, as a browser decrypts a push message (RFC 8291).
     */
    record App(PushSubscription subscription, ECPrivateKey privateKey) {

        static App create() {
            var keys = VapidKeys.generateKeyPair();
            var auth = new byte[16];
            Arrays.fill(auth, (byte) 7);
            var subscription = PushSubscription.of("https://push.example.com/send/" + System.nanoTime(),
                    encode(VapidKeys.rawPublicKey((java.security.interfaces.ECPublicKey) keys.getPublic())),
                    encode(auth));
            return new App(subscription, (ECPrivateKey) keys.getPrivate());
        }

        byte[] decrypt(byte[] body) throws GeneralSecurityException {
            var buffer = ByteBuffer.wrap(body);
            var salt = new byte[16];
            buffer.get(salt);
            assertThat(buffer.getInt()).as("Record size").isEqualTo(4096);
            var serverPublicKey = new byte[buffer.get()];
            buffer.get(serverPublicKey);
            var ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            var agreement = KeyAgreement.getInstance("ECDH");
            agreement.init(privateKey);
            agreement.doPhase(VapidKeys.publicKeyOf(serverPublicKey), true);
            var ikm = hkdf(subscription.authSecret(), agreement.generateSecret(),
                    concat("WebPush: info\0".getBytes(StandardCharsets.US_ASCII), subscription.publicKey(),
                            serverPublicKey), 32);
            var key = hkdf(salt, ikm, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
            var nonce = hkdf(salt, ikm, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
            var padded = cipher.doFinal(ciphertext);
            assertThat(padded[padded.length - 1]).as("Last record").isEqualTo((byte) 2);
            return Arrays.copyOf(padded, padded.length - 1);
        }

        private static byte[] hkdf(byte[] salt, byte[] ikm, byte[] info, int length) throws GeneralSecurityException {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(salt, "HmacSHA256"));
            var prk = mac.doFinal(ikm);
            mac.init(new SecretKeySpec(prk, "HmacSHA256"));
            return Arrays.copyOf(mac.doFinal(concat(info, new byte[]{1})), length);
        }

        private static byte[] concat(byte[]... parts) {
            var out = new java.io.ByteArrayOutputStream();
            for (var part : parts) {
                out.writeBytes(part);
            }
            return out.toByteArray();
        }
    }
}
