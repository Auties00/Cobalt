package com.github.auties00.cobalt.device.timestamp;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.time.Instant;
import java.util.Optional;

/**
 * Carries the three expected-timestamp tracking fields stamped onto a
 * {@link com.github.auties00.cobalt.wire.linked.device.info.DeviceList}.
 *
 * <p>This immutable tuple groups the next expected device-list version, the
 * timestamp of the last ADV device-info job that observed it, and the instant
 * at which it was last modified. It is produced by
 * {@link DeviceExpectedTsUtils#computeNewExpectedTimestamp(Instant, Instant, Instant, Instant, Instant, Instant)}
 * and
 * {@link DeviceExpectedTsUtils#computeExpectedTimestampForDeviceRecord(Instant, com.github.auties00.cobalt.wire.linked.device.info.DeviceList, Instant)},
 * then consumed when folding a USync response into the local device-list cache.
 * The three fields together let WhatsApp's ADV pipeline detect a device list
 * whose dhash still matches the server's but whose contents are known by the
 * server to be obsolete.
 *
 * @implNote This implementation models the WA Web result object as a Java class
 *           with {@link Optional}-returning accessors rather than a record so
 *           the field names match the long-form Cobalt naming used by the
 *           surrounding API. The {@code @ProtobufMessage} machinery is not
 *           needed because this type never crosses the wire; it is purely an
 *           internal transport tuple.
 */
@WhatsAppWebModule(moduleName = "WAWebAdvExpectedTsApi")
public final class ExpectedTimestampResult {
    /**
     * Holds the timestamp the server has declared as the next expected
     * version, or {@code null} when no expectation is currently tracked.
     */
    private final Instant expectedTimestamp;

    /**
     * Holds the {@link Instant} of the last ADV device job that observed
     * {@link #expectedTimestamp}, or {@code null} when unset.
     */
    private final Instant expectedTimestampLastDeviceJobTimestamp;

    /**
     * Holds the {@link Instant} at which {@link #expectedTimestamp} was last
     * modified, or {@code null} when unset.
     */
    private final Instant expectedTimestampUpdateTimestamp;

    /**
     * Constructs a new tracking tuple from the three fields.
     *
     * <p>Production code normally obtains an instance through the
     * {@link DeviceExpectedTsUtils} helpers rather than building the tuple by
     * hand. Each argument may be {@code null} to leave the corresponding field
     * untracked.
     *
     * @param expectedTimestamp                       the next expected
     *                                                version, or
     *                                                {@code null}
     * @param expectedTimestampLastDeviceJobTimestamp the last ADV job
     *                                                timestamp, or
     *                                                {@code null}
     * @param expectedTimestampUpdateTimestamp        the last update
     *                                                instant, or
     *                                                {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvExpectedTsApi",
            exports = "computeNewExpectedTs",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public ExpectedTimestampResult(
            Instant expectedTimestamp,
            Instant expectedTimestampLastDeviceJobTimestamp,
            Instant expectedTimestampUpdateTimestamp
    ) {
        this.expectedTimestamp = expectedTimestamp;
        this.expectedTimestampLastDeviceJobTimestamp = expectedTimestampLastDeviceJobTimestamp;
        this.expectedTimestampUpdateTimestamp = expectedTimestampUpdateTimestamp;
    }

    /**
     * Returns the next expected device-list version.
     *
     * @return the expected timestamp, or {@link Optional#empty()} when unset
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvExpectedTsApi",
            exports = "expectedTs",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<Instant> expectedTimestamp() {
        return Optional.ofNullable(expectedTimestamp);
    }

    /**
     * Returns the timestamp of the last ADV device-info job that observed
     * the cached expectation.
     *
     * <p>Read by
     * {@link DeviceExpectedTsUtils#shouldClearExpectedTimestamp(Instant, Instant, com.github.auties00.cobalt.wire.linked.device.info.DeviceList, Instant)}
     * to decide whether the cached expectation has already been superseded.
     *
     * @return the last job timestamp, or {@link Optional#empty()} when unset
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvExpectedTsApi",
            exports = "expectedTsLastDeviceJobTs",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<Instant> expectedTimestampLastDeviceJobTimestamp() {
        return Optional.ofNullable(expectedTimestampLastDeviceJobTimestamp);
    }

    /**
     * Returns the {@link Instant} at which the cached expectation was last
     * modified.
     *
     * <p>Combined with the 25-hour grace window in
     * {@link DeviceExpectedTsUtils#isDeviceListStale(com.github.auties00.cobalt.wire.linked.device.info.DeviceList, Instant, java.time.Duration, Instant)}
     * to decide when to re-query the device list.
     *
     * @return the update instant, or {@link Optional#empty()} when unset
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvExpectedTsApi",
            exports = "expectedTsUpdateTs",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<Instant> expectedTimestampUpdateTimestamp() {
        return Optional.ofNullable(expectedTimestampUpdateTimestamp);
    }
}
