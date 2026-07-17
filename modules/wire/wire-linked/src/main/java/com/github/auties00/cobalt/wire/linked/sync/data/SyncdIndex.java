package com.github.auties00.cobalt.wire.linked.sync.data;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Opaque MAC used as the lookup key for a record in an app-state collection.
 *
 * <p>Each record in a collection is keyed by a stable byte string derived from
 * the plaintext action data and the active sync key. Two devices of the same
 * account therefore derive the same index for the same logical action, which
 * is what allows {@code SET} and {@code REMOVE} mutations to target a specific
 * entry even though the plaintext is never sent in the clear.
 */
@ProtobufMessage(name = "SyncdIndex")
public final class SyncdIndex {
    /**
     * Opaque index bytes identifying a record inside its collection.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] blob;


    /**
     * Constructs a new index from the given MAC bytes.
     *
     * @param blob the index bytes, or {@code null} if absent
     */
    SyncdIndex(byte[] blob) {
        this.blob = blob;
    }

    /**
     * Returns the opaque index bytes.
     *
     * @return the index bytes, or empty if absent
     */
    public Optional<byte[]> blob() {
        return Optional.ofNullable(blob);
    }

    /**
     * Sets the opaque index bytes.
     *
     * @param blob the index bytes
     */
    public void setBlob(byte[] blob) {
        this.blob = blob;
    }
}
