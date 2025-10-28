package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.store.WhatsappStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

/**
 * Handles subscription actions.
 *
 * <p>This handler processes mutations that manage newsletter/channel subscriptions.
 *
 * <p>Index format: ["subscriptionAction", "subscriptionId"]
 */
public final class SubscriptionHandler implements WebAppStateActionHandler {
    public static final SubscriptionHandler INSTANCE = new SubscriptionHandler();

    private SubscriptionHandler() {

    }

    @Override
    public String actionName() {
        return "subscriptionAction";
    }

    @Override
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        // Not used in WhatsApp
        return true;
    }
}
