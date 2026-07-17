package com.github.auties00.cobalt.wire.linked.preference;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Optional;

/**
 * Mutation verb applied to the per-contact message-feedback preference of
 * the local business account.
 *
 * <p>The message-feedback subsystem lets a business account manage how it
 * receives traffic from a specific contact: silently muting the
 * conversation, re-enabling it, or escalating it as abusive content. The
 * verb is communicated to the relay through the {@code action} attribute
 * of the {@code <user_feedback>} payload of the outbound
 * {@code msg_feedback} stanza.
 */
@ProtobufEnum
public enum MessageFeedbackAction {
    /**
     * Mutes the conversation with the target contact, suppressing further
     * inbound notifications without escalating to a report. Wire value
     * {@code "block"}.
     */
    BLOCK(0, "block"),

    /**
     * Re-enables the conversation with a previously-blocked contact. Wire
     * value {@code "unblock"}.
     */
    UNBLOCK(1, "unblock"),

    /**
     * Escalates the conversation to the relay's abuse-handling pipeline by
     * filing a report against the target contact. Wire value
     * {@code "report"}.
     */
    REPORT(2, "report");

    /**
     * The protobuf wire-format index associated with this verb.
     */
    final int index;

    /**
     * The literal wire-level string carried by the {@code action}
     * attribute of the {@code <user_feedback>} child.
     */
    final String wireValue;

    /**
     * Constructs a new {@code MessageFeedbackAction} with the supplied
     * protobuf index and wire string.
     *
     * @param index     the protobuf wire-format index
     * @param wireValue the wire-level literal; never {@code null}
     */
    MessageFeedbackAction(@ProtobufEnumIndex int index, String wireValue) {
        this.index = index;
        this.wireValue = wireValue;
    }

    /**
     * Returns the protobuf wire-format index associated with this verb.
     *
     * @return the protobuf wire-format index
     */
    public int index() {
        return index;
    }

    /**
     * Returns the wire-level string carried by the {@code action}
     * attribute.
     *
     * @return the wire literal; never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a {@code MessageFeedbackAction} from the wire literal.
     *
     * @param wireValue the wire literal, possibly {@code null}
     * @return an {@link Optional} containing the matching constant, or
     *         empty when the literal is {@code null} or unrecognised
     */
    public static Optional<MessageFeedbackAction> ofWire(String wireValue) {
        if (wireValue == null) {
            return Optional.empty();
        }
        for (var value : values()) {
            if (value.wireValue.equals(wireValue)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
