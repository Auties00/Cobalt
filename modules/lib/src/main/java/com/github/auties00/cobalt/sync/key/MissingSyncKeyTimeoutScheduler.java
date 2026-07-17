package com.github.auties00.cobalt.sync.key;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.exception.linked.web.WhatsAppWebAppStateSyncException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.device.sync.MissingDeviceSyncKey;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.SyncdCoordinator;
import com.github.auties00.cobalt.util.ScheduledTask;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Schedules the wall-clock-driven follow-ups that bound how long the client waits for missing
 * sync keys before declaring the wait fatal.
 *
 * <p>Three concurrent tasks live here. The per-key wait-for-key timeout, configured by
 * {@code syncd_wait_for_key_timeout_days} (default seven days), fires a fatal
 * {@link WhatsAppWebAppStateSyncException.TimeoutWhileWaitingForMissingKey} when a tracked
 * key has aged past the threshold and still has not arrived. A short five-second grace period
 * fires a fatal {@link WhatsAppWebAppStateSyncException.MissingKeyOnAllDevices} when every
 * asked device has answered without the key. The periodic six-hour re-broadcast job re-issues
 * the request for every tracked key so a request lost in transit can still be recovered. Both
 * fatal exceptions reach the client through {@link LinkedWhatsAppClient#handleFailure}.
 *
 * <p>The scheduler is owned by {@link com.github.auties00.cobalt.sync.WebAppStateService} and
 * arms no timer until {@link #scheduleTimeoutCheck()} or {@link #startPeriodicReRequestJob()}
 * is called.
 *
 * @implNote This implementation runs each task as an independent virtual-thread
 * {@link ScheduledTask} rather than sharing a {@link java.util.concurrent.ScheduledExecutorService},
 * so no task serialises with the syncd virtual-thread pipeline and no daemon thread is held across
 * shutdown. The wait-for-key timeout and the all-devices-responded grace check are arming-and-cancelling
 * one-shot handles rather than persistent timers.
 */
@WhatsAppWebModule(moduleName = "WAWebSyncdStoreMissingKeys")
@WhatsAppWebModule(moduleName = "WAWebSyncdRequestAllSyncdMissingKeysJob")
public final class MissingSyncKeyTimeoutScheduler {
    /**
     * The logger for {@link MissingSyncKeyTimeoutScheduler}.
     */
    private static final System.Logger LOGGER = Log.get(MissingSyncKeyTimeoutScheduler.class);

    /**
     * Holds the interval, in hours, between two periodic re-broadcasts of the missing key
     * requests.
     *
     * @implNote This implementation uses {@code 6} to match the {@code HOUR_SECONDS * 6}
     * cadence of the WA Web re-request task.
     */
    private static final long RE_REQUEST_INTERVAL_HOURS = 6;

    /**
     * Holds the injected client used to surface the fatal exceptions raised when a missing-key
     * deadline elapses.
     */
    private final LinkedWhatsAppClient client;

    /**
     * Holds the shared store consulted for the live missing-key tracker and for the resolved
     * app state sync key store, the latter used to confirm that a key never arrived.
     */
    private final LinkedWhatsAppStore store;

    /**
     * Holds the AB prop source used to read the live {@code syncd_wait_for_key_timeout_days}
     * value.
     */
    private final ABPropsService abPropsService;

    /**
     * Holds the companion request service the periodic job drives to re-broadcast requests for
     * every tracked missing key.
     */
    private final MissingSyncKeyRequestService requestService;

    /**
     * Holds the shared syncd coordinator whose monitor makes the fatal-decision read of the
     * missing-key tracker and the sync key store atomic against the apply path and the inbound
     * key-share.
     */
    private final SyncdCoordinator coordinator;

    /**
     * Holds the currently armed wait-for-key timeout, or {@code null} when none is armed.
     *
     * @implNote This implementation declares the field {@code volatile} so a concurrent
     * {@link #cancel()} or {@link #scheduleTimeoutCheck()} observes the latest write without
     * holding a monitor across the schedule boundary.
     */
    private volatile ScheduledTask scheduledCheck;

    /**
     * Holds the currently armed five-second grace-period timer, independent of
     * {@link #scheduledCheck}.
     */
    private volatile ScheduledTask allDevicesCheck;

    /**
     * Holds the handle of the periodic six-hour re-broadcast job, or {@code null} until
     * {@link #startPeriodicReRequestJob()} is called.
     */
    private volatile ScheduledTask reRequestJob;

    /**
     * Whether {@link #shutdown()} has been called; guarded by this instance's monitor so the arming
     * methods refuse to schedule new work after teardown.
     *
     * @implNote This implementation replaces the rejection a shut-down
     * {@link java.util.concurrent.ScheduledExecutorService} used to raise on a post-teardown
     * submission, since each timer now owns an independent virtual thread with no shared executor to
     * reject against.
     */
    private boolean shutdown;

    /**
     * Constructs a new scheduler bound to the supplied dependencies.
     *
     * <p>The constructed scheduler arms no timer; the caller must subsequently invoke
     * {@link MissingSyncKeyRequestService#setTimeoutScheduler(MissingSyncKeyTimeoutScheduler)}
     * to close the cyclic dependency between this scheduler and its request service.
     *
     * @param client the client used to surface fatal sync exceptions
     * @param abPropsService the AB prop source used to read the timeout configuration
     * @param requestService the request service driven by the periodic job
     * @param coordinator the shared syncd coordinator serializing the fatal-decision read
     */
    public MissingSyncKeyTimeoutScheduler(LinkedWhatsAppClient client, ABPropsService abPropsService, MissingSyncKeyRequestService requestService, SyncdCoordinator coordinator) {
        this.client = client;
        this.store = client.store();
        this.abPropsService = abPropsService;
        this.requestService = requestService;
        this.coordinator = coordinator;
    }

    /**
     * Arms, or rearms, the wait-for-key timeout based on the timestamp of the oldest tracked
     * missing key.
     *
     * <p>The delay is the configured wait-for-key timeout minus the age of the earliest
     * tracked key, clamped to zero when already past due. Any previously armed wait-for-key
     * timeout is cancelled first so successive calls converge on a single live timer. When the
     * tracker is empty the method is a no-op. It is invoked inline by
     * {@link LiveMissingSyncKeyRequestService#trackMissingKeys(java.util.Collection, java.util.Set)}
     * whenever a key is added, and again from inside the periodic re-request job.
     *
     * @implNote This implementation clamps a negative remaining duration to zero so the
     * deferred check fires as soon as its worker thread is scheduled rather than being armed with a
     * negative delay.
     */
    public synchronized void scheduleTimeoutCheck() {
        if (shutdown) {
            return;
        }
        if (scheduledCheck != null) {
            scheduledCheck.cancel();
        }

        var timeout = getTimeout();
        var delay = store.syncStore().missingSyncKeys()
                .stream()
                .map(MissingDeviceSyncKey::timestamp)
                .min(Instant::compareTo)
                .map(earliest -> {
                    var elapsed = Duration.between(earliest, Instant.now());
                    var remaining = timeout.minus(elapsed);
                    return remaining.isNegative() ? Duration.ZERO : remaining;
                });

        if (delay.isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no missing sync keys, timeout check not scheduled");
            return;
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "scheduling missing sync key timeout check in {0}ms", delay.get().toMillis());

        scheduledCheck = ScheduledTask.scheduleDelayed(delay.get(), this::checkForExpiredKeys);
    }

    /**
     * Re-checks every tracked missing key against the live sync key store and raises a fatal
     * sync exception when any of them has aged past the wait-for-key timeout.
     *
     * <p>A tracked key may arrive between the moment the timer is armed and the moment it
     * fires, in which case the tracker entry has already been resolved by
     * {@link SyncKeyRotationService#handleKeyShare(int, List)} and no fatal is
     * raised. Keys that are still genuinely absent and older than the timeout cause a
     * {@link WhatsAppWebAppStateSyncException.TimeoutWhileWaitingForMissingKey} to be reported
     * to {@link LinkedWhatsAppClient#handleFailure}. When nothing has actually expired the check
     * rearms itself.
     *
     * @implNote This implementation reschedules itself after finding zero expired keys so a
     * torn-down-and-restored session that still carries a live tracker entry does not miss the
     * next deadline waiting for an external trigger.
     */
    private void checkForExpiredKeys() {
        var expiredMissingSyncKeys = coordinator.runLocked(() -> {
            var currentMissingKeys = store.syncStore().missingSyncKeys();
            if (currentMissingKeys.isEmpty()) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no missing sync keys remain, timeout check skipped");
                return List.<MissingDeviceSyncKey>of();
            }

            var actuallyMissing = currentMissingKeys.stream()
                    .filter(key -> store.syncStore().findWebAppStateKeyById(key.keyId()).isEmpty())
                    .toList();
            if (actuallyMissing.isEmpty()) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "all tracked missing keys have been received");
                return List.<MissingDeviceSyncKey>of();
            }

            var timeout = getTimeout();
            var now = Instant.now();
            var expired = actuallyMissing
                    .stream()
                    .filter(key -> Duration.between(key.timestamp(), now).compareTo(timeout) > 0)
                    .toList();
            if (expired.isEmpty()) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no expired missing sync keys");
                scheduleTimeoutCheck();
            }
            return expired;
        });

        if (expiredMissingSyncKeys.isEmpty()) {
            return;
        }

        if (Log.ERROR) LOGGER.log(Level.ERROR, "fatal sync error: timeout while waiting for {0} missing sync key(s)",
                expiredMissingSyncKeys.size());
        client.handleFailure(new WhatsAppWebAppStateSyncException.TimeoutWhileWaitingForMissingKey(
                expiredMissingSyncKeys.getFirst().keyId()));
    }

    /**
     * Raises a fatal {@link WhatsAppWebAppStateSyncException.MissingKeyOnAllDevices} when at
     * least one tracked missing key has now received a negative response from every asked
     * device.
     *
     * <p>This check is gated only on the predicate that every asked device has answered without
     * the key, which is a much faster unrecoverable signal than the multi-day timeout enforced
     * by {@link #checkForExpiredKeys()}. When no tracked key has yet exhausted its asked
     * devices the check rearms the wait-for-key timeout instead. The first such fully-exhausted
     * key is reported to {@link LinkedWhatsAppClient#handleFailure}.
     *
     * @implNote This implementation is invoked from a five-second grace period
     * ({@link #scheduleAllDevicesRespondedCheck()}) so a late positive response can still race
     * in and resolve the key before the fatal is raised.
     */
    private void checkForAllDevicesRespondedWithoutKey() {
        var missingOnAllDevices = coordinator.runLocked(() -> {
            var currentMissingKeys = store.syncStore().missingSyncKeys();
            if (currentMissingKeys.isEmpty()) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no missing sync keys remain, all-devices-responded check skipped");
                return List.<MissingDeviceSyncKey>of();
            }

            var result = currentMissingKeys.stream()
                    .filter(key -> store.syncStore().findMissingSyncKey(key.keyId()).isPresent())
                    .filter(MissingDeviceSyncKey::isMissingOnAllDevices)
                    .toList();
            if (result.isEmpty()) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no missing sync key has exhausted all device responses");
                scheduleTimeoutCheck();
            }
            return result;
        });

        if (missingOnAllDevices.isEmpty()) {
            return;
        }

        if (Log.ERROR) LOGGER.log(Level.ERROR, "fatal sync error: all asked devices responded without the requested sync key");
        client.handleFailure(new WhatsAppWebAppStateSyncException.MissingKeyOnAllDevices(
                missingOnAllDevices.getFirst().keyId()
        ));
    }

    /**
     * Reads the current {@code syncd_wait_for_key_timeout_days} value as a {@link Duration}.
     *
     * <p>Delegates to {@link SyncKeyUtils#getSyncdWaitForKeyTimeoutDays(ABPropsService)} and
     * converts the day count into a {@link Duration}.
     *
     * @return the configured wait-for-key timeout
     */
    private Duration getTimeout() {
        var days = SyncKeyUtils.getSyncdWaitForKeyTimeoutDays(abPropsService);
        return Duration.ofDays(days);
    }

    /**
     * Starts the periodic six-hour re-broadcast job that re-issues key requests for every
     * tracked missing key.
     *
     * <p>The job is idempotent: a second call while it is still pending is a no-op. Each tick
     * collects every tracked missing key id and, when at least one exists, re-broadcasts the
     * request through {@link MissingSyncKeyRequestService#reRequestMissingKeys(java.util.Collection)}.
     *
     * @implNote This implementation runs the re-broadcast directly on the job's own virtual thread
     * (its fixed-delay recurrence already isolates the peer-message fan-out from the other timers,
     * each of which owns a separate thread), then defers a 20-second reschedule of the wait-for-key
     * timeout to mirror the WA Web re-request task's sequel. Cobalt enforces the single-registration
     * guard explicitly because it has no equivalent of the WA Web task registry.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdRequestAllSyncdMissingKeysJob",
            exports = "requestAllSyncdMissingKeysJob",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSyncdHandleMissingKeys",
            exports = "requestAllMissingKeys",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public synchronized void startPeriodicReRequestJob() {
        if (shutdown || (reRequestJob != null && !reRequestJob.isCancelled())) {
            return;
        }

        reRequestJob = ScheduledTask.schedule(Duration.ofHours(RE_REQUEST_INTERVAL_HOURS), () -> {
            var missingKeys = store.syncStore().missingSyncKeys();
            var keyIds = missingKeys.stream()
                    .map(MissingDeviceSyncKey::keyId)
                    .toList();
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "requestAllMissingKeys: missing keys: [{0}]",
                        missingKeys.stream().map(MissingDeviceSyncKey::keyId).map(SyncKeyUtils::syncKeyIdToHex).toList());
            }
            if (keyIds.isEmpty()) {
                return;
            }
            requestService.reRequestMissingKeys(keyIds);
            ScheduledTask.scheduleDelayed(Duration.ofSeconds(20), this::scheduleTimeoutCheck);
        });
    }

    /**
     * Schedules the five-second grace period before
     * {@link #checkForAllDevicesRespondedWithoutKey()} fires.
     *
     * <p>Called by {@link SyncKeyRotationService#handleKeyShare(int, List)} when a
     * negative response from a peer brings a tracked key into the missing-on-every-asked-device
     * state. Any previously armed grace period is cancelled first so successive negative
     * responses do not stack independent timers.
     */
    public synchronized void scheduleAllDevicesRespondedCheck() {
        if (shutdown) {
            return;
        }
        if (allDevicesCheck != null) {
            allDevicesCheck.cancel();
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "scheduling 5-second grace period before missing key fatal");
        allDevicesCheck = ScheduledTask.scheduleDelayed(Duration.ofSeconds(5), this::checkForAllDevicesRespondedWithoutKey);
    }

    /**
     * Cancels the currently armed wait-for-key timeout, if any.
     *
     * <p>Disarms the timer without rearming it; the normal reschedule path goes through
     * {@link #scheduleTimeoutCheck()}.
     */
    public synchronized void cancel() {
        if (scheduledCheck != null) {
            scheduledCheck.cancel();
        }
    }

    /**
     * Cancels every armed timer and refuses any further scheduling.
     *
     * <p>Called by {@link com.github.auties00.cobalt.sync.WebAppStateService} on disconnect.
     * After this method returns every {@link #scheduleTimeoutCheck()},
     * {@link #scheduleAllDevicesRespondedCheck()}, and {@link #startPeriodicReRequestJob()} call is a
     * no-op, so a re-request job tick that fires concurrently cannot re-arm a timer past teardown.
     */
    public synchronized void shutdown() {
        shutdown = true;
        if (scheduledCheck != null) {
            scheduledCheck.cancel();
        }
        if (allDevicesCheck != null) {
            allDevicesCheck.cancel();
        }
        if (reRequestJob != null) {
            reRequestJob.cancel();
        }
    }
}
