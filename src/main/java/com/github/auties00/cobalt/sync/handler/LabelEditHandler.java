package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.model.sync.RecordSync;
import com.github.auties00.cobalt.store.WhatsappStore;

/**
 * Handles label edit actions.
 *
 * <p>This handler processes mutations that create, update, or delete chat/message labels.
 *
 * <p>Index format: ["labelEditAction", "labelId"]
 */
public final class LabelEditHandler implements WebAppStateActionHandler {
    public static final LabelEditHandler INSTANCE = new LabelEditHandler();

    private LabelEditHandler() {

    }

    @Override
    public String actionName() {
        return "labelEditAction";
    }

    @Override
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var action = mutation.value()
                .labelEditAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing labelEditAction"));

        var indexArray = JSON.parseArray(mutation.index());
        var labelId = indexArray.getInteger(1);

        if (mutation.operation() == RecordSync.Operation.SET) {
            if(action.deleted()) {
                store.removeLabel(labelId);
            }else {
                var label = store.findLabel(labelId);
                if(label.isPresent()) {
                    if(action.name().isPresent()) {
                        label.get().setName(action.name().get());
                    }
                    if(action.color().isPresent()) {
                        label.get().setColor(action.color().getAsInt());
                    }
                }else {
                    store.addLabel(action.toLabel());
                }
            }
        } else {
            store.removeLabel(labelId);
        }

        return true;
    }
}
