package com.github.auties00.cobalt.wire.linked.bot.ai;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;

/**
 * Metadata associated with a request to regenerate an AI bot response.
 *
 * <p>When a user taps the "regenerate" button on an AI-generated message,
 * the client includes this metadata to identify which specific response should
 * be regenerated. It carries the {@linkplain #messageKey() message key} that
 * uniquely identifies the original bot response and the
 * {@linkplain #responseTimestamp() timestamp} (in milliseconds) at which that
 * response was produced.
 *
 * <p>This metadata is included within the bot metadata of the regeneration
 * request message, allowing the server to locate the original response and
 * produce a new one.
 */
@ProtobufMessage(name = "AIRegenerateMetadata")
public final class AIRegenerateMetadata {
    /**
     * The key that uniquely identifies the original bot response message that
     * the user wants to regenerate. The server uses this key to locate the
     * original response context and produce a new answer.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey messageKey;

    /**
     * The timestamp, in milliseconds since the Unix epoch, at which the original
     * bot response was produced. This value helps the server disambiguate between
     * multiple responses in the same conversation thread.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant responseTimestamp;


    /**
     * Constructs a new {@code AIRegenerateMetadata} with the specified values.
     *
     * @param messageKey        the key of the original bot response, or {@code null}
     * @param responseTimestamp  the timestamp at which the original response was produced,
     *                          or {@code null} if unknown
     */
    AIRegenerateMetadata(MessageKey messageKey, Instant responseTimestamp) {
        this.messageKey = messageKey;
        this.responseTimestamp = responseTimestamp;
    }

    /**
     * Returns the key that uniquely identifies the original bot response message
     * to be regenerated.
     *
     * @return an {@code Optional} containing the message key, or an empty
     *         {@code Optional} if not set
     */
    public Optional<MessageKey> messageKey() {
        return Optional.ofNullable(messageKey);
    }

    /**
     * Returns the timestamp at which the original bot response was produced,
     * with millisecond precision.
     *
     * @return an {@code Optional} containing the response timestamp, or an empty
     *         {@code Optional} if not set
     */
    public Optional<Instant> responseTimestamp() {
        return Optional.ofNullable(responseTimestamp);
    }

    /**
     * Sets the key that uniquely identifies the original bot response message
     * to be regenerated.
     *
     * @param messageKey the new message key, or {@code null} to clear
     */
    public void setMessageKey(MessageKey messageKey) {
        this.messageKey = messageKey;
    }

    /**
     * Sets the timestamp at which the original bot response was produced.
     *
     * @param responseTimestamp the new response timestamp, or {@code null} to clear
     */
    public void setResponseTimestamp(Instant responseTimestamp) {
        this.responseTimestamp = responseTimestamp;
    }
}
