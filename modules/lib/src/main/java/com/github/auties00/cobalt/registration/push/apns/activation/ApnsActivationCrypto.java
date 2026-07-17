package com.github.auties00.cobalt.registration.push.apns.activation;

import com.github.auties00.cobalt.telemetry.log.Log;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.Base64;
import java.util.UUID;

/**
 * Provides the crypto helpers consumed exclusively by the {@code albert.apple.com} activation flow.
 *
 * <p>The class covers three narrow tasks the activation flow performs at the boundary between the
 * device-bound material and the activation HTTP request: holding the leaked FairPlay private key
 * plus the SHA-1-with-RSA primitive that signs the inner activation plist via
 * {@link #signActivationInfo(byte[])}, generating the device-bound 2048-bit RSA keypair the courier
 * later authenticates against via {@link #newRsaKeyPair()}, and DER encoding the PKCS#10
 * certificate request {@code albert.apple.com} signs into a device certificate via
 * {@link #generateCsr(KeyPair)}. It lives in the {@code activation} subpackage so the
 * activation-side crypto and the activation-side plist model stay together; the complementary
 * courier-side helpers (nonce signing, device-cert re-encoding, SHA-1 topic hashes, keypair
 * restore) live in {@link com.github.auties00.cobalt.registration.push.apns.courier.ApnsCourierCrypto}.
 *
 * @implNote This implementation hand-rolls the PKCS#10 DER encoder rather than pulling in
 * BouncyCastle: the activation flow only ever emits a single CSR shape (subject plus public key
 * plus empty attributes) so the cost of a writer that handles every PKCS#10 corner case would not
 * be repaid. The leaked FairPlay private key is materialised once on class load from its inline CRT
 * components.
 */
public final class ApnsActivationCrypto {
    /**
     * The logger for {@link ApnsActivationCrypto}.
     *
     * <p>Declared first so it is initialized before {@link #FAIRPLAY_PRIVATE_KEY}, whose own
     * initializer calls {@link #loadFairplayPrivateKey()} and logs through this field.
     */
    private static final System.Logger LOGGER = Log.get(ApnsActivationCrypto.class);

    /**
     * Holds the modulus {@code n} of the leaked FairPlay private key.
     *
     * <p>The value is materialised once on class load and passed to {@link RSAPrivateCrtKeySpec} by
     * {@link #loadFairplayPrivateKey()}.
     *
     * @implNote This implementation stores the modulus as a signed-byte two's-complement big-endian
     * array with a leading {@code 0x00} so the high bit stays clear and the value remains positive
     * when passed to {@link BigInteger#BigInteger(byte[])}.
     */
    private static final BigInteger FAIRPLAY_PRIVATE_KEY_MODULUS = new BigInteger(new byte[]{0, -73, 4, -86, -53, 60, -128, 90, 110, 26, 107, -5, -124, -81, -71, 1, -25, 108, 93, 44, -78, -92, 72, 67, -82, -1, -43, -76, -33, 2, -75, 110, 80, 94, -87, -10, -57, 92, -65, -67, 13, -6, -65, 50, 23, 23, -84, 14, 106, 83, -104, -88, -83, 44, -3, 58, -77, 50, -115, 0, -19, 100, -28, -107, 100, -9, -13, -11, 122, 60, -12, -16, 19, 30, -11, -32, 27, -30, 59, -128, -123, 85, -99, 124, 2, 50, 104, -98, -20, 24, 88, -34, 72, -67, 53, -26, 96, 47, -81, 20, -59, 112, -29, 53, -48, 95, -22, 74, 80, 17, 6, 39, 28, -60, 120, -49, 94, -37, -106, 86, -67, 31, 44, 106, -31, 51, -17, -64, 49});

    /**
     * Holds the public exponent {@code e} of the FairPlay key.
     *
     * <p>The value is the canonical {@code 0x010001} (65537), held as a constant so
     * {@link RSAPrivateCrtKeySpec} can consume it directly.
     */
    private static final BigInteger FAIRPLAY_PRIVATE_KEY_PUBLIC_EXPONENT = new BigInteger(new byte[]{1, 0, 1});

    /**
     * Holds the private exponent {@code d} of the FairPlay key.
     */
    private static final BigInteger FAIRPLAY_PRIVATE_KEY_PRIVATE_EXPONENT = new BigInteger(new byte[]{8, 101, -73, 108, 113, -49, 53, -42, -3, 113, 92, -19, -2, -98, 15, 127, 77, -46, -116, -99, 121, -70, 51, 24, -47, 118, 61, -63, 73, -65, -121, 91, 58, -84, -77, -68, -5, -3, 116, 48, 51, 4, 24, -55, 68, 117, -55, -121, -119, 100, 100, -64, -27, 98, -115, 17, -15, -52, -44, 113, 16, 3, 8, -13, -80, 43, -70, -121, -31, -115, -41, 91, 53, 82, -19, -22, -102, 86, -38, 114, -62, 43, -73, 4, 13, 2, 69, 106, 20, -42, -12, 100, 5, -50, -107, 81, 65, 117, -33, -58, -66, 28, -77, 85, -9, -110, -21, 101, 7, -85, -104, -39, 81, -91, -73, 20, 5, 98, -56, -7, 118, 59, -5, 15, -32, 59, -40, 13});

    /**
     * Holds the first CRT prime {@code p} of the FairPlay private key.
     */
    private static final BigInteger FAIRPLAY_PRIVATE_KEY_PRIME_P = new BigInteger(new byte[]{0, -39, -113, 84, 70, 119, 22, -34, 58, -114, 23, -49, 23, -64, -22, 126, 54, 26, -106, 90, -67, 57, 76, -35, 12, -58, 22, -128, 73, -128, 42, -86, 86, -19, 73, 64, -20, -24, -96, 3, 17, 79, -29, 79, 126, 10, 4, -13, -21, -84, 33, 85, 65, -32, -118, 52, 108, 37, 19, 88, -70, 100, 19, 43, 83});

    /**
     * Holds the second CRT prime {@code q} of the FairPlay private key.
     */
    private static final BigInteger FAIRPLAY_PRIVATE_KEY_PRIME_Q = new BigInteger(new byte[]{0, -41, 90, -13, 34, -111, -120, -124, 89, 15, -76, -41, 63, -33, 119, -17, 4, 88, 48, 81, -69, 35, 78, -51, 85, -28, 110, -77, -26, 92, -65, 94, 113, -67, 16, -41, -53, 110, 88, 4, -95, -78, -26, 74, 108, -9, -118, -69, -63, 69, 96, 3, -117, 5, 49, -25, 54, 58, 20, -33, -61, -102, -40, -71, -21});

    /**
     * Holds the CRT exponent {@code dP = d mod (p-1)} of the FairPlay key.
     */
    private static final BigInteger FAIRPLAY_PRIVATE_KEY_PRIME_EXPONENT_P = new BigInteger(new byte[]{118, 36, -100, 106, 75, -97, 114, 124, -81, -49, 4, 25, -19, 28, 41, -1, -83, 126, 122, -74, 9, 24, -47, 109, 111, 96, -90, -73, -61, 78, -24, 3, -98, -123, -54, 41, 28, -58, 80, 4, 37, -78, -43, -25, 38, -1, -69, -119, -2, -122, 119, 106, -9, -55, 117, 96, 72, -35, -15, -81, -2, 74, 94, -101});

    /**
     * Holds the CRT exponent {@code dQ = d mod (q-1)} of the FairPlay key.
     */
    private static final BigInteger FAIRPLAY_PRIVATE_KEY_PRIME_EXPONENT_Q = new BigInteger(new byte[]{24, -59, -14, -96, 48, 99, -90, -19, -29, -37, -90, -61, 71, 62, -79, -75, 43, 59, -21, -70, -2, 85, -53, 83, 45, 34, -6, -8, -18, 4, 105, -91, -27, -36, -15, 38, 10, -68, 127, 83, -26, -109, -115, 78, 57, -81, -80, -25, -117, -58, 126, -63, -40, 72, 36, 83, -35, -100, -105, 29, 22, 76, 6, 31});

    /**
     * Holds the CRT coefficient {@code qInv = q^-1 mod p} of the FairPlay key.
     */
    private static final BigInteger FAIRPLAY_PRIVATE_KEY_CRT_EXPONENT = new BigInteger(new byte[]{61, 15, -80, 86, -92, 99, 115, -120, 119, 31, 11, -68, 35, -87, 101, -109, -36, 33, -92, -81, 78, -17, 65, 75, -93, 81, 76, 85, -42, -78, -76, 73, 76, -54, -84, -48, -37, -3, 57, 124, -58, -5, 23, -84, -102, 90, 27, -66, 67, 97, -122, 94, -9, 101, 81, 24, -128, -34, -42, 52, 10, -57, -52, -45});

    /**
     * Holds the pre-encoded DER bytes of an {@code AlgorithmIdentifier} for sha256WithRSAEncryption.
     *
     * <p>The value encodes OID {@code 1.2.840.113549.1.1.11} with a {@code NULL} parameters tail.
     * It is wrapped into the outer {@code CertificationRequest} sequence verbatim by
     * {@link #encodeCertificationRequest(byte[], byte[])} so the activation flow never needs a
     * general-purpose OID encoder.
     */
    private static final byte[] SHA256_WITH_RSA_ALGORITHM_ID = {
            0x30, 0x0D,
            0x06, 0x09, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x0B,
            0x05, 0x00
    };

    /**
     * Holds the pre-encoded DER bytes for an empty {@code [0] IMPLICIT SET OF Attribute}, the
     * PKCS#10 attributes block.
     *
     * <p>The activation flow needs no CSR attributes, so the value is a constant inlined into
     * {@link #encodeCertificationRequestInfo(byte[], PublicKey)}.
     */
    private static final byte[] EMPTY_ATTRIBUTES = {(byte) 0xA0, 0x00};

    /**
     * Holds the number of base64 characters per line in the PEM-wrapped CSR.
     *
     * @implNote This implementation uses the conventional 64-character width OpenSSL produces so the
     * output looks indistinguishable from a real CSR.
     */
    private static final int CSR_LINE_BLOCK = 64;

    /**
     * Holds the modulus length in bits of the device-bound RSA key.
     *
     * @implNote This implementation uses {@code 2048} because that is the size the iPhone Device CA
     * expects in the CSR; a smaller value is rejected by the CA and a larger value slows the
     * activation handshake without the server caring.
     */
    private static final int RSA_KEY_SIZE = 2048;

    /**
     * Holds the leaked FairPlay {@link PrivateKey}, materialised once on class load.
     *
     * <p>It is used by {@link #signActivationInfo(byte[])} to sign the inner activation plist;
     * {@code albert.apple.com} validates the resulting signature against the bundled FairPlay
     * certificate chain.
     */
    private static final PrivateKey FAIRPLAY_PRIVATE_KEY = loadFairplayPrivateKey();


    /**
     * Materialises the leaked FairPlay key from its inline CRT components.
     *
     * <p>This is invoked once during class initialization to populate {@link #FAIRPLAY_PRIVATE_KEY}
     * from the eight {@link BigInteger} CRT constants held by this class.
     *
     * @implNote This implementation wraps any {@link GeneralSecurityException} from
     * {@link KeyFactory#generatePrivate(java.security.spec.KeySpec)} as {@link IllegalStateException}
     * because a JVM that cannot load an RSA private key is unrecoverable for the activation flow.
     *
     * @return the loaded FairPlay private key
     */
    private static PrivateKey loadFairplayPrivateKey() {
        try {
            var spec = new RSAPrivateCrtKeySpec(
                    FAIRPLAY_PRIVATE_KEY_MODULUS,
                    FAIRPLAY_PRIVATE_KEY_PUBLIC_EXPONENT,
                    FAIRPLAY_PRIVATE_KEY_PRIVATE_EXPONENT,
                    FAIRPLAY_PRIVATE_KEY_PRIME_P,
                    FAIRPLAY_PRIVATE_KEY_PRIME_Q,
                    FAIRPLAY_PRIVATE_KEY_PRIME_EXPONENT_P,
                    FAIRPLAY_PRIVATE_KEY_PRIME_EXPONENT_Q,
                    FAIRPLAY_PRIVATE_KEY_CRT_EXPONENT);
            return KeyFactory.getInstance("RSA")
                    .generatePrivate(spec);
        } catch (GeneralSecurityException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "fairplay private key load failed", e);
            throw new IllegalStateException("FairPlay private key load failed", e);
        }
    }

    /**
     * Prevents instantiation of this stateless namespace.
     *
     * <p>The class exposes only static helpers, so any attempt to construct it through reflection
     * fails fast.
     *
     * @throws AssertionError always
     */
    private ApnsActivationCrypto() {
        throw new AssertionError();
    }

    /**
     * Generates a fresh device-bound RSA keypair.
     *
     * <p>The keypair binds the activation CSR, whose signed certificate carries the public key, and
     * later signs the courier connect-time nonce verified against the same certificate. It is
     * generated once per fresh activation.
     *
     * @implNote This implementation hardcodes the modulus length to {@value #RSA_KEY_SIZE} bits
     * because that is the only value the iPhone Device CA accepts. JCA failures surface as
     * {@link IllegalStateException} because a JVM without an RSA generator is unrecoverable.
     *
     * @return a freshly generated 2048-bit RSA keypair
     * @throws IllegalStateException if the JVM does not provide RSA
     */
    public static KeyPair newRsaKeyPair() {
        try {
            var gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(RSA_KEY_SIZE);
            var pair = gen.generateKeyPair();
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "generated device rsa keypair, bits={0}", RSA_KEY_SIZE);
            return pair;
        } catch (GeneralSecurityException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "rsa generator unavailable", e);
            throw new IllegalStateException("RSA generator unavailable", e);
        }
    }

    /**
     * Builds a PEM-encoded PKCS#10 certificate request for the device keypair.
     *
     * <p>The resulting CSR is embedded in the inner activation plist under {@code DeviceCertRequest}
     * and is produced once per fresh activation. The subject CN is a random {@link UUID} so two
     * activations on the same host produce distinguishable certificates.
     *
     * @implNote This implementation builds the DER by hand:
     * {@link #encodeCertificationRequestInfo(byte[], PublicKey)} emits the inner
     * {@code CertificationRequestInfo}, {@link #signSha256(PrivateKey, byte[])} signs it, and
     * {@link #encodeCertificationRequest(byte[], byte[])} wraps both plus
     * {@link #SHA256_WITH_RSA_ALGORITHM_ID} into the outer {@code CertificationRequest}. The final
     * DER is PEM-wrapped via {@link #wrapAsPem(String, byte[], String)}.
     *
     * @param keyPair the device keypair the CSR is bound to
     * @return the PEM-wrapped CSR bytes
     * @throws IllegalStateException if the JCA cannot produce a SHA-256-with-RSA signature
     */
    public static byte[] generateCsr(KeyPair keyPair) {
        try {
            var subject = new X500Principal(
                    "C=US,ST=CA,L=Cupertino,O=Apple Inc.,OU=iPhone,CN=" + UUID.randomUUID());
            var requestInfo = encodeCertificationRequestInfo(
                    subject.getEncoded(), keyPair.getPublic());
            var signature = signSha256(keyPair.getPrivate(), requestInfo);
            var certificationRequest = encodeCertificationRequest(requestInfo, signature);
            var pem = wrapAsPem(
                    "-----BEGIN CERTIFICATE REQUEST-----",
                    certificationRequest,
                    "-----END CERTIFICATE REQUEST-----");
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "generated activation csr, bytes={0}", pem.length);
            return pem;
        } catch (GeneralSecurityException | IOException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "csr generation failed", e);
            throw new IllegalStateException("CSR generation failed", e);
        }
    }

    /**
     * Signs the inner activation plist with the leaked FairPlay private key.
     *
     * <p>The result is the {@code FairPlaySignature} entry of the outer activation plist;
     * {@code albert.apple.com} validates the signature against the FairPlay certificate chain
     * bundled in the same request.
     *
     * @implNote This implementation uses {@code SHA1withRSA} because that is the algorithm the
     * FairPlay chain is registered under; JCA failures surface as {@link IllegalStateException}
     * because they indicate an unrecoverable configuration problem.
     *
     * @param activationInfoXml the inner activation plist bytes
     * @return the SHA-1-with-RSA signature over the input
     * @throws IllegalStateException if the JCA cannot produce a {@code SHA1withRSA} signature
     */
    public static byte[] signActivationInfo(byte[] activationInfoXml) {
        try {
            var sig = Signature.getInstance("SHA1withRSA");
            sig.initSign(FAIRPLAY_PRIVATE_KEY);
            sig.update(activationInfoXml);
            var signature = sig.sign();
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "signed activation info, bytes={0}", signature.length);
            return signature;
        } catch (GeneralSecurityException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "fairplay signature failed", e);
            throw new IllegalStateException("FairPlay signature failed", e);
        }
    }

    /**
     * Signs arbitrary bytes with {@code SHA256withRSA}.
     *
     * <p>This signs the {@code CertificationRequestInfo} inside {@link #generateCsr(KeyPair)} using
     * the device private key.
     *
     * @param key  the RSA private key to sign with
     * @param data the bytes to sign
     * @return the signature bytes
     * @throws GeneralSecurityException if the JCA call fails
     */
    private static byte[] signSha256(PrivateKey key, byte[] data) throws GeneralSecurityException {
        var sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(key);
        sig.update(data);
        return sig.sign();
    }

    /**
     * Encodes the {@code CertificationRequestInfo} ASN.1 structure into DER.
     *
     * <p>Per PKCS#10 section 4.1 the structure is a {@code SEQUENCE} of version ({@code INTEGER 0}),
     * subject ({@code Name}), subjectPublicKeyInfo (the X.509 encoding of the public key), and an
     * empty attributes block.
     *
     * @implNote This implementation reuses {@link X500Principal#getEncoded()} and
     * {@link PublicKey#getEncoded()} for the two sub-structures that already have a canonical DER
     * form, then routes the concatenation through {@link #wrapInSequence(byte[])} for the outer
     * SEQUENCE.
     *
     * @param subjectDer the DER-encoded subject distinguished name
     * @param publicKey  the public key to embed
     * @return the DER-encoded {@code CertificationRequestInfo}
     * @throws IOException if the underlying writer fails (impossible for an in-memory sink)
     */
    private static byte[] encodeCertificationRequestInfo(byte[] subjectDer, PublicKey publicKey) throws IOException {
        var inner = new ByteArrayOutputStream();
        writeDer(inner, (byte) 0x02, new byte[]{0x00});
        inner.write(subjectDer);
        inner.write(publicKey.getEncoded());
        inner.write(EMPTY_ATTRIBUTES);
        return wrapInSequence(inner.toByteArray());
    }

    /**
     * Encodes the outer {@code CertificationRequest} ASN.1 structure into DER.
     *
     * <p>Per PKCS#10 section 4.2 the structure is a {@code SEQUENCE} of
     * {@code CertificationRequestInfo}, {@code AlgorithmIdentifier} (here
     * {@link #SHA256_WITH_RSA_ALGORITHM_ID}) and a {@code BIT STRING} holding the signature.
     *
     * @implNote This implementation prepends the canonical {@code 0x00} unused-bits byte that DER
     * {@code BIT STRING} encoding requires.
     *
     * @param requestInfo the DER-encoded request info from
     *                    {@link #encodeCertificationRequestInfo(byte[], PublicKey)}
     * @param signature   the SHA-256-with-RSA signature over {@code requestInfo}
     * @return the DER-encoded full PKCS#10 request
     * @throws IOException if the underlying writer fails (impossible for an in-memory sink)
     */
    private static byte[] encodeCertificationRequest(byte[] requestInfo, byte[] signature) throws IOException {
        var inner = new ByteArrayOutputStream();
        inner.write(requestInfo);
        inner.write(SHA256_WITH_RSA_ALGORITHM_ID);
        var bitString = new byte[signature.length + 1];
        bitString[0] = 0x00;
        System.arraycopy(signature, 0, bitString, 1, signature.length);
        writeDer(inner, (byte) 0x03, bitString);
        return wrapInSequence(inner.toByteArray());
    }

    /**
     * Wraps a payload in an ASN.1 {@code SEQUENCE} (tag {@code 0x30}) with a DER length prefix.
     *
     * <p>Both CSR encoder paths use this to close the outer SEQUENCE around an already-concatenated
     * TLV stream.
     *
     * @param payload the contents of the SEQUENCE
     * @return the DER-encoded SEQUENCE
     * @throws IOException if the underlying writer fails (impossible for an in-memory sink)
     */
    private static byte[] wrapInSequence(byte[] payload) throws IOException {
        var out = new ByteArrayOutputStream();
        writeDer(out, (byte) 0x30, payload);
        return out.toByteArray();
    }

    /**
     * Writes one DER TLV record into the given sink.
     *
     * <p>The method handles both the short-form length encoding (single length byte, payload
     * {@code < 128} bytes) and the long-form encoding ({@code 0x80 | n} followed by the {@code n}
     * big-endian length bytes) per X.690 section 8.1.3.
     *
     * @param out     the destination stream
     * @param tag     the ASN.1 tag byte
     * @param payload the value bytes
     * @throws IOException if the underlying writer fails (impossible for an in-memory sink)
     */
    private static void writeDer(ByteArrayOutputStream out, byte tag, byte[] payload) throws IOException {
        out.write(tag);
        var len = payload.length;
        if (len < 0x80) {
            out.write(len);
        } else {
            var lenBytes = new ByteArrayOutputStream();
            for (var v = len; v > 0; v >>>= 8) {
                lenBytes.write(v & 0xFF);
            }
            var raw = lenBytes.toByteArray();
            out.write(0x80 | raw.length);
            for (var i = raw.length - 1; i >= 0; i--) {
                out.write(raw[i] & 0xFF);
            }
        }
        out.write(payload);
    }

    /**
     * Wraps DER bytes in a PEM envelope.
     *
     * <p>This emits the {@code -----BEGIN CERTIFICATE REQUEST-----} and
     * {@code -----END CERTIFICATE REQUEST-----} envelope the inner activation plist's
     * {@code DeviceCertRequest} field expects, for {@link #generateCsr(KeyPair)}.
     *
     * @implNote This implementation breaks the base64 body into {@link #CSR_LINE_BLOCK}-character
     * lines so the output matches the line-wrapping convention OpenSSL produces.
     *
     * @param header the {@code -----BEGIN ...-----} line
     * @param der    the DER bytes to wrap
     * @param footer the {@code -----END ...-----} line
     * @return the PEM-wrapped UTF-8 bytes
     */
    private static byte[] wrapAsPem(String header, byte[] der, String footer) {
        var base64 = Base64.getEncoder().encodeToString(der);
        var sb = new StringBuilder(header.length() + footer.length() + base64.length() + 64);
        sb.append(header).append('\n');
        for (var i = 0; i < base64.length(); i += CSR_LINE_BLOCK) {
            sb.append(base64, i, Math.min(base64.length(), i + CSR_LINE_BLOCK)).append('\n');
        }
        sb.append(footer).append('\n');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
