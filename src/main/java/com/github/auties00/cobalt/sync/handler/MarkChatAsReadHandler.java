package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

/**
 * Handles mark chat as read actions.
 *
 * <p>This handler processes mutations that mark all messages in a chat as read.
 *
 * <p>Index format: ["markChatAsReadAction", "chatJid"]
 */
public final class MarkChatAsReadHandler implements WebAppStateActionHandler {
    public static final MarkChatAsReadHandler INSTANCE = new MarkChatAsReadHandler();

    private MarkChatAsReadHandler() {

    }

    @Override
    public String actionName() {
        return "markChatAsReadAction";
    }

    @Override
    public boolean applyMutation(WhatsAppClient client, DecryptedMutation.Trusted mutation) {
        var action = mutation.value()
                .markChatAsReadAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing markChatAsReadAction"));

        var chatJidString = JSON.parseArray(mutation.index())
                .getString(1);
        var chatJid = Jid.of(chatJidString);

        var chat = client.store()
                .findChatByJid(chatJid);
        if (chat.isEmpty()) {
            return false;
        }

        switch (mutation.operation()) {
            case SET -> {
                chat.get().setMarkedAsUnread(action.read());
                chat.get().setUnreadMessagesCount(0);
            }
            case REMOVE -> {
                chat.get().setMarkedAsUnread(true);
                chat.get().setUnreadMessagesCount(-1);
            }
        }

        return true;
    }
}
