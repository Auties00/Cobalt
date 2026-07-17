package com.github.auties00.cobalt.wire.linked.sync.action.chat;


import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments for a {@link ChatAssignmentOpenedStatusAction}.
 *
 * <p>Each opened-status record is scoped to a specific chat and a specific
 * agent: the same chat can have different opened flags for different agents
 * that received it. The index therefore includes both the chat JID and the
 * agent identifier.
 *
 * <p>The encoded index is
 * {@code ["agentChatAssignmentOpenedStatus", chatJid, agentId]}.
 *
 * @param chatJid the {@link Jid} of the chat whose opened status is tracked
 * @param agentId the device-agent identifier of the agent that opened the chat
 */
public record ChatAssignmentOpenedStatusActionArgs(Jid chatJid, String agentId) implements SyncActionArgs {
    /**
     * Converts this record into the tail portion of the sync index array.
     *
     * @return a two-element array containing the chat JID string and the
     *         agent identifier in that order
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{chatJid.toString(), agentId};
    }
}
