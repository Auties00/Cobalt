package com.github.auties00.cobalt.wire.linked.message.system;

import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.Message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A compatibility envelope that carries a {@link LinkedMessageContainer} produced by
 * a newer WhatsApp client than the one currently parsing the traffic.
 *
 * <p>When an older build encounters an unknown message type it wraps the
 * payload in this container so that it can be relayed, stored and forwarded
 * without being decoded, preserving forward compatibility across client
 * versions. Newer clients that understand the inner payload simply unwrap it.
 */
@ProtobufMessage(name = "Message.FutureProofMessage")
public final class FutureProofMessage implements Message {
    /**
     * The opaque inner payload, preserved verbatim as a {@link LinkedMessageContainer}
     * so that newer clients can decode it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    LinkedMessageContainer messageContainer;


    /**
     * Constructs a new future-proof envelope wrapping the given payload.
     *
     * @param messageContainer the inner payload to preserve, may be {@code null}
     */
    FutureProofMessage(LinkedMessageContainer messageContainer) {
        this.messageContainer = messageContainer;
    }

    /**
     * Returns the inner payload carried by this envelope.
     *
     * @return an {@link Optional} containing the {@link LinkedMessageContainer}, or
     *         {@link Optional#empty()} if no payload is set
     */
    public Optional<LinkedMessageContainer> message() {
        return Optional.ofNullable(messageContainer);
    }

    /**
     * Sets the inner payload carried by this envelope.
     *
     * @param messageContainer the new message container, or {@code null} to clear it
     */
    public void setMessage(LinkedMessageContainer messageContainer) {
        this.messageContainer = messageContainer;
    }
}
