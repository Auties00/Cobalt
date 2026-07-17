package com.github.auties00.cobalt.util.certificate;

import org.bouncycastle.tls.crypto.impl.bc.BcTlsCertificate;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the JDK-only certificate generator. The linchpin is {@code cert.verify(publicKey)}: the JDK
 * re-parses the assembled DER and checks the self-signature, validating the entire {@code TBSCertificate}
 * encoding and algorithm identifiers end-to-end. The suite covers ECDSA and RSA, the fluent API surface, the
 * tricky DER edges (high-bit serial, GeneralizedTime), and parsing by BouncyCastle's {@code BcTlsCertificate},
 * the real DTLS-side consumer of the produced bytes.
 */
@DisplayName("X509CertificateGenerator")
class X509CertificateGeneratorTest {
    private static final X500Principal SUBJECT = new X500Principal("CN=WebRTC");
    private static final SecureRandom RANDOM = new SecureRandom();

    private static Stream<Arguments> algorithms() throws Exception {
        return Stream.of(
                Arguments.of(ec("secp256r1"), "SHA256withECDSA", "1.2.840.10045.4.3.2"),
                Arguments.of(ec("secp384r1"), "SHA384withECDSA", "1.2.840.10045.4.3.3"),
                Arguments.of(rsa(2048), "SHA256withRSA", "1.2.840.113549.1.1.11"));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("algorithms")
    @DisplayName("produces a v3 self-signed certificate the JDK verifies and re-parses byte-identically")
    void generatesVerifiableCertificate(KeyPair keyPair, String signatureAlgorithm, String oid) throws Exception {
        var serial = new BigInteger(64, RANDOM).add(BigInteger.ONE);
        var now = Instant.now();
        var cert = X509CertificateGenerator.getInstance(signatureAlgorithm).generate(spec(keyPair, serial, now));

        cert.verify(keyPair.getPublic());
        assertEquals(3, cert.getVersion());
        assertEquals(SUBJECT, cert.getSubjectX500Principal());
        assertEquals(cert.getSubjectX500Principal(), cert.getIssuerX500Principal());
        assertEquals(serial, cert.getSerialNumber());
        assertEquals(oid, cert.getSigAlgOID());
        assertDoesNotThrow(() -> cert.checkValidity(Date.from(now)));

        var reparsed = CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(cert.getEncoded()));
        assertArrayEquals(cert.getEncoded(), reparsed.getEncoded());
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("algorithms")
    @DisplayName("emits DER that BouncyCastle's BcTlsCertificate parses with a matching fingerprint")
    void bouncyCastleConsumesDer(KeyPair keyPair, String signatureAlgorithm, String oid) throws Exception {
        var der = X509CertificateGenerator.getInstance(signatureAlgorithm)
                .generateEncoded(spec(keyPair, BigInteger.ONE, Instant.now()));
        var parsed = new BcTlsCertificate(new BcTlsCrypto(RANDOM), der);
        assertArrayEquals(sha256(der), sha256(parsed.getEncoded()));
    }

    @Nested
    @DisplayName("API surface")
    class Surface {
        @Test
        @DisplayName("getInstance rejects an unsupported algorithm")
        void rejectsUnknownAlgorithm() {
            assertThrows(NoSuchAlgorithmException.class, () -> X509CertificateGenerator.getInstance("nonsense"));
        }

        @Test
        @DisplayName("build rejects a spec missing required fields")
        void rejectsIncompleteSpec() throws Exception {
            var builder = X509CertificateSpec.selfSigned().keyPair(ec("secp256r1"));
            assertThrows(IllegalStateException.class, builder::build);
        }

        @Test
        @DisplayName("build rejects a non-positive serial")
        void rejectsNonPositiveSerial() throws Exception {
            var builder = X509CertificateSpec.selfSigned().keyPair(ec("secp256r1")).subject("CN=x")
                    .serialNumber(BigInteger.ZERO).validity(Instant.now(), Instant.now());
            assertThrows(IllegalArgumentException.class, builder::build);
        }
    }

    @Nested
    @DisplayName("DER edges")
    class Edges {
        @Test
        @DisplayName("preserves a serial whose high bit is set")
        void highBitSerial() throws Exception {
            var serial = new BigInteger(1, new byte[]{(byte) 0xFF, 0x01, 0x02, 0x03});
            var cert = X509CertificateGenerator.getInstance("SHA256withECDSA")
                    .generate(spec(ec("secp256r1"), serial, Instant.now()));
            assertEquals(serial, cert.getSerialNumber());
        }

        @Test
        @DisplayName("uses GeneralizedTime for a not-after past 2050 and still verifies")
        void generalizedTimeValidity() throws Exception {
            var keyPair = ec("secp256r1");
            var now = Instant.now();
            var notAfter = now.plus(365L * 30, ChronoUnit.DAYS);
            var cert = X509CertificateGenerator.getInstance("SHA256withECDSA").generate(
                    X509CertificateSpec.selfSigned().keyPair(keyPair).subject(SUBJECT)
                            .serialNumber(BigInteger.ONE).validity(now, notAfter).build());
            assertDoesNotThrow(() -> cert.verify(keyPair.getPublic()));
            assertTrue(cert.getNotAfter().toInstant().isAfter(Instant.parse("2050-01-01T00:00:00Z")));
        }
    }

    private static X509CertificateSpec spec(KeyPair keyPair, BigInteger serial, Instant now) {
        return X509CertificateSpec.selfSigned()
                .keyPair(keyPair)
                .subject(SUBJECT)
                .serialNumber(serial)
                .validity(now.minus(1, ChronoUnit.DAYS), now.plus(30, ChronoUnit.DAYS))
                .build();
    }

    private static KeyPair ec(String curve) throws GeneralSecurityException {
        var generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec(curve));
        return generator.generateKeyPair();
    }

    private static KeyPair rsa(int bits) throws GeneralSecurityException {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(bits);
        return generator.generateKeyPair();
    }

    private static byte[] sha256(byte[] data) throws GeneralSecurityException {
        return MessageDigest.getInstance("SHA-256").digest(data);
    }
}
