package com.github.auties00.cobalt.wire.linked.sync.data;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Encrypted payload stored at an index inside an app-state collection.
 *
 * <p>The bytes are the ciphertext of a sync action (encoded as a
 * {@code SyncActionData}) followed by its MAC, all produced with the sync key
 * identified by the enclosing record's {@code keyId}. Decryption yields the
 * logical action that the mutation conveys.
 */
@ProtobufMessage(name = "SyncdValue")
public final class SyncdValue {
    /**
     * Raw ciphertext of the encoded action plus trailing MAC.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] blob;


    /**
     * Constructs a new value wrapper.
     *
     * @param blob the ciphertext bytes, or {@code null} if absent
     */
    SyncdValue(byte[] blob) {
        this.blob = blob;
    }

    /**
     * Returns the raw ciphertext bytes.
     *
     * @return the ciphertext bytes, or empty if absent
     */
    public Optional<byte[]> blob() {
        return Optional.ofNullable(blob);
    }

    /**
     * Sets the raw ciphertext bytes.
     *
     * @param blob the ciphertext bytes
     */
    public void setBlob(byte[] blob) {
        this.blob = blob;
    }
}
