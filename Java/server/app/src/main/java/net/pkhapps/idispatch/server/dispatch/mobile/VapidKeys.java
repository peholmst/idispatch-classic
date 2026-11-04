package net.pkhapps.idispatch.server.dispatch.mobile;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;

import static java.util.Objects.requireNonNull;

/**
 * The server's VAPID key pair (RFC 8292), a P-256 key pair that identifies the server to the push services: the app
 * subscribes with the public key, and the push service accepts only messages signed with the private key. Written as
 * the {@code web-push} tools write them: the public key as an uncompressed point and the private key as its 32-byte
 * scalar, both base64url-encoded.
 */
record VapidKeys(ECPublicKey publicKey, ECPrivateKey privateKey) {

    private static final ECParameterSpec P256 = p256();

    VapidKeys {
        requireNonNull(publicKey);
        requireNonNull(privateKey);
    }

    /**
     * Reads the key pair as configured.
     *
     * @throws IllegalArgumentException if either is not a P-256 key, or they don't belong together
     */
    static VapidKeys of(String publicKey, String privateKey) {
        try {
            var keys = new VapidKeys(publicKeyOf(decode(publicKey.trim())), privateKeyOf(decode(privateKey.trim())));
            keys.checkPair();
            return keys;
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Not a VAPID key pair: " + ex.getMessage(), ex);
        }
    }

    /**
     * A new random key pair, as the deployment docs tell how to make with OpenSSL.
     */
    static VapidKeys generate() {
        var pair = generateKeyPair();
        return new VapidKeys((ECPublicKey) pair.getPublic(), (ECPrivateKey) pair.getPrivate());
    }

    /**
     * The public key as the app subscribes with it: an uncompressed point, base64url-encoded without padding.
     */
    String publicKeyBase64() {
        return encode(rawPublicKey(publicKey));
    }

    String privateKeyBase64() {
        return encode(unsigned(privateKey.getS()));
    }

    /**
     * Signs with the private key and checks the signature with the public key, which fails unless they are a pair.
     */
    private void checkPair() throws GeneralSecurityException {
        var data = new byte[]{1, 2, 3};
        var signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(privateKey);
        signer.update(data);
        var verifier = Signature.getInstance("SHA256withECDSA");
        verifier.initVerify(publicKey);
        verifier.update(data);
        if (!verifier.verify(signer.sign())) {
            throw new IllegalArgumentException("the public key is not that of the private key");
        }
    }

    static KeyPair generateKeyPair() {
        try {
            var generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            return generator.generateKeyPair();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Every Java platform has P-256", ex);
        }
    }

    /**
     * Reads an uncompressed P-256 point.
     */
    static ECPublicKey publicKeyOf(byte[] raw) throws GeneralSecurityException {
        if (raw.length != 65 || raw[0] != 0x04) {
            throw new IllegalArgumentException("a public key is an uncompressed P-256 point of 65 bytes");
        }
        var point = new ECPoint(new BigInteger(1, Arrays.copyOfRange(raw, 1, 33)),
                new BigInteger(1, Arrays.copyOfRange(raw, 33, 65)));
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(point, P256));
    }

    static ECPrivateKey privateKeyOf(byte[] raw) throws GeneralSecurityException {
        if (raw.length != 32) {
            throw new IllegalArgumentException("a private key is 32 bytes");
        }
        return (ECPrivateKey) KeyFactory.getInstance("EC")
                .generatePrivate(new ECPrivateKeySpec(new BigInteger(1, raw), P256));
    }

    /**
     * The key as an uncompressed point, 65 bytes.
     */
    static byte[] rawPublicKey(ECPublicKey key) {
        var raw = new byte[65];
        raw[0] = 0x04;
        System.arraycopy(unsigned(key.getW().getAffineX()), 0, raw, 1, 32);
        System.arraycopy(unsigned(key.getW().getAffineY()), 0, raw, 33, 32);
        return raw;
    }

    /**
     * The number as exactly 32 unsigned bytes.
     */
    private static byte[] unsigned(BigInteger value) {
        var bytes = value.toByteArray();
        var result = new byte[32];
        var length = Math.min(bytes.length, 32);
        System.arraycopy(bytes, bytes.length - length, result, 32 - length, length);
        return result;
    }

    static byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    static String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static ECParameterSpec p256() {
        try {
            var parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            return parameters.getParameterSpec(ECParameterSpec.class);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Every Java platform has P-256", ex);
        }
    }

    @Override
    public String toString() {
        return "VapidKeys[" + publicKeyBase64() + "]";
    }
}
