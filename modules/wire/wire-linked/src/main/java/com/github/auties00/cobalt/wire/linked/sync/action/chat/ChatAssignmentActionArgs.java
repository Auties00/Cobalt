package com.github.auties00.cobalt.wire.linked.sync.action.chat;


import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments for a {@link ChatAssignmentAction}.
 *
 * <p>The assignment is keyed by the chat being assigned, so the app state
 * mutation index is built from the canonical action name followed by the
 * chat's JID.
 *
 * <p>The encoded index is {@code ["agentChatAssignment", chatJid]}.
 *
 * @param chatJid the {@link Jid} of the chat being assigned to an agent
 */
public record ChatAssignmentActionArgs(Jid chatJid) implements SyncActionArgs {
    /**
     * Converts this record into the tail portion of the sync index array.
     *
     * @return a single-element array containing the chat JID as a string
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{chatJid.toString()};
    }
}
