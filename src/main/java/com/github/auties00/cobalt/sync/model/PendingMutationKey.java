package com.github.auties00.cobalt.sync.model;

/**
 * Tracks a request for a missing encryption key.
 *
 * <p>When a mutation is encrypted with an unknown key, a key request is sent
 * to other devices. This record tracks such requests.
 *
 * @param keyId the missing key ID (hex string)
 * @param requestedAt the timestamp when the key was first requested (Unix millis)
 * @param retryCount the number of times the key request has been sent
 */
public record PendingMutationKey(
    String keyId,
    long requestedAt,
    int retryCount
) {
    /**
     * Creates a new missing key request with retry count 0.
     *
     * @param keyId the missing key ID
     */
    public PendingMutationKey(String keyId) {
        this(keyId, System.currentTimeMillis(), 0);
    }

    /**
     * Creates a copy with incremented retry count.
     *
     * @return a new request with retry count + 1
     */
    public PendingMutationKey incrementRetry() {
        return new PendingMutationKey(keyId, requestedAt, retryCount + 1);
    }
}
