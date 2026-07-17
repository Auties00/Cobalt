package com.github.auties00.cobalt.wire.linked.message.status;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.Message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Interaction sent by a viewer when they react to a status using a sticker.
 *
 * <p>WhatsApp statuses can be reacted to with stickers just like chat
 * messages. When a viewer taps a sticker reaction on a status, the client
 * sends this message: it identifies the original status via {@link #key()},
 * supplies the identifier of the chosen sticker in {@link #stickerKey()},
 * and records which interaction flow produced it via {@link #type()}.
 *
 * @see MessageKey
 * @see StatusStickerType
 */
@ProtobufMessage(name = "Message.StatusStickerInteractionMessage")
public final class StatusStickerInteractionMessage implements Message {
    /**
     * Key of the status that the viewer reacted to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey key;

    /**
     * Identifier of the sticker chosen by the viewer.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String stickerKey;

    /**
     * Interaction flow that produced this sticker message.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    StatusStickerType type;

    /**
     * Constructs a new {@code StatusStickerInteractionMessage} with the
     * supplied fields.
     *
     * @param key        the original status key, or {@code null} if absent
     * @param stickerKey the sticker identifier, or {@code null} if absent
     * @param type       the interaction flow, or {@code null} if absent
     */
    StatusStickerInteractionMessage(MessageKey key, String stickerKey, StatusStickerType type) {
        this.key = key;
        this.stickerKey = stickerKey;
        this.type = type;
    }

    /**
     * Returns the key of the status that the viewer reacted to.
     *
     * @return the original status key, or {@code Optional.empty()} if absent
     */
    public Optional<MessageKey> key() {
        return Optional.ofNullable(key);
    }

    /**
     * Returns the identifier of the sticker chosen by the viewer.
     *
     * @return the sticker identifier, or {@code Optional.empty()} if absent
     */
    public Optional<String> stickerKey() {
        return Optional.ofNullable(stickerKey);
    }

    /**
     * Returns the interaction flow that produced this sticker message.
     *
     * @return the interaction flow, or {@code Optional.empty()} if absent
     */
    public Optional<StatusStickerType> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Sets the key of the status that the viewer reacted to.
     *
     * @param key the original status key, or {@code null} to clear
     */
    public void setKey(MessageKey key) {
        this.key = key;
    }

    /**
     * Sets the identifier of the sticker chosen by the viewer.
     *
     * @param stickerKey the sticker identifier, or {@code null} to clear
     */
    public void setStickerKey(String stickerKey) {
        this.stickerKey = stickerKey;
    }

    /**
     * Sets the interaction flow that produced this sticker message.
     *
     * @param type the interaction flow, or {@code null} to clear
     */
    public void setType(StatusStickerType type) {
        this.type = type;
    }

    /**
     * Enumerates the sticker-based interaction flows that can produce a
     * {@link StatusStickerInteractionMessage}.
     */
    @ProtobufEnum(name = "Message.StatusStickerInteractionMessage.StatusStickerType")
    public static enum StatusStickerType {
        /**
         * Interaction flow was not recognised or is not set.
         */
        UNKNOWN(0),
        /**
         * Sticker was used as a reaction to the status.
         */
        REACTION(1);

        /**
         * Constructs a new {@code StatusStickerType} enum constant.
         *
         * @param index the protobuf wire index
         */
        StatusStickerType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Protobuf wire index used to serialise this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this constant.
         *
         * @return the wire index
         */
        public int index() {
            return this.index;
        }
    }
}
