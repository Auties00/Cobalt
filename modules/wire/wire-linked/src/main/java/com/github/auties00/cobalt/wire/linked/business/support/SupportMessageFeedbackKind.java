package com.github.auties00.cobalt.wire.linked.business.support;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Objects;
import java.util.Optional;

/**
 * Rating attached to a WhatsApp support-assistant message.
 *
 * <p>The WhatsApp support assistant is the help-centre chatbot a user can
 * interact with to report problems with their account. Each assistant reply
 * carries a thumbs-up and a thumbs-down control: tapping thumbs-up records
 * {@link #POSITIVE}, while tapping thumbs-down prompts the user to pick one
 * of the {@code NEGATIVE_*} reasons describing why the reply was unhelpful.
 * Each constant carries the wire token the WhatsApp client expects to see in
 * the submitted feedback payload.
 */
@ProtobufEnum(name = "SupportMessageFeedbackKind")
public enum SupportMessageFeedbackKind {
    /**
     * Positive feedback on the assistant reply, rendered on the wire as
     * {@code "POSITIVE"}.
     */
    POSITIVE(0, "POSITIVE"),

    /**
     * Negative feedback flagging the assistant reply as harmful, rendered on
     * the wire as {@code "NEGATIVE_HARMFUL"}.
     */
    NEGATIVE_HARMFUL(1, "NEGATIVE_HARMFUL"),

    /**
     * Negative feedback flagging the assistant reply as inaccurate, rendered
     * on the wire as {@code "NEGATIVE_INACCURATE"}.
     */
    NEGATIVE_INACCURATE(2, "NEGATIVE_INACCURATE"),

    /**
     * Negative feedback flagging the assistant reply as irrelevant to the
     * question, rendered on the wire as {@code "NEGATIVE_IRRELEVANT"}.
     */
    NEGATIVE_IRRELEVANT(3, "NEGATIVE_IRRELEVANT"),

    /**
     * Negative feedback for a reason not covered by the other categories,
     * rendered on the wire as {@code "NEGATIVE_OTHER"}.
     */
    NEGATIVE_OTHER(4, "NEGATIVE_OTHER"),

    /**
     * Negative feedback flagging the assistant reply as repetitive of an
     * earlier reply, rendered on the wire as {@code "NEGATIVE_REPETITIVE"}.
     */
    NEGATIVE_REPETITIVE(5, "NEGATIVE_REPETITIVE");

    /**
     * The protobuf wire-format index associated with this feedback kind.
     */
    final int index;

    /**
     * The wire-level string used to identify this feedback kind in the
     * support-message feedback payload.
     */
    final String wireValue;

    /**
     * Constructs a new {@code SupportMessageFeedbackKind} bound to the
     * supplied protobuf index and wire literal.
     *
     * @param index     the protobuf wire-format index
     * @param wireValue the wire-level literal; never {@code null}
     */
    SupportMessageFeedbackKind(@ProtobufEnumIndex int index, String wireValue) {
        this.index = index;
        this.wireValue = wireValue;
    }

    /**
     * Returns the protobuf wire-format index associated with this feedback
     * kind.
     *
     * @return the protobuf wire-format index
     */
    public int index() {
        return index;
    }

    /**
     * Returns the wire-level literal used to identify this feedback kind in
     * the support-message feedback payload.
     *
     * @return the wire literal; never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a {@code SupportMessageFeedbackKind} from its wire literal.
     *
     * <p>The lookup is lenient: a {@code null} or unrecognised token
     * resolves to {@link Optional#empty()} rather than raising.
     *
     * @param wireValue the wire literal to resolve, possibly {@code null}
     * @return the matching kind, or empty when the literal is {@code null}
     *         or unrecognised
     */
    public static Optional<SupportMessageFeedbackKind> ofWireValue(String wireValue) {
        if (wireValue == null) {
            return Optional.empty();
        }
        for (var value : values()) {
            if (Objects.equals(value.wireValue, wireValue)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
