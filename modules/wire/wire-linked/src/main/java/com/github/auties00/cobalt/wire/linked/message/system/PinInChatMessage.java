package com.github.auties00.cobalt.wire.linked.message.system;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.Message;

import java.time.Instant;

import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A system message that pins or unpins a specific message inside a chat for
 * every participant.
 *
 * <p>WhatsApp lets participants pin a message so it is highlighted at the top
 * of the conversation for a bounded duration. When a pin or unpin action
 * occurs, this record is broadcast to all clients so they can update the
 * conversation UI. The target message is referenced by its {@link MessageKey},
 * while {@link Type} describes the action performed and the sender timestamp
 * lets clients order concurrent operations.
 */
@ProtobufMessage(name = "Message.PinInChatMessage")
public final class PinInChatMessage implements Message {
    /**
     * The {@link MessageKey} that uniquely identifies the pinned or unpinned message.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey key;

    /**
     * The kind of pin operation represented by this message.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    Type type;

    /**
     * The timestamp, in milliseconds, at which the sender issued the pin
     * operation.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant senderTimestampMs;


    /**
     * Constructs a new pin-in-chat message.
     *
     * @param key                the target message key, may be {@code null}
     * @param type               the pin operation type, may be {@code null}
     * @param senderTimestampMs  the sender timestamp, may be {@code null}
     */
    PinInChatMessage(MessageKey key, Type type, Instant senderTimestampMs) {
        this.key = key;
        this.type = type;
        this.senderTimestampMs = senderTimestampMs;
    }

    /**
     * Returns the {@link MessageKey} of the pinned or unpinned message.
     *
     * @return an {@link Optional} containing the key, or
     *         {@link Optional#empty()} if no key is set
     */
    public Optional<MessageKey> key() {
        return Optional.ofNullable(key);
    }

    /**
     * Returns the kind of pin operation represented by this message.
     *
     * @return an {@link Optional} containing the pin type, or
     *         {@link Optional#empty()} if no type is set
     */
    public Optional<Type> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Returns the timestamp at which the sender issued the pin operation.
     *
     * @return an {@link Optional} containing the timestamp, or
     *         {@link Optional#empty()} if no timestamp is set
     */
    public Optional<Instant> senderTimestampMs() {
        return Optional.ofNullable(senderTimestampMs);
    }

    /**
     * Sets the {@link MessageKey} of the pinned or unpinned message.
     *
     * @param key the new message key, or {@code null} to clear it
     */
    public void setKey(MessageKey key) {
        this.key = key;
    }

    /**
     * Sets the kind of pin operation represented by this message.
     *
     * @param type the new pin type, or {@code null} to clear it
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Sets the timestamp at which the sender issued the pin operation.
     *
     * @param senderTimestampMs the new timestamp, or {@code null} to clear it
     */
    public void setSenderTimestampMs(Instant senderTimestampMs) {
        this.senderTimestampMs = senderTimestampMs;
    }

    /**
     * Enumerates the possible pin operations that can be applied to a
     * message inside a chat.
     */
    @ProtobufEnum(name = "Message.PinInChatMessage.Type")
    public static enum Type {
        /**
         * The pin operation type is unspecified or not recognised.
         */
        UNKNOWN_TYPE(0),
        /**
         * Pin the referenced message for every participant of the chat.
         */
        PIN_FOR_ALL(1),
        /**
         * Remove the pin from the referenced message for every participant
         * of the chat.
         */
        UNPIN_FOR_ALL(2);

        /**
         * Constructs a new enum constant with the given protobuf index.
         *
         * @param index the protobuf wire index for this constant
         */
        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index associated with this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index associated with this constant.
         *
         * @return the protobuf wire index
         */
        public int index() {
            return this.index;
        }
    }
}
