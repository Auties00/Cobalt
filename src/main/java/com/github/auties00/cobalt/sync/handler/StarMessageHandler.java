package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.store.WhatsappStore;

/**
 * Handles star message actions.
 *
 * <p>This handler processes mutations that star or unstar messages.
 *
 * <p>Index format: ["starAction", "chatJid", "messageId", "fromMe"]
 */
public final class StarMessageHandler implements WebAppStateActionHandler {
    public static final StarMessageHandler INSTANCE = new StarMessageHandler();

    private StarMessageHandler() {

    }

    @Override
    public String actionName() {
        return "starAction";
    }

    @Override
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {

        var action = mutation.value().starAction().orElseThrow(() -> new IllegalArgumentException("Missing starAction"));

        var indexArray = JSON.parseArray(mutation.index());
        var chatJidString = indexArray.getString(1);
        var messageId = indexArray.getString(2);
        // var fromMe = indexArray.getBoolean(3);

        var chatJid = Jid.of(chatJidString);

        var message = store.findMessageById(chatJid, messageId);
        if (message.isEmpty()) {
            return false;
        }

        switch (mutation.operation()) {
            case SET -> message.get().setStarred(action.starred());
            case REMOVE -> message.get().setStarred(false);
        }

        return true;
    }
}
