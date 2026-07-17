package com.github.auties00.cobalt.wire.linked.chat;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Represents the type of "keep in chat" action applied to an ephemeral message
 * in WhatsApp.
 *
 * <p>When disappearing messages are enabled in a chat, individual messages can
 * be "kept" so that they persist beyond the ephemeral timer. This enum indicates
 * whether a message has been kept for all participants, or whether a previously
 * kept message has been un-kept (reverted to ephemeral behavior).
 *
 * @see ChatMessageInfo.KeepInChat
 */
@ProtobufEnum(name = "KeepType")
public enum ChatKeepType {
    /**
     * The keep type is unknown or unset. This is the default value when
     * no keep action has been performed on the message.
     */
    UNKNOWN(0),

    /**
     * The message has been kept for all participants in the chat, preventing
     * it from being deleted when the disappearing messages timer expires.
     */
    KEEP_FOR_ALL(1),

    /**
     * A previously kept message has been reverted to ephemeral behavior for
     * all participants, allowing the disappearing messages timer to apply
     * again.
     */
    UNDO_KEEP_FOR_ALL(2);

    /**
     * Constructs a {@code ChatKeepType} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    ChatKeepType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * The protobuf-assigned index for this enum constant.
     */
    final int index;

    /**
     * Returns the protobuf index of this keep type.
     *
     * @return the integer index used for wire serialization
     */
    public int index() {
        return this.index;
    }
}
