package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.sync.RecordSync;
import com.github.auties00.cobalt.store.WhatsappStore;

/**
 * Handles delete chat actions.
 *
 * <p>This handler processes mutations that delete entire chats.
 *
 * <p>Index format: ["deleteChatAction", "chatJid"]
 */
public final class DeleteChatHandler implements WebAppStateActionHandler {
    public static final DeleteChatHandler INSTANCE = new DeleteChatHandler();

    private DeleteChatHandler() {

    }

    @Override
    public String actionName() {
        return "deleteChatAction";
    }

    @Override
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var action = mutation.value()
                .deleteChatAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing deleteChatAction"));

        var chatJidString = extractChatIdFromIndex(mutation.index());
        var chatJid = Jid.of(chatJidString);

        var chat = store.findChatByJid(chatJid);
        if (chat.isPresent()) {
            if (mutation.operation() == RecordSync.Operation.SET) {
                store.removeChat(chatJid);
            }
        }

        return true;
    }

    private String extractChatIdFromIndex(String index) {
        // Index format: ["deleteChatAction", "chatJid"]
        return JSON.parseArray(index).getString(1);
    }
}
