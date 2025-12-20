package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

/**
 * Handles archive chat actions.
 *
 * <p>This handler processes mutations that archive or unarchive chats.
 * The action is identified by the "archiveChatAction" field in ActionValueSync.
 *
 * <p>Index format: ["archiveChatAction", "chatJid"]
 */
public final class ArchiveChatHandler implements WebAppStateActionHandler {
    public static final ArchiveChatHandler INSTANCE = new ArchiveChatHandler();

    private ArchiveChatHandler() {

    }

    @Override
    public String actionName() {
        return "archiveChatAction";
    }

    @Override
    public boolean applyMutation(WhatsAppClient client, DecryptedMutation.Trusted mutation) {
        var action = mutation.value().archiveChatAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing archiveChatAction"));

        var chatJidString = JSON.parseArray(mutation.index())
                .getString(1);
        var chatJid = Jid.of(chatJidString);

        var chat = client.store()
                .findChatByJid(chatJid);
        if (chat.isEmpty()) {
            return false;
        }

        switch (mutation.operation()) {
            case SET -> chat.get().setArchived(action.archived());
            case REMOVE -> chat.get().setArchived(false);
        }

        return true;
    }
}
