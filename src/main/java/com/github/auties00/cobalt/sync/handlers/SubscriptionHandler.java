package com.github.auties00.cobalt.sync.handlers;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.sync.model.DecryptedMutation;
import com.github.auties00.cobalt.model.proto.jid.Jid;
import com.github.auties00.cobalt.store.WhatsappStore;
import com.github.auties00.cobalt.sync.WebAppStateActionHandler;

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
        var action = mutation.value()
                .subscriptionAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing subscriptionAction"));

        var indexArray = JSON.parseArray(mutation.index());
        var subscriptionId = indexArray.getString(1);
        var subscriptionJid = Jid.of(subscriptionId);

        var newsletter = store.findNewsletterByJid(subscriptionJid);
        if(newsletter.isEmpty()) {
            return false;
        }

        // TODO: Not sure what to do
        switch (mutation.operation()) {
            case SET -> {

            }

            case REMOVE -> {

            }
        }

        return true;
    }
}
