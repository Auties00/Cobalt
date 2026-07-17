package com.github.auties00.cobalt.wire.linked.bot.session;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

/**
 * Container for the list of origins that describe how a bot message was
 * initiated on WhatsApp.
 *
 * <p>A single bot message may be attributed to multiple origination sources
 * (for example, if a proactive AI message was triggered by multiple signals).
 * This metadata wraps a repeated list of {@link BotMessageOrigin} entries,
 * each identifying one such source.
 *
 * <p>This metadata is carried inside
 * {@link com.github.auties00.cobalt.wire.linked.bot.BotMetadata#botMessageOriginMetadata()}.
 */
@ProtobufMessage(name = "BotMessageOriginMetadata")
public final class BotMessageOriginMetadata {
    /**
     * The list of origination sources for this bot message.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    List<BotMessageOrigin> origins;


    /**
     * Constructs a new {@code BotMessageOriginMetadata} with the specified
     * origins.
     *
     * @param origins the list of message origins, or {@code null}
     */
    BotMessageOriginMetadata(List<BotMessageOrigin> origins) {
        this.origins = origins;
    }

    /**
     * Returns the list of origination sources for this bot message.
     *
     * @return an unmodifiable list of message origins, or an empty list if
     *         none were set
     */
    public List<BotMessageOrigin> origins() {
        return origins == null ? List.of() : Collections.unmodifiableList(origins);
    }

    /**
     * Sets the list of origination sources for this bot message.
     *
     * @param origins the new list of message origins, or {@code null}
     */
    public void setOrigins(List<BotMessageOrigin> origins) {
        this.origins = origins;
    }
}
