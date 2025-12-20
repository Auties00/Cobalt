package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

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
    public boolean applyMutation(WhatsAppClient client, DecryptedMutation.Trusted mutation) {
        var setting = mutation.value()
                .securityNotificationSetting()
                .orElseThrow(() -> new IllegalArgumentException("Missing securityNotificationSetting"));

        client.store()
                .setShowSecurityNotifications(setting.showNotification());

        return true;
    }
}
