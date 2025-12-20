package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

/**
 * Handles locale setting changes.
 *
 * <p>This handler processes mutations that update the user's language preference.
 */
public final class LocaleSettingHandler implements WebAppStateActionHandler {
    public static final LocaleSettingHandler INSTANCE = new LocaleSettingHandler();

    private LocaleSettingHandler() {

    }

    @Override
    public String actionName() {
        return "locale";
    }

    @Override
    public boolean applyMutation(WhatsAppClient client, DecryptedMutation.Trusted mutation) {
        var setting = mutation.value()
                .localeSetting()
                .orElseThrow(() -> new IllegalArgumentException("Missing localeSetting"));

        var oldLocale = client.store()
                .locale()
                .orElse(null);
        var newLocale = setting.locale();

        client.store()
                .setLocale(newLocale);

        for(var listener : client.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onLocaleChanged(client, oldLocale, newLocale));
        }

        return true;
    }
}
