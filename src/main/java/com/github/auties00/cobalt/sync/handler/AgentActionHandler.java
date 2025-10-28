package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSONArray;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.model.sync.RecordSync;
import com.github.auties00.cobalt.store.WhatsappStore;

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
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var action = mutation.value().agentAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing agentAction"));

        var agentId = JSONArray.parseArray(mutation.index())
                .getString(1);

        if (mutation.operation() == RecordSync.Operation.SET) {
            var agent = store.findAgentById(agentId)
                    .orElseGet(() -> store.createAgent(agentId));
            action.name().ifPresent(agent::setName);
            action.deviceId().ifPresent(agent::setDeviceId);
            action.isDeleted().ifPresent(agent::setDeleted);
        } else {
            store.findAgentById(agentId)
                    .ifPresent(store::deleteAgent);
        }

        return true;
    }
}
