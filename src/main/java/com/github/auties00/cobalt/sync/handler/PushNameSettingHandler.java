package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

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
    public boolean applyMutation(WhatsAppClient client, DecryptedMutation.Trusted mutation) {
        var setting = mutation.value()
                .pushNameSetting()
                .orElseThrow(() -> new IllegalArgumentException("Missing pushNameSetting"));

        client.store()
                .setName(setting.name());

        client.store()
                .jid()
                .flatMap(entry -> client.store().findContactByJid(entry.withoutData()))
                .ifPresent(contact -> contact.setChosenName(setting.name()));

        return true;
    }
}
