package com.github.auties00.cobalt.wire.linked.chat.group;

import com.github.auties00.cobalt.stanza.model.SizedInputStream;
import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Represents an intent expressed against a WhatsApp group's profile
 * picture on a {@link GroupMetadataEdit}.
 *
 * <p>Like {@link GroupDescription}, WhatsApp Web models a picture update
 * as a tri-state: "replace with new bytes", "remove the existing picture",
 * or "leave untouched". The third state is encoded by leaving the picture
 * field on the enclosing {@link GroupMetadataEdit} {@code null}. The
 * first two states are the variants of this sealed interface:
 * {@link Set} carries the new picture as a {@link SizedInputStream}, while
 * {@link Clear} requests removal of any existing picture.
 *
 * <p>On the wire, the protobuf encoding remains a plain {@code BYTES}
 * field: the bytes drained from a {@link Set}'s stream and an empty
 * {@code byte[0]} for a {@link Clear}. The custom deserializer round-trips
 * {@code null} to {@code null}, {@code byte[0]} to {@link Clear} and any
 * other value to {@link Set}. A {@link Set} whose stream is empty therefore
 * serializes as a removal; callers that mean to remove the picture should
 * use {@link Clear} explicitly.
 */
public sealed interface GroupPicture {
    /**
     * Replaces the existing group picture with the bytes drained from the
     * supplied {@link SizedInputStream}.
     *
     * <p>The stream is read once at dispatch (or fully on protobuf
     * serialization); the supplier behind it must yield a fresh, readable
     * stream of exactly its advertised length each time it is opened.
     *
     * @param stream the new picture payload (typically a 256x256 JPEG),
     *               never {@code null}
     */
    record Set(SizedInputStream stream) implements GroupPicture {
        /**
         * Compact canonical constructor that validates the stream.
         *
         * @throws NullPointerException if {@code stream} is {@code null}
         */
        public Set {
            Objects.requireNonNull(stream, "stream cannot be null; use Clear to remove the picture");
        }
    }

    /**
     * Requests removal of the existing group picture. The editor
     * translates this into a {@code w:profile:picture} {@code iq} of
     * type {@code set} with no body, matching WA Web's
     * {@code WAWebSendProfilePictureJob} removal path where the
     * {@code picture} argument is {@code null}.
     */
    record Clear() implements GroupPicture {
    }

    /**
     * Deserializes a wire-level {@code BYTES} value into the matching
     * {@code GroupPicture} variant.
     *
     * <p>A {@code null} wire value yields a {@code null}
     * {@code GroupPicture} (the enclosing edit treats this as "leave
     * untouched"). A zero-length wire value yields a {@link Clear}; any
     * non-empty value yields a {@link Set} whose stream replays the
     * decoded bytes.
     *
     * @param wire the protobuf BYTES value, possibly {@code null}
     * @return the matching variant, or {@code null} when {@code wire} is
     *         {@code null}
     */
    @ProtobufDeserializer
    static GroupPicture of(byte[] wire) {
        if (wire == null) {
            return null;
        }
        if (wire.length == 0) {
            return new Clear();
        }
        return new Set(SizedInputStream.of(wire));
    }

    /**
     * Serializes this variant into the wire-level {@code BYTES}
     * representation.
     *
     * <p>{@link Set} drains its {@link Set#stream() stream} into the raw
     * bytes. {@link Clear} is encoded as an empty {@code byte[0]}, which the
     * matching deserializer reconstitutes back into a {@link Clear}.
     *
     * @return the protobuf BYTES value, never {@code null}
     * @throws UncheckedIOException if reading a {@link Set}'s stream fails
     */
    @ProtobufSerializer
    default byte[] toProtoBytes() {
        return switch (this) {
            case Set set -> set.stream().toBytes();
            case Clear ignored -> new byte[0];
        };
    }
}
