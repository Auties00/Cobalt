package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.sync.RecordSync;
import com.github.auties00.cobalt.store.WhatsappStore;

/**
 * Handles delete message for me actions.
 *
 * <p>This handler processes mutations that delete messages locally
 * (not for everyone in the chat).
 *
 * <p>Index format: ["deleteMessageForMeAction", "chatJid", "fromMe", "messageId"]
 */
public final class DeleteMessageForMeHandler implements WebAppStateActionHandler {
    public static final DeleteMessageForMeHandler INSTANCE = new DeleteMessageForMeHandler();

    private DeleteMessageForMeHandler() {

    }

    @Override
    public String actionName() {
        return "deleteMessageForMeAction";
    }

    @Override
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var action = mutation.value()
                .deleteMessageForMeAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing deleteMessageForMeAction"));

        var indexArray = JSON.parseArray(mutation.index());
        var chatJidString = indexArray.getString(1);
        // var fromMe = indexArray.getBoolean(2);
        var messageId = indexArray.getString(3);

        var chatJid = Jid.of(chatJidString);

        var chat = store.findChatByJid(chatJid);
        if (chat.isEmpty()) {
            return false;
        }

        var message = store.findMessageById(chat.get(), messageId);
        if(message.isPresent()) {
            if(mutation.operation() == RecordSync.Operation.SET) {
                chat.get().removeMessage(message.get().id());
            }
        }

        return true;
    }
}
