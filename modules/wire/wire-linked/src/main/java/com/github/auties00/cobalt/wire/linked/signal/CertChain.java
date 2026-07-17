package com.github.auties00.cobalt.wire.linked.signal;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Represents a chain of Noise protocol certificates used by WhatsApp to verify
 * the identity of the server during the encrypted handshake.
 *
 * <p>WhatsApp secures its client-to-server channel with the Noise protocol
 * framework. During the handshake, the server presents a certificate chain
 * that the client validates against a pinned root authority. A chain consists
 * of a leaf certificate, which carries the server's static Noise public key,
 * and an optional intermediate certificate that bridges the leaf to the
 * trusted root. Clients reject the handshake if either certificate is invalid,
 * expired, or signed by an unknown issuer.
 *
 * <p>This class is used transparently by the socket layer when establishing
 * a connection to WhatsApp's servers and is rarely touched by application code.
 */
@ProtobufMessage(name = "CertChain")
public final class CertChain {
    /**
     * The leaf certificate, which carries the server's static Noise public key
     * and is signed by the intermediate certificate.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    NoiseCertificate leaf;

    /**
     * The intermediate certificate, which links the leaf to the trusted root
     * authority. May be absent when the leaf is directly signed by the root.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    NoiseCertificate intermediate;

    /**
     * Constructs a new certificate chain from the given leaf and intermediate
     * certificates.
     *
     * @param leaf         the leaf certificate, or {@code null} if not set
     * @param intermediate the intermediate certificate, or {@code null} if not set
     */
    CertChain(NoiseCertificate leaf, NoiseCertificate intermediate) {
        this.leaf = leaf;
        this.intermediate = intermediate;
    }

    /**
     * Returns the leaf certificate of this chain.
     *
     * <p>The leaf certificate carries the server's static Noise public key and
     * is the certificate actually used to authenticate the remote peer during
     * the Noise handshake.
     *
     * @return an {@link Optional} containing the leaf certificate, or
     *         {@link Optional#empty()} if the chain does not include one
     */
    public Optional<NoiseCertificate> leaf() {
        return Optional.ofNullable(leaf);
    }

    /**
     * Returns the intermediate certificate of this chain.
     *
     * <p>When present, the intermediate certificate must verify against the
     * pinned root authority and its public key must in turn verify the leaf
     * certificate's signature.
     *
     * @return an {@link Optional} containing the intermediate certificate, or
     *         {@link Optional#empty()} if the chain has no intermediate
     */
    public Optional<NoiseCertificate> intermediate() {
        return Optional.ofNullable(intermediate);
    }

    /**
     * Replaces the leaf certificate of this chain.
     *
     * @param leaf the new leaf certificate, or {@code null} to clear it
     */
    public void setLeaf(NoiseCertificate leaf) {
        this.leaf = leaf;
    }

    /**
     * Replaces the intermediate certificate of this chain.
     *
     * @param intermediate the new intermediate certificate, or {@code null} to clear it
     */
    public void setIntermediate(NoiseCertificate intermediate) {
        this.intermediate = intermediate;
    }

    /**
     * Represents a single Noise protocol certificate within a {@link CertChain}.
     *
     * <p>A Noise certificate packages two fields: a {@code details} payload
     * describing the certified identity and validity window, and a
     * {@code signature} computed by the issuer over those details. Verification
     * consists of parsing the details, checking the validity period, and
     * cryptographically verifying the signature with the issuer's public key.
     *
     * <p>This inner variant is used exclusively inside {@link CertChain} and
     * mirrors the protobuf message that WhatsApp sends nested within the chain.
     */
    @ProtobufMessage(name = "CertChain.NoiseCertificate")
    public static final class NoiseCertificate {
        /**
         * The raw encoded bytes of the certificate's {@link Details} payload,
         * carrying the identity and validity information that the signature
         * authenticates.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
        byte[] details;

        /**
         * The signature computed by the issuing authority over the
         * {@code details} bytes.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] signature;

        /**
         * Constructs a new Noise certificate with the given encoded details
         * and signature.
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
         * <p>The returned bytes can be decoded into a {@link Details} message
         * to inspect the serial number, validity window, and certified public key.
         *
         * @return an {@link Optional} containing the encoded details bytes, or
         *         {@link Optional#empty()} if the certificate carries no details
         */
        public Optional<byte[]> details() {
            return Optional.ofNullable(details);
        }

        /**
         * Returns the signature over this certificate's details.
         *
         * <p>The signature is produced by the issuer and verified by the client
         * using the issuer's public key to confirm authenticity.
         *
         * @return an {@link Optional} containing the signature bytes, or
         *         {@link Optional#empty()} if the certificate is unsigned
         */
        public Optional<byte[]> signature() {
            return Optional.ofNullable(signature);
        }

        /**
         * Replaces the encoded details payload of this certificate.
         *
         * @param details the new encoded details bytes, or {@code null} to clear
         */
        public void setDetails(byte[] details) {
            this.details = details;
    }

        /**
         * Replaces the signature of this certificate.
         *
         * @param signature the new signature bytes, or {@code null} to clear
         */
        public void setSignature(byte[] signature) {
            this.signature = signature;
    }

        /**
         * Represents the decoded details of a nested {@link NoiseCertificate}.
         *
         * <p>The details describe the certified identity and its validity. They
         * include a unique {@code serial} number, the {@code issuerSerial} of
         * the signing authority, the certified public {@code key}, and the
         * {@code notBefore} and {@code notAfter} timestamps that bound the
         * certificate's active lifetime.
         *
         * <p>Clients decode this structure from the raw {@code details} bytes
         * of a certificate, check that the current time falls within
         * {@code notBefore} and {@code notAfter}, and then use {@code key} as
         * the trust anchor for downstream verification.
         */
        @ProtobufMessage(name = "CertChain.NoiseCertificate.Details")
        public static final class Details {
            /**
             * The unique serial number identifying this certificate among
             * those issued by the same authority.
             */
            @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
            Integer serial;

            /**
             * The serial number of the issuer's certificate, used to locate
             * the public key that verifies this certificate's signature.
             */
            @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
            Integer issuerSerial;

            /**
             * The certified public key bytes carried by this certificate.
             */
            @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
            byte[] key;

            /**
             * The Unix timestamp, in seconds, before which this certificate
             * is not yet valid.
             */
            @ProtobufProperty(index = 4, type = ProtobufType.UINT64)
            Long notBefore;

            /**
             * The Unix timestamp, in seconds, after which this certificate
             * is no longer valid.
             */
            @ProtobufProperty(index = 5, type = ProtobufType.UINT64)
            Long notAfter;

            /**
             * Constructs new certificate details.
             *
             * @param serial       the certificate's serial number, or {@code null}
             * @param issuerSerial the issuer's serial number, or {@code null}
             * @param key          the certified public key bytes, or {@code null}
             * @param notBefore    the start of the validity window in seconds, or {@code null}
             * @param notAfter     the end of the validity window in seconds, or {@code null}
             */
            Details(Integer serial, Integer issuerSerial, byte[] key, Long notBefore, Long notAfter) {
                this.serial = serial;
                this.issuerSerial = issuerSerial;
                this.key = key;
                this.notBefore = notBefore;
                this.notAfter = notAfter;
            }

            /**
             * Returns the serial number of this certificate.
             *
             * @return an {@link OptionalInt} with the serial number, or
             *         {@link OptionalInt#empty()} if not set
             */
            public OptionalInt serial() {
                return serial == null ? OptionalInt.empty() : OptionalInt.of(serial);
            }

            /**
             * Returns the serial number of the issuer that signed this
             * certificate.
             *
             * @return an {@link OptionalInt} with the issuer's serial, or
             *         {@link OptionalInt#empty()} if not set
             */
            public OptionalInt issuerSerial() {
                return issuerSerial == null ? OptionalInt.empty() : OptionalInt.of(issuerSerial);
            }

            /**
             * Returns the certified public key bytes.
             *
             * @return an {@link Optional} containing the key bytes, or
             *         {@link Optional#empty()} if not set
             */
            public Optional<byte[]> key() {
                return Optional.ofNullable(key);
            }

            /**
             * Returns the start of this certificate's validity window as a
             * Unix timestamp in seconds.
             *
             * <p>Clients must reject the certificate if the current time is
             * earlier than this value.
             *
             * @return an {@link OptionalLong} with the start of validity, or
             *         {@link OptionalLong#empty()} if not set
             */
            public OptionalLong notBefore() {
                return notBefore == null ? OptionalLong.empty() : OptionalLong.of(notBefore);
            }

            /**
             * Returns the end of this certificate's validity window as a
             * Unix timestamp in seconds.
             *
             * <p>Clients must reject the certificate if the current time is
             * later than this value.
             *
             * @return an {@link OptionalLong} with the end of validity, or
             *         {@link OptionalLong#empty()} if not set
             */
            public OptionalLong notAfter() {
                return notAfter == null ? OptionalLong.empty() : OptionalLong.of(notAfter);
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
             * Replaces the issuer's serial number.
             *
             * @param issuerSerial the new issuer serial number, or {@code null} to clear
             */
            public void setsuerSerial(Integer issuerSerial) {
                this.issuerSerial = issuerSerial;
    }

            /**
             * Replaces the certified public key bytes.
             *
             * @param key the new key bytes, or {@code null} to clear
             */
            public void setKey(byte[] key) {
                this.key = key;
    }

            /**
             * Replaces the start of this certificate's validity window.
             *
             * @param notBefore the new start of validity as a Unix timestamp in seconds,
             *                  or {@code null} to clear
             */
            public void setNotBefore(Long notBefore) {
                this.notBefore = notBefore;
    }

            /**
             * Replaces the end of this certificate's validity window.
             *
             * @param notAfter the new end of validity as a Unix timestamp in seconds,
             *                 or {@code null} to clear
             */
            public void setNotAfter(Long notAfter) {
                this.notAfter = notAfter;
    }
        }
    }
}
