package com.github.auties00.cobalt.device.adv;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceService;
import com.github.auties00.cobalt.device.timestamp.DeviceExpectedTsUtils;
import com.github.auties00.cobalt.exception.linked.WhatsAppAdvValidationException;
import com.github.auties00.cobalt.exception.WhatsAppException;
import com.github.auties00.cobalt.exception.linked.WhatsAppOwnDeviceListExpiredException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.device.info.DeviceList;
import com.github.auties00.cobalt.wire.linked.device.info.DeviceListBuilder;
import com.github.auties00.cobalt.wire.linked.device.sync.PendingDeviceSync;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncContext;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.util.ScheduledTask;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.AdvStoredTimestampExpiredEventBuilder;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps cached device lists fresh by periodically detecting expiration and triggering proactive
 * resyncs.
 *
 * <p>The checker is started once per session by {@link DeviceService#startAdvCheckScheduler()} and
 * wakes up every 24 hours, the cadence held by {@link #CHECK_INTERVAL}. On each tick it walks every
 * cached device list, classifies entries as expired (timestamp older than the
 * {@code num_days_key_index_list_expiration} AB prop) or close to expiration (within the
 * {@code num_days_before_device_expiry_check} warning window), tears down Signal sessions for
 * expired companion devices, rotates group sender keys for the affected users, queues a proactive
 * USync for every classified user, and logs the local user out when its own device list expired and
 * the {@code web_adv_logout_on_self_device_list_expired} AB prop is on.
 *
 * <p>Embedders that drive the full multi-device lifecycle leave the scheduler on via
 * {@link DeviceService#startAdvCheckScheduler()}; embedders that only use short-lived single-message
 * sessions can leave it off and accept stale device lists.
 */
@WhatsAppWebModule(moduleName = "WAWebAdvDeviceInfoCheckJob")
public final class DeviceADVChecker implements AutoCloseable {
    /**
     * The logger for {@link DeviceADVChecker}.
     */
    private static final System.Logger LOGGER = Log.get(DeviceADVChecker.class);

    /**
     * Holds the recurrence interval for the ADV check, fixed at 24 hours.
     *
     * <p>The interval pins WA Web's cadence of one tick per day and is not configurable.
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvDeviceInfoCheckJob",
            exports = "scheduleAdvDeviceInfoCheck",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static final Duration CHECK_INTERVAL = Duration.ofHours(24);

    /**
     * Holds the WhatsApp client used for store access and failure reporting.
     */
    private final LinkedWhatsAppClient client;

    /**
     * Holds the device service consulted for the last-check time and for queueing pending syncs.
     */
    private final DeviceService deviceService;

    /**
     * Holds the AB props service used to read expiration thresholds.
     */
    private final ABPropsService abPropsService;

    /**
     * Holds the WAM telemetry service used to commit ADV check events.
     */
    private final WamService wamService;

    /**
     * Holds the running periodic check, or {@code null} when the check job is stopped.
     */
    private volatile ScheduledTask checkJob;

    /**
     * Constructs an ADV check scheduler bound to the given collaborators.
     *
     * <p>The constructor only captures its collaborators; it does not schedule anything until
     * {@link #start()} is invoked.
     *
     * @param client         the WhatsApp client
     * @param deviceService  the device service
     * @param abPropsService the AB props service
     * @param wamService     the WAM telemetry service
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvDeviceInfoCheckJob",
            exports = "runAdvDeviceInfoCheck",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public DeviceADVChecker(LinkedWhatsAppClient client, DeviceService deviceService, ABPropsService abPropsService, WamService wamService) {
        this.client = client;
        this.deviceService = deviceService;
        this.abPropsService = abPropsService;
        this.wamService = wamService;
    }

    /**
     * Starts the ADV check scheduler and begins the 24-hour cadence.
     *
     * <p>The call is idempotent: a second invocation while the scheduler is already running is a
     * no-op. The first tick fires immediately when no previous check time is stored, but that tick
     * only records the timestamp without running the actual job (matching WA Web's first-run no-op
     * {@code Promise.resolve()}).
     *
     * @implNote
     * This implementation runs the periodic check as a single fixed-delay
     * {@link ScheduledTask#schedule(Duration, Duration, Runnable)} recurrence on its own virtual
     * thread. {@link #computeInitialDelay()} subtracts the elapsed time since the last check so a
     * session that reconnects within the {@link #CHECK_INTERVAL} window does not get a fresh full
     * delay.
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvDeviceInfoCheckJob",
            exports = "scheduleAdvDeviceInfoCheck",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public void start() {
        if (checkJob != null && !checkJob.isCancelled()) {
            return;
        }
        synchronized (this) {
            if (checkJob != null && !checkJob.isCancelled()) {
                return;
            }
            var initialDelay = computeInitialDelay();
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "adv check scheduler starting with initial delay of {0} seconds",
                        initialDelay.toSeconds());
            }
            checkJob = ScheduledTask.schedule(initialDelay, CHECK_INTERVAL, this::performCheck);
        }
    }

    /**
     * Computes the initial delay before the first scheduled tick.
     *
     * <p>The delay honours any check that already ran during a previous session: a session
     * reconnecting after twelve hours waits the remaining twelve rather than a full twenty-four. The
     * result is zero when no previous check exists or when the previous check is at least
     * {@link #CHECK_INTERVAL} old.
     *
     * @return the computed initial delay
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvDeviceInfoCheckJob",
            exports = "scheduleAdvDeviceInfoCheck",
            adaptation = WhatsAppAdaptation.DIRECT)
    private Duration computeInitialDelay() {
        var lastCheck = deviceService.lastAdvCheckTime();
        if (lastCheck.isEmpty()) {
            return Duration.ZERO;
        }

        var now = Instant.now();
        var timeSinceLastCheck = Duration.between(lastCheck.get(), now);

        if (timeSinceLastCheck.compareTo(CHECK_INTERVAL) >= 0) {
            return Duration.ZERO;
        }

        return CHECK_INTERVAL.minus(timeSinceLastCheck);
    }

    /**
     * Performs one ADV device info check tick.
     *
     * <p>On the first ever invocation (no previous check time) this records the timestamp and
     * returns immediately, matching WA Web's no-op first run. Subsequent ticks read the two
     * expiration AB props, classify cached lists via {@link #analyzeDeviceLists}, emit one
     * {@link com.github.auties00.cobalt.wire.wam.event.AdvStoredTimestampExpiredEvent} per expired list,
     * log the local user out when self-expired and the AB prop agrees, clear expired records, and
     * queue proactive USyncs for the rest. The check time is updated in a {@code finally} block so
     * it advances even when the body throws.
     *
     * @implNote
     * This implementation surfaces unexpected exceptions through
     * {@link LinkedWhatsAppClient#handleFailure(WhatsAppException)} as a
     * {@link WhatsAppAdvValidationException.MaintenanceJobFailed}
     * so the error pipeline routes them the same way as any other ADV failure. The WAM events fire
     * before the self-expired logout so telemetry is not lost on the logout path.
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvDeviceInfoCheckJob",
            exports = "runAdvDeviceInfoCheck",
            adaptation = WhatsAppAdaptation.DIRECT)
    private void performCheck() {
        var lastCheck = deviceService.lastAdvCheckTime();
        if (lastCheck.isEmpty()) {
            deviceService.updateAdvCheckTime();
            return;
        }

        try {
            var expiryDays = abPropsService.getInt(ABProp.NUM_DAYS_KEY_INDEX_LIST_EXPIRATION);
            var warningDays = abPropsService.getInt(ABProp.NUM_DAYS_BEFORE_DEVICE_EXPIRY_CHECK);
            var expiryThreshold = Duration.ofDays(expiryDays);
            var warningThreshold = Duration.ofDays(expiryDays - warningDays);

            var now = Instant.now();
            var myUserJid = client.store().accountStore().jid()
                    .map(Jid::toUserJid)
                    .orElse(null);

            var result = analyzeDeviceLists(
                    client.store().contactStore().deviceLists(),
                    now,
                    expiryThreshold,
                    warningThreshold,
                    lastCheck.get(),
                    myUserJid
            );

            sendAdvStoredTimestampExpiredEvents(result.expiredLists(), now, expiryThreshold);

            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "adv check tick classified {0} expired and {1} device lists needing sync",
                        result.expiredLists().size(), result.jidsNeedingSync().size());
            }

            if (result.selfExpired() && shouldLogoutOnSelfExpired()) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "own device list expired, triggering logout");
                client.handleFailure(new WhatsAppOwnDeviceListExpiredException());
                return;
            }

            for (var expiredList : result.expiredLists()) {
                clearExpiredDeviceRecord(expiredList);
            }

            if (!result.jidsNeedingSync().isEmpty()) {
                scheduleProactiveSync(result.jidsNeedingSync());
            }
        } catch (Exception e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "adv check tick failed", e);
            client.handleFailure(new WhatsAppAdvValidationException.MaintenanceJobFailed(e));
        } finally {
            deviceService.updateAdvCheckTime();
        }
    }

    /**
     * Classifies cached device lists as expired or close to expiration.
     *
     * <p>This method skips deleted lists and primary-only lists (which have no companion to chase).
     * For each remaining list it consults {@link DeviceExpectedTsUtils#isDeviceListStale} for
     * expiration and {@link DeviceExpectedTsUtils#isDeviceListCloseToExpiration} for the warning
     * window: expired lists are added to the result and to the sync queue, while
     * close-to-expiration lists are only queued. It is package-private so unit tests can pass a
     * synthetic {@code now} and {@code lastCheck} without driving the real scheduler.
     *
     * @param deviceLists      the device lists to analyze
     * @param now              the reference instant for "now"
     * @param expiryThreshold  the full-expiration threshold (from the
     *                         {@code num_days_key_index_list_expiration} AB prop)
     * @param warningThreshold the close-to-expiration warning window
     * @param lastCheck        the time the last ADV check ran
     * @param myUserJid        the local user's JID, or {@code null} when no self JID is set
     * @return the analysis result
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvDeviceInfoCheckJob",
            exports = "runAdvDeviceInfoCheck",
            adaptation = WhatsAppAdaptation.DIRECT)
    AnalysisResult analyzeDeviceLists(
            Iterable<DeviceList> deviceLists,
            Instant now,
            Duration expiryThreshold,
            Duration warningThreshold,
            Instant lastCheck,
            Jid myUserJid
    ) {
        var selfExpired = false;
        var expiredLists = new ArrayList<DeviceList>();
        var jidsNeedingSync = new ArrayList<Jid>();

        for (var deviceList : deviceLists) {
            if (deviceList.deleted() || isPrimaryOnly(deviceList)) {
                continue;
            }

            if (DeviceExpectedTsUtils.isDeviceListStale(deviceList, now, expiryThreshold, lastCheck)) {
                expiredLists.add(deviceList);
                if (deviceList.userJid().equals(myUserJid)) {
                    selfExpired = true;
                }
                jidsNeedingSync.add(deviceList.userJid());
            } else if (DeviceExpectedTsUtils.isDeviceListCloseToExpiration(deviceList, now, warningThreshold)) {
                jidsNeedingSync.add(deviceList.userJid());
            }
        }

        return new AnalysisResult(selfExpired, expiredLists, jidsNeedingSync);
    }

    /**
     * Returns whether a device list contains only the primary device.
     *
     * <p>Primary-only users are exempt from the staleness check because they have no companion
     * device to refresh.
     *
     * @param deviceList the device list to check
     * @return {@code true} when the list has exactly one device and that device is primary
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvDeviceInfoCheckJob",
            exports = "runAdvDeviceInfoCheck",
            adaptation = WhatsAppAdaptation.DIRECT)
    private boolean isPrimaryOnly(DeviceList deviceList) {
        var devices = deviceList.devices();
        return devices.size() == 1 && devices.getFirst().isPrimary();
    }

    /**
     * Returns whether the local user should be logged out when its own device list has expired.
     *
     * <p>The {@code web_adv_logout_on_self_device_list_expired} AB prop acts as a server-side kill
     * switch and is consulted by {@link #performCheck()} immediately after the {@code selfExpired}
     * flag is set.
     *
     * @return {@code true} when the AB prop is on
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvDeviceInfoCheckJob",
            exports = "runAdvDeviceInfoCheck",
            adaptation = WhatsAppAdaptation.DIRECT)
    private boolean shouldLogoutOnSelfExpired() {
        return abPropsService.getBool(ABProp.WEB_ADV_LOGOUT_ON_SELF_DEVICE_LIST_EXPIRED);
    }

    /**
     * Clears an expired device record.
     *
     * <p>This tears down Signal sessions for every companion device, rotates group sender keys for
     * the user, and replaces the cached list with a deleted marker. The {@code deletedChangedToHost}
     * marker stays unset on this path because the call site carries no account-type arguments; it
     * only fires on the coexistence transition path.
     *
     * @param deviceList the expired device list to clear
     */
    private void clearExpiredDeviceRecord(DeviceList deviceList) {
        var userJid = deviceList.userJid();

        deviceList.devices()
                .stream()
                .filter(device -> !device.isPrimary())
                .map(device -> device.toDeviceJid(userJid.user(), userJid.server()))
                .forEach(client.store().signalStore()::cleanupSignalSessions);

        client.store().signalStore().markKeyRotation(userJid);

        var deletedList = new DeviceListBuilder()
                .userJid(userJid)
                .deleted(true)
                .build();
        client.store().contactStore().addDeviceList(deletedList);

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "marked device list as deleted for {0}, cleaned up {1} companion devices",
                    userJid, deviceList.devices().size() - 1);
        }
    }

    /**
     * Emits one {@link com.github.auties00.cobalt.wire.wam.event.AdvStoredTimestampExpiredEvent} WAM
     * event per expired device list, reporting overshoot in hours.
     *
     * <p>The overshoot of each list is computed as {@code now - (timestamp + expiryThreshold)};
     * lists whose overshoot is negative are skipped because they are not yet past the cutoff,
     * matching WA Web's inline {@code if (!(r < 0))} guard. The events drive the
     * {@code advExpireTimeInHours} server-side dashboard.
     *
     * @implNote
     * This implementation rounds overshoot seconds to the nearest hour via {@link Math#round(double)}
     * and forwards each event through {@link WamService#commit} so the broader WAM batching policy
     * decides when the events ship.
     *
     * @param expiredLists    the device lists classified as expired
     * @param now             the reference instant used to compute overshoot
     * @param expiryThreshold the configured key-index-list expiration threshold
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvDeviceInfoCheckJob",
            exports = "runAdvDeviceInfoCheck",
            adaptation = WhatsAppAdaptation.DIRECT)
    private void sendAdvStoredTimestampExpiredEvents(List<DeviceList> expiredLists, Instant now, Duration expiryThreshold) {
        if (expiredLists.isEmpty()) {
            return;
        }
        for (var expiredList : expiredLists) {
            var overshoot = Duration.between(expiredList.timestamp().plus(expiryThreshold), now);
            if (overshoot.isNegative()) {
                continue;
            }
            var hours = Math.toIntExact(Math.round(overshoot.toSeconds() / 3600.0));
            wamService.commit(new AdvStoredTimestampExpiredEventBuilder()
                    .advExpireTimeInHours(hours)
                    .build());
        }
    }

    /**
     * Queues a proactive USync for the given user JIDs through the pending device sync pipeline.
     *
     * <p>This is called at the end of every tick with the union of expired and close-to-expiration
     * user JIDs; the pipeline coalesces and flushes them on the next eligible event-loop window.
     *
     * @param jids the JIDs to enqueue
     */
    private void scheduleProactiveSync(List<Jid> jids) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "proactively syncing {0} device lists", jids.size());
        var pendingSync = PendingDeviceSync.of(jids, UsyncContext.BACKGROUND.wireValue());
        client.store().syncStore().addPendingDeviceSync(pendingSync);
        deviceService.retryPendingSyncs();
    }

    /**
     * Stops the ADV check job and cancels any pending check.
     *
     * <p>The call is invoked on client teardown and is idempotent: a second call once the job is
     * already cancelled is a no-op. Cancelling wakes a pending check so it never fires and interrupts
     * one already running, so it is safe to call from within a check tick.
     */
    @Override
    public void close() {
        synchronized (this) {
            if (checkJob != null) {
                checkJob.cancel();
                checkJob = null;
            }
        }
    }

    /**
     * Bundles the output of {@link #analyzeDeviceLists}: the self-expiration flag, the expired
     * lists, and the union of user JIDs needing a proactive sync.
     *
     * <p>This package-private container lets the staleness analysis stay pure (input in, output out)
     * so unit tests can assert against the classification without driving the scheduler's side
     * effects.
     *
     * @param selfExpired     {@code true} when the local user's own device list is expired
     * @param expiredLists    the device lists classified as expired
     * @param jidsNeedingSync the JIDs needing proactive device sync (expired plus
     *                        close-to-expiration)
     */
    record AnalysisResult(
            boolean selfExpired,
            List<DeviceList> expiredLists,
            List<Jid> jidsNeedingSync
    ) {

    }
}
