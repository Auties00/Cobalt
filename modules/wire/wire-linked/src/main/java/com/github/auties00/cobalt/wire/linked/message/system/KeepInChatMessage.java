package com.github.auties00.cobalt.wire.linked.message.system;

import com.github.auties00.cobalt.wire.linked.chat.ChatKeepType;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.Message;

import java.time.Instant;

import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A system message that marks a single message inside a disappearing-messages
 * chat as exempt from automatic deletion.
 *
 * <p>WhatsApp lets users preserve selected messages in conversations where
 * disappearing messages are enabled. When the user keeps (or unkeeps) a
 * message, this event is replicated across devices as a Keep-In-Chat record
 * that references the affected message by its {@link MessageKey}, carries the
 * new {@link ChatKeepType} state, and records the timestamp at which the
 * change was requested.
 */
@ProtobufMessage(name = "Message.KeepInChatMessage")
public final class KeepInChatMessage implements Message {
    /**
     * The {@link MessageKey} that uniquely identifies the message whose
     * keep state is being changed.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey key;

    /**
     * The new keep-in-chat state of the referenced message.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    ChatKeepType keepType;

    /**
     * The timestamp, in milliseconds, at which the keep request was issued.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant timestampMs;


    /**
     * Constructs a new keep-in-chat message.
     *
     * @param key          the message key, may be {@code null}
     * @param keepType     the new keep state, may be {@code null}
     * @param timestampMs  the timestamp of the operation, may be {@code null}
     */
    KeepInChatMessage(MessageKey key, ChatKeepType keepType, Instant timestampMs) {
        this.key = key;
        this.keepType = keepType;
        this.timestampMs = timestampMs;
    }

    /**
     * Returns the {@link MessageKey} of the message whose keep state is being changed.
     *
     * @return an {@link Optional} containing the key, or
     *         {@link Optional#empty()} if no key is set
     */
    public Optional<MessageKey> key() {
        return Optional.ofNullable(key);
    }

    /**
     * Returns the new keep-in-chat state of the referenced message.
     *
     * @return an {@link Optional} containing the keep type, or
     *         {@link Optional#empty()} if no keep type is set
     */
    public Optional<ChatKeepType> keepType() {
        return Optional.ofNullable(keepType);
    }

    /**
     * Returns the timestamp at which the keep request was issued.
     *
     * @return an {@link Optional} containing the timestamp, or
     *         {@link Optional#empty()} if no timestamp is set
     */
    public Optional<Instant> timestampMs() {
        return Optional.ofNullable(timestampMs);
    }

    /**
     * Sets the {@link MessageKey} of the message whose keep state is being changed.
     *
     * @param key the new message key, or {@code null} to clear it
     */
    public void setKey(MessageKey key) {
        this.key = key;
    }

    /**
     * Sets the new keep-in-chat state of the referenced message.
     *
     * @param keepType the new keep type, or {@code null} to clear it
     */
    public void setKeepType(ChatKeepType keepType) {
        this.keepType = keepType;
    }

    /**
     * Sets the timestamp at which the keep request was issued.
     *
     * @param timestampMs the new timestamp, or {@code null} to clear it
     */
    public void setTimestampMs(Instant timestampMs) {
        this.timestampMs = timestampMs;
    }
}
