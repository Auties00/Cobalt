package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

/**
 * Handles agent actions.
 *
 * <p>This handler processes mutations that manage business account agents
 * (customer service representatives).
 *
 * <p>Index format: ["agentAction", "agentId"]
 */
public final class AgentActionHandler implements WebAppStateActionHandler {
    public static final AgentActionHandler INSTANCE = new AgentActionHandler();

    private AgentActionHandler() {

    }

    @Override
    public String actionName() {
        return "agentAction";
    }

    @Override
    public boolean applyMutation(WhatsAppClient client, DecryptedMutation.Trusted mutation) {
        // Not handled
        return true;
    }
}
