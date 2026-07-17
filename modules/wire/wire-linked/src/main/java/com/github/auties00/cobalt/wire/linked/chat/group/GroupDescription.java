package com.github.auties00.cobalt.wire.linked.chat.group;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

import java.util.Objects;

/**
 * Represents an intent expressed against a WhatsApp group's description
 * field on a {@link GroupMetadataEdit}.
 *
 * <p>WhatsApp Web models a description update as a tri-state: "rewrite to
 * a new body", "clear the existing body", or "leave untouched". The third
 * state is encoded by leaving the description field on the enclosing
 * {@link GroupMetadataEdit} {@code null}. The first two states are the
 * variants of this sealed interface: {@link Set} carries the new body
 * verbatim, while {@link Clear} requests removal of any existing body.
 *
 * <p>An empty {@link String} body is rejected by {@link Set} so that it
 * cannot collide with the {@link Clear} encoding. Callers wanting to wipe
 * the description must use {@link Clear} explicitly.
 *
 * <p>On the wire, the protobuf encoding remains a plain {@code STRING}:
 * the body of a {@link Set} for a non-empty description and the empty
 * string {@code ""} for a {@link Clear}. The custom deserializer
 * round-trips {@code null} to {@code null}, {@code ""} to {@link Clear}
 * and any other value to {@link Set}.
 */
public sealed interface GroupDescription {
    /**
     * Replaces the existing group description with the supplied body.
     *
     * <p>The body must be non-{@code null} and non-empty; the empty
     * string is reserved as the {@link Clear} sentinel on the wire.
     *
     * @param body the new description text, never {@code null} or empty
     */
    record Set(String body) implements GroupDescription {
        /**
         * Compact canonical constructor that validates the body.
         *
         * @throws NullPointerException     if {@code body} is
         *                                  {@code null}
         * @throws IllegalArgumentException if {@code body} is the empty
         *                                  string
         */
        public Set {
            Objects.requireNonNull(body, "body cannot be null; use Clear to remove the description");
            if (body.isEmpty()) {
                throw new IllegalArgumentException("body cannot be empty; use Clear to remove the description");
            }
        }
    }

    /**
     * Requests removal of the existing group description. The editor
     * translates this into a {@code <description delete="true"/>} body
     * inside a {@code w:g2} {@code iq} of type {@code set}.
     */
    record Clear() implements GroupDescription {
    }

    /**
     * Deserializes a wire-level {@code STRING} into the matching
     * {@code GroupDescription} variant.
     *
     * <p>A {@code null} wire value yields a {@code null}
     * {@code GroupDescription} (the enclosing edit treats this as "leave
     * untouched"). An empty wire value yields a {@link Clear}; any
     * non-empty value yields a {@link Set}.
     *
     * @param wire the protobuf STRING value, possibly {@code null}
     * @return the matching variant, or {@code null} when {@code wire} is
     *         {@code null}
     */
    @ProtobufDeserializer
    static GroupDescription of(String wire) {
        if (wire == null) {
            return null;
        }
        if (wire.isEmpty()) {
            return new Clear();
        }
        return new Set(wire);
    }

    /**
     * Serializes this variant into the wire-level {@code STRING}
     * representation.
     *
     * <p>{@link Set} round-trips its {@link Set#body() body} verbatim.
     * {@link Clear} is encoded as the empty string {@code ""}, which the
     * matching deserializer reconstitutes back into a {@link Clear}.
     *
     * @return the protobuf STRING value, never {@code null}
     */
    @ProtobufSerializer
    default String toProtoString() {
        return switch (this) {
            case Set set -> set.body();
            case Clear ignored -> "";
        };
    }
}
