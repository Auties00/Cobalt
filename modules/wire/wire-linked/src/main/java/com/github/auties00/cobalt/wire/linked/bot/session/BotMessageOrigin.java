package com.github.auties00.cobalt.wire.linked.bot.session;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Describes the origination mechanism for a single bot message on WhatsApp.
 *
 * <p>Each instance carries a {@link #type()} that identifies how the
 * message was initiated. Currently the only defined origin is
 * {@link BotMessageOriginType#AI_INITIATED AI_INITIATED}, indicating
 * that the bot proactively sent the message without a direct user prompt
 * (for example, a scheduled reminder or a proactive notification).
 *
 * @see BotMessageOriginMetadata
 */
@ProtobufMessage(name = "BotMessageOrigin")
public final class BotMessageOrigin {
    /**
     * The type of origination for this bot message.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    BotMessageOriginType type;


    /**
     * Constructs a new {@code BotMessageOrigin} with the specified type.
     *
     * @param type the origination type, or {@code null}
     */
    BotMessageOrigin(BotMessageOriginType type) {
        this.type = type;
    }

    /**
     * Returns the type of origination for this bot message.
     *
     * @return an {@code Optional} describing the origination type, or an
     *         empty {@code Optional} if not set
     */
    public Optional<BotMessageOriginType> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Sets the type of origination for this bot message.
     *
     * @param type the new origination type, or {@code null}
     */
    public void setType(BotMessageOriginType type) {
        this.type = type;
    }

    /**
     * Enumerates the mechanisms by which a bot message can be initiated.
     */
    @ProtobufEnum(name = "BotMessageOrigin.BotMessageOriginType")
    public static enum BotMessageOriginType {
        /**
         * The message was proactively initiated by the AI bot without a
         * direct user prompt.
         */
        AI_INITIATED(0);

        /**
         * Constructs a new bot message origin type constant with the
         * specified protobuf index.
         *
         * @param index the protobuf enum index
         */
        BotMessageOriginType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        final int index;

        /**
         * Returns the protobuf enum index of this origin type.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }
}
