package com.github.auties00.cobalt.wire.linked.sync.action.contact;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments that locate a specific {@link OutContactAction} inside a
 * sync patch.
 *
 * <p>Each outgoing contact entry is uniquely addressed by the JID string of
 * the user the contact card was shared with. When building or reading a patch
 * the sync engine translates these arguments into the index tuple
 * {@code ["out_contact", userJid]}.
 *
 * @param userJid the JID string of the user the contact card was shared with
 */
public record OutContactActionArgs(String userJid) implements SyncActionArgs {
    /**
     * Returns the index components used by the sync engine to address this
     * outgoing contact entry.
     *
     * @return a single-element array containing the user JID string
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{userJid};
    }
}
