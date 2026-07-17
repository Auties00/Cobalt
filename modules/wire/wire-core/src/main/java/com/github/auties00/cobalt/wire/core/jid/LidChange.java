package com.github.auties00.cobalt.wire.core.jid;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Linked-identity (LID) rotation pair reported by the relay.
 *
 * <p>WhatsApp's non-phone account identifier — the "linked identity"
 * (LID) — is rotated periodically during the LID migration rollout.
 * When the relay rotates an account's LID it tracks the old-to-new
 * mapping in a separate notification table; clients reconcile their
 * local stores by issuing the corresponding query and applying the
 * mapping carried by this record so chat references can be updated
 * without losing history.
 *
 * <p>The {@link #oldValue()} carries the previous LID literal and
 * {@link #newValue()} carries the post-rotation literal. Both are
 * decimal-only LID literals (no {@code @lid} suffix) — callers that
 * need a {@link Jid} can wrap them themselves.
 */
@ProtobufMessage
public final class LidChange {
    /**
     * The pre-rotation LID literal. {@code null} when the relay
     * omitted the field.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String oldValue;

    /**
     * The post-rotation LID literal. {@code null} when the relay
     * omitted the field.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String newValue;

    /**
     * Constructs a new {@code LidChange} carrying the rotation pair.
     *
     * @param oldValue the pre-rotation LID literal, or {@code null}
     *                 when omitted
     * @param newValue the post-rotation LID literal, or {@code null}
     *                 when omitted
     */
    LidChange(String oldValue, String newValue) {
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /**
     * Returns the pre-rotation LID literal.
     *
     * @return an {@link Optional} carrying the literal, or empty
     *         when the relay omitted the field
     */
    public Optional<String> oldValue() {
        return Optional.ofNullable(oldValue);
    }

    /**
     * Returns the post-rotation LID literal.
     *
     * @return an {@link Optional} carrying the literal, or empty
     *         when the relay omitted the field
     */
    public Optional<String> newValue() {
        return Optional.ofNullable(newValue);
    }

    /**
     * Sets the pre-rotation LID literal.
     *
     * @param oldValue the new value, or {@code null}
     */
    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    /**
     * Sets the post-rotation LID literal.
     *
     * @param newValue the new value, or {@code null}
     */
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
}
