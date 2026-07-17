package com.github.auties00.cobalt.wire.linked.sync.action.business;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments identifying a {@link CustomerDataAction} inside a sync patch.
 *
 * <p>The resulting sync index is {@code ["customer_data", chatJid]}, which binds the CRM
 * record to the specific chat it describes.
 *
 * @param chatJid the JID of the chat associated with the customer data being synced
 */
public record CustomerDataActionArgs(String chatJid) implements SyncActionArgs {
    /**
     * Returns the index argument array that uniquely keys this customer record within the sync patch.
     *
     * @return a single-element array containing the chat JID
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{chatJid};
    }
}
