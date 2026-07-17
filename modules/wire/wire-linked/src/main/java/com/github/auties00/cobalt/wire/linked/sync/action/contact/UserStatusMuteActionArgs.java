package com.github.auties00.cobalt.wire.linked.sync.action.contact;


import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments that locate a specific {@link UserStatusMuteAction} inside
 * a sync patch.
 *
 * <p>A status-mute entry is uniquely addressed by the JID of the contact
 * whose status feed is being muted or unmuted. When building or reading a
 * patch the sync engine translates these arguments into the index tuple
 * {@code ["userStatusMute", contactJid]}.
 *
 * @param contactJid the JID of the contact whose status feed is being muted
 *                   or unmuted
 */
public record UserStatusMuteActionArgs(Jid contactJid) implements SyncActionArgs {
    /**
     * Returns the index components used by the sync engine to address this
     * status-mute entry.
     *
     * @return a single-element array containing the contact JID string
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{contactJid.toString()};
    }
}
