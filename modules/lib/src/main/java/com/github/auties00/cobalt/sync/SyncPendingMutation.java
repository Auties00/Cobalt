package com.github.auties00.cobalt.sync;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.util.Objects;
import java.util.UUID;

/**
 * One row of the local pending-mutation queue: a decrypted sync action that
 * has been produced by Cobalt and is waiting to be uploaded on the next
 * syncd round-trip.
 *
 * <p>Rows are produced by the per-handler {@code <Name>MutationFactory}
 * classes and persisted by {@link WebAppStateService}; they are not
 * constructed by embedders. Each row carries the trusted decrypted mutation,
 * the number of upload attempts already made for it, and a generated
 * identifier that correlates the upload IQ stanza with the server-side
 * acknowledgement that follows.
 *
 * @implNote This implementation adds an {@link #attemptCount()} field beyond
 * the shape of WhatsApp Web's pending-mutation IndexedDB rows so the syncd
 * retry loop can count attempts per row without consulting the
 * collection-level state machine. There is no IndexedDB-backed primary-key
 * column; the {@code mutationId} field is a {@link UUID#randomUUID()} string
 * created at construction time, which serves the same correlation role.
 */
@WhatsAppWebModule(moduleName = "WAWebPendingMutationStore")
public final class SyncPendingMutation {
    /**
     * The opaque identifier used to correlate this row with its server
     * acknowledgement.
     */
    private final String mutationId;

    /**
     * The decrypted, trusted sync action waiting to be uploaded.
     */
    private final DecryptedMutation.Trusted mutation;

    /**
     * The number of upload attempts already made for this row.
     */
    private final int attemptCount;

    /**
     * Builds a fresh pending-mutation row with a generated identifier and
     * the given attempt count.
     *
     * <p>The generated id is what {@link WebAppStateService} later uses to
     * correlate the server acknowledgement.
     *
     * @param mutation     the trusted decrypted mutation to enqueue
     * @param attemptCount the initial attempt count, typically {@code 0} for
     *                     a freshly enqueued row
     */
    public SyncPendingMutation(DecryptedMutation.Trusted mutation, int attemptCount) {
        this(UUID.randomUUID().toString(), mutation, attemptCount);
    }

    /**
     * Builds a pending-mutation row with an explicit identifier.
     *
     * <p>Used by {@link #incrementAttempt()} to preserve the original
     * identifier across retry attempts.
     *
     * @param mutationId   the identifier to bind to the new row
     * @param mutation     the trusted decrypted mutation to enqueue
     * @param attemptCount the initial attempt count for the new row
     */
    public SyncPendingMutation(String mutationId, DecryptedMutation.Trusted mutation, int attemptCount) {
        this.mutationId = mutationId;
        this.mutation = mutation;
        this.attemptCount = attemptCount;
    }

    /**
     * Returns a new row that carries the same identifier and mutation as
     * this one but with {@code attemptCount + 1}.
     *
     * <p>Called by the syncd retry path after a failed upload, before
     * re-enqueuing the row for the next attempt. The original instance is
     * left untouched.
     *
     * @return a new {@link SyncPendingMutation} instance with the bumped
     *         {@link #attemptCount()}
     */
    public SyncPendingMutation incrementAttempt() {
        return new SyncPendingMutation(mutationId, mutation, attemptCount + 1);
    }

    /**
     * Returns the opaque identifier bound to this row at construction time.
     *
     * <p>The id is the only stable handle through which the upload IQ can be
     * correlated with the server acknowledgement that closes the
     * round-trip.
     *
     * @return the identifier supplied at construction
     */
    public String mutationId() {
        return mutationId;
    }

    /**
     * Returns the trusted decrypted mutation wrapped by this row.
     *
     * <p>The upload path reads this payload to re-encrypt and serialise it
     * into the syncd patch IQ.
     *
     * @return the wrapped {@link DecryptedMutation.Trusted}
     */
    public DecryptedMutation.Trusted mutation() {
        return mutation;
    }

    /**
     * Returns the number of upload attempts that have already been made for
     * this row.
     *
     * <p>The syncd retry loop reads this to gate per-row backoff decisions
     * independently of the collection-level state machine.
     *
     * @return the current attempt count, monotonically increased by
     *         {@link #incrementAttempt()}
     */
    public int attemptCount() {
        return attemptCount;
    }

    /**
     * Indicates whether the given object is equal to this row.
     *
     * <p>Two rows are equal when they share the same {@link #mutationId()},
     * {@link #mutation()}, and {@link #attemptCount()}, so that two rows with
     * the same payload but different identifiers, or the same identifier but
     * different attempt counts, are considered distinct.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a {@link SyncPendingMutation} with
     *         the same three fields
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof SyncPendingMutation that
                            && attemptCount == that.attemptCount
                            && Objects.equals(mutationId, that.mutationId)
                            && Objects.equals(mutation, that.mutation);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * <p>The hash combines the same triple of fields that
     * {@link #equals(Object)} compares.
     *
     * @return the hash code for this row
     */
    @Override
    public int hashCode() {
        return Objects.hash(mutationId, mutation, attemptCount);
    }

    /**
     * Returns a single-line, comma-separated rendering of this row with
     * every field labeled.
     *
     * <p>The output is intended for log lines rather than user-facing
     * display.
     *
     * @return the string rendering of this row
     */
    @Override
    public String toString() {
        return "SyncPendingMutation[" +
               "mutationId=" + mutationId + ", " +
               "mutation=" + mutation + ", " +
               "attemptCount=" + attemptCount + ']';
    }
}
