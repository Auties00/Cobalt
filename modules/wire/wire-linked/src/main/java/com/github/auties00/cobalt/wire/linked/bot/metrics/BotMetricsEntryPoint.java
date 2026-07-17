package com.github.auties00.cobalt.wire.linked.bot.metrics;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Enumerates the UI surfaces and navigation paths from which a user can
 * initiate a conversation with an AI bot on WhatsApp.
 *
 * <p>Each constant identifies a distinct location in the WhatsApp client where
 * the user tapped, clicked, or otherwise triggered a bot interaction. The
 * entry point is recorded in {@link BotMetricsMetadata#destinationEntryPoint()}
 * and sent to the server for analytics and engagement tracking.
 *
 * <p>The constants span several categories of entry points:
 * <ul>
 *   <li><b>Direct navigation</b>: {@link #FAVICON}, {@link #CHATLIST},
 *       {@link #DEEPLINK}, {@link #NOTIFICATION}, {@link #APP_SHORTCUT}
 *   <li><b>AI search surfaces</b>: {@link #AISEARCH_NULL_STATE_PAPER_PLANE},
 *       {@link #AISEARCH_NULL_STATE_SUGGESTION},
 *       {@link #AISEARCH_TYPE_AHEAD_SUGGESTION}, and related constants
 *   <li><b>AI voice</b>: {@link #AIVOICE_SEARCH_BAR},
 *       {@link #AIVOICE_FAVICON}, {@link #AIVOICE_FAVICON_CALL_HISTORY}
 *   <li><b>AI tabs and home</b>: {@link #AI_TAB}, {@link #AI_HOME},
 *       {@link #AI_DEEPLINK}, {@link #AI_DEEPLINK_IMMERSIVE}
 *   <li><b>Context menus</b>: {@link #ASK_META_AI_CONTEXT_MENU},
 *       {@link #ASK_META_AI_CONTEXT_MENU_1ON1},
 *       {@link #ASK_META_AI_CONTEXT_MENU_GROUP}
 *   <li><b>Message actions</b>: {@link #MESSAGE_QUICK_ACTION_1_ON_1_CHAT},
 *       {@link #MESSAGE_QUICK_ACTION_GROUP_CHAT}
 *   <li><b>Media surfaces</b>: {@link #ATTACHMENT_TRAY_1_ON_1_CHAT},
 *       {@link #MEDIA_PICKER_1_ON_1_CHAT},
 *       {@link #ASK_META_AI_MEDIA_VIEWER_1ON1}, and related constants
 *   <li><b>AI Studio</b>: {@link #AISTUDIO},
 *       {@link #META_AI_CHAT_SHORTCUT_AI_STUDIO},
 *       {@link #UGC_CHAT_SHORTCUT_AI_STUDIO}, {@link #NEW_CHAT_AI_STUDIO}
 *   <li><b>Other</b>: {@link #FORWARD}, {@link #META_AI_FORWARD},
 *       {@link #PROFILE_MESSAGE_BUTTON}, {@link #META_AI_SETTINGS}
 * </ul>
 */
@ProtobufEnum(name = "BotMetricsEntryPoint")
public enum BotMetricsEntryPoint {
    /**
     * The entry point is not defined or was not recognized by the client.
     * This is the protobuf default value (index {@code 0}).
     */
    UNDEFINED_ENTRY_POINT(0),

    /**
     * The small bot icon (favicon) displayed in the chat list header or
     * conversation toolbar.
     */
    FAVICON(1),

    /**
     * The main chat list surface, where the user selected an existing bot
     * conversation from their chat list.
     */
    CHATLIST(2),

    /**
     * The send (paper-plane) button shown when the AI search bar is in its
     * initial empty state with no typed query.
     */
    AISEARCH_NULL_STATE_PAPER_PLANE(3),

    /**
     * A pre-populated suggestion chip displayed in the AI search bar when no
     * query has been typed yet.
     */
    AISEARCH_NULL_STATE_SUGGESTION(4),

    /**
     * A type-ahead suggestion shown in the AI search bar as the user types
     * a query.
     */
    AISEARCH_TYPE_AHEAD_SUGGESTION(5),

    /**
     * The send (paper-plane) button shown in the AI search bar while
     * type-ahead suggestions are visible.
     */
    AISEARCH_TYPE_AHEAD_PAPER_PLANE(6),

    /**
     * A chat-list result item surfaced by AI search type-ahead, which the
     * user tapped to open a bot conversation.
     */
    AISEARCH_TYPE_AHEAD_RESULT_CHATLIST(7),

    /**
     * A message result item surfaced by AI search type-ahead, which the
     * user tapped to interact with Meta AI.
     */
    AISEARCH_TYPE_AHEAD_RESULT_MESSAGES(8),

    /**
     * The AI voice interaction button located in the global search bar.
     */
    AIVOICE_SEARCH_BAR(9),

    /**
     * The AI voice favicon in the main client interface.
     */
    AIVOICE_FAVICON(10),

    /**
     * The AI Studio management surface, where users create and configure
     * custom AI bots.
     */
    AISTUDIO(11),

    /**
     * A deep link URL (for example, from a web page or QR code) that
     * opens a bot conversation directly.
     */
    DEEPLINK(12),

    /**
     * A push notification tapped by the user that navigated to a bot
     * conversation.
     */
    NOTIFICATION(13),

    /**
     * The "Message" button displayed on a bot's profile page.
     */
    PROFILE_MESSAGE_BUTTON(14),

    /**
     * A forwarded message action that triggers a new bot interaction in a
     * conversation.
     */
    FORWARD(15),

    /**
     * An OS-level app shortcut (for example, a long-press action on the
     * home screen icon) that opens the bot directly.
     */
    APP_SHORTCUT(16),

    /**
     * The Family Features entry point for bot interactions related to
     * parental or family controls.
     */
    FF_FAMILY(17),

    /**
     * The dedicated AI tab in the bottom navigation bar of the WhatsApp
     * client.
     */
    AI_TAB(18),

    /**
     * The AI home screen, a dedicated surface for discovering and
     * interacting with AI bots.
     */
    AI_HOME(19),

    /**
     * An immersive deep link that opens the AI conversation in a
     * full-screen view.
     */
    AI_DEEPLINK_IMMERSIVE(20),

    /**
     * A standard deep link that opens the AI conversation within the
     * normal chat interface.
     */
    AI_DEEPLINK(21),

    /**
     * The Meta AI chat shortcut within the AI Studio surface.
     */
    META_AI_CHAT_SHORTCUT_AI_STUDIO(22),

    /**
     * A user-generated content (UGC) bot chat shortcut within the AI
     * Studio surface.
     */
    UGC_CHAT_SHORTCUT_AI_STUDIO(23),

    /**
     * The "New Chat" action triggered from within the AI Studio surface.
     */
    NEW_CHAT_AI_STUDIO(24),

    /**
     * The AI voice favicon displayed in the call history screen.
     */
    AIVOICE_FAVICON_CALL_HISTORY(25),

    /**
     * The "Ask Meta AI" option in a generic context menu (not specific to
     * any chat type).
     */
    ASK_META_AI_CONTEXT_MENU(26),

    /**
     * The "Ask Meta AI" option in the context menu of a one-on-one chat.
     */
    ASK_META_AI_CONTEXT_MENU_1ON1(27),

    /**
     * The "Ask Meta AI" option in the context menu of a group chat.
     */
    ASK_META_AI_CONTEXT_MENU_GROUP(28),

    /**
     * Direct invocation of Meta AI from within a one-on-one chat, such as
     * by typing an {@code @} mention.
     */
    INVOKE_META_AI_1ON1(29),

    /**
     * Direct invocation of Meta AI from within a group chat, such as
     * by typing an {@code @} mention.
     */
    INVOKE_META_AI_GROUP(30),

    /**
     * Forwarding a selected message to Meta AI for analysis or follow-up.
     */
    META_AI_FORWARD(31),

    /**
     * Starting a new chat with an AI contact from the new-chat contact
     * picker.
     */
    NEW_CHAT_AI_CONTACT(32),

    /**
     * A quick-action button displayed on a message bubble in a one-on-one
     * chat that triggers a Meta AI interaction.
     */
    MESSAGE_QUICK_ACTION_1_ON_1_CHAT(33),

    /**
     * A quick-action button displayed on a message bubble in a group chat
     * that triggers a Meta AI interaction.
     */
    MESSAGE_QUICK_ACTION_GROUP_CHAT(34),

    /**
     * The attachment tray in a one-on-one chat, used to send media or files
     * to Meta AI.
     */
    ATTACHMENT_TRAY_1_ON_1_CHAT(35),

    /**
     * The attachment tray in a group chat, used to send media or files
     * to Meta AI.
     */
    ATTACHMENT_TRAY_GROUP_CHAT(36),

    /**
     * The "Ask Meta AI" option in the media viewer when viewing media from
     * a one-on-one chat.
     */
    ASK_META_AI_MEDIA_VIEWER_1ON1(37),

    /**
     * The "Ask Meta AI" option in the media viewer when viewing media from
     * a group chat.
     */
    ASK_META_AI_MEDIA_VIEWER_GROUP(38),

    /**
     * The media picker in a one-on-one chat, used to select images or
     * videos to send to Meta AI.
     */
    MEDIA_PICKER_1_ON_1_CHAT(39),

    /**
     * The media picker in a group chat, used to select images or videos
     * to send to Meta AI.
     */
    MEDIA_PICKER_GROUP_CHAT(40),

    /**
     * The "Ask Meta AI" prompt displayed when a regular search yields no
     * matching results.
     */
    ASK_META_AI_NO_SEARCH_RESULTS(41),

    /**
     * The Meta AI settings page, where the user can configure preferences
     * related to AI features.
     */
    META_AI_SETTINGS(45);

    /**
     * Constructs a new entry point constant with the given protobuf index.
     *
     * @param index the protobuf wire-format index for this constant
     */
    BotMetricsEntryPoint(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * The protobuf wire-format index for this constant.
     */
    final int index;

    /**
     * Returns the protobuf wire-format index for this entry point constant.
     *
     * @return the protobuf index
     */
    public int index() {
        return this.index;
    }
}
