package com.github.auties00.cobalt.wire.cloud.content;

import com.github.auties00.cobalt.wire.core.message.ReactionContent;

import java.util.Optional;

/**
 * The Cloud transport's reaction content body.
 */
public final class CloudReactionContent implements ReactionContent {
    /**
     * The id of the reacted-to message, or {@code null} when unset.
     */
    private final String targetMessageId;

    /**
     * The reaction emoji, or {@code null} to remove a prior reaction.
     */
    private final String emoji;

    /**
     * Constructs a Cloud reaction body.
     *
     * @param targetMessageId the id of the reacted-to message, or {@code null} when unset
     * @param emoji           the reaction emoji, or {@code null} to remove a prior reaction
     */
    public CloudReactionContent(String targetMessageId, String emoji) {
        this.targetMessageId = targetMessageId;
        this.emoji = emoji;
    }

    @Override
    public Optional<String> targetMessageId() {
        return Optional.ofNullable(targetMessageId);
    }

    @Override
    public Optional<String> emoji() {
        return Optional.ofNullable(emoji);
    }
}
