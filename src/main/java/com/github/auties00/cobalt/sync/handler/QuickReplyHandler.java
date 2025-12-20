package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

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
    public boolean applyMutation(WhatsAppClient client, DecryptedMutation.Trusted mutation) {
        var action = mutation.value()
                .quickReplyAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing quickReplyAction"));

        var indexArray = JSON.parseArray(mutation.index());
        var shortcut = indexArray.getString(1);

        switch (mutation.operation()) {
            case SET -> {
                if(action.deleted()) {
                    client.store()
                            .removeQuickReply(shortcut);
                }else {
                    client.store()
                            .addQuickReply(action.toQuickReply());
                }
            }
            case REMOVE -> client.store()
                    .removeQuickReply(shortcut);
        }

        return true;
    }
}
