package com.github.auties00.cobalt.wire.linked.sync.action.contact;


import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments that locate a specific {@link ContactAction} inside a sync
 * patch.
 *
 * <p>Each contact entry in the app state is uniquely addressed by the JID of
 * the contact it refers to. When building or reading a patch the sync engine
 * translates these arguments into the index tuple
 * {@code ["contact", contactJid]}.
 *
 * @param contactJid the JID of the contact this entry describes
 */
public record ContactActionArgs(Jid contactJid) implements SyncActionArgs {
    /**
     * Returns the index components used by the sync engine to address this
     * contact entry.
     *
     * @return a single-element array containing the contact JID string
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{contactJid.toString()};
    }
}
