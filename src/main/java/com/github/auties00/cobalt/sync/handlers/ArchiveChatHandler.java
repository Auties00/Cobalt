package com.github.auties00.cobalt.sync.handlers;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.sync.model.DecryptedMutation;
import com.github.auties00.cobalt.model.proto.jid.Jid;
import com.github.auties00.cobalt.model.proto.sync.RecordSync;
import com.github.auties00.cobalt.store.WhatsappStore;
import com.github.auties00.cobalt.sync.WebAppStateActionHandler;

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
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var action = mutation.value().archiveChatAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing archiveChatAction"));

        var chatJidString = JSON.parseArray(mutation.index())
                .getString(1);
        var chatJid = Jid.of(chatJidString);

        var chat = store.findChatByJid(chatJid);
        if (chat.isEmpty()) {
            return false;
        }

        if (mutation.operation() == RecordSync.Operation.SET) {
            chat.get().setArchived(action.archived());
        } else {
            chat.get().setArchived(false);
        }

        return true;
    }
}
