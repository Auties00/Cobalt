package com.github.auties00.cobalt.wire.linked.message.addon;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Carries additional state associated with a poll creation message.
 *
 * <p>Because a poll can be invalidated by its creator, for example when the
 * original poll message is edited or retracted, existing votes may no longer
 * be meaningful. This metadata is attached to the poll so that clients know
 * to hide results and prevent new votes from being cast.
 */
@ProtobufMessage(name = "PollAdditionalMetadata")
public final class PollAdditionalMetadata {
    /**
     * Whether this poll has been invalidated and should no longer accept or
     * display votes.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean pollInvalidated;


    /**
     * Constructs a new poll metadata entry.
     *
     * @param pollInvalidated whether the poll has been invalidated, or {@code null} if unspecified
     */
    PollAdditionalMetadata(Boolean pollInvalidated) {
        this.pollInvalidated = pollInvalidated;
    }

    /**
     * Returns whether the poll has been invalidated.
     *
     * <p>When {@code true} clients should hide current results and prevent
     * additional votes from being cast. Returns {@code false} both when the
     * flag is explicitly unset and when it is {@code false} on the wire.
     *
     * @return {@code true} if the poll has been invalidated, {@code false} otherwise
     */
    public boolean pollInvalidated() {
        return pollInvalidated != null && pollInvalidated;
    }

    /**
     * Sets whether the poll has been invalidated.
     *
     * @param pollInvalidated the new invalidation flag, or {@code null} to clear
     */
    public void setPollInvalidated(Boolean pollInvalidated) {
        this.pollInvalidated = pollInvalidated;
    }
}
