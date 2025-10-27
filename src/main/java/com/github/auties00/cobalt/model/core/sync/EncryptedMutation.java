package com.github.auties00.cobalt.model.core.sync;

import com.github.auties00.cobalt.model.proto.sync.RecordSync.Operation;

/**
 * Represents an encrypted mutation ready to be sent to the server.
 *
 * @param indexMac       the index MAC identifying the mutation (32 bytes)
 * @param encryptedValue the encrypted value (IV + ciphertext + MAC)
 * @param keyId          the ID of the encryption key used
 * @param operation      the operation type (SET or REMOVE)
 */
public record EncryptedMutation(
        byte[] indexMac,
        byte[] encryptedValue,
        byte[] keyId,
        Operation operation
) {
}
