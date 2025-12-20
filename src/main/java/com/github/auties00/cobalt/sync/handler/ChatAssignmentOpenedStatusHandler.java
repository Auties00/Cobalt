package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

/**
 * Handles chat assignment opened status actions.
 *
 * <p>This handler processes mutations that track whether an assigned chat
 * has been opened by the agent.
 *
 * <p>Index format: ["chatAssignmentOpenedStatusAction", "chatJid"]
 */
public final class ChatAssignmentOpenedStatusHandler implements WebAppStateActionHandler {
    public static final ChatAssignmentOpenedStatusHandler INSTANCE = new ChatAssignmentOpenedStatusHandler();

    private ChatAssignmentOpenedStatusHandler() {

    }

    @Override
    public String actionName() {
        return "chatAssignmentOpenedStatusAction";
    }

    @Override
    public boolean applyMutation(WhatsAppClient client, DecryptedMutation.Trusted mutation) {
        // Not handled
        return true;
    }
}
