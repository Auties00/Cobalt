package com.github.auties00.cobalt.wire.linked.sync.action.chat;


import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments for an {@link ArchiveChatAction}.
 *
 * <p>A sync action's index is used by the app state protocol as the mutation
 * key and determines which entity an action mutates. For archive actions the
 * key identifies a specific chat, so the index is built from the canonical
 * action name followed by the chat JID.
 *
 * <p>The encoded index is {@code ["archive", chatJid]}.
 *
 * @param chatJid the {@link Jid} of the chat being archived or unarchived
 */
public record ArchiveChatActionArgs(Jid chatJid) implements SyncActionArgs {
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
