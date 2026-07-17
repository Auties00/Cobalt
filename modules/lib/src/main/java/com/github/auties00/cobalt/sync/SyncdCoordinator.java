package com.github.auties00.cobalt.sync;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.MdCriticalEventEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.Collection;
import com.github.auties00.cobalt.wire.wam.type.MdSyncdCriticalEventCode;

import java.lang.System.Logger.Level;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Serializes every app-state (syncd) state mutation onto a single logical
 * queue and tracks which collections are mid-round.
 *
 * <p>WhatsApp Web runs the entire syncd subsystem on one serial async queue:
 * pull rounds, push rounds, key-share ingestion, key rotation, missing-key
 * bookkeeping, and orphan replays all execute one at a time, and the queue is
 * released at every {@code await} (network round trip) so an in-flight network
 * call never blocks an unrelated mutation. A module-level in-flight set (the
 * {@code A} set in the {@code WAWebSyncd} {@code triggerSync} driver) excludes a
 * collection that is already mid-round from a concurrent {@code serverSync} and
 * re-triggers it once the in-flight round finishes.
 *
 * <p>This class is the blocking-virtual-thread translation of that design. The
 * {@link #runLocked(Runnable)} family runs a synchronous state segment under a
 * single reentrant {@link #monitor}; callers must never perform network I/O
 * inside a segment, instead releasing the monitor between segments so a blocking
 * {@code sendNode}, media transfer, or peer-reply wait never holds the lock.
 * {@link #admitForRound(Set)} and {@link #clearInFlight(SyncPatchType)} maintain
 * the in-flight set so a collection's per-collection state cannot be mutated by
 * two rounds at once even across the unlocked I/O gaps.
 *
 * @implNote
 * This implementation uses a {@link ReentrantLock} rather than an intrinsic
 * monitor so the same thread can re-enter a segment from a nested helper, and so
 * {@link #monitor} can be asserted held by the current thread on the
 * monitor-confined helpers. The in-flight and pending-retrigger sets are
 * {@link EnumSet}s guarded by the same lock, so a single acquisition covers both
 * the set bookkeeping and any per-collection state read performed in the same
 * segment.
 */
@WhatsAppWebModule(moduleName = "WAWebSyncd")
public final class SyncdCoordinator {
    /**
     * The logger for {@link SyncdCoordinator}.
     */
    private static final System.Logger LOGGER = Log.get(SyncdCoordinator.class);

    /**
     * The single reentrant lock that serializes every syncd state segment.
     *
     * <p>Held only for the duration of a synchronous {@link #runLocked(Runnable)}
     * body; never across network I/O. Reentrant so a segment may call a helper
     * that opens its own segment on the same thread without self-deadlocking.
     */
    private final ReentrantLock monitor;

    /**
     * The collections currently mid-round (an IQ has been admitted and the round
     * has not yet cleared it).
     *
     * <p>Guarded by {@link #monitor}. A collection in this set is excluded from a
     * concurrent {@link #admitForRound(Set)} so two rounds never touch its
     * version, LT-Hash, or sync-action entries at the same time.
     */
    private final Set<SyncPatchType> inFlight;

    /**
     * The collections that were requested for a round while already in-flight and
     * therefore must be re-triggered once their in-flight round completes.
     *
     * <p>Guarded by {@link #monitor}. Mirrors the re-run marker the
     * {@code WAWebSyncd} driver records when {@code triggerSync} is called for a
     * collection already present in its in-flight set.
     */
    private final Set<SyncPatchType> pendingRetrigger;

    /**
     * The telemetry sink that receives the {@code MdCriticalEventEvent} emitted by
     * {@link #reportCriticalEvent(MdSyncdCriticalEventCode, SyncPatchType, String)},
     * or {@code null} when this coordinator was constructed without one.
     *
     * <p>WhatsApp Web routes every syncd critical fault through the single
     * {@code uploadMdCriticalEventMetric} reporter in {@code WAWebSyncdMetrics};
     * this field is that reporter's WAM back end. A {@code null} value disables
     * emission so the coordinator can still be built purely for state coordination
     * (for example in unit tests) without a telemetry pipeline attached.
     */
    private final WamService wamService;

    /**
     * Constructs an empty coordinator with no telemetry sink and no collection in
     * flight.
     *
     * <p>Equivalent to {@link #SyncdCoordinator(WamService)} with a {@code null}
     * sink: state coordination works fully, but
     * {@link #reportCriticalEvent(MdSyncdCriticalEventCode, SyncPatchType, String)}
     * becomes a no-op because there is no pipeline to commit the event to.
     */
    public SyncdCoordinator() {
        this(null);
    }

    /**
     * Constructs an empty coordinator that emits syncd critical-event telemetry
     * through the given sink.
     *
     * <p>One instance is created per {@link WebAppStateService} and shared with the
     * key-rotation, missing-key request, and missing-key timeout collaborators so
     * all of them serialize against the same monitor and in-flight set. The sink is
     * the WAM back end for
     * {@link #reportCriticalEvent(MdSyncdCriticalEventCode, SyncPatchType, String)}.
     *
     * @param wamService the telemetry sink for critical-event reporting, or
     *                   {@code null} to disable emission
     */
    public SyncdCoordinator(WamService wamService) {
        this.monitor = new ReentrantLock();
        this.inFlight = EnumSet.noneOf(SyncPatchType.class);
        this.pendingRetrigger = EnumSet.noneOf(SyncPatchType.class);
        this.wamService = wamService;
    }

    /**
     * Runs a synchronous state segment that produces a value under the
     * {@link #monitor}.
     *
     * <p>The body must touch only in-memory syncd state (collection version and
     * LT-Hash, sync-action entries, the key store, the missing-key tracker, the
     * in-flight set) and pure computation; it must never perform blocking network
     * I/O, so the monitor is released the moment the body returns.
     *
     * @param body the segment to run while holding the monitor
     * @param <T>  the type of value the segment produces
     * @return the value returned by {@code body}
     */
    public <T> T runLocked(Supplier<T> body) {
        monitor.lock();
        try {
            return body.get();
        } finally {
            monitor.unlock();
        }
    }

    /**
     * Runs a synchronous state segment that produces no value under the
     * {@link #monitor}.
     *
     * <p>Behaves as {@link #runLocked(Supplier)} for a {@link Runnable} body; the
     * same no-I/O contract applies.
     *
     * @param body the segment to run while holding the monitor
     */
    public void runLocked(Runnable body) {
        monitor.lock();
        try {
            body.run();
        } finally {
            monitor.unlock();
        }
    }

    /**
     * Runs a blocking I/O step with the {@link #monitor} fully released, then
     * re-acquires it to the exact hold count the caller held on entry.
     *
     * <p>This is the {@code await} of the serial-queue translation: a round holds
     * the monitor across its synchronous segments but calls this for every
     * blocking network step (the sync IQ, media transfers, the snapshot-recovery
     * peer-reply wait, peer sends), so no lock is ever held across I/O. While the
     * step runs, another round for a different collection, an inbound key share,
     * or the recovery-reply handler can take the monitor and make progress;
     * delivering the recovery reply therefore can never deadlock against a round
     * waiting for it. The collection that owns the calling round stays in the
     * in-flight set throughout, so its own state cannot change across the gap and
     * needs no re-validation on re-acquire.
     *
     * @implNote
     * This implementation reads {@link ReentrantLock#getHoldCount()} and fully
     * unwinds the lock so a nested round (a batched round paginating a single
     * collection) does not leave the monitor held during the step; it then
     * re-locks the same number of times in a {@code finally} so the caller's hold
     * depth is preserved even if the step throws.
     *
     * @param step the blocking I/O step to run with the monitor released
     * @param <T>  the type of value the step produces
     * @return the value returned by {@code step}
     * @throws IllegalStateException if the calling thread does not hold the monitor
     */
    public <T> T runWithMonitorReleased(Supplier<T> step) {
        if (!monitor.isHeldByCurrentThread()) {
            throw new IllegalStateException("runWithMonitorReleased requires the monitor to be held");
        }
        var holds = monitor.getHoldCount();
        for (var i = 0; i < holds; i++) {
            monitor.unlock();
        }
        try {
            return step.get();
        } finally {
            for (var i = 0; i < holds; i++) {
                monitor.lock();
            }
        }
    }

    /**
     * Runs a blocking I/O step that produces no value with the {@link #monitor}
     * fully released.
     *
     * <p>Behaves as {@link #runWithMonitorReleased(Supplier)} for a {@link Runnable}
     * step.
     *
     * @param step the blocking I/O step to run with the monitor released
     * @throws IllegalStateException if the calling thread does not hold the monitor
     */
    public void runWithMonitorReleased(Runnable step) {
        runWithMonitorReleased(() -> {
            step.run();
            return null;
        });
    }

    /**
     * Admits the requested collections into a fresh round, excluding any that are
     * already in flight and recording those for a later re-trigger.
     *
     * <p>Each requested collection that is not currently in flight is added to the
     * in-flight set and returned as admitted; each one that is already in flight is
     * added to {@link #pendingRetrigger} so the round that owns it re-triggers it on
     * completion, and is omitted from the returned set. The returned set preserves
     * the iteration order of {@code requested}.
     *
     * @implNote
     * This implementation acquires the {@link #monitor} for the whole partition so
     * the admit decision and the in-flight mutation are atomic; the lock is
     * reentrant, so calling this inside a {@link #runLocked(Runnable)} body is safe.
     *
     * @param requested the collections a caller wants to sync this round
     * @return the subset that was admitted (now marked in flight)
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncd", exports = "triggerSync", adaptation = WhatsAppAdaptation.ADAPTED)
    public Set<SyncPatchType> admitForRound(Set<SyncPatchType> requested) {
        monitor.lock();
        try {
            var admitted = new LinkedHashSet<SyncPatchType>();
            for (var patchType : requested) {
                if (inFlight.add(patchType)) {
                    admitted.add(patchType);
                } else {
                    pendingRetrigger.add(patchType);
                    if (Log.DEBUG) {
                        LOGGER.log(Level.DEBUG, "collection {0} already in flight, deferring for retrigger", patchType);
                    }
                }
            }
            return admitted;
        } finally {
            monitor.unlock();
        }
    }

    /**
     * Clears a collection from the in-flight set once its round has completed and
     * reports whether it must be re-triggered.
     *
     * <p>A collection requires a re-trigger when it was requested for another round
     * while this round held it in flight; that request was deferred into
     * {@link #pendingRetrigger} by {@link #admitForRound(Set)}. The caller is
     * responsible for issuing the follow-up pull for every collection this method
     * reports {@code true} for, after releasing the monitor.
     *
     * @param patchType the collection whose round has completed
     * @return {@code true} if the collection was deferred while in flight and now
     *         needs a fresh round; {@code false} otherwise
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncd", exports = "triggerSync", adaptation = WhatsAppAdaptation.ADAPTED)
    public boolean clearInFlight(SyncPatchType patchType) {
        monitor.lock();
        try {
            inFlight.remove(patchType);
            var retrigger = pendingRetrigger.remove(patchType);
            if (Log.DEBUG && retrigger) {
                LOGGER.log(Level.DEBUG, "collection {0} retriggered after its in-flight round completed", patchType);
            }
            return retrigger;
        } finally {
            monitor.unlock();
        }
    }

    /**
     * Returns whether the given collection is currently mid-round.
     *
     * <p>Surfaced for diagnostics; the result is a point-in-time read and may be
     * stale the moment the monitor is released.
     *
     * @param patchType the collection to test
     * @return {@code true} if the collection is in the in-flight set
     */
    public boolean isInFlight(SyncPatchType patchType) {
        monitor.lock();
        try {
            return inFlight.contains(patchType);
        } finally {
            monitor.unlock();
        }
    }

    /**
     * Returns whether the current thread already holds the {@link #monitor}.
     *
     * <p>Used by the syncd collaborators to assert that a monitor-confined store
     * mutation is being performed inside a {@link #runLocked(Runnable)} segment
     * rather than on a bare thread.
     *
     * @return {@code true} if the calling thread holds the monitor
     */
    public boolean isHeldByCurrentThread() {
        return monitor.isHeldByCurrentThread();
    }

    /**
     * Reports a critical syncd processing fault as a WAM
     * {@link com.github.auties00.cobalt.wire.wam.event.MdCriticalEventEvent}.
     *
     * <p>WhatsApp Web funnels every unrecoverable app-state condition (a message
     * range that fails validation, a malformed pending mutation, invalid action
     * index data, an anti-tampering missing-remove, an LT-Hash inconsistency, an
     * unapplied mutation, and the rest of {@link MdSyncdCriticalEventCode}) through
     * a single {@code uploadMdCriticalEventMetric} reporter; this method is the
     * Cobalt equivalent. {@code code} identifies which fault fired, {@code collection}
     * names the affected app-state collection (mapped to the WAM {@link Collection}
     * enum), and {@code mutationActionName} carries the sync-action name of the
     * offending mutation when one is known. When this coordinator was constructed
     * without a telemetry sink the call is a no-op.
     *
     * @apiNote
     * Callers on the syncd decode, patch-apply, anti-tampering, and message-range
     * validation paths invoke this the moment they detect an unrecoverable fault,
     * before they discard the mutation or fail the round; the codes are mutually
     * exclusive, so exactly one call is made per detected fault.
     *
     * @implNote
     * This implementation derives {@code mdCriticalEventStage} from {@code code}
     * because the processing stage is fully determined by which fault fired, and
     * leaves {@code mdCriticalEventErrorMessage} unset here; use the
     * {@link #reportCriticalEvent(MdSyncdCriticalEventCode, SyncPatchType, String, Throwable)}
     * overload to attach a decode or apply exception message.
     *
     * @param code               the critical-event code that fired, must not be {@code null}
     * @param collection         the affected collection, or {@code null} if the fault is not collection-scoped
     * @param mutationActionName the sync-action name of the offending mutation, or {@code null} if unknown
     * @throws NullPointerException if {@code code} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdMetrics", exports = "uploadMdCriticalEventMetric", adaptation = WhatsAppAdaptation.ADAPTED)
    public void reportCriticalEvent(MdSyncdCriticalEventCode code, SyncPatchType collection, String mutationActionName) {
        reportCriticalEvent(code, collection, mutationActionName, null);
    }

    /**
     * Reports a critical syncd processing fault, attaching the causing exception's
     * message as {@code mdCriticalEventErrorMessage}.
     *
     * <p>Behaves as
     * {@link #reportCriticalEvent(MdSyncdCriticalEventCode, SyncPatchType, String)}
     * but additionally records {@code cause}'s message when the fault surfaced as a
     * thrown exception (a decode or patch-apply failure), giving the telemetry the
     * concrete failure text alongside the coded reason. A {@code null} {@code cause}
     * or a {@code cause} with no message leaves the field unset.
     *
     * @param code               the critical-event code that fired, must not be {@code null}
     * @param collection         the affected collection, or {@code null} if the fault is not collection-scoped
     * @param mutationActionName the sync-action name of the offending mutation, or {@code null} if unknown
     * @param cause              the exception that surfaced the fault, or {@code null} if the fault was detected without one
     * @throws NullPointerException if {@code code} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdMetrics", exports = "uploadMdCriticalEventMetric", adaptation = WhatsAppAdaptation.ADAPTED)
    public void reportCriticalEvent(MdSyncdCriticalEventCode code, SyncPatchType collection, String mutationActionName, Throwable cause) {
        Objects.requireNonNull(code, "code cannot be null");
        if (Log.WARNING) {
            if (cause != null) {
                LOGGER.log(Level.WARNING, "syncd critical event " + code + " for collection " + collection, cause);
            } else {
                LOGGER.log(Level.WARNING, "syncd critical event: code={0} collection={1}", code, collection);
            }
        }
        if (wamService == null) {
            return;
        }
        var builder = new MdCriticalEventEventBuilder()
                .mdCriticalEventCode(code)
                .mdCriticalEventStage(stageOf(code));
        if (collection != null) {
            builder.collection(toCollectionMetric(collection));
        }
        if (mutationActionName != null) {
            builder.mutationActionName(mutationActionName);
        }
        if (cause != null && cause.getMessage() != null) {
            builder.mdCriticalEventErrorMessage(cause.getMessage());
        }
        wamService.commit(builder.build());
    }

    /**
     * Maps a {@link SyncPatchType} to the matching WAM {@link Collection} constant.
     *
     * @param collection the affected collection, must not be {@code null}
     * @return the corresponding WAM collection enum constant
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdMetrics", exports = "collectionNameToMetric", adaptation = WhatsAppAdaptation.DIRECT)
    private static Collection toCollectionMetric(SyncPatchType collection) {
        return switch (collection) {
            case CRITICAL_BLOCK -> Collection.CRITICAL_BLOCK;
            case CRITICAL_UNBLOCK_LOW -> Collection.CRITICAL_UNBLOCK_LOW;
            case REGULAR -> Collection.REGULAR;
            case REGULAR_HIGH -> Collection.REGULAR_HIGH;
            case REGULAR_LOW -> Collection.REGULAR_LOW;
        };
    }

    /**
     * Returns the syncd processing stage at which the given critical-event code is
     * raised.
     *
     * <p>The stage is fully determined by the code: the message-range codes come
     * from message-range validation; {@link MdSyncdCriticalEventCode#MISSING_MUTATION_TO_REMOVE},
     * {@link MdSyncdCriticalEventCode#NO_CONFIRMED_SET_MUTATION_FOR_A_PENDING_REMOVE}, and
     * {@link MdSyncdCriticalEventCode#NO_KEY_DATA_FOR_A_PENDING_REMOVE_MUTATION} come from the
     * anti-tampering pass; the LT-Hash codes come from the daily consistency check and the
     * snapshot MAC verification respectively; and the remaining codes name their own
     * decode or apply stage. The returned label is a short lowercase identifier suitable
     * for the WAM {@code mdCriticalEventStage} string.
     *
     * @param code the critical-event code, must not be {@code null}
     * @return a short identifier naming the processing stage
     */
    private static String stageOf(MdSyncdCriticalEventCode code) {
        return switch (code) {
            case MESSAGE_RANGE_UNSET,
                 MESSAGE_RANGE_LAST_SYSTEM_MESSAGE_TIMESTAMP_SET,
                 MESSAGE_RANGE_MESSAGES_UNSET,
                 MESSAGE_RANGE_MESSAGES_EMPTY,
                 MESSAGE_RANGE_MESSAGES_CROSS_LIMIT,
                 MESSAGE_RANGE_MESSAGE_KEY_UNSET,
                 MESSAGE_RANGE_MESSAGE_KEY_REMOTE_JID_UNSET,
                 MESSAGE_RANGE_MESSAGE_KEY_FROM_ME_UNSET,
                 MESSAGE_RANGE_MESSAGE_KEY_STANZA_ID_UNSET,
                 MESSAGE_RANGE_MESSAGE_KEY_REMOTE_JID_INVALID,
                 MESSAGE_RANGE_MESSAGE_KEY_PARTICIPANT_UNSET -> "message_range_validation";
            case MALFORMED_PENDING_MUTATION -> "pending_mutation_decode";
            case ACTION_INVALID_INDEX_DATA -> "index_decode";
            case MISSING_MUTATION_TO_REMOVE,
                 NO_CONFIRMED_SET_MUTATION_FOR_A_PENDING_REMOVE,
                 NO_KEY_DATA_FOR_A_PENDING_REMOVE_MUTATION -> "anti_tampering";
            case LTHASH_INCONSISTENCY_ON_DAILY_CHECK -> "daily_lthash_check";
            case LTHASH_INCONSISTENCY_ON_SNAPSHOT_MAC_MISMATCH -> "snapshot_mac_verification";
            case EMPTY_PATCH -> "patch_decode";
            case HISTORY_SYNC_ILLEGAL_STATE_EXCEPTION -> "history_sync";
            case MUTATION_SKIPPED_ON_STORE_LOAD -> "store_load";
            case MUTATION_SKIPPED_ON_OUTGOING_SYNC_REQUEST_PREPARATION -> "outgoing_sync_request_preparation";
            case UNAPPLIED_MUTATION_ON_PATCH_APPLICATION -> "patch_application";
        };
    }
}
