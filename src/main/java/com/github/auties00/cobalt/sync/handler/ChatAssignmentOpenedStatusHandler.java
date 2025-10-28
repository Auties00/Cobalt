package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.sync.RecordSync;
import com.github.auties00.cobalt.store.WhatsappStore;

/**
 * Handles chat assignment opened status actions.
 *
 * <p>This handler processes mutations that track whether an assigned chat
 * has been opened by the agent.
 *
 * <p>Index format: ["chatAssignmentOpenedStatusAction", "chatJid"]
 */
public final class ChatAssignmentOpenedStatusHandler implements WebAppStateActionHandler {
    public static final ChatAssignmentOpenedStatusHandler INSTANCE = new ChatAssignmentOpenedStatusHandler();

    private ChatAssignmentOpenedStatusHandler() {

    }

    @Override
    public String actionName() {
        return "chatAssignmentOpenedStatusAction";
    }

    @Override
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var action = mutation.value()
                .chatAssignmentOpenedStatusAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing chatAssignmentOpenedStatusAction"));

        var indexArray = JSON.parseArray(mutation.index());
        var chatJidString = indexArray.getString(1);
        var chatJid = Jid.of(chatJidString);

        var chat = store.findChatByJid(chatJid);
        if (chat.isEmpty()) {
            return false;
        }

        // Apply the action
        if (mutation.operation() == RecordSync.Operation.SET) {
            chat.get().setAssignmentOpened(action.chatOpened());
        } else {
            chat.get().setAssignmentOpened(false);
        }

        return true;
    }
}
