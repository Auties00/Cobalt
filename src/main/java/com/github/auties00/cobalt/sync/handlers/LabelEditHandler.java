package com.github.auties00.cobalt.sync.handlers;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.sync.model.DecryptedMutation;
import com.github.auties00.cobalt.model.proto.sync.RecordSync;
import com.github.auties00.cobalt.store.WhatsappStore;
import com.github.auties00.cobalt.sync.WebAppStateActionHandler;

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
        var labelId = indexArray.getString(1);

        if (mutation.operation() == RecordSync.Operation.SET) {
            var label = store.findLabelById(labelId)
                    .orElseGet(() -> store.createLabel(labelId));

            action.name().ifPresent(label::setName);
            action.color().ifPresent(label::setColor);
            action.predefinedId().ifPresent(label::setPredefinedId);
            action.deleted().ifPresent(label::setDeleted);

        } else {
            store.findLabelById(labelId).ifPresent(store::deleteLabel);
        }

        return true;
    }
}
