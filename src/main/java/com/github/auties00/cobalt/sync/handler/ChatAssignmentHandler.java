package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.sync.RecordSync;
import com.github.auties00.cobalt.store.WhatsappStore;

/**
 * Handles chat assignment actions.
 *
 * <p>This handler processes mutations that assign chats to agents
 * in business accounts.
 *
 * <p>Index format: ["chatAssignmentAction", "chatJid"]
 */
public final class ChatAssignmentHandler implements WebAppStateActionHandler {
    public static final ChatAssignmentHandler INSTANCE = new ChatAssignmentHandler();

    private ChatAssignmentHandler() {

    }

    @Override
    public String actionName() {
        return "chatAssignmentAction";
    }

    @Override
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var action = mutation.value().chatAssignmentAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing chatAssignmentAction"));

        var indexArray = JSON.parseArray(mutation.index());
        var chatJidString = indexArray.getString(1);
        var chatJid = Jid.of(chatJidString);

        var chat = store.findChatByJid(chatJid);
        if (chat.isEmpty()) {
            return false;
        }

        if (mutation.operation() == RecordSync.Operation.SET) {
            chat.get().setAssignedAgent(action.deviceAgentId());
        } else {
            chat.get().setAssignedAgent(null);
        }

        return true;
    }
}
