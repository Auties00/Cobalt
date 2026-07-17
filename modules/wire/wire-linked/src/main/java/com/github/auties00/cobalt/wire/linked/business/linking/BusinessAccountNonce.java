package com.github.auties00.cobalt.wire.linked.business.linking;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Single-use nonce issued for a WhatsApp Business account-linking handshake.
 *
 * <p>The WhatsApp-to-Facebook account-linking flow requires the WhatsApp client
 * to prove possession of the account by echoing a server-issued nonce back to
 * the Meta-side surface. The server returns the issued {@linkplain #nonce()
 * nonce} together with the identifier of the request that produced it
 * ({@linkplain #requestId() request id}) so the caller can correlate the
 * issued nonce with its evidencing request.
 */
@ProtobufMessage(name = "BusinessAccountNonce")
public final class BusinessAccountNonce {
    /**
     * Issued single-use nonce. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String nonce;

    /**
     * Identifier of the request that produced the nonce. {@code null} when the
     * server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String requestId;

    /**
     * Constructs a new {@code BusinessAccountNonce}. The reference arguments
     * may be {@code null} when the server omitted them.
     *
     * @param nonce     the issued single-use nonce, or {@code null}
     * @param requestId the originating request identifier, or {@code null}
     */
    BusinessAccountNonce(String nonce, String requestId) {
        this.nonce = nonce;
        this.requestId = requestId;
    }

    /**
     * Returns the issued single-use nonce.
     *
     * @return the nonce, or empty when the server omitted it
     */
    public Optional<String> nonce() {
        return Optional.ofNullable(nonce);
    }

    /**
     * Returns the identifier of the request that produced the nonce.
     *
     * @return the request identifier, or empty when the server omitted it
     */
    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }
}
