package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ChatAssignmentOpenedStatusAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ChatAssignmentOpenedStatusActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing app-state mutations that record whether a Business agent has opened a chat.
 *
 * <p>When an agent opens (or closes) an assigned chat, the resulting mutation
 * is pushed via
 * {@link com.github.auties00.cobalt.sync.WebAppStateService#pushPatches} so the
 * primary device and other agent devices see the same open state. This factory
 * builds the outgoing mutation; the inbound counterpart is
 * {@link com.github.auties00.cobalt.sync.handler.ChatAssignmentOpenedStatusHandler}.
 *
 * @implNote
 * This implementation accepts only a single triple per call, unlike WA Web's
 * batched form; Cobalt callers loop at their own level when several open-state
 * transitions must be flushed together.
 */
public final class ChatAssignmentOpenedStatusMutationFactory {
    /**
     * The logger for {@link ChatAssignmentOpenedStatusMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(ChatAssignmentOpenedStatusMutationFactory.class);

    /**
     * Creates a stateless factory with no collaborators.
     *
     * <p>A single instance may be shared across the lifetime of the client.
     */
    public ChatAssignmentOpenedStatusMutationFactory() {

    }

    /**
     * Returns a SET mutation that records whether the given agent has the chat open.
     *
     * <p>The mutation index follows
     * {@snippet :
     *     ["agentChatAssignmentOpenedStatus", chatJid.toString(), agentId]
     * }
     * and the {@link ChatAssignmentOpenedStatusAction} sub-message carries the
     * {@code chatOpened} flag. The composite index allows multiple agents to
     * record open state for the same chat independently. The receive-side
     * handler orphans the mutation if the underlying chat-assignment row does
     * not yet exist in the agent collection.
     *
     * @implNote
     * This implementation pins the action version through
     * {@link ChatAssignmentOpenedStatusAction#ACTION_VERSION}.
     *
     * @param chatJid    the chat {@link Jid} whose open state is being recorded
     * @param agentId    the identifier of the agent whose state changed
     * @param chatOpened {@code true} when the agent has the chat open, {@code false} when they have left it
     * @param timestamp  the mutation timestamp
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebChatAssignmentOpenedStatusSync", exports = "createChatOpenedMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation createChatOpenedMutation(
            Jid chatJid,
            String agentId,
            boolean chatOpened,
            Instant timestamp
    ) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building chat assignment opened status mutation chat={0} agent={1} opened={2}", chatJid, agentId, chatOpened);
        var action = new ChatAssignmentOpenedStatusActionBuilder()
                .chatOpened(chatOpened)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .chatAssignmentOpenedStatus(action)
                .build();
        var index = JSON.toJSONString(List.of(ChatAssignmentOpenedStatusAction.ACTION_NAME, chatJid.toString(), agentId));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                ChatAssignmentOpenedStatusAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
