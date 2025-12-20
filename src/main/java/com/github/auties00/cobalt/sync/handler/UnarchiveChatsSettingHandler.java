package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

/**
 * Handles unarchive chats setting changes.
 *
 * <p>This handler processes mutations that control whether archived chats
 * should be automatically unarchived when a new message arrives.
 */
public final class UnarchiveChatsSettingHandler implements WebAppStateActionHandler {
    public static final UnarchiveChatsSettingHandler INSTANCE = new UnarchiveChatsSettingHandler();

    private UnarchiveChatsSettingHandler() {

    }

    @Override
    public String actionName() {
        return "unarchiveChats";
    }

    @Override
    public boolean applyMutation(WhatsAppClient client, DecryptedMutation.Trusted mutation) {
        var setting = mutation.value()
                .unarchiveChatsSetting()
                .orElseThrow(() -> new IllegalArgumentException("Missing unarchiveChatsSetting"));

        client.store()
                .setUnarchiveChats(setting.unarchiveChats());

        return true;
    }
}
