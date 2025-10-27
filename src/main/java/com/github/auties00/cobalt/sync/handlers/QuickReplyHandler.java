package com.github.auties00.cobalt.sync.handlers;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.sync.model.DecryptedMutation;
import com.github.auties00.cobalt.store.WhatsappStore;
import com.github.auties00.cobalt.sync.WebAppStateActionHandler;

/**
 * Handles quick reply actions.
 *
 * <p>This handler processes mutations that create, update, or delete quick reply templates
 * for business accounts.
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

        var indexArray = JSON.parseArray(mutation.index());
        var shortcut = indexArray.getString(1);

        switch (mutation.operation()) {
            case SET -> {
                var quickReply = store.findQuickReply(shortcut)
                        .orElseGet(() -> store.createQuickReply(shortcut));

                action.message().ifPresent(quickReply::setMessage);
                action.keywords().ifPresent(quickReply::setKeywords);
                action.count().ifPresent(quickReply::setCount);
                action.deleted().ifPresent(quickReply::setDeleted);
            }
            case REMOVE ->  store.removeQuickReply(shortcut);
        }

        return true;
    }
}
