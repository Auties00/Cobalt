package com.github.auties00.cobalt.wire.core.message;

import java.util.Optional;

/**
 * Transport-agnostic contract for a reaction message body.
 *
 * <p>A reaction targets an earlier message by id and carries an emoji (empty to remove a prior
 * reaction). The Linked transport additionally carries a grouping key and sender timestamp that the
 * Cloud wire never delivers.
 */
public interface ReactionContent extends MessageContent {
    /**
     * Returns the id of the message this reaction targets.
     *
     * @return an {@link Optional} holding the target message id, or empty when unset
     */
    Optional<String> targetMessageId();

    /**
     * Returns the reaction emoji, or empty when the reaction removes a prior one.
     *
     * @return an {@link Optional} holding the emoji, or empty when none
     */
    Optional<String> emoji();
}
