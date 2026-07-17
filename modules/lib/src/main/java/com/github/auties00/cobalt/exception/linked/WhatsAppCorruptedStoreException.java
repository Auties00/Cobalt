package com.github.auties00.cobalt.exception.linked;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;

/**
 * Thrown when the persisted Cobalt store cannot be loaded because its
 * bytes are corrupt.
 *
 * Cobalt keeps a single on-disk store containing the registration
 * credentials, Signal protocol keys, identity material, and the cached
 * chat state. When the store is truncated, tampered with, or written by an
 * incompatible version, the deserializer raises this exception instead of
 * silently continuing with cryptographic material that cannot be trusted.
 *
 * @apiNote
 * {@link #toErrorResult()} reports {@link WhatsAppLinkedClientErrorResult#DISCONNECT}:
 * the store holds the keys needed to resume the session, so there is no usable
 * identity to recover. The configured error handler typically logs the device
 * out so the application can drive a fresh pairing.
 *
 * @implNote
 * Cobalt collapses WA Web's multi-IndexedDB schema into a single store, so
 * a single corruption affects every entity type at once.
 */
public final class WhatsAppCorruptedStoreException extends WhatsAppLinkedException {
    /**
     * Constructs a new corrupted store exception wrapping the specified cause.
     *
     * @param cause the underlying deserialization failure
     */
    public WhatsAppCorruptedStoreException(Throwable cause) {
        super("Store data is corrupted and cannot be deserialized", cause);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns
     * {@link WhatsAppLinkedClientErrorResult#DISCONNECT}: a corrupted store yields
     * no usable keys, so the session cannot resume.
     */
    @Override
    public WhatsAppLinkedClientErrorResult toErrorResult() {
        return WhatsAppLinkedClientErrorResult.DISCONNECT;
    }
}
