package com.github.auties00.cobalt.wire.linked.sync.action.business;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments identifying a {@link BusinessBroadcastInsightsAction} inside a sync patch.
 *
 * <p>The resulting sync index is {@code ["business_broadcast_insights_sync", campaignId]},
 * which binds the insights record to the campaign it reports statistics for.
 *
 * @param campaignId the unique identifier of the business broadcast campaign whose insights are being synced
 */
public record BusinessBroadcastInsightsActionArgs(String campaignId) implements SyncActionArgs {
    /**
     * Returns the index argument array that uniquely keys this insights record within the sync patch.
     *
     * @return a single-element array containing the campaign identifier
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{campaignId};
    }
}
