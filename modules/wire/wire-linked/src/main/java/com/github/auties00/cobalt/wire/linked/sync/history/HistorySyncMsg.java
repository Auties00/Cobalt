package com.github.auties00.cobalt.wire.linked.sync.history;

import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * Single message entry transported inside a history-sync payload.
 *
 * <p>When a companion device receives the account's message history from the
 * primary device, each message is wrapped in this envelope. The envelope
 * carries the full {@link ChatMessageInfo} together with a {@code msgOrderId}:
 * a monotonically increasing identifier used to preserve the conversation
 * ordering across platforms even when timestamps collide.
 */
@ProtobufMessage(name = "HistorySyncMsg")
public final class HistorySyncMsg {
    /**
     * Transferred chat message, including key, sender, timestamps and body.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    ChatMessageInfo message;

    /**
     * Monotonic ordering identifier used to reconstruct the conversation
     * sequence on the receiving device.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    Long msgOrderId;

    /**
     * Constructs a new history-sync message envelope.
     *
     * @param message the transferred chat message
     * @param msgOrderId the monotonic ordering identifier
     */
    HistorySyncMsg(ChatMessageInfo message, Long msgOrderId) {
        this.message = message;
        this.msgOrderId = msgOrderId;
    }

    /**
     * Returns the transferred chat message.
     *
     * @return the message, or empty if absent
     */
    public Optional<ChatMessageInfo> message() {
        return Optional.ofNullable(message);
    }

    /**
     * Returns the monotonic ordering identifier.
     *
     * @return the order id, or empty if absent
     */
    public OptionalLong msgOrderId() {
        return msgOrderId == null ? OptionalLong.empty() : OptionalLong.of(msgOrderId);
    }

    /**
     * Sets the transferred chat message.
     *
     * @param message the message
     */
    public void setMessage(ChatMessageInfo message) {
        this.message = message;
    }

    /**
     * Sets the monotonic ordering identifier.
     *
     * @param msgOrderId the order id
     */
    public void setMsgOrderId(Long msgOrderId) {
        this.msgOrderId = msgOrderId;
    }
}
