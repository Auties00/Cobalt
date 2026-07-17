package com.github.auties00.cobalt.sync;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.sync.*;
import com.github.auties00.cobalt.sync.key.SyncKeyRotationService;

import java.util.Collection;
import java.util.SequencedCollection;
import java.util.Set;

/**
 * Drives the WhatsApp Web app-state (syncd) pipeline: pushing local mutations to the server,
 * pulling server-announced collection changes, applying snapshots and patches, and owning the
 * background jobs that keep app state eventually consistent.
 *
 * <p>Code that mutates app state (archiving a chat, pinning a message, toggling a setting) reaches
 * this service through {@link LinkedWhatsAppClient#pushWebAppState}; the socket layer drives the
 * pull side when the server announces a dirty collection. The service owns the sync-key rotation
 * service, the missing-key request and timeout machinery, and the snapshot-recovery handshake.
 *
 * @implSpec
 * Implementations must serialise every sync round so no two rounds touch a collection's version and
 * LT-Hash concurrently, and must drive pushes inline so a local mutation is on the server by the
 * time {@link #pushPatches(SyncPatchType, SequencedCollection)} returns.
 */
public interface WebAppStateService {
    /**
     * Enqueues local mutations for the given collection and synchronously drives a sync round to
     * push them to the server.
     *
     * <p>Triggered whenever a local action produces an app-state change. Callers normally reach this
     * through {@link LinkedWhatsAppClient#pushWebAppState}.
     *
     * @implSpec
     * Implementations must enqueue the mutations and run the push round before returning.
     *
     * @param patchType the collection these mutations belong to
     * @param patches   the pending mutations to enqueue
     */
    void pushPatches(SyncPatchType patchType, SequencedCollection<SyncPendingMutation> patches);

    /**
     * Drives a batched sync round for the given collections and reports whether any response carried
     * real state changes.
     *
     * <p>Called whenever the server announces that one or more collections may have new data. The
     * return value lets the caller detect false-positive dirty bits.
     *
     * @param patchTypes the collection types to sync; empty causes a no-op {@code false} return
     * @return {@code true} if at least one collection response carried a patch or a snapshot
     *         reference; {@code false} otherwise
     */
    boolean pullPatches(SyncPatchType... patchTypes);

    /**
     * Returns the {@link SyncKeyRotationService} owned by this service.
     *
     * <p>Exposed so the protocol-message receiver that dispatches incoming
     * {@code AppStateSyncKeyShare} payloads can hand them off without a separately injected
     * reference.
     *
     * @return the {@link SyncKeyRotationService} instance
     */
    SyncKeyRotationService syncKeyRotationService();

    /**
     * Unblocks every collection parked in {@link SyncCollectionState#BLOCKED} and drives a fresh
     * sync round for them.
     *
     * <p>Invoked after a successful {@code AppStateSyncKeyShare} delivery has replenished the
     * missing key material.
     */
    void syncBlockedCollections();

    /**
     * Returns an unmodifiable snapshot of the collections currently mid-flight.
     *
     * @return an unmodifiable {@link Set} of collections in {@link SyncCollectionState#IN_FLIGHT}
     */
    Set<SyncPatchType> getInFlightCollections();

    /**
     * Returns an unmodifiable snapshot of the collections re-marked dirty while already in flight.
     *
     * @return an unmodifiable {@link Set} of collections in {@link SyncCollectionState#PENDING}
     */
    Set<SyncPatchType> getPendingCollections();

    /**
     * Commits the current syncd telemetry counters by forwarding to the stats and key-stats
     * reporting passes.
     *
     * <p>Triggers an immediate flush of the syncd telemetry, equivalent to the reporter that fires
     * on application resume.
     */
    void reportWam();

    /**
     * Performs the syncd key-info logging that the WhatsApp internal build channel exposes; a no-op
     * in production.
     */
    void logKeysInfoInIntern();

    /**
     * Replays every previously deferred orphan mutation across all collections.
     *
     * <p>Called as part of {@link #resumeAfterRestart()} so mutations whose target entity was not
     * present when the snapshot or patch was applied get a second chance.
     */
    void retryAllOrphanMutations();

    /**
     * Retries orphan mutations whose target entities now appear in the supplied identifier sets.
     *
     * <p>Called when fresh entities become known (a history sync chunk landed, an incoming message
     * was accepted, a new thread surfaced).
     *
     * @param msgIds    the message identifiers to match for {@code Msg}-typed orphans
     * @param chatIds   the chat identifiers to match for both {@code Chat}- and {@code Account}-typed
     *                  orphans
     * @param threadIds the thread identifiers to match for {@code Thread}-typed orphans;
     *                  {@code null} or empty skips the thread sweep
     */
    void checkOrphanMutations(Collection<String> msgIds, Collection<String> chatIds, Collection<String> threadIds);

    /**
     * Retries orphan mutations of model type {@code Msg} whose target message id is in
     * {@code msgIds}.
     *
     * @param msgIds the message identifiers to retry against; {@code null} or empty is a no-op
     */
    void checkOrphanMessages(Collection<String> msgIds);

    /**
     * Retries orphan mutations of model type {@code Chat} whose target chat id is in
     * {@code chatIds}.
     *
     * @param chatIds the chat identifiers to retry against; {@code null} or empty is a no-op
     */
    void checkOrphanChats(Collection<String> chatIds);

    /**
     * Retries orphan mutations of model type {@code Thread} whose target thread id is in
     * {@code threadIds}.
     *
     * @param threadIds the thread identifiers to retry against; {@code null} or empty is a no-op
     */
    void checkOrphanThreads(Collection<String> threadIds);

    /**
     * Retries orphan mutations of model type {@code Agent} whose target agent id is in
     * {@code agentIds}.
     *
     * @param agentIds the agent identifiers to retry against; {@code null} or empty is a no-op
     */
    void checkOrphanAgents(Collection<String> agentIds);

    /**
     * Retries orphan mutations of model type {@code ChatAssignment} whose target assignment id is in
     * {@code assignmentIds}.
     *
     * @param assignmentIds the chat-assignment identifiers to retry against; {@code null} or empty
     *                      is a no-op
     */
    void checkOrphanChatAssignments(Collection<String> assignmentIds);

    /**
     * Retries orphan mutations of model type {@code UserStatusMute} whose target contact id is in
     * {@code contactIds}.
     *
     * @param contactIds the contact identifiers to retry against; {@code null} or empty is a no-op
     */
    void checkOrphanUserStatusMutes(Collection<String> contactIds);

    /**
     * Retries orphan mutations of model type {@code FavoriteSticker}, gated by the
     * {@code favorite_sticker} primary feature and its AB prop.
     *
     * <p>Triggered after a fresh pairing once the sticker pack inventory has been fetched.
     */
    void checkOrphanFavoriteStickers();

    /**
     * Recovers interrupted sync state after a JVM restart and kicks off follow-up sync, orphan, and
     * unsupported-mutation passes.
     *
     * <p>Called once on client startup to discharge state that was left in a non-terminal sync state
     * when the previous process died, then sync everything that still has pending mutations or
     * unfinished server-side work.
     *
     * @implSpec
     * Implementations must rewrite each recoverable non-terminal collection state back to
     * {@link SyncCollectionState#DIRTY} so the next pull picks it up.
     */
    void resumeAfterRestart();

    /**
     * Schedules the all-peers-negative check inside the missing-sync-key timeout machinery.
     *
     * <p>Invoked from the key-share receiver whenever a companion device replies to a sync-key
     * request without supplying the requested key; after the grace period elapses with every
     * companion having responded negatively, the missing key is escalated to a fatal sync failure.
     */
    void scheduleAllDevicesRespondedCheck();

    /**
     * Forces the missing-sync-key timeout deadline to be recomputed against the current contents of
     * the missing-key store.
     *
     * <p>Must be called whenever a missing key has been resolved so the deadline tracks the new
     * earliest-pending key, or clears entirely when no missing keys remain.
     */
    void rescheduleMissingSyncKeyTimeout();

    /**
     * Drives an outbound sync round for every collection currently parked in
     * {@link SyncCollectionState#DIRTY}.
     *
     * <p>Called immediately before a graceful logout so the sentinel mutation that carries
     * key-rotation state reaches every companion device.
     *
     * @implSpec
     * Implementations must swallow per-collection failures because the caller is about to disconnect.
     */
    void flushDirtyCollections();

    /**
     * Starts the recurring background jobs that keep app-state sync eventually consistent and
     * instrumented.
     *
     * <p>The jobs catch missed dirty-bit notifications, ship the daily action and key telemetry, and
     * rotate the active app state sync key. Safe to call after a previous {@link #stopPeriodicSyncJob()};
     * idempotent if already started.
     *
     * @implSpec
     * Implementations must cancel any in-flight periodic sync handle before scheduling a new one.
     */
    void startPeriodicSyncJob();

    /**
     * Cancels the in-flight catch-up sweep handle if one is scheduled.
     *
     * <p>Pairs with {@link #startPeriodicSyncJob()}; safe to call when no job is scheduled.
     */
    void stopPeriodicSyncJob();

    /**
     * Cancels the in-flight key-stats task handle if one is scheduled.
     *
     * <p>Safe to call when no job is scheduled; invoked from {@link #reset()} during disconnect.
     */
    void stopPeriodicReportSyncdKeyStatsJob();

    /**
     * Cancels the in-flight action-stats task handle if one is scheduled.
     *
     * <p>Safe to call when no job is scheduled; invoked from {@link #reset()} during disconnect.
     */
    void stopPeriodicReportSyncdStatsJob();

    /**
     * Tears down every background scheduler owned by this service so the JVM can quiesce.
     *
     * <p>Called from the connection-shutdown path during graceful disconnect or logout; pairs with
     * {@link #startPeriodicSyncJob()}. Idempotent.
     */
    void reset();
}
