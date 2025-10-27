package com.github.auties00.cobalt.sync;

import com.github.auties00.cobalt.store.WhatsappStore;
import com.github.auties00.cobalt.sync.handlers.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for action handlers.
 *
 * <p>This class maintains a mapping from action names to their handlers
 * and provides lookup functionality.
 */
public final class WebAppStateActionHandlerRegistry {
    private final Map<String, WebAppStateActionHandler> handlers;
    private final WhatsappStore store;

    /**
     * Creates a new ActionHandlerRegistry and registers all handlers.
     *
     * @param store the WhatsappStore
     */
    public WebAppStateActionHandlerRegistry(WhatsappStore store) {
        this.store = store;
        this.handlers = new HashMap<>();
        registerDefaultHandlers();
    }

    /**
     * Registers all default action handlers.
     *
     * <p>This method is called during initialization to register built-in handlers.
     * Additional handlers can be registered via {@link #registerHandler(WebAppStateActionHandler)}.
     */
    private void registerDefaultHandlers() {
        // Chat actions
        registerHandler(ArchiveChatHandler.INSTANCE);
        registerHandler(PinChatHandler.INSTANCE);
        registerHandler(MuteChatHandler.INSTANCE);
        registerHandler(MarkChatAsReadHandler.INSTANCE);
        registerHandler(ClearChatHandler.INSTANCE);
        registerHandler(DeleteChatHandler.INSTANCE);

        // Message actions
        registerHandler(StarMessageHandler.INSTANCE);
        registerHandler(DeleteMessageForMeHandler.INSTANCE);

        // Contact actions
        registerHandler(ContactActionHandler.INSTANCE);

        // Label actions
        registerHandler(LabelEditHandler.INSTANCE);
        registerHandler(LabelAssociationHandler.INSTANCE);

        // Business actions
        registerHandler(QuickReplyHandler.INSTANCE);
        registerHandler(AgentActionHandler.INSTANCE);
        registerHandler(SubscriptionHandler.INSTANCE);
        registerHandler(ChatAssignmentHandler.INSTANCE);
        registerHandler(ChatAssignmentOpenedStatusHandler.INSTANCE);

        // Sticker actions
        registerHandler(FavoriteStickerHandler.INSTANCE);
        registerHandler(StickerHandler.INSTANCE);
        registerHandler(RemoveRecentStickerHandler.INSTANCE);

        // User preference actions
        registerHandler(UserStatusMuteHandler.INSTANCE);
        registerHandler(TimeFormatHandler.INSTANCE);
        registerHandler(RecentEmojiWeightsHandler.INSTANCE);

        // System actions
        registerHandler(NuxActionHandler.INSTANCE);
        registerHandler(PrimaryVersionHandler.INSTANCE);

        // Settings
        registerHandler(PushNameSettingHandler.INSTANCE);
        registerHandler(LocaleSettingHandler.INSTANCE);
        registerHandler(UnarchiveChatsSettingHandler.INSTANCE);
        registerHandler(SecurityNotificationSettingHandler.INSTANCE);
    }

    /**
     * Registers a custom action handler.
     *
     * @param handler the handler to register
     */
    public void registerHandler(WebAppStateActionHandler handler) {
        handlers.put(handler.actionName(), handler);
    }

    /**
     * Finds a handler for the given action name.
     *
     * @param actionName the action name
     * @return the handler, or empty if not found
     */
    public Optional<WebAppStateActionHandler> findHandler(String actionName) {
        return Optional.ofNullable(handlers.get(actionName));
    }

    /**
     * Gets the WhatsappStore.
     *
     * @return the store
     */
    public WhatsappStore getStore() {
        return store;
    }
}
