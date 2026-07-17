package com.github.auties00.cobalt.wire.linked.bot.metrics;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Telemetry metadata that records how and where a user navigated to an AI
 * bot conversation on WhatsApp.
 *
 * <p>This message appears at field index 17 of the
 * {@link com.github.auties00.cobalt.wire.linked.bot.BotMetadata} envelope and
 * captures three pieces of navigation context:
 * <ol>
 *   <li>The {@linkplain #destinationId() destination bot JID} that the user
 *       navigated to
 *   <li>The {@linkplain #destinationEntryPoint() UI entry point} from which
 *       the interaction was initiated (for example, the AI tab, a context menu,
 *       or a deep link)
 *   <li>The {@linkplain #threadOrigin() thread type} in which the interaction
 *       takes place (for example, an AI tab thread or a deep-link thread)
 * </ol>
 *
 * <p>The server uses this data for engagement analytics, and the client
 * populates it before sending each message to a bot.
 */
@ProtobufMessage(name = "BotMetricsMetadata")
public final class BotMetricsMetadata {
    /**
     * The JID string of the destination bot that the user navigated to,
     * for example {@code "13135550002@s.whatsapp.net"}. May be {@code null}
     * if the destination was not recorded.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String destinationId;

    /**
     * The UI surface or navigation path from which the user initiated the
     * bot interaction, such as the AI tab, a context menu, or a deep link.
     * May be {@code null} if the entry point was not recorded.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    BotMetricsEntryPoint destinationEntryPoint;

    /**
     * The type of AI conversation thread from which the interaction originated,
     * indicating whether the user was in an AI tab thread, an AI home thread,
     * or another thread surface. May be {@code null} if the thread type was
     * not recorded.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    BotMetricsThreadEntryPoint threadOrigin;

    /**
     * Constructs a new {@code BotMetricsMetadata} with the specified navigation
     * context values.
     *
     * @param destinationId        the JID string of the destination bot, or
     *                             {@code null} if unknown
     * @param destinationEntryPoint the UI entry point from which the interaction
     *                             was initiated, or {@code null} if not recorded
     * @param threadOrigin         the type of AI thread in which the interaction
     *                             takes place, or {@code null} if not recorded
     */
    BotMetricsMetadata(String destinationId, BotMetricsEntryPoint destinationEntryPoint, BotMetricsThreadEntryPoint threadOrigin) {
        this.destinationId = destinationId;
        this.destinationEntryPoint = destinationEntryPoint;
        this.threadOrigin = threadOrigin;
    }

    /**
     * Returns the JID string of the destination bot that the user navigated to.
     *
     * @return an {@link Optional} containing the bot JID string, or an empty
     *         {@code Optional} if the destination was not recorded
     */
    public Optional<String> destinationId() {
        return Optional.ofNullable(destinationId);
    }

    /**
     * Returns the UI surface or navigation path from which the user initiated
     * the bot interaction.
     *
     * @return an {@link Optional} containing the {@link BotMetricsEntryPoint},
     *         or an empty {@code Optional} if the entry point was not recorded
     */
    public Optional<BotMetricsEntryPoint> destinationEntryPoint() {
        return Optional.ofNullable(destinationEntryPoint);
    }

    /**
     * Returns the type of AI conversation thread from which the interaction
     * originated.
     *
     * @return an {@link Optional} containing the {@link BotMetricsThreadEntryPoint},
     *         or an empty {@code Optional} if the thread type was not recorded
     */
    public Optional<BotMetricsThreadEntryPoint> threadOrigin() {
        return Optional.ofNullable(threadOrigin);
    }

    /**
     * Sets the JID string of the destination bot.
     *
     * @param destinationId the new destination bot JID string, or {@code null}
     *                      to clear
     */
    public void setDestinationId(String destinationId) {
        this.destinationId = destinationId;
    }

    /**
     * Sets the UI surface or navigation path from which the user initiated
     * the bot interaction.
     *
     * @param destinationEntryPoint the new entry point, or {@code null}
     *                              to clear
     */
    public void setDestinationEntryPoint(BotMetricsEntryPoint destinationEntryPoint) {
        this.destinationEntryPoint = destinationEntryPoint;
    }

    /**
     * Sets the type of AI conversation thread from which the interaction
     * originated.
     *
     * @param threadOrigin the new thread origin type, or {@code null}
     *                     to clear
     */
    public void setThreadOrigin(BotMetricsThreadEntryPoint threadOrigin) {
        this.threadOrigin = threadOrigin;
    }
}
