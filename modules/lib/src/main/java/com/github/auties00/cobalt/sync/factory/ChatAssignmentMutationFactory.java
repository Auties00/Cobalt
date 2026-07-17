package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ChatAssignmentAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ChatAssignmentActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing app-state mutations that assign a chat to (or unassign it from) a Business agent.
 *
 * <p>SMB admins assign chats to specific agents through the Web UI, and the
 * resulting mutations are pushed so every agent device sees the same
 * assignment state. This factory builds the outgoing mutation; the inbound
 * counterpart is
 * {@link com.github.auties00.cobalt.sync.handler.ChatAssignmentHandler}.
 *
 * @implNote
 * This implementation does not perform WA Web's batch-style fan-out over a
 * list of {@code {agentId, chatId}} pairs; Cobalt callers loop at their own
 * level and invoke {@link #createChatAssignmentMutation} once per pair.
 */
public final class ChatAssignmentMutationFactory {
    /**
     * The logger for {@link ChatAssignmentMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(ChatAssignmentMutationFactory.class);

    /**
     * Creates a stateless factory with no collaborators.
     *
     * <p>A single instance may be shared across the lifetime of the client.
     */
    public ChatAssignmentMutationFactory() {

    }

    /**
     * Returns a SET mutation that assigns the given chat to the given agent.
     *
     * <p>Pass an empty {@code agentId} to unassign the chat; the receive-side
     * handler treats an empty {@code deviceAgentID} as a removal. The mutation
     * index follows
     * {@snippet :
     *     ["agentChatAssignment", chatJid.toString()]
     * }
     * and the {@link ChatAssignmentAction} sub-message carries
     * {@code deviceAgentID}.
     *
     * @implNote
     * This implementation pins the action version through
     * {@link ChatAssignmentAction#ACTION_VERSION}.
     *
     * @param chatJid   the chat {@link Jid} being assigned
     * @param agentId   the target agent identifier, or empty string to unassign
     * @param timestamp the mutation timestamp
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebChatAssignmentSync", exports = "createChatAssignmentMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation createChatAssignmentMutation(
            Jid chatJid,
            String agentId,
            Instant timestamp
    ) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building chat assignment mutation chat={0} agent={1}", chatJid, agentId);
        var action = new ChatAssignmentActionBuilder()
                .deviceAgentID(agentId)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .chatAssignment(action)
                .build();
        var index = JSON.toJSONString(List.of(ChatAssignmentAction.ACTION_NAME, chatJid.toString()));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                ChatAssignmentAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
