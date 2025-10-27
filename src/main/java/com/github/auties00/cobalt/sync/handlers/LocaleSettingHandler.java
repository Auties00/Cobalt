package com.github.auties00.cobalt.sync.handlers;

import com.github.auties00.cobalt.model.core.sync.DecryptedMutation;
import com.github.auties00.cobalt.store.WhatsappStore;
import com.github.auties00.cobalt.sync.WebAppStateActionHandler;

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
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var setting = mutation.value()
                .localeSetting()
                .orElseThrow(() -> new IllegalArgumentException("Missing localeSetting"));

        store.setLocale(setting.locale());

        return true;
    }
}
