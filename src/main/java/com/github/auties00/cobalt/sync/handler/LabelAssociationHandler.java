package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.sync.RecordSync;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

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
    public boolean applyMutation(WhatsAppClient client, DecryptedMutation.Trusted mutation) {
        var action = mutation.value()
                .labelAssociationAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing labelAssociationAction"));

        // Extract info from index
        var indexArray = JSON.parseArray(mutation.index());
        var targetJidString = indexArray.getString(1);
        var labelId = indexArray.getInteger(2);

        var targetJid = Jid.of(targetJidString);

        // Find label
        var label = client.store()
                .findLabel(labelId);
        if (label.isEmpty()) {
            return false;
        }

        // Apply the action
        if (mutation.operation() == RecordSync.Operation.SET) {
            // Associate label
            if (action.labeled()) {
                label.get().addAssignment(targetJid);
            } else {
                label.get().removeAssignment(targetJid);
            }
        } else {
            // Remove association
            label.get().removeAssignment(targetJid);
        }

        return true;
    }
}
