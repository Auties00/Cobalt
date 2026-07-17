package com.github.auties00.cobalt.wire.linked.sync.action.chat;


import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments for a {@link PnForLidChatAction}.
 *
 * <p>The mapping is keyed by the LID chat JID, so the sync index is built
 * from the canonical action name followed by the chat JID.
 *
 * <p>The encoded index is {@code ["pnForLidChat", chatJid]}.
 *
 * @param chatJid the {@link Jid} of the LID chat whose phone-number
 *                association is being synced
 */
public record PnForLidChatActionArgs(Jid chatJid) implements SyncActionArgs {
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
