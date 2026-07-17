package com.github.auties00.cobalt.wire.linked.message.poll;

import com.github.auties00.cobalt.wire.linked.message.Message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Carries an encrypted vote payload exchanged as part of a poll update.
 *
 * <p>A poll vote is encrypted end-to-end so that only the poll creator can
 * tally the results. This container holds the ciphertext and the
 * initialisation vector produced during encryption. Decryption uses the
 * symmetric key published in the originating {@link PollCreationMessage}.
 */
@ProtobufMessage(name = "Message.PollEncValue")
public final class PollEncValue implements Message {
    /**
     * The ciphertext of the encrypted vote payload.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] encPayload;

    /**
     * The initialisation vector used when encrypting the payload.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] encIv;


    /**
     * Creates a new encrypted vote container with the provided ciphertext and IV.
     *
     * @param encPayload the encrypted payload
     * @param encIv      the initialisation vector used for encryption
     */
    PollEncValue(byte[] encPayload, byte[] encIv) {
        this.encPayload = encPayload;
        this.encIv = encIv;
    }

    /**
     * Returns the ciphertext of the encrypted vote payload.
     *
     * @return an {@link Optional} containing the payload bytes, or empty when absent
     */
    public Optional<byte[]> encPayload() {
        return Optional.ofNullable(encPayload);
    }

    /**
     * Returns the initialisation vector used when encrypting the payload.
     *
     * @return an {@link Optional} containing the IV bytes, or empty when absent
     */
    public Optional<byte[]> encIv() {
        return Optional.ofNullable(encIv);
    }

    /**
     * Sets the ciphertext of the encrypted vote payload.
     *
     * @param encPayload the encrypted payload
     */
    public void setEncPayload(byte[] encPayload) {
        this.encPayload = encPayload;
    }

    /**
     * Sets the initialisation vector used when encrypting the payload.
     *
     * @param encIv the initialisation vector
     */
    public void setEncIv(byte[] encIv) {
        this.encIv = encIv;
    }
}
