package com.github.auties00.cobalt.wire.linked.message.system.history;

import com.github.auties00.cobalt.wire.linked.message.Message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Carries metadata that correlates a full on-demand history sync operation with
 * its originating request.
 *
 * <p>When a user asks WhatsApp to deliver the complete chat history that is not
 * already cached on the current device, the companion device dispatches a peer
 * request and later receives one or more chunked responses. This metadata is
 * attached to both the request and the response so the client can match an
 * incoming chunk to the operation that asked for it.
 */
@ProtobufMessage(name = "Message.FullHistorySyncOnDemandRequestMetadata")
public final class FullHistorySyncOnDemandRequestMetadata implements Message {
    /**
     * The opaque identifier of the originating full history sync request.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String requestId;


    /**
     * Constructs a new metadata instance for a full on-demand history sync
     * request.
     *
     * @param requestId the opaque request identifier, may be {@code null}
     */
    FullHistorySyncOnDemandRequestMetadata(String requestId) {
        this.requestId = requestId;
    }

    /**
     * Returns the identifier of the originating full history sync request.
     *
     * @return an {@link Optional} containing the request identifier, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }

    /**
     * Sets the identifier of the originating full history sync request.
     *
     * @param requestId the opaque request identifier, may be {@code null}
     */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
