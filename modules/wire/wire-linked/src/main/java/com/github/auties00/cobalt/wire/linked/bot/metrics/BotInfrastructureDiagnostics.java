package com.github.auties00.cobalt.wire.linked.bot.metrics;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.List;
import java.util.Optional;

/**
 * Diagnostics metadata that identifies which backend infrastructure processed
 * a WhatsApp AI bot interaction and which server-side tools were invoked.
 *
 * <p>Every AI bot response in WhatsApp carries a {@link com.github.auties00.cobalt.wire.linked.bot.BotMetadata}
 * envelope. This message appears at field index 37 of that envelope and provides
 * infrastructure-level transparency: which processing backend handled the
 * request and which tools (such as web search, code execution, or image
 * generation) the backend invoked while composing the response.
 *
 * <p>This information is primarily intended for debugging and server-side
 * monitoring. Client applications can use it to display tool-usage indicators
 * in the UI or to log processing details for support purposes.
 *
 * <p>The nested {@link BotBackend} enum enumerates the known backend systems
 * that Meta uses internally to serve AI bot responses.
 */
@ProtobufMessage(name = "BotInfrastructureDiagnostics")
public final class BotInfrastructureDiagnostics {
    /**
     * The backend system that processed this bot interaction, identifying
     * which Meta infrastructure path served the response. May be
     * {@code null} if the server did not populate this field.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    BotBackend botBackend;

    /**
     * The names of the server-side tools that the backend invoked while
     * processing this bot interaction. Common tool names include values such
     * as {@code "web_search"} and {@code "code_interpreter"}, though the
     * exact set of tool names is determined by the server and may evolve
     * over time. The list may be empty if no tools were invoked or if the
     * server did not report tool usage.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    List<String> toolsUsed;

    /**
     * Constructs a new {@code BotInfrastructureDiagnostics} with the specified
     * backend and tool list.
     *
     * @param botBackend the backend system that handled the request, or
     *                   {@code null} if unknown
     * @param toolsUsed  the list of tool names invoked during processing, or
     *                   {@code null} for an empty list
     */
    BotInfrastructureDiagnostics(BotBackend botBackend, List<String> toolsUsed) {
        this.botBackend = botBackend;
        this.toolsUsed = toolsUsed;
    }

    /**
     * Returns the backend system that processed this bot interaction.
     *
     * @return an {@link Optional} containing the {@link BotBackend} that
     *         handled the request, or an empty {@code Optional} if the
     *         server did not specify a backend
     */
    public Optional<BotBackend> botBackend() {
        return Optional.ofNullable(botBackend);
    }

    /**
     * Returns the names of the server-side tools invoked while processing
     * this bot interaction.
     *
     * @return an unmodifiable list of tool name strings; never {@code null},
     *         but may be empty if no tools were used
     */
    public List<String> toolsUsed() {
        return toolsUsed != null ? toolsUsed : List.of();
    }

    /**
     * Sets the backend system that processed this bot interaction.
     *
     * @param botBackend the new backend value, or {@code null} to clear
     */
    public void setBotBackend(BotBackend botBackend) {
        this.botBackend = botBackend;
    }

    /**
     * Sets the names of the server-side tools invoked during processing.
     *
     * @param toolsUsed the new list of tool name strings, or {@code null}
     *                  to clear
     */
    public void setToolsUsed(List<String> toolsUsed) {
        this.toolsUsed = toolsUsed;
    }

    /**
     * Enumerates the server-side backend systems that Meta uses to process
     * AI bot requests on WhatsApp.
     *
     * <p>Each constant represents an internal Meta infrastructure codename
     * for a distinct processing pipeline. The backend used for a given
     * interaction depends on server-side routing and the type of bot being
     * invoked. Client applications typically do not need to branch on this
     * value, but it can be useful for diagnostic logging.
     */
    @ProtobufEnum(name = "BotInfrastructureDiagnostics.BotBackend")
    public static enum BotBackend {
        /**
         * The primary AI API backend, used as the default processing pipeline
         * for Meta AI bot responses on WhatsApp. This is the protobuf default
         * value (index {@code 0}).
         */
        AAPI(0),

        /**
         * An alternative bot processing backend, representing a secondary
         * infrastructure path distinct from the primary {@link #AAPI} pipeline.
         */
        CLIPPY(1);

        /**
         * Constructs a new backend constant with the given protobuf index.
         *
         * @param index the protobuf wire-format index for this constant
         */
        BotBackend(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire-format index for this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire-format index for this backend constant.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }
}
