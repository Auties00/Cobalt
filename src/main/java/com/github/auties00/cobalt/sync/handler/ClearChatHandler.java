package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.sync.RecordSync;
import com.github.auties00.cobalt.store.WhatsappStore;

/**
 * Handles clear chat actions.
 *
 * <p>This handler processes mutations that clear all messages from a chat
 * while keeping the chat itself.
 *
 * <p>Index format: ["clearChatAction", "chatJid"]
 */
public final class ClearChatHandler implements WebAppStateActionHandler {

    public static final ClearChatHandler INSTANCE = new ClearChatHandler();

    private ClearChatHandler() {
    }

    @Override
    public String actionName() {
        return "clearChatAction";
    }

    @Override
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var action = mutation.value()
                .clearChatAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing clearChatAction"));

        var chatJidString = JSON.parseArray(mutation.index())
                .getString(1);
        var chatJid = Jid.of(chatJidString);

        var chat = store.findChatByJid(chatJid);
        if (chat.isEmpty()) {
            return false;
        }

        if (mutation.operation() == RecordSync.Operation.SET) {
            chat.get().removeMessages();;
        }

        return true;
    }
}
