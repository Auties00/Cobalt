package com.github.auties00.cobalt.wire.linked.bot.response;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Classifies the overall format of an AI rich response message sent by the
 * WhatsApp AI bot.
 *
 * <p>The type determines how the client should interpret and render the
 * list of {@link AIRichResponseSubMessage} fragments contained in the
 * response. Currently two values exist: {@link #UNKNOWN} for unrecognised
 * formats and {@link #STANDARD} for the normal multi-fragment layout.
 */
@ProtobufEnum(name = "AIRichResponseMessageType")
public enum AIRichResponseMessageType {
    /**
     * An unrecognised or unsupported message type.
     *
     * <p>Clients should treat responses carrying this type as
     * unparseable and fall back to a plain-text representation.
     */
    UNKNOWN(0),

    /**
     * A standard rich response composed of one or more typed
     * {@link AIRichResponseSubMessage} fragments such as text,
     * code, tables, images, maps, or LaTeX expressions.
     */
    STANDARD(1);

    /**
     * Constructs a message type constant with the given protobuf index.
     *
     * @param index the protobuf enum index
     */
    AIRichResponseMessageType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * The protobuf enum index for this message type.
     */
    final int index;

    /**
     * Returns the protobuf index associated with this message type.
     *
     * @return the protobuf index
     */
    public int index() {
        return this.index;
    }
}
