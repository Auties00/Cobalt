package com.github.auties00.cobalt.registration.push.apns.courier;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Groups the cryptographic helpers consumed exclusively by the APNS courier
 * connection flow.
 *
 * <p>The class covers four narrow tasks the courier connection performs at the
 * boundary between the persisted session and the on-wire packets: restoring the
 * device RSA keypair from the PKCS#8 and X.509 DER blobs captured during
 * activation, building and {@code SHA1withRSA} signing the connect-time nonce
 * embedded in {@link ApnsPayloadTag#CONNECT}, re-encoding the Apple-signed device
 * certificate into its canonical DER form, and computing the SHA-1 topic hashes
 * the courier expects in {@link ApnsPayloadTag#FILTER} subscription lists and
 * {@link ApnsPayloadTag#GET_TOKEN} requests. It sits in the {@code courier}
 * subpackage so the courier-side crypto and the courier wire model stay together;
 * the complementary activation-side helpers (FairPlay key, CSR generation, RSA
 * keypair generation, activation-info signing) live in
 * {@code ApnsActivationCrypto}.
 *
 * @implNote This implementation hand-rolls only the wire-level glue (nonce layout,
 * signature algorithm tag) and routes every cryptographic primitive through the
 * JCA. The class is a stateless namespace and cannot be instantiated.
 */
public final class ApnsCourierCrypto {
    /**
     * The logger for {@link ApnsCourierCrypto}.
     */
    private static final System.Logger LOGGER = Log.get(ApnsCourierCrypto.class);

    /**
     * Holds the total length in bytes of the connect-time nonce.
     *
     * <p>The nonce is laid out as one version byte, then an 8-byte big-endian Unix
     * milliseconds timestamp, then 8 random bytes, for a total of {@code 17}.
     */
    private static final int NONCE_LENGTH = 17;

    /**
     * Holds the two-byte algorithm tag the courier expects prepended to a nonce
     * signature.
     *
     * <p>The pair {@code 0x01 0x01} identifies {@code SHA1withRSA}; the courier's
     * signature parser switches on these two bytes to pick a verifier.
     */
    private static final byte[] NONCE_SIGNATURE_TAG = {0x01, 0x01};

    /**
     * Prevents instantiation of this stateless namespace.
     */
    private ApnsCourierCrypto() {
    }

    /**
     * Restores an RSA {@link KeyPair} from the persisted DER blobs.
     *
     * <p>The public key is read from an {@link X509EncodedKeySpec} over the
     * {@code SubjectPublicKeyInfo} bytes and the private key from a
     * {@link PKCS8EncodedKeySpec}, both under the {@code RSA} {@link KeyFactory}.
     * This is called once per courier connection from the session-bound fields
     * populated during activation.
     *
     * @param publicKeyDer  the {@code SubjectPublicKeyInfo} DER bytes
     * @param privateKeyDer the PKCS#8 DER bytes
     * @return the reconstructed keypair
     * @throws IOException if either DER blob is invalid for an RSA key
     * @implNote This implementation collapses any {@link GeneralSecurityException}
     * thrown by the JCA into an {@link IOException} so callers treat key-restore
     * failures as ordinary transport-setup failures.
     */
    public static KeyPair restoreKeyPair(byte[] publicKeyDer, byte[] privateKeyDer) throws IOException {
        try {
            var keyFactory = KeyFactory.getInstance("RSA");
            var pub = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyDer));
            var priv = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyDer));
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "restored apns keypair");
            return new KeyPair(pub, priv);
        } catch (GeneralSecurityException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "apns keypair restore failed", e);
            throw new IOException("APNS keypair restore failed", e);
        }
    }

    /**
     * Builds a fresh connect-time nonce.
     *
     * <p>The nonce is embedded in {@link ApnsPayloadTag#CONNECT} together with the
     * signature from {@link #signNonce(KeyPair, byte[])} and is built once per
     * courier handshake.
     *
     * @param random the source of randomness used for the trailing 8 bytes
     * @return the freshly built nonce
     * @implNote This implementation lays the nonce out as one version byte
     * ({@code 0x00}, left untouched by {@link ByteBuffer#allocate(int)}) followed by
     * an 8-byte big-endian Unix-ms timestamp at offset 1 and 8 random bytes at
     * offset 9, for a total of {@value #NONCE_LENGTH} bytes.
     */
    public static byte[] createNonce(SecureRandom random) {
        var buf = ByteBuffer.allocate(NONCE_LENGTH);
        buf.putLong(1, System.currentTimeMillis());
        var rnd = new byte[8];
        random.nextBytes(rnd);
        buf.put(9, rnd);
        var nonce = buf.array();
        if (Log.TRACE) LOGGER.log(Level.TRACE, "created connect nonce, bytes={0}", nonce);
        return nonce;
    }

    /**
     * Signs the connect-time nonce with the device RSA private key.
     *
     * <p>The result becomes the value of the {@code 0x0E} field of the
     * {@link ApnsPayloadTag#CONNECT} packet, which the courier verifies against the
     * public key embedded in the device certificate.
     *
     * @param keyPair the device keypair holding the signing private key
     * @param nonce   the nonce bytes returned by {@link #createNonce(SecureRandom)}
     * @return the {@code 0x01 0x01}-prefixed signature bytes
     * @throws IllegalStateException if the JCA cannot produce a {@code SHA1withRSA}
     *                               signature
     * @implNote This implementation computes a raw {@code SHA1withRSA} signature and
     * prepends the {@link #NONCE_SIGNATURE_TAG} so the courier's signature parser
     * picks the right verifier. JCA failures are wrapped as
     * {@link IllegalStateException} because they indicate a configuration problem (a
     * JVM without SHA-1 or RSA), not a transient failure the caller can react to.
     */
    public static byte[] signNonce(KeyPair keyPair, byte[] nonce) {
        try {
            var sig = Signature.getInstance("SHA1withRSA");
            sig.initSign(keyPair.getPrivate());
            sig.update(nonce);
            var raw = sig.sign();
            var out = new byte[raw.length + NONCE_SIGNATURE_TAG.length];
            System.arraycopy(NONCE_SIGNATURE_TAG, 0, out, 0, NONCE_SIGNATURE_TAG.length);
            System.arraycopy(raw, 0, out, NONCE_SIGNATURE_TAG.length, raw.length);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "signed connect nonce, bytes={0}", out.length);
            return out;
        } catch (GeneralSecurityException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "nonce signature failed", e);
            throw new IllegalStateException("nonce signature failed", e);
        }
    }

    /**
     * Re-encodes a device certificate into the canonical DER form the courier
     * expects.
     *
     * <p>The result becomes the value of the {@code 0x0C} field of the
     * {@link ApnsPayloadTag#CONNECT} packet, so it must be byte-for-byte what the
     * courier validates against. Both PEM and DER input are tolerated because the
     * activation response itself may use either form.
     *
     * @param certificateBytes the certificate blob in PEM or DER form
     * @return the canonical DER encoding of the same certificate
     * @throws CertificateException if the input is not a valid X.509 certificate
     * @implNote This implementation routes the input through
     * {@link CertificateFactory#generateCertificate(InputStream)} and re-emits
     * {@link X509Certificate#getEncoded()} so any extra envelope (PEM headers,
     * trailing whitespace) is dropped.
     */
    public static byte[] reencodeDeviceCertificate(byte[] certificateBytes) throws CertificateException {
        try (var in = new ByteArrayInputStream(certificateBytes)) {
            var factory = CertificateFactory.getInstance("X.509");
            var cert = (X509Certificate) factory.generateCertificate(in);
            var encoded = cert.getEncoded();
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "reencoded device certificate, bytes={0}", encoded.length);
            return encoded;
        } catch (IOException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "device certificate reencode failed", e);
            throw new CertificateException(e);
        }
    }

    /**
     * Returns the SHA-1 digest of a string under UTF-8.
     *
     * <p>The courier indexes subscriptions by topic hash, so the SHA-1 of a bundle
     * id appears verbatim in the {@link ApnsPayloadTag#FILTER} list and in the
     * {@link ApnsPayloadTag#GET_TOKEN} request.
     *
     * @param value the source string (typically an iOS bundle id)
     * @return the 20-byte SHA-1 digest of the UTF-8 encoded input
     * @throws IllegalStateException if the JVM does not provide SHA-1
     * @implNote This implementation surfaces a {@link NoSuchAlgorithmException} as
     * {@link IllegalStateException} because it indicates a JVM without SHA-1, not a
     * transient failure the caller can react to.
     */
    public static byte[] sha1(String value) {
        try {
            var md = MessageDigest.getInstance("SHA-1");
            return md.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "sha-1 unavailable", e);
            throw new IllegalStateException("SHA-1 unavailable", e);
        }
    }
}
