package com.github.auties00.cobalt.wire.linked.sync.action.business;


import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments identifying a {@link CtwaPerCustomerDataSharingAction} inside a sync patch.
 *
 * <p>The resulting sync index is {@code ["ctwaPerCustomerDataSharing", accountLidJid]}, which
 * binds the opt-in record to the specific customer account being synced.
 *
 * @param accountLidJid the account LID JID identifying the customer whose data-sharing preference is being synced
 */
public record CtwaPerCustomerDataSharingActionArgs(Jid accountLidJid) implements SyncActionArgs {
    /**
     * Returns the index argument array that uniquely keys this preference within the sync patch.
     *
     * @return a single-element array containing the stringified account LID JID
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{accountLidJid.toString()};
    }
}
