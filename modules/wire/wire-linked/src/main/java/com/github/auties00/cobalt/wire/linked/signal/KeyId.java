package com.github.auties00.cobalt.wire.linked.signal;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Wraps the raw identifier of a Signal protocol key.
 *
 * <p>The Signal protocol, which WhatsApp uses for end-to-end encryption,
 * assigns an identifier to every key it publishes or consumes (pre-keys,
 * signed pre-keys, sender keys, and so on). These identifiers are transmitted
 * inside protobuf stanzas whenever a peer needs to reference a specific key
 * from the other side's bundle. This class represents that identifier as an
 * opaque byte sequence so it can be embedded into other protobuf messages
 * without exposing any structure beyond the raw bytes.
 *
 * <p>The interpretation of the bytes depends on the context in which the
 * identifier is used and is not prescribed by this wrapper.
 */
@ProtobufMessage(name = "KeyId")
public final class KeyId {
    /**
     * The raw bytes identifying the key. Their meaning is determined by the
     * surrounding protocol message.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] id;

    /**
     * Constructs a new key identifier wrapping the given bytes.
     *
     * @param id the raw identifier bytes, or {@code null} if not set
     */
    KeyId(byte[] id) {
        this.id = id;
    }

    /**
     * Returns the raw identifier bytes.
     *
     * @return an {@link Optional} containing the identifier bytes, or
     *         {@link Optional#empty()} if not set
     */
    public Optional<byte[]> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Replaces the raw identifier bytes.
     *
     * @param id the new identifier bytes, or {@code null} to clear
     */
    public void setId(byte[] id) {
        this.id = id;
    }
}
