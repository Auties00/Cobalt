package com.github.auties00.cobalt.wire.linked.sync.action.business;


import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments identifying a {@link MarketingMessageBroadcastAction} inside a sync patch.
 *
 * <p>The resulting sync index is
 * {@code ["marketingMessageBroadcast", marketingMessageId, broadcastMessageId]}, pairing the
 * source template with the specific broadcast send whose metrics are being synced.
 *
 * @param marketingMessageId the unique identifier of the marketing (premium) message template
 * @param broadcastMessageId the identifier of the individual broadcast message sent from the template
 */
public record MarketingMessageBroadcastActionArgs(String marketingMessageId, String broadcastMessageId) implements SyncActionArgs {
    /**
     * Returns the index argument array that uniquely keys this broadcast record within the sync patch.
     *
     * @return a two-element array containing the marketing message identifier and the broadcast message identifier
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{marketingMessageId, broadcastMessageId};
    }
}
