package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Objects;
import java.util.Optional;

/**
 * On-state of a WhatsApp Business AI assistant's auto-reply per thread.
 *
 * <p>A WhatsApp Business operator can attach an AI assistant to their account
 * that automatically answers incoming messages; the assistant is toggled per
 * chat thread rather than globally. This two-state discriminator carries the
 * thread's requested on-state alongside the wire token the WhatsApp client
 * expects: the discriminator is rendered as the literal {@code "ENABLED"}
 * when the assistant is being switched on for the thread, or {@code "MUTED"}
 * when it is being switched off.
 */
@ProtobufEnum(name = "BusinessAiAutoReplyState")
public enum BusinessAiAutoReplyState {
    /**
     * Requests that the AI assistant be enabled for the thread, rendered on
     * the wire as {@code "ENABLED"}.
     */
    ENABLED(0, "ENABLED"),

    /**
     * Requests that the AI assistant be muted for the thread, rendered on the
     * wire as {@code "MUTED"}.
     */
    MUTED(1, "MUTED");

    /**
     * The protobuf wire-format index associated with this state.
     */
    final int index;

    /**
     * The wire-level string used to identify this state in the AI auto-reply
     * control payload.
     */
    final String wireValue;

    /**
     * Constructs a new {@code BusinessAiAutoReplyState} bound to the supplied
     * protobuf index and wire literal.
     *
     * @param index     the protobuf wire-format index
     * @param wireValue the wire-level literal; never {@code null}
     */
    BusinessAiAutoReplyState(@ProtobufEnumIndex int index, String wireValue) {
        this.index = index;
        this.wireValue = wireValue;
    }

    /**
     * Returns the protobuf wire-format index associated with this state.
     *
     * @return the protobuf wire-format index
     */
    public int index() {
        return index;
    }

    /**
     * Returns the wire-level literal used to identify this state in the AI
     * auto-reply control payload.
     *
     * @return the wire literal; never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a {@code BusinessAiAutoReplyState} from its wire literal.
     *
     * <p>The lookup is lenient: a {@code null} or unrecognised token
     * resolves to {@link Optional#empty()} rather than raising.
     *
     * @param wireValue the wire literal to resolve, possibly {@code null}
     * @return the matching state, or empty when the literal is {@code null}
     *         or unrecognised
     */
    public static Optional<BusinessAiAutoReplyState> ofWireValue(String wireValue) {
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
