package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.store.WhatsappStore;

/**
 * Handles primary version actions.
 *
 * <p>This handler processes mutations that track the primary WhatsApp client version.
 */
public final class PrimaryVersionHandler implements WebAppStateActionHandler {
    public static final PrimaryVersionHandler INSTANCE = new PrimaryVersionHandler();

    private PrimaryVersionHandler() {

    }

    @Override
    public String actionName() {
        return "primaryVersionAction";
    }

    @Override
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var action = mutation.value()
                .primaryVersionAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing primaryVersionAction"));

        // Update primary version in the store
        action.version().ifPresent(store::setPrimaryVersion);

        return true;
    }
}
