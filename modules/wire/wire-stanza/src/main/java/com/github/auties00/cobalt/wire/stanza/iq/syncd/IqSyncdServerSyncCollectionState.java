package com.github.auties00.cobalt.wire.stanza.iq.syncd;

/**
 * Enumerates the per-collection sync outcomes derived from the inbound
 * {@code <collection/>} wire shape returned by the syncd relay.
 *
 * <p>One value is computed per collection in a syncd server-sync IQ reply and
 * drives the next-iteration decision: the {@code SUCCESS*} arms apply the
 * returned patches or snapshot, the {@code CONFLICT*} arms reconcile against the
 * relay's snapshot, the {@link #ERROR_FATAL} arm drops the collection, and the
 * {@link #ERROR_RETRY} arm schedules a retry. The {@code *_HAS_MORE} suffix
 * indicates that the relay still has additional patches queued past the cursor it
 * served, so the caller must issue a follow-up sync to drain them.
 *
 * @implNote
 * This implementation collapses WA Web's {@code Blocked} state onto
 * {@link #ERROR_FATAL}: WA Web's {@code Blocked} encodes a quota-limit rejection
 * the relay produces only when the caller's session has hit a server-side rate
 * cap, which Cobalt treats as a non-retryable fatal error.
 */
public enum IqSyncdServerSyncCollectionState {
    /**
     * Indicates that the collection synced cleanly with no further patches queued.
     *
     * <p>The caller applies any returned patches or snapshot and stops the
     * per-collection loop for this iteration.
     */
    SUCCESS,

    /**
     * Indicates that the collection synced cleanly but the relay has additional
     * patches queued past the served cursor.
     *
     * <p>The caller must issue a follow-up sync for this collection to drain the
     * remaining patches.
     */
    SUCCESS_HAS_MORE,

    /**
     * Indicates that the relay rejected the local patches because the collection
     * has diverged.
     *
     * <p>The caller reconciles against the relay's snapshot, regenerates the local
     * mutations, and re-uploads.
     */
    CONFLICT,

    /**
     * Indicates the {@link #CONFLICT} outcome with additional patches queued past
     * the served cursor.
     *
     * <p>The caller reconciles and then issues a follow-up sync to drain the
     * queued patches.
     */
    CONFLICT_HAS_MORE,

    /**
     * Indicates that the relay rejected the collection with a fatal error
     * ({@code 400}, {@code 404} or {@code 405}).
     *
     * <p>The caller stops the per-collection loop without retrying.
     */
    ERROR_FATAL,

    /**
     * Indicates that the relay rejected the collection with a transient error.
     *
     * <p>The caller may retry the collection in the next sync iteration with the
     * same active cursor.
     */
    ERROR_RETRY
}
