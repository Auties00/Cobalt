package com.github.auties00.cobalt.wire.linked.sync.action.business;


import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments identifying a {@link BusinessBroadcastListAction} inside a sync patch.
 *
 * <p>The resulting sync index is {@code ["business_broadcast_list", broadcastListId]}, so
 * the broadcast list identifier alone uniquely keys the record across linked devices.
 *
 * @param broadcastListId the unique identifier of the business broadcast list
 */
public record BusinessBroadcastListActionArgs(String broadcastListId) implements SyncActionArgs {
    /**
     * Returns the index argument array that uniquely keys this broadcast list within the sync patch.
     *
     * @return a single-element array containing the broadcast list identifier
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{broadcastListId};
    }
}
