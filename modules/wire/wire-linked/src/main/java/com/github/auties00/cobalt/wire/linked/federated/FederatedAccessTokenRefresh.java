package com.github.auties00.cobalt.wire.linked.federated;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Reply payload of a federated-identity ("Waffle") access-token refresh.
 *
 * <p>Once a WhatsApp account is linked to a Meta-side identity, the bridge
 * holds short-lived OAuth-style access tokens that the linked surfaces (for
 * instance, the cross-app messaging surface to Instagram or Facebook) consume
 * on the WhatsApp side. The client must rotate these tokens periodically.
 *
 * <p>The refresh RPC submits an encrypted payload that proves the client still
 * controls the linked Facebook id and asks the bridge to mint fresh tokens.
 * The relay returns the rotated tokens wrapped inside a {@link
 * FederatedRsaEncryption} envelope; the client can only decrypt it using the
 * symmetric key it embedded in the request.
 */
@ProtobufMessage(name = "FederatedAccessTokenRefresh")
public final class FederatedAccessTokenRefresh {
    /**
     * RSA-2048 envelope carrying the rotated access tokens. The plaintext is
     * a Meta-side payload understood by the linked surface; only the client
     * holds the symmetric key to decrypt it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    FederatedRsaEncryption encryption;

    /**
     * Constructs a new {@code FederatedAccessTokenRefresh} reply.
     *
     * @param encryption the relay-returned encryption envelope, or
     *                   {@code null} when absent
     */
    FederatedAccessTokenRefresh(FederatedRsaEncryption encryption) {
        this.encryption = encryption;
    }

    /**
     * Returns the relay-returned encryption envelope carrying the rotated
     * access tokens.
     *
     * @return an {@link Optional} containing the envelope, or empty
     */
    public Optional<FederatedRsaEncryption> encryption() {
        return Optional.ofNullable(encryption);
    }

    /**
     * Replaces the encryption envelope.
     *
     * @param encryption the new envelope, or {@code null} to clear
     */
    public void setEncryption(FederatedRsaEncryption encryption) {
        this.encryption = encryption;
    }
}
