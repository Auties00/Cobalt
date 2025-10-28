package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.store.WhatsappStore;

/**
 * Handles push name setting changes.
 *
 * <p>This handler processes mutations that update the user's display name (push name).
 */
public final class PushNameSettingHandler implements WebAppStateActionHandler {
    public static final PushNameSettingHandler INSTANCE = new PushNameSettingHandler();

    private PushNameSettingHandler() {

    }

    @Override
    public String actionName() {
        return "pushName";
    }

    @Override
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var setting = mutation.value()
                .pushNameSetting()
                .orElseThrow(() -> new IllegalArgumentException("Missing pushNameSetting"));

        store.setName(setting.name());

        store.jid()
                .flatMap(entry -> store.findContactByJid(entry.withoutData()))
                .ifPresent(contact -> contact.setChosenName(setting.name()));

        return true;
    }
}
