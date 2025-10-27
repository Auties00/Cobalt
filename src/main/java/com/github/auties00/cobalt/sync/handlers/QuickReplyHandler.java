package com.github.auties00.cobalt.sync.handlers;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.model.core.sync.DecryptedMutation;
import com.github.auties00.cobalt.model.proto.sync.RecordSync;
import com.github.auties00.cobalt.store.WhatsappStore;
import com.github.auties00.cobalt.sync.WebAppStateActionHandler;

/**
 * Handles quick reply actions.
 *
 * <p>This handler processes mutations that create, update, or delete quick reply templates
 * for business accounts.
 *
 * <p>When conflicts occur, quick reply fields are merged intelligently to preserve
 * the most complete information from both devices.
 *
 * <p>Index format: ["quickReplyAction", "shortcut"]
 */
public final class QuickReplyHandler implements WebAppStateActionHandler {
    public static final QuickReplyHandler INSTANCE = new QuickReplyHandler();

    private QuickReplyHandler() {

    }

    @Override
    public String actionName() {
        return "quickReplyAction";
    }

    @Override
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var action = mutation.value()
                .quickReplyAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing quickReplyAction"));

        // Extract shortcut from index
        var indexArray = JSON.parseArray(mutation.index());
        var shortcut = indexArray.getString(1);

        // Apply the action
        if (mutation.operation() == RecordSync.Operation.SET) {
            // Create or update quick reply
            var quickReply = store.findQuickReplyByShortcut(shortcut).orElseGet(() -> store.createQuickReply(shortcut));

            action.message().ifPresent(quickReply::setMessage);
            action.keywords().ifPresent(quickReply::setKeywords);
            action.count().ifPresent(quickReply::setCount);
            action.deleted().ifPresent(quickReply::setDeleted);

        } else {
            // REMOVE operation - delete quick reply
            store.findQuickReplyByShortcut(shortcut).ifPresent(store::deleteQuickReply);
        }

        return true;
    }
}
