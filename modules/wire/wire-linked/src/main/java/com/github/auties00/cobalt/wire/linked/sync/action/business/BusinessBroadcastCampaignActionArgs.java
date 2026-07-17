package com.github.auties00.cobalt.wire.linked.sync.action.business;


import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments identifying a {@link BusinessBroadcastCampaignAction} inside a sync patch.
 *
 * <p>The resulting sync index is {@code ["business_broadcast_campaign", campaignId]}, so
 * the campaign identifier alone uniquely keys the record across every linked device.
 *
 * @param campaignId the unique identifier of the business broadcast campaign
 */
public record BusinessBroadcastCampaignActionArgs(String campaignId) implements SyncActionArgs {
    /**
     * Returns the index argument array that uniquely keys this campaign within the sync patch.
     *
     * @return a single-element array containing the campaign identifier
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{campaignId};
    }
}
