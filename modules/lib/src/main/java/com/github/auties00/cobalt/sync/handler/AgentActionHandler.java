package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.business.AgentStateBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.device.AgentAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Reconciles the business-account device-agent roster with sync mutations from the server.
 *
 * <p>A device agent is another device acting on behalf of the business
 * account. {@link SyncdOperation#SET} mutations upsert an agent entry by id;
 * {@link SyncdOperation#REMOVE} mutations drop an entry by id. The reconciled
 * roster is read back through
 * {@link LinkedWhatsAppBusinessStore#findAgentState(String)}.
 *
 * @implNote
 * This implementation omits two side effects that WA Web performs after
 * each batch:
 * <ul>
 *   <li>The {@code WAWebUnattributedMessageCollection} reconciliation
 *       pass that retroactively assigns {@code agentId} to messages whose
 *       {@code deviceId} now resolves to a known agent. Cobalt does not
 *       maintain an unattributed-message collection.</li>
 *   <li>The {@code WAWebAgentModelUtils.getFormattedAgentName} call that
 *       expands the stored name into a localized {@code "{name} (Admin)"}
 *       string for the primary device. Cobalt stores the raw
 *       {@link AgentAction#name()} and defers display formatting to the
 *       UI layer.</li>
 * </ul>
 */
@WhatsAppWebModule(moduleName = "WAWebAgentSync")
public final class AgentActionHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link AgentActionHandler}.
     */
    private static final System.Logger LOGGER = Log.get(AgentActionHandler.class);

    /**
     * Constructs the singleton agent action handler.
     *
     * <p>The sync handler registry instantiates this type exactly once.
     *
     * @implNote
     * This implementation has nothing to initialize; the
     * {@code collectionName} field that WA Web sets in its constructor is
     * surfaced via {@link #collectionName()} instead.
     */
    @WhatsAppWebExport(moduleName = "WAWebAgentSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public AgentActionHandler() {

    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebAgentSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return AgentAction.ACTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebAgentSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return AgentAction.COLLECTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebAgentSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return AgentAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads the agent id from the JSON index {@code ["deviceAgent", agentId]}.
     * A {@link SyncdOperation#REMOVE} drops the entry keyed by that id; a
     * {@link SyncdOperation#SET} upserts the {@link AgentAction} value into the
     * store. Any other operation yields
     * {@link MutationApplicationResult#unsupported()}, and an empty or absent
     * agent id yields {@link SyncdIndexUtils#malformedActionIndex(String, String)}.
     *
     * @implNote
     * This implementation merges the agent into the store regardless of
     * the {@link AgentAction#isDeleted()} flag, matching the WA Web
     * behaviour where the deleted flag is preserved on the stored entry
     * so other devices converge on the same tombstone state. An unknown
     * {@link SyncdOperation} returns
     * {@link MutationApplicationResult#unsupported()} as a defensive
     * guard; WA Web's {@code applyMutations} has no equivalent path
     * because it switches on a string operation field.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebAgentSync", exports = {"applyMutations", "getValidatedContentSet", "getValidatedContentRemove"}, adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        var indexArray = JSON.parseArray(mutation.index());
        if (indexArray.size() <= 1) {
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }
        var agentId = indexArray.getString(1);
        if (agentId == null || agentId.isEmpty()) {
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }

        if (mutation.operation() == SyncdOperation.REMOVE) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "agent action: removing agent id={0}", agentId);
            client.store().businessStore().removeAgentState(agentId);
            return MutationApplicationResult.success();
        }

        if (mutation.operation() != SyncdOperation.SET) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "agent action: unsupported operation {0}", mutation.operation());
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof AgentAction action)) {
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        if (Log.DEBUG)
            LOGGER.log(Level.DEBUG, "agent action: upserting agent id={0}, deviceId={1}, deleted={2}",
                    agentId, action.deviceID().isPresent() ? action.deviceID().getAsInt() : null, action.isDeleted());
        client.store().businessStore().putAgentState(new AgentStateBuilder()
                .agentId(agentId)
                .name(action.name().orElse(null))
                .deviceId(action.deviceID().isPresent() ? action.deviceID().getAsInt() : null)
                .deleted(action.isDeleted())
                .build());
        return MutationApplicationResult.success();
    }
}
