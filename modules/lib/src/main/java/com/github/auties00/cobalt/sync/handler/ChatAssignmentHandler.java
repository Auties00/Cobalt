package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.ChatAssignmentBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ChatAssignmentAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Routes a business chat to a specific agent in response to {@code agentChatAssignment} sync mutations.
 *
 * <p>This handler drives the Business inbox agent-routing surface where a
 * single chat is assigned to (or unassigned from) a named agent. When the
 * assignment changes on another device, the server replays it here and the
 * result becomes observable through
 * {@link LinkedWhatsAppBusinessStore#findChatAssignment(Jid)}.
 *
 * @implNote
 * This implementation collapses WA Web's per-batch
 * {@code bulkCreateOrMerge} / {@code bulkRemove} /
 * {@code processChatAssignments} pipeline into per-mutation read-modify-write
 * operations on the typed quintet keyed by chat JID. WA Web's
 * downstream side effects (system-message creation,
 * {@code triggerChatAssignmentNotification}, and
 * {@code checkOrphanChatAssignments}) are intentionally omitted
 * because Cobalt has no UI notification or system-message pipeline
 * here.
 */
@WhatsAppWebModule(moduleName = "WAWebChatAssignmentSync")
public final class ChatAssignmentHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link ChatAssignmentHandler}.
     */
    private static final System.Logger LOGGER = Log.get(ChatAssignmentHandler.class);

    /**
     * Constructs the singleton chat-assignment handler.
     *
     * <p>The sync handler registry instantiates this once during client
     * bootstrap.
     */
    @WhatsAppWebExport(moduleName = "WAWebChatAssignmentSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public ChatAssignmentHandler() {

    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebChatAssignmentSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return ChatAssignmentAction.ACTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebChatAssignmentSync", exports = "collectionName", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return ChatAssignmentAction.COLLECTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebChatAssignmentSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return ChatAssignmentAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Validates the JSON index {@code ["agentChatAssignment", chatJid]} and
     * the {@link ChatAssignmentAction#deviceAgentID()} payload, verifies the
     * agent exists in the store (when non-empty), then either drops the
     * existing assignment for the chat (when the agent id is the empty string,
     * mirroring WA Web's unassign sentinel) or upserts a new
     * {@link com.github.auties00.cobalt.wire.linked.chat.ChatAssignment}. The
     * previous {@code opened} flag is preserved across the upsert so
     * reassignment does not reset the agent-opened indicator. Returns
     * {@link MutationApplicationResult#unsupported()} for non-{@code SET}
     * operations and orphan results when the agent or chat is not in the
     * store.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebChatAssignmentSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        try {
            var indexArray = JSON.parseArray(mutation.index());
            if (indexArray.size() <= 1) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "chat assignment mutation malformed: index size={0}", indexArray.size());
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }
            var chatJidString = indexArray.getString(1);
            if (chatJidString == null || chatJidString.isEmpty()) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "chat assignment mutation malformed: missing chat jid");
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            if (mutation.operation() != SyncdOperation.SET) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "chat assignment mutation unsupported: operation={0}", mutation.operation());
                return MutationApplicationResult.unsupported();
            }

            if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof ChatAssignmentAction action)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "chat assignment mutation malformed: missing action value");
                return SyncdIndexUtils.malformedActionValue(collectionName().name());
            }

            var agentId = action.deviceAgentID().orElse("");
            if (!agentId.isEmpty() && client.store().businessStore().findAgentState(agentId).isEmpty()) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "chat assignment mutation orphaned: agent not found id={0}", agentId);
                return MutationApplicationResult.orphan(agentId, "Agent");
            }

            var chatJid = Jid.of(chatJidString);
            var chat = client.store().chatStore().findChatByJid(chatJid);
            if (chat.isEmpty()) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "chat assignment mutation orphaned: chat not found {0}", chatJid);
                return MutationApplicationResult.orphan(chatJidString, "Chat");
            }

            var resolvedChatJid = chat.get().toJid();
            if (agentId.isEmpty()) {
                client.store().businessStore().removeChatAssignment(resolvedChatJid);
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "chat assignment removed: chat={0}", resolvedChatJid);
            } else {
                var existing = client.store().businessStore().findChatAssignment(resolvedChatJid).orElse(null);
                client.store().businessStore().putChatAssignment(new ChatAssignmentBuilder()
                        .chatJid(resolvedChatJid)
                        .agentId(agentId)
                        .opened(existing != null && existing.opened())
                        .build());
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "chat assignment upserted: chat={0} agent={1}", resolvedChatJid, agentId);
            }
            return MutationApplicationResult.success();
        } catch (Exception e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "chat assignment mutation failed", e);
            return MutationApplicationResult.failed();
        }
    }

}
