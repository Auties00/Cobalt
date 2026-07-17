package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Batched envelope for one or more {@link AppDataMessage} payloads.
 *
 * <p>WhatsApp's AppData stream coalesces application-data messages into
 * batches so the receiver can apply them atomically. A producer that has
 * one reaction to send still wraps it in an {@code AppDataPayloads} with
 * a single-entry list.
 */
@ProtobufMessage(name = "appDataPayloads")
public final class AppDataPayloads {
    /**
     * The batched payloads.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<AppDataMessage> messages;

    /**
     * Constructs a new {@code AppDataPayloads}.
     *
     * @param messages the batched payloads
     */
    AppDataPayloads(List<AppDataMessage> messages) {
        this.messages = messages;
    }

    /**
     * Returns the batched payloads.
     *
     * @return an unmodifiable list, never {@code null}
     */
    public List<AppDataMessage> messages() {
        return messages == null ? List.of() : Collections.unmodifiableList(messages);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof AppDataPayloads that
                && Objects.equals(this.messages, that.messages));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(messages);
    }

    @Override
    public String toString() {
        return "AppDataPayloads[messages=" + messages() + ']';
    }
}
