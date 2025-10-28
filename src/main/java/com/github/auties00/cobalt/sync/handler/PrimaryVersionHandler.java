package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.model.auth.Version;
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

        action.version()
                .map(Version::of)
                .ifPresent(store::setCompanionVersion);

        return true;
    }
}
