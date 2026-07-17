package com.github.auties00.cobalt.wire.linked.signal;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Represents a standalone Noise protocol certificate presented by the WhatsApp
 * server during the Noise handshake.
 *
 * <p>WhatsApp secures its socket-level connection with the Noise protocol
 * framework. On every connection, the server sends a certificate containing
 * its static Noise public key together with a signature issued by a trusted
 * authority. The client pins the authority's public key, verifies the
 * signature, checks the certificate has not expired, and only then proceeds
 * with the handshake.
 *
 * <p>A Noise certificate is composed of two opaque parts: a {@code details}
 * payload that describes the certified identity, the serial number, the
 * certified public key, and the validity window, and a {@code signature}
 * computed by the issuer over those details. Verification consists of
 * decoding the details, confirming the certificate is currently valid, and
 * verifying the signature with the issuer's public key.
 */
@ProtobufMessage(name = "NoiseCertificate")
public final class NoiseCertificate {
    /**
     * The raw encoded {@link Details} payload carrying the identity, public
     * key, and validity window that the signature authenticates.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] details;

    /**
     * The signature computed by the issuing authority over the {@code details}
     * bytes. Verified by the client against the authority's pinned public key.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] signature;

    /**
     * Constructs a new Noise certificate from the given encoded details and
     * signature.
     *
     * @param details   the raw encoded details payload, or {@code null}
     * @param signature the signature over the details, or {@code null}
     */
    NoiseCertificate(byte[] details, byte[] signature) {
        this.details = details;
        this.signature = signature;
    }

    /**
     * Returns the encoded details payload of this certificate.
     *
     * <p>The returned bytes can be decoded into a {@link Details} message to
     * inspect the serial number, issuer, subject, validity window, and the
     * certified public key.
     *
     * @return an {@link Optional} containing the encoded details bytes, or
     *         {@link Optional#empty()} if the certificate carries no details
     */
    public Optional<byte[]> details() {
        return Optional.ofNullable(details);
    }

    /**
     * Returns the signature computed over this certificate's details.
     *
     * @return an {@link Optional} containing the signature bytes, or
     *         {@link Optional#empty()} if the certificate is unsigned
     */
    public Optional<byte[]> signature() {
        return Optional.ofNullable(signature);
    }

    /**
     * Replaces the encoded details payload.
     *
     * @param details the new encoded details bytes, or {@code null} to clear
     */
    public void setDetails(byte[] details) {
        this.details = details;
    }

    /**
     * Replaces the signature.
     *
     * @param signature the new signature bytes, or {@code null} to clear
     */
    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    /**
     * Represents the decoded content of a {@link NoiseCertificate}.
     *
     * <p>The details describe who issued the certificate, whom it was issued
     * to, the public key it certifies, and the moment at which it expires.
     * A client decodes this structure from the raw {@code details} bytes of
     * a {@link NoiseCertificate}, confirms the current time is before
     * {@code expires}, and then uses {@code key} as the authenticated public
     * key of the certificate's {@code subject}.
     */
    @ProtobufMessage(name = "NoiseCertificate.Details")
    public static final class Details {
        /**
         * The unique serial number assigned to this certificate by its issuer.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
        Integer serial;

        /**
         * A textual label identifying the authority that issued this
         * certificate.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String issuer;

        /**
         * The Unix timestamp, in seconds, at which this certificate ceases
         * to be valid.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
        Long expires;

        /**
         * A textual label identifying the entity this certificate was issued
         * to. Clients use this field to confirm that the certificate applies
         * to the peer they are connecting to.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        String subject;

        /**
         * The certified public key bytes. After verification, this is the
         * key that can be trusted as belonging to {@code subject}.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.BYTES)
        byte[] key;

        /**
         * Constructs new certificate details.
         *
         * @param serial  the certificate's serial number, or {@code null}
         * @param issuer  the issuer label, or {@code null}
         * @param expires the expiry time as a Unix timestamp in seconds, or {@code null}
         * @param subject the subject label, or {@code null}
         * @param key     the certified public key bytes, or {@code null}
         */
        Details(Integer serial, String issuer, Long expires, String subject, byte[] key) {
            this.serial = serial;
            this.issuer = issuer;
            this.expires = expires;
            this.subject = subject;
            this.key = key;
        }

        /**
         * Returns this certificate's serial number.
         *
         * @return an {@link OptionalInt} with the serial number, or
         *         {@link OptionalInt#empty()} if not set
         */
        public OptionalInt serial() {
            return serial == null ? OptionalInt.empty() : OptionalInt.of(serial);
        }

        /**
         * Returns the label identifying the issuing authority.
         *
         * @return an {@link Optional} with the issuer label, or
         *         {@link Optional#empty()} if not set
         */
        public Optional<String> issuer() {
            return Optional.ofNullable(issuer);
        }

        /**
         * Returns the expiry time of this certificate as a Unix timestamp in
         * seconds.
         *
         * <p>Clients must reject the certificate if the current time is at
         * or after this value.
         *
         * @return an {@link OptionalLong} with the expiry time, or
         *         {@link OptionalLong#empty()} if not set
         */
        public OptionalLong expires() {
            return expires == null ? OptionalLong.empty() : OptionalLong.of(expires);
        }

        /**
         * Returns the label identifying the entity this certificate was
         * issued to.
         *
         * @return an {@link Optional} with the subject label, or
         *         {@link Optional#empty()} if not set
         */
        public Optional<String> subject() {
            return Optional.ofNullable(subject);
        }

        /**
         * Returns the certified public key bytes.
         *
         * @return an {@link Optional} with the key bytes, or
         *         {@link Optional#empty()} if not set
         */
        public Optional<byte[]> key() {
            return Optional.ofNullable(key);
        }

        /**
         * Replaces this certificate's serial number.
         *
         * @param serial the new serial number, or {@code null} to clear
         */
        public void setSerial(Integer serial) {
            this.serial = serial;
    }

        /**
         * Replaces the issuer label.
         *
         * @param issuer the new issuer label, or {@code null} to clear
         */
        public void setsuer(String issuer) {
            this.issuer = issuer;
    }

        /**
         * Replaces the expiry time.
         *
         * @param expires the new expiry as a Unix timestamp in seconds,
         *                or {@code null} to clear
         */
        public void setExpires(Long expires) {
            this.expires = expires;
    }

        /**
         * Replaces the subject label.
         *
         * @param subject the new subject label, or {@code null} to clear
         */
        public void setSubject(String subject) {
            this.subject = subject;
    }

        /**
         * Replaces the certified public key bytes.
         *
         * @param key the new key bytes, or {@code null} to clear
         */
        public void setKey(byte[] key) {
            this.key = key;
    }
    }
}
