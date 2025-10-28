package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.model.sync.RecordSync;
import com.github.auties00.cobalt.store.WhatsappStore;

/**
 * Handles NUX (New User Experience) actions.
 *
 * <p>This handler processes mutations that track completion of onboarding steps
 * and new feature introductions.
 *
 * <p>Index format: ["nuxAction", "nuxId"]
 */
public final class NuxActionHandler implements WebAppStateActionHandler {

    public static final NuxActionHandler INSTANCE = new NuxActionHandler();

    private NuxActionHandler() {
    }

    @Override
    public String actionName() {
        return "nuxAction";
    }

    @Override
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var action = mutation.value()
                .nuxAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing nuxAction"));

        var indexArray = JSON.parseArray(mutation.index());
        var nuxId = indexArray.getString(1);

        // Apply the action
        if (mutation.operation() == RecordSync.Operation.SET) {
            action.acknowledged().ifPresent(acknowledged -> {
                if (acknowledged) {
                    store.markNuxCompleted(nuxId);
                }
            });
        } else {
            // REMOVE operation - reset NUX state
            store.resetNux(nuxId);
        }

        return true;
    }
}
