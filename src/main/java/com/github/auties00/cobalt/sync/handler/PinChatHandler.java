package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.util.Clock;

/**
 * Handles pin chat actions.
 *
 * <p>This handler processes mutations that pin or unpin chats to the top of the chat list.
 *
 * <p>Index format: ["pinAction", "chatJid", "timestamp"]
 */
public final class PinChatHandler implements WebAppStateActionHandler {
    public static final PinChatHandler INSTANCE = new PinChatHandler();

    private PinChatHandler() {

    }

    @Override
    public String actionName() {
        return "pinAction";
    }

    @Override
    public boolean applyMutation(WhatsAppClient client, DecryptedMutation.Trusted mutation) {
        var action = mutation.value()
                .pinAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing pinAction"));

        var chatJidString = JSON.parseArray(mutation.index()).getString(1);
        var chatJid = Jid.of(chatJidString);

        var chat = client.store()
                .findChatByJid(chatJid);
        if (chat.isEmpty()) {
            return false;
        }

        switch (mutation.operation()) {
            case SET -> {
                if (action.pinned()) {
                    chat.get().setPinnedTimestampSeconds((int) Clock.nowSeconds());
                } else {
                    chat.get().setPinnedTimestampSeconds(0);
                }
            }
            case REMOVE -> chat.get().setPinnedTimestampSeconds(0);
        }

        return true;
    }
}
