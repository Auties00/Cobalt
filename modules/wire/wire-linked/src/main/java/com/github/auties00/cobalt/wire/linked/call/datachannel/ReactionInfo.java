package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * One emoji reaction broadcast over the call's AppData DataChannel.
 *
 * <p>Each participant-emitted reaction (thumbs-up, heart, etc.) is wrapped
 * in a {@link AppDataMessage} and shipped on the AppData stream. The
 * receiver displays the {@linkplain #reaction() emoji} as a transient
 * UI overlay; the {@linkplain #transactionId() transaction id} lets the
 * sender's UI suppress its own echoed reaction and lets receivers
 * deduplicate retransmissions.
 *
 * <p>This is the canonical wire shape replacing the empirical
 * {@code 12-byte RTP header + 12-byte zero wrapper + emoji} layout
 * formerly used by Cobalt's {@code CallInteractionEncoder}.
 */
@ProtobufMessage(name = "reactionInfo")
public final class ReactionInfo {
    /**
     * The sender-side transaction identifier of this reaction.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT64)
    final Long transactionId;

    /**
     * The reaction emoji string.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String reaction;

    /**
     * Constructs a new {@code ReactionInfo}.
     *
     * @param transactionId the sender's transaction id
     * @param reaction      the emoji string
     */
    ReactionInfo(Long transactionId, String reaction) {
        this.transactionId = transactionId;
        this.reaction = reaction;
    }

    /**
     * Returns the sender-side transaction identifier.
     *
     * @return an {@link OptionalLong} with the id, or empty
     */
    public OptionalLong transactionId() {
        return transactionId == null ? OptionalLong.empty() : OptionalLong.of(transactionId);
    }

    /**
     * Returns the emoji string.
     *
     * @return an {@link Optional} with the emoji, or empty
     */
    public Optional<String> reaction() {
        return Optional.ofNullable(reaction);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof ReactionInfo that
                && Objects.equals(this.transactionId, that.transactionId)
                && Objects.equals(this.reaction, that.reaction));
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, reaction);
    }

    @Override
    public String toString() {
        return "ReactionInfo[transactionId=" + transactionId
                + ", reaction=" + reaction + ']';
    }
}
