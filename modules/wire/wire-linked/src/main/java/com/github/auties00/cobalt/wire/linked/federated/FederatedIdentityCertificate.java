package com.github.auties00.cobalt.wire.linked.federated;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Aggregate of PEM bundles the federated-identity ("Waffle") relay hands back
 * to the client to bootstrap subsequent encrypted exchanges.
 *
 * <p>The {@code GetCertificate} RPC is the second step of the linking flow.
 * The relay returns a server-stamped reply timestamp together with up to three
 * PEM bundles: an encryption PEM (used to seal encrypted payloads sent to the
 * bridge), a signature PEM (used to verify bridge-signed responses), and a
 * password PEM (used by the password-rotation surface). Which subset is
 * returned depends on the request flags; any unrequested bundle is absent.
 *
 * <p>The reply timestamp is wall-clock, in seconds since the UNIX epoch, and
 * is the anchor against which each PEM's {@link FederatedIdentityPem#ttl()}
 * is measured.
 */
@ProtobufMessage(name = "FederatedIdentityCertificate")
public final class FederatedIdentityCertificate {
    /**
     * Server-stamped reply timestamp, in seconds since the UNIX epoch. Each
     * PEM's TTL is measured from this anchor.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT64)
    long replyTimestamp;

    /**
     * The encryption PEM, when the client requested it and the relay supplied
     * one. Wraps the public key used to seal encrypted payloads sent to the
     * bridge.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    FederatedIdentityPem encryptionPem;

    /**
     * The signature PEM, when the client requested it and the relay supplied
     * one. Wraps the public key used to verify bridge-signed responses.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    FederatedIdentityPem signaturePem;

    /**
     * The password PEM, when the client requested it and the relay supplied
     * one. Wraps the public key used by the password-rotation surface; this
     * is the only PEM that ships with a non-empty
     * {@link FederatedIdentityPem#keyId()}.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    FederatedIdentityPem passwordPem;

    /**
     * Constructs a new {@code FederatedIdentityCertificate}.
     *
     * @param replyTimestamp the server-stamped reply timestamp, in seconds
     * @param encryptionPem  the encryption PEM, or {@code null} when not
     *                       included
     * @param signaturePem   the signature PEM, or {@code null} when not
     *                       included
     * @param passwordPem    the password PEM, or {@code null} when not
     *                       included
     */
    FederatedIdentityCertificate(long replyTimestamp, FederatedIdentityPem encryptionPem,
                                 FederatedIdentityPem signaturePem, FederatedIdentityPem passwordPem) {
        this.replyTimestamp = replyTimestamp;
        this.encryptionPem = encryptionPem;
        this.signaturePem = signaturePem;
        this.passwordPem = passwordPem;
    }

    /**
     * Returns the server-stamped reply timestamp, in seconds since the UNIX
     * epoch.
     *
     * @return the reply timestamp
     */
    public long replyTimestamp() {
        return replyTimestamp;
    }

    /**
     * Returns the encryption PEM, when the relay supplied one.
     *
     * @return an {@link Optional} containing the encryption PEM, or empty
     */
    public Optional<FederatedIdentityPem> encryptionPem() {
        return Optional.ofNullable(encryptionPem);
    }

    /**
     * Returns the signature PEM, when the relay supplied one.
     *
     * @return an {@link Optional} containing the signature PEM, or empty
     */
    public Optional<FederatedIdentityPem> signaturePem() {
        return Optional.ofNullable(signaturePem);
    }

    /**
     * Returns the password PEM, when the relay supplied one.
     *
     * @return an {@link Optional} containing the password PEM, or empty
     */
    public Optional<FederatedIdentityPem> passwordPem() {
        return Optional.ofNullable(passwordPem);
    }

    /**
     * Replaces the reply timestamp.
     *
     * @param replyTimestamp the new reply timestamp, in seconds
     */
    public void setReplyTimestamp(long replyTimestamp) {
        this.replyTimestamp = replyTimestamp;
    }

    /**
     * Replaces the encryption PEM.
     *
     * @param encryptionPem the new encryption PEM, or {@code null} to clear
     */
    public void setEncryptionPem(FederatedIdentityPem encryptionPem) {
        this.encryptionPem = encryptionPem;
    }

    /**
     * Replaces the signature PEM.
     *
     * @param signaturePem the new signature PEM, or {@code null} to clear
     */
    public void setSignaturePem(FederatedIdentityPem signaturePem) {
        this.signaturePem = signaturePem;
    }

    /**
     * Replaces the password PEM.
     *
     * @param passwordPem the new password PEM, or {@code null} to clear
     */
    public void setPasswordPem(FederatedIdentityPem passwordPem) {
        this.passwordPem = passwordPem;
    }
}
