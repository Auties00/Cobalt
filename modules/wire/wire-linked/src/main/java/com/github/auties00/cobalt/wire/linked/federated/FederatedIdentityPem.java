package com.github.auties00.cobalt.wire.linked.federated;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * One PEM-encoded certificate or key bundle returned by the federated-identity
 * ("Waffle") {@code GetCertificate} RPC.
 *
 * <p>The reply to {@code GetCertificate} carries up to three PEM bundles, all
 * sharing the same {@code (ttl, key-id?, content-bytes)} shape:
 *
 * <ul>
 *   <li>The <b>encryption PEM</b> wraps the public key the client uses to
 *       seal subsequent encrypted payloads to the bridge.</li>
 *   <li>The <b>signature PEM</b> wraps the public key the client uses to
 *       verify bridge-signed responses.</li>
 *   <li>The <b>password PEM</b> wraps the public key used to seal the linked
 *       Meta-account password material; this is the only variant that
 *       carries a non-empty {@link #keyId()}.</li>
 * </ul>
 *
 * <p>The TTL is in seconds; clients must refetch the bundle once it elapses.
 */
@ProtobufMessage(name = "FederatedIdentityPem")
public final class FederatedIdentityPem {
    /**
     * Time-to-live for this bundle, in seconds. The client must consider the
     * PEM expired and refetch it once this many seconds have elapsed since
     * the {@link FederatedIdentityCertificate#replyTimestamp() reply
     * timestamp}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    int ttl;

    /**
     * Numeric key identifier for this PEM. Only set on the password PEM, where
     * the bridge needs to identify which of multiple rotating password keys
     * the client should use.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    Integer keyId;

    /**
     * Raw PEM bytes for the wrapped certificate or public key. Encoded as the
     * relay produced it, including the ASCII armour markers; callers parse it
     * with their preferred PEM reader.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] pem;

    /**
     * Constructs a new {@code FederatedIdentityPem}.
     *
     * @param ttl   the time-to-live in seconds
     * @param keyId the key identifier, or {@code null} when not set
     * @param pem   the raw PEM bytes, or {@code null} when not set
     */
    FederatedIdentityPem(int ttl, Integer keyId, byte[] pem) {
        this.ttl = ttl;
        this.keyId = keyId;
        this.pem = pem;
    }

    /**
     * Returns the time-to-live for this bundle, in seconds.
     *
     * @return the TTL
     */
    public int ttl() {
        return ttl;
    }

    /**
     * Returns the numeric key identifier for this PEM.
     *
     * @return an {@link OptionalInt} containing the key id, or empty when not
     *         set
     */
    public OptionalInt keyId() {
        return keyId == null ? OptionalInt.empty() : OptionalInt.of(keyId);
    }

    /**
     * Returns the raw PEM bytes for the wrapped certificate or public key.
     *
     * @return an {@link Optional} containing the PEM bytes, or empty when not
     *         set
     */
    public Optional<byte[]> pem() {
        return Optional.ofNullable(pem);
    }

    /**
     * Replaces the time-to-live.
     *
     * @param ttl the new TTL, in seconds
     */
    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    /**
     * Replaces the numeric key identifier.
     *
     * @param keyId the new key id, or {@code null} to clear
     */
    public void setKeyId(Integer keyId) {
        this.keyId = keyId;
    }

    /**
     * Replaces the raw PEM bytes.
     *
     * @param pem the new PEM bytes, or {@code null} to clear
     */
    public void setPem(byte[] pem) {
        this.pem = pem;
    }
}
