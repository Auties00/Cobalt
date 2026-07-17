package com.github.auties00.cobalt.util.certificate;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.io.ByteArrayInputStream;
import java.lang.System.Logger.Level;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Generates X.509 certificates using only public {@code java.base} APIs, with no third-party security provider
 * and no non-exported JDK internals.
 *
 * <p>The JDK ships no public API for building a certificate (the request is tracked but unscheduled), and the
 * usual escape hatch, {@code sun.security.x509.*}, is non-exported and discouraged. This generator fills that
 * gap for the one shape the project needs, a self-signed certificate, by assembling the DER directly: the bulk
 * of the certificate is marshalled by JDK types that already emit DER ({@link javax.security.auth.x500.X500Principal}
 * for the {@code Name}, {@link java.security.PublicKey#getEncoded()} for the {@code SubjectPublicKeyInfo},
 * {@link Signature} for the signature value), and the small remainder is built with {@link Der}.
 *
 * <p>The surface follows the modern JDK idiom of {@link javax.crypto.KDF}: an immutable engine obtained from a
 * factory keyed by algorithm, fed an immutable {@link X509CertificateSpec}, and driven by a terminal operation:
 *
 * {@snippet :
 * X509Certificate certificate = X509CertificateGenerator.getInstance("SHA256withECDSA")
 *         .generate(X509CertificateSpec.selfSigned()
 *                 .keyPair(keyPair)
 *                 .subject("CN=WebRTC")
 *                 .serialNumber(serial)
 *                 .validity(notBefore, notAfter)
 *                 .build());
 * }
 *
 * <p>The supported signature algorithms are {@code SHA256withECDSA}, {@code SHA384withECDSA},
 * {@code SHA512withECDSA}, {@code SHA256withRSA}, {@code SHA384withRSA}, and {@code SHA512withRSA}. Instances are
 * immutable and may be shared between threads; each {@link #generate(X509CertificateSpec)} uses its own fresh
 * {@link Signature}.
 *
 * @implNote This implementation validates the generated certificate by round-tripping it through the JDK's own
 *           X.509 {@link CertificateFactory}: malformed DER is rejected at generation time rather than surfacing
 *           later at the consumer, and the returned {@link X509Certificate#getEncoded()} yields the same bytes
 *           the generator produced. The signature {@code AlgorithmIdentifier} is encoded once and reused in both
 *           required positions (the {@code TBSCertificate} and the outer {@code Certificate}); RSA algorithm
 *           identifiers carry an explicit {@code NULL} parameter per RFC 3279, whereas ECDSA ones omit
 *           parameters.
 */
public final class X509CertificateGenerator {
    /**
     * The logger for {@link X509CertificateGenerator}.
     */
    private static final System.Logger LOGGER = Log.get(X509CertificateGenerator.class);

    /**
     * Holds the certificate version field {@code [0] EXPLICIT INTEGER 2}, the constant denoting X.509 v3.
     */
    private static final byte[] VERSION_V3 = Der.explicit(0, Der.integer(BigInteger.TWO)).encode();

    /**
     * Holds the supported signature algorithms keyed by upper-cased standard name.
     */
    private static final Map<String, AlgorithmEntry> ALGORITHMS = buildAlgorithmTable();

    /**
     * Holds the standard {@link Signature} algorithm name this generator signs with.
     */
    private final String signatureAlgorithm;

    /**
     * Holds the pre-encoded DER signature {@code AlgorithmIdentifier} for {@link #signatureAlgorithm}.
     */
    private final byte[] algorithmIdentifier;

    /**
     * Constructs a generator bound to a signature algorithm and its encoded identifier.
     *
     * @param signatureAlgorithm  the standard {@link Signature} algorithm name
     * @param algorithmIdentifier the encoded DER {@code AlgorithmIdentifier}
     */
    private X509CertificateGenerator(String signatureAlgorithm, byte[] algorithmIdentifier) {
        this.signatureAlgorithm = signatureAlgorithm;
        this.algorithmIdentifier = algorithmIdentifier;
    }

    /**
     * Returns a generator that signs certificates with the given algorithm.
     *
     * @param signatureAlgorithm the standard signature algorithm name, for example {@code "SHA256withECDSA"}
     * @return an immutable generator bound to that algorithm
     * @throws NoSuchAlgorithmException if the algorithm is not one this generator supports or the platform
     *                                  cannot provide it
     * @throws NullPointerException     if {@code signatureAlgorithm} is {@code null}
     */
    public static X509CertificateGenerator getInstance(String signatureAlgorithm) throws NoSuchAlgorithmException {
        Objects.requireNonNull(signatureAlgorithm, "signatureAlgorithm cannot be null");
        var entry = ALGORITHMS.get(signatureAlgorithm.toUpperCase(Locale.ROOT));
        if (entry == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "unsupported certificate signature algorithm {0}", signatureAlgorithm);
            throw new NoSuchAlgorithmException("Unsupported certificate signature algorithm: " + signatureAlgorithm);
        }
        Signature.getInstance(entry.canonicalName());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "obtained certificate generator for {0}", entry.canonicalName());
        return new X509CertificateGenerator(entry.canonicalName(), entry.algorithmIdentifier().clone());
    }

    /**
     * Builds, signs, and returns the certificate described by {@code spec}.
     *
     * <p>The {@code TBSCertificate} is assembled as {@code SEQUENCE { version, serial, signatureAlgorithm,
     * issuer, validity, subject, subjectPublicKeyInfo }} with issuer equal to subject, signed over its exact DER
     * bytes, wrapped with the signature, and parsed back through the JDK X.509 {@link CertificateFactory} so the
     * encoding is validated before it is returned.
     *
     * @param spec the certificate description
     * @return the generated certificate
     * @throws GeneralSecurityException if signing fails or the assembled certificate is not valid DER
     * @throws NullPointerException     if {@code spec} is {@code null}
     */
    public X509Certificate generate(X509CertificateSpec spec) throws GeneralSecurityException {
        Objects.requireNonNull(spec, "spec cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "generating certificate, algorithm={0}", signatureAlgorithm);
        try {
            var name = Der.raw(spec.subject().getEncoded());
            var tbsCertificate = Der.sequence()
                    .add(Der.raw(VERSION_V3))
                    .add(Der.integer(spec.serialNumber()))
                    .add(Der.raw(algorithmIdentifier))
                    .add(name)
                    .add(Der.sequence().add(Der.time(spec.notBefore())).add(Der.time(spec.notAfter())).build())
                    .add(name)
                    .add(Der.raw(spec.keyPair().getPublic().getEncoded()))
                    .encode();
            var signature = sign(tbsCertificate, spec.keyPair().getPrivate());
            var certificate = Der.sequence()
                    .add(Der.raw(tbsCertificate))
                    .add(Der.raw(algorithmIdentifier))
                    .add(Der.bitString(signature))
                    .encode();
            var result = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(certificate));
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "generated certificate, algorithm={0}", signatureAlgorithm);
            return result;
        } catch (GeneralSecurityException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "certificate generation failed, algorithm=" + signatureAlgorithm, e);
            throw e;
        }
    }

    /**
     * Builds and signs the certificate described by {@code spec} and returns its DER encoding.
     *
     * @param spec the certificate description
     * @return the DER-encoded certificate
     * @throws GeneralSecurityException if signing fails or the assembled certificate is not valid DER
     * @throws NullPointerException     if {@code spec} is {@code null}
     */
    public byte[] generateEncoded(X509CertificateSpec spec) throws GeneralSecurityException {
        return generate(spec).getEncoded();
    }

    /**
     * Signs the given {@code TBSCertificate} bytes with the private key under {@link #signatureAlgorithm}.
     *
     * @param tbsCertificate the to-be-signed certificate bytes
     * @param privateKey     the signing key
     * @return the DER signature value
     * @throws GeneralSecurityException if the key is unusable or signing fails
     */
    private byte[] sign(byte[] tbsCertificate, PrivateKey privateKey) throws GeneralSecurityException {
        var signer = Signature.getInstance(signatureAlgorithm);
        signer.initSign(privateKey);
        signer.update(tbsCertificate);
        return signer.sign();
    }

    /**
     * Builds the immutable table of supported signature algorithms, each with its encoded
     * {@code AlgorithmIdentifier}.
     *
     * @return the algorithm table keyed by upper-cased standard name
     */
    private static Map<String, AlgorithmEntry> buildAlgorithmTable() {
        var table = new HashMap<String, AlgorithmEntry>();
        register(table, "SHA256withECDSA", "1.2.840.10045.4.3.2", false);
        register(table, "SHA384withECDSA", "1.2.840.10045.4.3.3", false);
        register(table, "SHA512withECDSA", "1.2.840.10045.4.3.4", false);
        register(table, "SHA256withRSA", "1.2.840.113549.1.1.11", true);
        register(table, "SHA384withRSA", "1.2.840.113549.1.1.12", true);
        register(table, "SHA512withRSA", "1.2.840.113549.1.1.13", true);
        return Map.copyOf(table);
    }

    /**
     * Registers one signature algorithm by encoding its {@code AlgorithmIdentifier} and storing it under the
     * upper-cased name.
     *
     * @param table         the table to populate
     * @param canonicalName the standard {@link Signature} algorithm name
     * @param oid           the dotted-decimal signature OID
     * @param nullParameter whether the identifier carries an explicit {@code NULL} parameter (RSA) rather than
     *                      omitting parameters (ECDSA)
     */
    private static void register(Map<String, AlgorithmEntry> table, String canonicalName, String oid, boolean nullParameter) {
        var identifier = nullParameter
                ? Der.sequence().add(Der.oid(oid)).add(Der.nullValue()).encode()
                : Der.sequence().add(Der.oid(oid)).encode();
        table.put(canonicalName.toUpperCase(Locale.ROOT), new AlgorithmEntry(canonicalName, identifier));
    }

    /**
     * Pairs a standard signature algorithm name with its pre-encoded DER {@code AlgorithmIdentifier}.
     *
     * @param canonicalName       the standard {@link Signature} algorithm name
     * @param algorithmIdentifier the encoded DER {@code AlgorithmIdentifier}
     */
    private record AlgorithmEntry(String canonicalName, byte[] algorithmIdentifier) {
    }
}
