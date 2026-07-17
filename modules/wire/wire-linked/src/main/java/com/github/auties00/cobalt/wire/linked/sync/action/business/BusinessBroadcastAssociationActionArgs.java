package com.github.auties00.cobalt.wire.linked.sync.action.business;


import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments identifying a {@link BusinessBroadcastAssociationAction} inside a sync patch.
 *
 * <p>The resulting sync index is {@code ["broadcast_jid", broadcastListId, recipientJid]},
 * which uniquely locates the association record so that additions and removals of the same
 * recipient on different devices can be reconciled.
 *
 * @param broadcastListId the identifier of the business broadcast list whose membership is changing
 * @param recipientJid    the JID string of the recipient being added to or removed from the list
 */
public record BusinessBroadcastAssociationActionArgs(String broadcastListId, String recipientJid) implements SyncActionArgs {
    /**
     * Returns the index argument array that uniquely keys this association within the sync patch.
     *
     * @return a two-element array containing the broadcast list identifier and the recipient JID
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{broadcastListId, recipientJid};
    }
}
