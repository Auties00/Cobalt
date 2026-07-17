package com.github.auties00.cobalt.wire.linked.federated;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Reply payload of a federated-identity ("Waffle") encrypted-action RPC.
 *
 * <p>The encrypted-action RPC is the catch-all path through which the client
 * asks the bridge to perform any Meta-side action on the linked account that
 * does not have a more specific RPC: profile fetches, settings updates,
 * cross-surface state queries, and so on. The action body is a Meta-side
 * payload that the relay forwards verbatim; the response carries either the
 * Meta-side reply (wrapped inside a {@link FederatedRsaEncryption} envelope)
 * or a {@code wf_deleted} marker that tells the client the bridge has dropped
 * the federated-identity link entirely.
 *
 * <p>When {@link #deleted()} is {@code true} the client must purge any local
 * state that depended on the link and surface a re-link prompt to the user
 * before issuing further encrypted RPCs.
 */
@ProtobufMessage(name = "FederatedEncryptedAction")
public final class FederatedEncryptedAction {
    /**
     * RSA-2048 envelope carrying the Meta-side response payload. Always
     * present alongside a successful action.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    FederatedRsaEncryption encryption;

    /**
     * Whether the bridge surfaced a {@code <wf_deleted/>} marker in the
     * reply. {@code true} means the federated-identity link was dropped on
     * the bridge side and the client must purge its local link state.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    Boolean deleted;

    /**
     * Constructs a new {@code FederatedEncryptedAction} reply.
     *
     * @param encryption the relay-returned encryption envelope, or
     *                   {@code null} when absent
     * @param deleted    the {@code wf_deleted} marker, or {@code null} when
     *                   the relay did not surface one
     */
    FederatedEncryptedAction(FederatedRsaEncryption encryption, Boolean deleted) {
        this.encryption = encryption;
        this.deleted = deleted;
    }

    /**
     * Returns the relay-returned encryption envelope.
     *
     * @return an {@link Optional} containing the envelope, or empty
     */
    public Optional<FederatedRsaEncryption> encryption() {
        return Optional.ofNullable(encryption);
    }

    /**
     * Returns the {@code wf_deleted} marker, when present.
     *
     * @return an {@link Optional} containing the deletion flag, or empty
     *         when the relay did not surface one
     */
    public Optional<Boolean> deleted() {
        return Optional.ofNullable(deleted);
    }

    /**
     * Replaces the encryption envelope.
     *
     * @param encryption the new envelope, or {@code null} to clear
     */
    public void setEncryption(FederatedRsaEncryption encryption) {
        this.encryption = encryption;
    }

    /**
     * Replaces the {@code wf_deleted} marker.
     *
     * @param deleted the new flag, or {@code null} to clear
     */
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
