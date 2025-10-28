package com.github.auties00.cobalt.sync;

import com.github.auties00.cobalt.sync.handler.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class WebAppStateHandlerRegistry {
    private final Map<String, WebAppStateActionHandler> handlers;

    public WebAppStateHandlerRegistry() {
        this.handlers = new HashMap<>();
        registerDefaultHandlers();
    }

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

    public void registerHandler(WebAppStateActionHandler handler) {
        handlers.put(handler.actionName(), handler);
    }

    public Optional<WebAppStateActionHandler> findHandler(String actionName) {
        return Optional.ofNullable(handlers.get(actionName));
    }
}
