package com.github.auties00.cobalt.wire.linked.sync.action.chat;


import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments for a {@link QuickReplyAction}.
 *
 * <p>Quick replies are identified by a stable, account-scoped identifier
 * that allows creates, updates and deletes of the same template to share
 * the same sync index.
 *
 * <p>The encoded index is {@code ["quick_reply", quickReplyId]}.
 *
 * @param quickReplyId the unique identifier of the quick reply template
 */
public record QuickReplyActionArgs(String quickReplyId) implements SyncActionArgs {
    /**
     * Converts this record into the tail portion of the sync index array.
     *
     * @return a single-element array containing the quick reply identifier
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{quickReplyId};
    }
}
