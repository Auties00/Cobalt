package com.github.auties00.cobalt.device.timestamp;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.device.info.DeviceList;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Implements the expected-timestamp staleness logic of the ADV device-info job.
 *
 * <p>This utility holds the stateless predicates that track the three
 * expected-timestamp fields stamped onto each {@link DeviceList}: the next
 * expected version, the timestamp of the last ADV device-info job that
 * observed it, and the instant at which it was last modified. Together these
 * let Cobalt detect a device list whose dhash still matches the server's but
 * is known by the server to be obsolete, so the daily ADV scheduler can decide
 * whether to re-query a user's device list and the USync handling path can keep
 * the cached expectation consistent with the latest server signal.
 *
 * @implNote This implementation duplicates the 25-hour threshold inline because
 *           Cobalt has no equivalent of the {@code WATimeUtils.HOUR_SECONDS}
 *           constant the JS module reuses.
 */
@WhatsAppWebModule(moduleName = "WAWebAdvExpectedTsApi")
@WhatsAppWebModule(moduleName = "WAWebAdvDeviceInfoCheckJob")
public final class DeviceExpectedTsUtils {
    /**
     * The logger for {@link DeviceExpectedTsUtils}.
     */
    private static final System.Logger LOGGER = Log.get(DeviceExpectedTsUtils.class);

    /**
     * Holds the twenty-five hour grace window the daily ADV job allows before
     * it treats an expected-timestamp record as stale.
     *
     * <p>One hour of slack is added on top of the nominal 24-hour scheduler
     * tick so a slightly delayed job run does not flag every record as stale.
     * Read by {@link #isDeviceListStale(DeviceList, Instant, Duration, Instant)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvDeviceInfoCheckJob",
            exports = "runAdvDeviceInfoCheck",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final Duration EXPECTED_TIMESTAMP_UPDATE_THRESHOLD = Duration.ofHours(25);

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private DeviceExpectedTsUtils() {
        throw new AssertionError();
    }

    /**
     * Decides whether to clear the cached expected-timestamp triple after
     * folding in a USync response.
     *
     * <p>Returns {@code true} either when the new server timestamp has caught
     * up to the cached expectation (so the expectation is no longer
     * interesting), or when the incoming expectation matches the cached one but
     * a newer ADV job has run since the cached observation (so the expectation
     * is now redundant); returns {@code false} otherwise. A {@code null} cached
     * list, a deleted cached list, or a cached list with no
     * {@link DeviceList#expectedTimestamp()} short-circuits to {@code false}. A
     * {@code null} {@code lastADVCheckTime} is treated as older than any cached
     * last-job timestamp.
     *
     * @implNote This implementation treats a {@code null}
     *           {@code expectedTsLastDeviceJobTs} as "older than any timestamp"
     *           to match the JS {@code ==null || r > n.expectedTsLastDeviceJobTs}
     *           comparison.
     * @param incomingTimestamp         the {@code ts} attribute from the
     *                                  server response
     * @param incomingExpectedTimestamp the {@code expected_ts} attribute from
     *                                  the response, or {@code null}
     * @param cachedList                the currently cached device list, or
     *                                  {@code null}
     * @param lastADVCheckTime          the timestamp of the most recent ADV
     *                                  device-info job run, or {@code null}
     * @return {@code true} if the cached expected-timestamp triple should be
     *         cleared
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvExpectedTsApi",
            exports = "shouldClearExpectedTs",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static boolean shouldClearExpectedTimestamp(
            Instant incomingTimestamp,
            Instant incomingExpectedTimestamp,
            DeviceList cachedList,
            Instant lastADVCheckTime
    ) {
        if (cachedList == null || cachedList.deleted() || cachedList.expectedTimestamp() == null) {
            return false;
        }

        var cachedExpectedTimestamp = cachedList.expectedTimestamp();

        if (!incomingTimestamp.isBefore(cachedExpectedTimestamp)) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "clearing expected timestamp for {0}: incoming timestamp caught up to cached expectation", cachedList.userJid());
            }
            return true;
        }

        if (incomingExpectedTimestamp == null || !incomingExpectedTimestamp.equals(cachedExpectedTimestamp) || lastADVCheckTime == null) {
            return false;
        }

        var cachedLastJobTimestamp = cachedList.expectedTimestampLastDeviceJobTimestamp();
        var clear = cachedLastJobTimestamp == null || lastADVCheckTime.isAfter(cachedLastJobTimestamp);
        if (Log.DEBUG && clear) {
            LOGGER.log(Level.DEBUG, "clearing expected timestamp for {0}: newer adv job observed cached expectation", cachedList.userJid());
        }
        return clear;
    }

    /**
     * Compares two expected-timestamp {@link Instant} values with
     * {@code null}-tolerant equality.
     *
     * <p>Returns {@code true} when the two values differ. Two {@code null}
     * values count as unchanged, and a {@code null} paired with a
     * non-{@code null} counts as changed.
     *
     * @param oldExpectedTimestamp the previous expected timestamp, or
     *                             {@code null}
     * @param newExpectedTimestamp the new expected timestamp, or {@code null}
     * @return {@code true} if the two values differ
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvExpectedTsApi",
            exports = "computeNewExpectedTs",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static boolean hasExpectedTimestampChanged(Instant oldExpectedTimestamp, Instant newExpectedTimestamp) {
        if (oldExpectedTimestamp == null && newExpectedTimestamp == null) {
            return false;
        }
        if (oldExpectedTimestamp == null || newExpectedTimestamp == null) {
            return true;
        }
        return !oldExpectedTimestamp.equals(newExpectedTimestamp);
    }

    /**
     * Computes the updated expected-timestamp triple from a cached
     * {@link DeviceList}.
     *
     * <p>Reads the current expected-timestamp triple from the cached list when
     * it is present and not deleted, then forwards those values to
     * {@link #computeNewExpectedTimestamp(Instant, Instant, Instant, Instant, Instant, Instant)}.
     * A {@code null} cached list or a cached list missing a {@code timestamp}
     * short-circuits to a blank tuple, and a deleted cached record contributes
     * nothing to the carry forward. Callers fold a fresh USync response into
     * the cache through this helper before persisting the device record.
     *
     * @param incomingTimestamp the {@code ts} attribute from the server
     *                          response
     * @param cachedList        the currently cached device list, or
     *                          {@code null}
     * @param lastADVCheckTime  the timestamp of the most recent ADV
     *                          device-info job run, or {@code null}
     * @return the updated expected-timestamp triple
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvExpectedTsApi",
            exports = "computeExpectedTsForDeviceRecord",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static ExpectedTimestampResult computeExpectedTimestampForDeviceRecord(
            Instant incomingTimestamp,
            DeviceList cachedList,
            Instant lastADVCheckTime
    ) {
        if (cachedList == null) {
            return new ExpectedTimestampResult(null, null, null);
        }

        var currentTimestamp = cachedList.timestamp();
        if (currentTimestamp == null) {
            return new ExpectedTimestampResult(null, null, null);
        }
        Instant currentExpectedTimestamp = null;
        Instant currentExpectedTimestampLastDeviceJobTimestamp = null;
        Instant currentExpectedTimestampUpdateTimestamp = null;

        if (!cachedList.deleted()) {
            currentExpectedTimestamp = cachedList.expectedTimestamp();
            currentExpectedTimestampLastDeviceJobTimestamp = cachedList.expectedTimestampLastDeviceJobTimestamp();
            currentExpectedTimestampUpdateTimestamp = cachedList.expectedTimestampUpdateTimestamp();
        }

        return computeNewExpectedTimestamp(
                incomingTimestamp,
                currentTimestamp,
                lastADVCheckTime,
                currentExpectedTimestamp,
                currentExpectedTimestampLastDeviceJobTimestamp,
                currentExpectedTimestampUpdateTimestamp
        );
    }

    /**
     * Computes the updated expected-timestamp triple from the six raw inputs.
     *
     * <p>When neither the current timestamp nor the current expectation has
     * fallen behind the incoming timestamp the cached triple is returned
     * unchanged. Otherwise the expectation moves to the incoming timestamp, the
     * last-job timestamp moves to {@code lastADVCheckTime}, and the
     * update-instant refreshes to {@link Instant#now()} only when a new
     * expectation target is set, not when the existing target is reaffirmed.
     * Production callers normally reach this through
     * {@link #computeExpectedTimestampForDeviceRecord(Instant, DeviceList, Instant)},
     * which marshals the inputs from a cached {@link DeviceList}.
     *
     * @param incomingTimestamp                              the {@code ts}
     *                                                       attribute from
     *                                                       the response
     * @param currentTimestamp                               the cached
     *                                                       record's
     *                                                       {@code timestamp}
     * @param lastADVCheckTime                               the most recent
     *                                                       ADV check time,
     *                                                       or {@code null}
     * @param currentExpectedTimestamp                       the currently
     *                                                       cached
     *                                                       expectation, or
     *                                                       {@code null}
     * @param currentExpectedTimestampLastDeviceJobTimestamp the currently
     *                                                       cached last-job
     *                                                       timestamp, or
     *                                                       {@code null}
     * @param currentExpectedTimestampUpdateTimestamp        the currently
     *                                                       cached
     *                                                       update-instant,
     *                                                       or {@code null}
     * @return the updated expected-timestamp triple
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvExpectedTsApi",
            exports = "computeNewExpectedTs",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static ExpectedTimestampResult computeNewExpectedTimestamp(
            Instant incomingTimestamp,
            Instant currentTimestamp,
            Instant lastADVCheckTime,
            Instant currentExpectedTimestamp,
            Instant currentExpectedTimestampLastDeviceJobTimestamp,
            Instant currentExpectedTimestampUpdateTimestamp
    ) {
        var result = new ExpectedTimestampResult(
                currentExpectedTimestamp,
                currentExpectedTimestampLastDeviceJobTimestamp,
                currentExpectedTimestampUpdateTimestamp
        );

        if (!currentTimestamp.isBefore(incomingTimestamp)) {
            return result;
        }

        if (currentExpectedTimestamp != null && !currentExpectedTimestamp.isBefore(incomingTimestamp)) {
            return result;
        }

        var newExpectedTimestampUpdateTimestamp = currentExpectedTimestampUpdateTimestamp;
        if (currentExpectedTimestamp == null || !currentTimestamp.isBefore(currentExpectedTimestamp)) {
            newExpectedTimestampUpdateTimestamp = Instant.now();
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "expected timestamp advanced to incoming server timestamp");
        return new ExpectedTimestampResult(incomingTimestamp, lastADVCheckTime, newExpectedTimestampUpdateTimestamp);
    }

    /**
     * Tests whether a cached {@link DeviceList} has aged out and must be
     * re-queried.
     *
     * <p>Returns {@code true} when the raw age of the list beats the configured
     * expiry threshold, or when the expected-timestamp tracking record is older
     * than the 25-hour grace window held in
     * {@code EXPECTED_TIMESTAMP_UPDATE_THRESHOLD} and a new ADV check has run
     * since the cached observation. A {@code null}
     * {@link DeviceList#expectedTimestampUpdateTimestamp()} short-circuits the
     * second branch to {@code false}. Invoked by the daily ADV device-info
     * scheduler to identify users whose device lists are considered expired.
     *
     * @implNote This implementation collapses the JS {@code !=r} job-comparison
     *           into {@link Objects#equals(Object, Object)} so a {@code null}
     *           {@code lastADVCheckTime} compared against a {@code null}
     *           {@code expectedTsLastDeviceJobTs} reports "matching" rather
     *           than "diverging", matching the JS truthiness semantics of
     *           {@code !==}.
     * @param deviceList       the device list to check
     * @param currentTime      the current wall-clock time
     * @param expiryThreshold  the {@code num_days_key_index_list_expiration}
     *                         budget converted to a {@link Duration}
     * @param lastADVCheckTime the most recent ADV check time, or
     *                         {@code null}
     * @return {@code true} when the list should be re-queried
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvDeviceInfoCheckJob",
            exports = "runAdvDeviceInfoCheck",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static boolean isDeviceListStale(
            DeviceList deviceList,
            Instant currentTime,
            Duration expiryThreshold,
            Instant lastADVCheckTime
    ) {
        var timestamp = deviceList.timestamp();

        if (Duration.between(timestamp, currentTime).compareTo(expiryThreshold) >= 0) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "device list stale for {0}: age exceeds expiry threshold", deviceList.userJid());
            return true;
        }

        var expectedTimestampUpdateTimestamp = deviceList.expectedTimestampUpdateTimestamp();
        if (expectedTimestampUpdateTimestamp == null) {
            return false;
        }

        var elapsedSinceUpdate = Duration.between(expectedTimestampUpdateTimestamp, currentTime);
        if (elapsedSinceUpdate.compareTo(EXPECTED_TIMESTAMP_UPDATE_THRESHOLD) < 0) {
            return false;
        }

        var expectedTimestampLastDeviceJobTimestamp = deviceList.expectedTimestampLastDeviceJobTimestamp();
        var stale = !Objects.equals(expectedTimestampLastDeviceJobTimestamp, lastADVCheckTime);
        if (Log.DEBUG && stale) {
            LOGGER.log(Level.DEBUG, "device list stale for {0}: expected timestamp record exceeded grace window", deviceList.userJid());
        }
        return stale;
    }

    /**
     * Tests whether a cached {@link DeviceList} is close enough to expiring
     * to warrant a pre-emptive refresh.
     *
     * <p>Returns {@code true} when the raw age of the list exceeds the warning
     * budget, or when the server has signalled that a newer device list exists
     * via an {@link DeviceList#expectedTimestamp()} that sits ahead of the
     * cached {@link DeviceList#timestamp()}. Companion to
     * {@link #isDeviceListStale(DeviceList, Instant, Duration, Instant)} used by
     * the daily ADV scheduler to populate its close-to-expiration bucket.
     *
     * @implNote The warning budget is supplied by callers; Cobalt currently
     *           passes the expiry threshold minus the
     *           {@code num_days_before_device_expiry_check} margin.
     * @param deviceList       the device list to check
     * @param currentTime      the current wall-clock time
     * @param warningThreshold the budget after which a warning should fire
     * @return {@code true} when the device list is close to expiration
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvDeviceInfoCheckJob",
            exports = "runAdvDeviceInfoCheck",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static boolean isDeviceListCloseToExpiration(
            DeviceList deviceList,
            Instant currentTime,
            Duration warningThreshold
    ) {
        var timestamp = deviceList.timestamp();

        if (Duration.between(timestamp, currentTime).compareTo(warningThreshold) >= 0) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "device list close to expiration for {0}: age exceeds warning threshold", deviceList.userJid());
            return true;
        }

        var expectedTimestamp = deviceList.expectedTimestamp();
        var close = expectedTimestamp != null && expectedTimestamp.isAfter(timestamp);
        if (Log.DEBUG && close) {
            LOGGER.log(Level.DEBUG, "device list close to expiration for {0}: server signalled newer expectation", deviceList.userJid());
        }
        return close;
    }
}
