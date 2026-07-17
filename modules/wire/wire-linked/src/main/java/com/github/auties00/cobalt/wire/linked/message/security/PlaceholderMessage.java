package com.github.auties00.cobalt.wire.linked.message.security;

import com.github.auties00.cobalt.wire.linked.message.Message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Represents a placeholder message used to replace content that must not
 * be shown in clear to a specific audience.
 *
 * <p>Placeholder messages are synthesized by the server or by the client
 * in response to policy rules (for example when linked devices should
 * not receive the full message body). The only semantic payload is the
 * {@link PlaceholderType} that describes why the real content has been
 * omitted, which lets the receiving client render an appropriate stub
 * in place of the original message.
 */
@ProtobufMessage(name = "Message.PlaceholderMessage")
public final class PlaceholderMessage implements Message {
    /**
     * Indicates the reason this placeholder was produced.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    PlaceholderType type;


    /**
     * Constructs a new placeholder message.
     *
     * @param type the reason the original content has been replaced by
     *             a placeholder, or {@code null} if unset
     */
    PlaceholderMessage(PlaceholderType type) {
        this.type = type;
    }

    /**
     * Returns the reason this placeholder was produced.
     *
     * @return an {@link Optional} holding the {@link PlaceholderType},
     *         or {@link Optional#empty()} if the field is unset
     */
    public Optional<PlaceholderType> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Sets the reason this placeholder was produced.
     *
     * @param type the new {@link PlaceholderType}, or {@code null} to
     *             clear the field
     */
    public void setType(PlaceholderType type) {
        this.type = type;
    }

    /**
     * Enumerates the reasons a real message may be replaced by a
     * {@link PlaceholderMessage}.
     */
    @ProtobufEnum(name = "Message.PlaceholderMessage.PlaceholderType")
    public static enum PlaceholderType {
        /**
         * Indicates that the original content has been hidden because it
         * was targeted at a linked device that is not allowed to see the
         * full payload.
         */
        MASK_LINKED_DEVICES(0);

        /**
         * Constructs a new placeholder type constant.
         *
         * @param index the protobuf wire index associated with this
         *              enumeration value
         */
        PlaceholderType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index associated with this enumeration value.
         */
        final int index;

        /**
         * Returns the protobuf wire index associated with this value.
         *
         * @return the integer index used to serialize this constant
         */
        public int index() {
            return this.index;
        }
    }
}
