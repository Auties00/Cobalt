package com.github.auties00.cobalt.wire.linked.bot.feedback;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Enumerates the types of transparency notices that can be displayed to a user
 * during an AI bot session.
 *
 * <p>Each constant represents a specific regulatory or policy-driven disclosure
 * requirement. The client uses this value to determine which disclaimer UI to
 * render alongside the AI conversation.
 *
 * @see SessionTransparencyMetadata
 */
@ProtobufEnum(name = "SessionTransparencyType")
public enum SessionTransparencyType {
    /**
     * The transparency type is unknown or unspecified.
     */
    UNKNOWN_TYPE(0),

    /**
     * A New York State AI safety disclaimer, required by local regulations.
     */
    NEW_YORK_AI_SAFETY_DISCLAIMER(1);

    /**
     * Constructs a new {@code SessionTransparencyType} with the specified protobuf index.
     *
     * @param index the protobuf index value
     */
    SessionTransparencyType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * The protobuf index of this enum constant.
     */
    final int index;

    /**
     * Returns the protobuf index of this enum constant.
     *
     * @return the protobuf index
     */
    public int index() {
        return this.index;
    }
}
