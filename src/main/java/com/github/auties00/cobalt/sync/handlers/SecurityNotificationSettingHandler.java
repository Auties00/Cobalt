package com.github.auties00.cobalt.sync.handlers;

import com.github.auties00.cobalt.model.core.sync.DecryptedMutation;
import com.github.auties00.cobalt.store.WhatsappStore;
import com.github.auties00.cobalt.sync.WebAppStateActionHandler;

/**
 * Handles security notification setting changes.
 *
 * <p>This handler processes mutations that control whether to show security
 * code change notifications when chatting with a contact.
 */
public final class SecurityNotificationSettingHandler implements WebAppStateActionHandler {
    public static final SecurityNotificationSettingHandler INSTANCE = new SecurityNotificationSettingHandler();

    private SecurityNotificationSettingHandler() {

    }

    @Override
    public String actionName() {
        return "security";
    }

    @Override
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var setting = mutation.value()
                .securityNotificationSetting()
                .orElseThrow(() -> new IllegalArgumentException("Missing securityNotificationSetting"));

        // Update the security notification setting in the store
        store.setShowSecurityNotifications(setting.showNotification());

        return true;
    }
}
