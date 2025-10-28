package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.store.WhatsappStore;

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
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var setting = mutation.value()
                .unarchiveChatsSetting()
                .orElseThrow(() -> new IllegalArgumentException("Missing unarchiveChatsSetting"));

        store.setUnarchiveChats(setting.unarchiveChats());

        return true;
    }
}
