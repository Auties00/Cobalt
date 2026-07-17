package com.github.auties00.cobalt.wire.linked.sync.action.contact;


import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments that locate a specific {@link PinAction} inside a sync
 * patch.
 *
 * <p>A pinned chat is uniquely addressed by the JID of the chat it refers
 * to. When building or reading a patch the sync engine translates these
 * arguments into the index tuple {@code ["pin_v1", chatJid]}.
 *
 * @param chatJid the JID of the chat that is being pinned or unpinned
 */
public record PinActionArgs(Jid chatJid) implements SyncActionArgs {
    /**
     * Returns the index components used by the sync engine to address this
     * pinned chat entry.
     *
     * @return a single-element array containing the chat JID string
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{chatJid.toString()};
    }
}
