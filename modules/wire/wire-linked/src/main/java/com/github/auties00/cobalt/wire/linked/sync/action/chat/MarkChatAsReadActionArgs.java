package com.github.auties00.cobalt.wire.linked.sync.action.chat;


import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments for a {@link MarkChatAsReadAction}.
 *
 * <p>Read status is keyed by the target chat, so the sync index is built
 * from the canonical action name followed by the chat JID.
 *
 * <p>The encoded index is {@code ["markChatAsRead", chatJid]}.
 *
 * @param chatJid the {@link Jid} of the chat being marked as read or unread
 */
public record MarkChatAsReadActionArgs(Jid chatJid) implements SyncActionArgs {
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
