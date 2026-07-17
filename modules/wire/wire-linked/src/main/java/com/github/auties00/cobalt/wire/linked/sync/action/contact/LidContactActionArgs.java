package com.github.auties00.cobalt.wire.linked.sync.action.contact;


import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments that locate a specific {@link LidContactAction} inside a
 * sync patch.
 *
 * <p>A LID contact entry is uniquely addressed by the contact's LID JID.
 * When building or reading a patch the sync engine translates these arguments
 * into the index tuple {@code ["lid_contact", contactLidJid]}.
 *
 * @param contactLidJid the LID-based JID of the contact this entry describes
 */
public record LidContactActionArgs(Jid contactLidJid) implements SyncActionArgs {
    /**
     * Returns the index components used by the sync engine to address this
     * LID contact entry.
     *
     * @return a single-element array containing the contact LID JID string
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{contactLidJid.toString()};
    }
}
