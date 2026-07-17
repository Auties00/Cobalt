package com.github.auties00.cobalt.wire.linked.sync.action.business;


import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments identifying a {@link MarketingMessageAction} inside a sync patch.
 *
 * <p>The resulting sync index is {@code ["marketingMessage", marketingMessageId]}, so the
 * template identifier alone uniquely keys the record across linked devices.
 *
 * @param marketingMessageId the unique identifier of the marketing (premium) message template
 */
public record MarketingMessageActionArgs(String marketingMessageId) implements SyncActionArgs {
    /**
     * Returns the index argument array that uniquely keys this marketing template within the sync patch.
     *
     * @return a single-element array containing the marketing message identifier
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{marketingMessageId};
    }
}
