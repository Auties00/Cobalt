package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

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
    public boolean applyMutation(WhatsAppClient client, DecryptedMutation.Trusted mutation) {
        var action = mutation.value()
                .deleteChatAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing deleteChatAction"));

        var chatJidString = JSON.parseArray(mutation.index())
                .getString(1);
        var chatJid = Jid.of(chatJidString);

        // TODO: Consider message range from action
        client.store()
                .findChatByJid(chatJid)
                .ifPresent(client.store()::removeChat);

        return true;
    }

}
