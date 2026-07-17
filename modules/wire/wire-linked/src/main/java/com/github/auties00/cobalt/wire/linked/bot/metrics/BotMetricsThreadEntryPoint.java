package com.github.auties00.cobalt.wire.linked.bot.metrics;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Enumerates the types of AI conversation threads from which a bot interaction
 * can originate on WhatsApp.
 *
 * <p>Each constant identifies a distinct thread surface in the WhatsApp client
 * where the user was located when they sent a message to an AI bot. This value
 * is recorded in {@link BotMetricsMetadata#threadOrigin()} and sent to the
 * server for engagement analytics, allowing Meta to understand which thread
 * presentation surfaces drive the most bot usage.
 *
 * <p>Unlike {@link BotMetricsEntryPoint}, which captures the initial UI
 * action that started the interaction (such as tapping a button or following
 * a link), this enum captures the type of thread container in which the
 * conversation takes place.
 */
@ProtobufEnum(name = "BotMetricsThreadEntryPoint")
public enum BotMetricsThreadEntryPoint {
    /**
     * A conversation thread opened from the dedicated AI tab in the bottom
     * navigation bar.
     */
    AI_TAB_THREAD(1),

    /**
     * A conversation thread opened from the AI home screen surface.
     */
    AI_HOME_THREAD(2),

    /**
     * A conversation thread opened via an immersive deep link, rendering
     * the AI conversation in a full-screen view.
     */
    AI_DEEPLINK_IMMERSIVE_THREAD(3),

    /**
     * A conversation thread opened via a standard deep link, rendering
     * the AI conversation within the normal chat interface.
     */
    AI_DEEPLINK_THREAD(4),

    /**
     * A conversation thread opened via the "Ask Meta AI" context menu
     * action on a selected message or chat.
     */
    ASK_META_AI_CONTEXT_MENU_THREAD(5);

    /**
     * Constructs a new thread entry point constant with the given protobuf
     * index.
     *
     * @param index the protobuf wire-format index for this constant
     */
    BotMetricsThreadEntryPoint(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * The protobuf wire-format index for this constant.
     */
    final int index;

    /**
     * Returns the protobuf wire-format index for this thread entry point
     * constant.
     *
     * @return the protobuf index
     */
    public int index() {
        return this.index;
    }
}
