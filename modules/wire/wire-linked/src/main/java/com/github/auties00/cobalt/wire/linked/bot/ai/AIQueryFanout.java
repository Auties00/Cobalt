package com.github.auties00.cobalt.wire.linked.bot.ai;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;

/**
 * Represents an AI query message that has been fanned out (distributed) to
 * participants in a conversation.
 *
 * <p>When a user sends a prompt to Meta AI within a group chat or across a
 * multi-device session, the server distributes the query to all relevant
 * participants so they can see both the original question and the AI-generated
 * response. This protobuf message captures the original
 * {@linkplain #messageKey() message key} that uniquely identifies the query,
 * the full {@linkplain #message() message content} of the query, and the
 * {@linkplain #timestamp() timestamp} at which the fanout occurred.
 *
 * <p>This type is embedded within the end-to-end encrypted message structure
 * as part of the {@code ContextInfo} associated with AI bot messages.
 */
@ProtobufMessage(name = "AIQueryFanout")
public final class AIQueryFanout {
    /**
     * The key that uniquely identifies the original AI query message within the
     * conversation. This allows recipients to correlate the fanout with the
     * original user prompt.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey messageKey;

    /**
     * The full content of the AI query message that was fanned out. This contains
     * the complete end-to-end encrypted message payload, including the user's
     * prompt text and any associated media or context.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    LinkedMessageContainer messageContainer;

    /**
     * The timestamp, in seconds since the Unix epoch, at which the fanout event
     * occurred on the server.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant timestamp;


    /**
     * Constructs a new {@code AIQueryFanout} with the specified values.
     *
     * @param messageKey       the key of the original query message, or {@code null}
     * @param messageContainer the full message content of the query, or {@code null}
     * @param timestamp        the server-side fanout timestamp, or {@code null}
     */
    AIQueryFanout(MessageKey messageKey, LinkedMessageContainer messageContainer, Instant timestamp) {
        this.messageKey = messageKey;
        this.messageContainer = messageContainer;
        this.timestamp = timestamp;
    }

    /**
     * Returns the key that uniquely identifies the original AI query message.
     *
     * @return an {@code Optional} containing the message key, or an empty
     *         {@code Optional} if not set
     */
    public Optional<MessageKey> messageKey() {
        return Optional.ofNullable(messageKey);
    }

    /**
     * Returns the full content of the AI query message that was fanned out.
     *
     * @return an {@code Optional} containing the message content, or an empty
     *         {@code Optional} if not set
     */
    public Optional<LinkedMessageContainer> message() {
        return Optional.ofNullable(messageContainer);
    }

    /**
     * Returns the timestamp at which the fanout event occurred on the server.
     *
     * @return an {@code Optional} containing the fanout timestamp, or an empty
     *         {@code Optional} if not set
     */
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }

    /**
     * Sets the key that uniquely identifies the original AI query message.
     *
     * @param messageKey the new message key, or {@code null} to clear
     */
    public void setMessageKey(MessageKey messageKey) {
        this.messageKey = messageKey;
    }

    /**
     * Sets the full content of the AI query message that was fanned out.
     *
     * @param messageContainer the new message content, or {@code null} to clear
     */
    public void setMessage(LinkedMessageContainer messageContainer) {
        this.messageContainer = messageContainer;
    }

    /**
     * Sets the timestamp at which the fanout event occurred.
     *
     * @param timestamp the new fanout timestamp, or {@code null} to clear
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
