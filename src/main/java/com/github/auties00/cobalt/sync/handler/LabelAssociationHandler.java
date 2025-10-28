package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.sync.RecordSync;
import com.github.auties00.cobalt.store.WhatsappStore;

/**
 * Handles label association actions.
 *
 * <p>This handler processes mutations that assign labels to chats or messages.
 *
 * <p>Index format: ["labelAssociationAction", "chatOrMessageJid", "labelId"]
 */
public final class LabelAssociationHandler implements WebAppStateActionHandler {
    public static final LabelAssociationHandler INSTANCE = new LabelAssociationHandler();

    private LabelAssociationHandler() {

    }

    @Override
    public String actionName() {
        return "labelAssociationAction";
    }

    @Override
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var action = mutation.value()
                .labelAssociationAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing labelAssociationAction"));

        // Extract info from index
        var indexArray = JSON.parseArray(mutation.index());
        var targetJidString = indexArray.getString(1);
        var labelId = indexArray.getString(2);

        var targetJid = Jid.of(targetJidString);

        // Find label
        var label = store.findLabelById(labelId);
        if (label.isEmpty()) {
            return false;
        }

        // Check if target is chat or message
        var chat = store.findChatByJid(targetJid);
        if (chat.isEmpty()) {
            return false;
        }

        // Apply the action
        if (mutation.operation() == RecordSync.Operation.SET) {
            // Associate label
            if (action.labeled()) {
                chat.get().addLabel(label.get());
            } else {
                chat.get().removeLabel(label.get());
            }
        } else {
            // REMOVE operation - remove label association
            chat.get().removeLabel(label.get());
        }

        return true;
    }
}
