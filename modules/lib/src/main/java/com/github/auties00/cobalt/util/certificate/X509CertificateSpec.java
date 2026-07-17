package com.github.auties00.cobalt.util.certificate;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.KeyPair;
import java.time.Instant;
import java.util.Objects;

/**
 * An immutable description of the certificate {@link X509CertificateGenerator} should produce, assembled
 * through a fluent builder.
 *
 * <p>The spec carries everything that is independent of the signature algorithm: the {@link KeyPair} whose
 * public half becomes the certified key and whose private half signs (a self-signed certificate signs itself),
 * the subject (and, because the certificate is self-signed, issuer) distinguished name, the serial number, and
 * the validity window. The signature algorithm itself is supplied separately to
 * {@link X509CertificateGenerator#getInstance(String)}, mirroring how {@link javax.crypto.KDF} separates the
 * algorithm-bearing engine from its {@link java.security.spec.AlgorithmParameterSpec} inputs.
 *
 * <p>A spec is obtained through {@link #selfSigned()} and is immutable once {@link Builder#build()} returns:
 *
 * {@snippet :
 * X509CertificateSpec spec = X509CertificateSpec.selfSigned()
 *         .keyPair(keyPair)
 *         .subject("CN=WebRTC")
 *         .serialNumber(serial)
 *         .validity(notBefore, notAfter)
 *         .build();
 * }
 */
public final class X509CertificateSpec {
    /**
     * Holds the key pair whose public key is certified and whose private key signs the certificate.
     */
    private final KeyPair keyPair;

    /**
     * Holds the subject distinguished name, which is also the issuer for a self-signed certificate.
     */
    private final X500Principal subject;

    /**
     * Holds the certificate serial number, a positive integer.
     */
    private final BigInteger serialNumber;

    /**
     * Holds the inclusive start of the validity window.
     */
    private final Instant notBefore;

    /**
     * Holds the inclusive end of the validity window.
     */
    private final Instant notAfter;

    /**
     * Constructs an immutable spec from a validated builder.
     *
     * @param builder the builder carrying the validated fields
     */
    private X509CertificateSpec(Builder builder) {
        this.keyPair = builder.keyPair;
        this.subject = builder.subject;
        this.serialNumber = builder.serialNumber;
        this.notBefore = builder.notBefore;
        this.notAfter = builder.notAfter;
    }

    /**
     * Begins building a self-signed certificate spec.
     *
     * @return a fresh fluent builder
     */
    public static Builder selfSigned() {
        return new Builder();
    }

    /**
     * Returns the key pair whose public key is certified and whose private key signs.
     *
     * @return the key pair
     */
    KeyPair keyPair() {
        return keyPair;
    }

    /**
     * Returns the subject and issuer distinguished name.
     *
     * @return the subject name
     */
    X500Principal subject() {
        return subject;
    }

    /**
     * Returns the serial number.
     *
     * @return the serial number
     */
    BigInteger serialNumber() {
        return serialNumber;
    }

    /**
     * Returns the inclusive start of the validity window.
     *
     * @return the not-before instant
     */
    Instant notBefore() {
        return notBefore;
    }

    /**
     * Returns the inclusive end of the validity window.
     *
     * @return the not-after instant
     */
    Instant notAfter() {
        return notAfter;
    }

    /**
     * A fluent, single-use builder for an {@link X509CertificateSpec}.
     *
     * <p>Each setter returns {@code this} so calls chain; {@link #build()} validates that every field is set and
     * mutually consistent, then produces the immutable spec.
     */
    public static final class Builder {
        /**
         * Holds the key pair set so far, or {@code null} until {@link #keyPair(KeyPair)} is called.
         */
        private KeyPair keyPair;

        /**
         * Holds the subject name set so far, or {@code null} until {@link #subject(X500Principal)} is called.
         */
        private X500Principal subject;

        /**
         * Holds the serial number set so far, or {@code null} until {@link #serialNumber(BigInteger)} is called.
         */
        private BigInteger serialNumber;

        /**
         * Holds the not-before instant set so far, or {@code null} until {@link #validity(Instant, Instant)} is
         * called.
         */
        private Instant notBefore;

        /**
         * Holds the not-after instant set so far, or {@code null} until {@link #validity(Instant, Instant)} is
         * called.
         */
        private Instant notAfter;

        /**
         * Restricts construction to {@link X509CertificateSpec#selfSigned()}.
         */
        private Builder() {
        }

        /**
         * Sets the key pair whose public key is certified and whose private key signs the certificate.
         *
         * @param keyPair the key pair
         * @return this builder
         * @throws NullPointerException if {@code keyPair} is {@code null}
         */
        public Builder keyPair(KeyPair keyPair) {
            this.keyPair = Objects.requireNonNull(keyPair, "keyPair cannot be null");
            return this;
        }

        /**
         * Sets the subject (and, for a self-signed certificate, issuer) distinguished name.
         *
         * @param subject the subject name
         * @return this builder
         * @throws NullPointerException if {@code subject} is {@code null}
         */
        public Builder subject(X500Principal subject) {
            this.subject = Objects.requireNonNull(subject, "subject cannot be null");
            return this;
        }

        /**
         * Sets the subject distinguished name from its RFC 2253 string form.
         *
         * @param distinguishedName the distinguished name, for example {@code "CN=WebRTC"}
         * @return this builder
         * @throws NullPointerException     if {@code distinguishedName} is {@code null}
         * @throws IllegalArgumentException if {@code distinguishedName} is not a valid distinguished name
         */
        public Builder subject(String distinguishedName) {
            return subject(new X500Principal(Objects.requireNonNull(distinguishedName, "distinguishedName cannot be null")));
        }

        /**
         * Sets the certificate serial number.
         *
         * @param serialNumber the serial number, which must be positive
         * @return this builder
         * @throws NullPointerException if {@code serialNumber} is {@code null}
         */
        public Builder serialNumber(BigInteger serialNumber) {
            this.serialNumber = Objects.requireNonNull(serialNumber, "serialNumber cannot be null");
            return this;
        }

        /**
         * Sets the validity window.
         *
         * @param notBefore the inclusive start
         * @param notAfter  the inclusive end
         * @return this builder
         * @throws NullPointerException if {@code notBefore} or {@code notAfter} is {@code null}
         */
        public Builder validity(Instant notBefore, Instant notAfter) {
            this.notBefore = Objects.requireNonNull(notBefore, "notBefore cannot be null");
            this.notAfter = Objects.requireNonNull(notAfter, "notAfter cannot be null");
            return this;
        }

        /**
         * Validates the accumulated fields and produces the immutable spec.
         *
         * @return the immutable certificate spec
         * @throws IllegalStateException    if the key pair, subject, serial number, or validity window was never
         *                                  set
         * @throws IllegalArgumentException if the serial number is not positive or {@code notBefore} is after
         *                                  {@code notAfter}
         */
        public X509CertificateSpec build() {
            if (keyPair == null || subject == null || serialNumber == null || notBefore == null || notAfter == null) {
                throw new IllegalStateException("keyPair, subject, serialNumber, and validity must all be set");
            }
            if (serialNumber.signum() <= 0) {
                throw new IllegalArgumentException("serialNumber must be positive: " + serialNumber);
            }
            if (notBefore.isAfter(notAfter)) {
                throw new IllegalArgumentException("notBefore must not be after notAfter");
            }
            return new X509CertificateSpec(this);
        }
    }
}
