package com.github.auties00.cobalt.registration.push.apns;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Holds the full serialisable state of an {@link ApnsClient}.
 *
 * <p>Carries the immutable {@link ApnsConfig} plus the device-bound credentials accumulated by the
 * activation pipeline. Two of the credentials persist for roughly three years (the RSA keypair and
 * the {@code deviceCertificate} Apple signs against it); the third piece of state held by a running
 * client, the courier auth token, is rebuilt on every connection and is not part of this
 * serialisable snapshot. A caller round-trips this class via {@link ApnsClient#getSession()} and
 * {@link ApnsClient#loadSession(ApnsSession)} to keep the same device identity across process
 * restarts without re-running the activation flow.
 */
@ProtobufMessage(name = "ApnsSession")
public final class ApnsSession {
    /**
     * Holds the configuration this session was created with.
     *
     * <p>Bundled in the serialised output so a saved session loads back without the caller having to
     * remember which {@link ApnsConfig} it was originally created against.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    ApnsConfig config;

    /**
     * Holds the PKCS#8-encoded RSA private key (DER).
     *
     * <p>Generated once and bound to {@link #deviceCertificate}; zero-length until activation
     * succeeds.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] privateKeyDer;

    /**
     * Holds the X.509-encoded RSA public key (DER), the {@code SubjectPublicKeyInfo} that goes into
     * the activation CSR.
     *
     * <p>Zero-length until activation succeeds.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] publicKeyDer;

    /**
     * Holds the DER-encoded device certificate returned by {@code albert.apple.com} after a
     * successful activation.
     *
     * <p>Valid for roughly three years; once it expires {@link ApnsClient} must re-run the activation
     * flow against {@code albert.apple.com} to obtain a fresh certificate. Zero-length until
     * activation succeeds.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    byte[] deviceCertificate;

    /**
     * Constructs a session from a configuration and its credentials.
     *
     * <p>Reachable by the generated {@code ApnsSessionBuilder} and by {@link #newSession(ApnsConfig)}.
     * Normalises {@code null} byte arrays into zero-length arrays so the activation flow can probe
     * "credential present" via {@code length > 0}.
     *
     * @param config            the topic-list configuration
     * @param privateKeyDer     the PKCS#8 DER bytes, or {@code null}
     * @param publicKeyDer      the X.509 DER bytes, or {@code null}
     * @param deviceCertificate the device certificate DER bytes, or {@code null}
     */
    ApnsSession(ApnsConfig config, byte[] privateKeyDer, byte[] publicKeyDer, byte[] deviceCertificate) {
        this.config = config;
        this.privateKeyDer = privateKeyDer == null ? new byte[0] : privateKeyDer;
        this.publicKeyDer = publicKeyDer == null ? new byte[0] : publicKeyDer;
        this.deviceCertificate = deviceCertificate == null ? new byte[0] : deviceCertificate;
    }

    /**
     * Creates an empty session bound to a configuration.
     *
     * <p>Used by {@link ApnsClient#authenticate} on a first-run client, where every credential field
     * is zero-length until the activation flow populates it.
     *
     * @param config the topic list to bind the new session to
     * @return a fresh credential-less session
     */
    static ApnsSession newSession(ApnsConfig config) {
        return new ApnsSession(config, new byte[0], new byte[0], new byte[0]);
    }

    /**
     * Returns the bound configuration.
     *
     * @return the configuration this session was created with
     */
    public ApnsConfig config() {
        return config;
    }

    /**
     * Returns the PKCS#8-encoded RSA private key bytes.
     *
     * @return the private key DER bytes, or a zero-length array before activation has populated them
     */
    public byte[] privateKeyDer() {
        return privateKeyDer;
    }

    /**
     * Returns the X.509-encoded RSA public key bytes.
     *
     * @return the public key DER bytes, or a zero-length array before activation has populated them
     */
    public byte[] publicKeyDer() {
        return publicKeyDer;
    }

    /**
     * Returns the device certificate bytes.
     *
     * @return the device certificate DER bytes, or a zero-length array before activation has
     *         populated them
     */
    public byte[] deviceCertificate() {
        return deviceCertificate;
    }

    /**
     * Stores the freshly generated private key bytes.
     *
     * <p>Invoked by the activation flow on a successful round-trip.
     *
     * @param privateKeyDer the PKCS#8 DER bytes
     */
    void setPrivateKeyDer(byte[] privateKeyDer) {
        this.privateKeyDer = privateKeyDer;
    }

    /**
     * Stores the freshly generated public key bytes.
     *
     * <p>Invoked by the activation flow on a successful round-trip.
     *
     * @param publicKeyDer the X.509 DER bytes
     */
    void setPublicKeyDer(byte[] publicKeyDer) {
        this.publicKeyDer = publicKeyDer;
    }

    /**
     * Stores the Apple-signed device certificate bytes.
     *
     * <p>Invoked by the activation flow on a successful round-trip.
     *
     * @param deviceCertificate the device certificate DER bytes
     */
    void setDeviceCertificate(byte[] deviceCertificate) {
        this.deviceCertificate = deviceCertificate;
    }
}
