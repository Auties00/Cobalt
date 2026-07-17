package com.github.auties00.cobalt.wire.linked.preference;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Optional;

/**
 * Mutation verb applied to the local user's Public Service Announcement
 * (PSA) chat block list.
 *
 * <p>Public Service Announcements are official broadcast channels operated
 * by health authorities and similar institutions. Users can choose to
 * mute incoming PSA chats — the action verb is communicated to the relay
 * via the {@code action} attribute of the {@code <blocking>} payload of the
 * outbound PSA-chat-block-set stanza.
 */
@ProtobufEnum
public enum PsaChatBlockAction {
    /**
     * Mutes the PSA broadcast channel so subsequent announcements no
     * longer surface as conversations. Wire value {@code "block"}.
     */
    BLOCK(0, "block"),

    /**
     * Re-enables the PSA broadcast channel so future announcements again
     * surface as conversations. Wire value {@code "unblock"}.
     */
    UNBLOCK(1, "unblock");

    /**
     * The protobuf wire-format index associated with this verb.
     */
    final int index;

    /**
     * The literal wire-level string carried by the {@code action}
     * attribute of the {@code <blocking>} child.
     */
    final String wireValue;

    /**
     * Constructs a new {@code PsaChatBlockAction} with the supplied
     * protobuf index and wire string.
     *
     * @param index     the protobuf wire-format index
     * @param wireValue the wire-level literal; never {@code null}
     */
    PsaChatBlockAction(@ProtobufEnumIndex int index, String wireValue) {
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
     * Resolves a {@code PsaChatBlockAction} from the wire literal.
     *
     * @param wireValue the wire literal, possibly {@code null}
     * @return an {@link Optional} containing the matching constant, or
     *         empty when the literal is {@code null} or unrecognised
     */
    public static Optional<PsaChatBlockAction> ofWire(String wireValue) {
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
