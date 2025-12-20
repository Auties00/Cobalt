package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

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
    public boolean applyMutation(WhatsAppClient client, DecryptedMutation.Trusted mutation) {
        // Have no clue if this is used in Whatsapp / if doc is right
        return true;
    }
}
