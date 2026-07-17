package com.github.auties00.cobalt.wire.linked.bot.session;

import com.github.auties00.cobalt.wire.linked.bot.metrics.BotMetricsEntryPoint;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Metadata about how a bot message was shared or forwarded on WhatsApp.
 *
 * <p>When a user forwards a bot-generated message, this metadata is attached
 * to the message's context info. It records the UI entry point from which
 * the original bot interaction was initiated and a forward score that
 * tracks how many times the message has been forwarded, which allows the
 * server to apply anti-virality measures if needed.
 */
@ProtobufMessage(name = "BotMessageSharingInfo")
public final class BotMessageSharingInfo {
    /**
     * The UI entry point from which the original bot interaction was
     * initiated before the message was shared.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    BotMetricsEntryPoint botEntryPointOrigin;

    /**
     * The cumulative number of times this bot message has been forwarded.
     * Higher values indicate viral spread and may trigger server-side
     * restrictions.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    Integer forwardScore;


    /**
     * Constructs a new {@code BotMessageSharingInfo} with the specified
     * values.
     *
     * @param botEntryPointOrigin the originating entry point, or
     *        {@code null}
     * @param forwardScore        the forward count, or {@code null}
     */
    BotMessageSharingInfo(BotMetricsEntryPoint botEntryPointOrigin, Integer forwardScore) {
        this.botEntryPointOrigin = botEntryPointOrigin;
        this.forwardScore = forwardScore;
    }

    /**
     * Returns the UI entry point from which the original bot interaction
     * was initiated.
     *
     * @return an {@code Optional} describing the entry point, or an empty
     *         {@code Optional} if not set
     */
    public Optional<BotMetricsEntryPoint> botEntryPointOrigin() {
        return Optional.ofNullable(botEntryPointOrigin);
    }

    /**
     * Returns the number of times this bot message has been forwarded.
     *
     * @return an {@code OptionalInt} describing the forward score, or an
     *         empty {@code OptionalInt} if not set
     */
    public OptionalInt forwardScore() {
        return forwardScore == null ? OptionalInt.empty() : OptionalInt.of(forwardScore);
    }

    /**
     * Sets the UI entry point from which the original bot interaction was
     * initiated.
     *
     * @param botEntryPointOrigin the new entry point, or {@code null}
     */
    public void setBotEntryPointOrigin(BotMetricsEntryPoint botEntryPointOrigin) {
        this.botEntryPointOrigin = botEntryPointOrigin;
    }

    /**
     * Sets the number of times this bot message has been forwarded.
     *
     * @param forwardScore the new forward score, or {@code null}
     */
    public void setForwardScore(Integer forwardScore) {
        this.forwardScore = forwardScore;
    }
}
