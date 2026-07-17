package com.github.auties00.cobalt.device.timestamp;

import com.github.auties00.cobalt.wire.linked.device.info.DeviceList;
import com.github.auties00.cobalt.wire.linked.device.info.DeviceListBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the pure timestamp pipeline of {@link DeviceExpectedTsUtils}: the clear, change, recompute,
 * and staleness predicates over the four tracked device-list timestamps. Each nested suite drives one helper
 * with deterministic synthetic {@link Instant}s anchored at a fixed date, so no captured fixtures are needed.
 */
@DisplayName("DeviceExpectedTsUtils")
class DeviceExpectedTsUtilsTest {
    private static final Jid USER_JID = Jid.of("19254863482@s.whatsapp.net");

    private static DeviceList list(Instant timestamp, Instant expected, Instant lastJob, Instant updated, boolean deleted) {
        return new DeviceListBuilder()
                .userJid(USER_JID)
                .devices(List.of())
                .timestamp(timestamp)
                .deleted(deleted)
                .expectedTimestamp(expected)
                .expectedTimestampLastDeviceJobTimestamp(lastJob)
                .expectedTimestampUpdateTimestamp(updated)
                .currentIndex(0)
                .validIndexes(new LinkedHashSet<>())
                .build();
    }

    @Nested
    @DisplayName("shouldClearExpectedTimestamp")
    class ShouldClearExpectedTimestamp {
        private final Instant t0 = Instant.parse("2026-01-01T00:00:00Z");
        private final Instant t1 = t0.plus(Duration.ofMinutes(5));
        private final Instant t2 = t0.plus(Duration.ofMinutes(10));

        @Test
        @DisplayName("returns false when the cached list is null")
        void nullCachedList() {
            assertFalse(DeviceExpectedTsUtils.shouldClearExpectedTimestamp(t1, t2, null, t0));
        }

        @Test
        @DisplayName("returns false when the cached list is deleted")
        void deletedCachedList() {
            var cached = list(t0, t1, null, null, true);
            assertFalse(DeviceExpectedTsUtils.shouldClearExpectedTimestamp(t1, t2, cached, t0));
        }

        @Test
        @DisplayName("returns false when the cached list has no expected timestamp")
        void noCachedExpected() {
            var cached = list(t0, null, null, null, false);
            assertFalse(DeviceExpectedTsUtils.shouldClearExpectedTimestamp(t1, t2, cached, t0));
        }

        @Test
        @DisplayName("returns true when the server timestamp has caught up to the cached expectation")
        void caughtUp() {
            var cached = list(t0, t1, null, null, false);
            assertTrue(DeviceExpectedTsUtils.shouldClearExpectedTimestamp(t1, null, cached, t0));
        }

        @Test
        @DisplayName("returns true when the server timestamp has overtaken the cached expectation")
        void overtaken() {
            var cached = list(t0, t1, null, null, false);
            assertTrue(DeviceExpectedTsUtils.shouldClearExpectedTimestamp(t2, null, cached, t0));
        }

        @Test
        @DisplayName("returns false when the incoming expectation differs from the cached one")
        void expectationChanged() {
            var cached = list(t0, t1, null, null, false);
            assertFalse(DeviceExpectedTsUtils.shouldClearExpectedTimestamp(t0, t2, cached, t1));
        }

        @Test
        @DisplayName("returns false when the incoming expectation matches but no ADV check time is given")
        void noAdvCheck() {
            var cached = list(t0, t1, null, null, false);
            assertFalse(DeviceExpectedTsUtils.shouldClearExpectedTimestamp(t0, t1, cached, null));
        }

        @Test
        @DisplayName("returns true when the incoming expectation matches and a newer ADV job ran")
        void newerAdvCheck() {
            var cached = list(t0, t1, t0, null, false);
            assertTrue(DeviceExpectedTsUtils.shouldClearExpectedTimestamp(t0, t1, cached, t1));
        }

        @Test
        @DisplayName("returns false when the incoming expectation matches but the ADV job has not advanced")
        void sameAdvCheck() {
            var cached = list(t0, t1, t1, null, false);
            assertFalse(DeviceExpectedTsUtils.shouldClearExpectedTimestamp(t0, t1, cached, t1));
        }
    }

    @Nested
    @DisplayName("hasExpectedTimestampChanged")
    class HasExpectedTimestampChanged {
        private final Instant a = Instant.parse("2026-01-01T00:00:00Z");
        private final Instant b = Instant.parse("2026-01-02T00:00:00Z");

        @Test
        @DisplayName("both null counts as unchanged")
        void bothNull() {
            assertFalse(DeviceExpectedTsUtils.hasExpectedTimestampChanged(null, null));
        }

        @Test
        @DisplayName("null vs non-null counts as changed")
        void asymmetricNull() {
            assertTrue(DeviceExpectedTsUtils.hasExpectedTimestampChanged(null, a));
            assertTrue(DeviceExpectedTsUtils.hasExpectedTimestampChanged(a, null));
        }

        @Test
        @DisplayName("equal instants count as unchanged")
        void equalInstants() {
            assertFalse(DeviceExpectedTsUtils.hasExpectedTimestampChanged(a, Instant.ofEpochSecond(a.getEpochSecond())));
        }

        @Test
        @DisplayName("different instants count as changed")
        void differentInstants() {
            assertTrue(DeviceExpectedTsUtils.hasExpectedTimestampChanged(a, b));
        }
    }

    @Nested
    @DisplayName("computeExpectedTimestampForDeviceRecord")
    class ComputeExpectedTimestampForDeviceRecord {
        private final Instant t0 = Instant.parse("2026-01-01T00:00:00Z");
        private final Instant t1 = t0.plus(Duration.ofMinutes(5));

        @Test
        @DisplayName("returns blank result when the cached list is null")
        void nullCachedList() {
            var result = DeviceExpectedTsUtils.computeExpectedTimestampForDeviceRecord(t1, null, t0);
            assertTrue(result.expectedTimestamp().isEmpty());
            assertTrue(result.expectedTimestampLastDeviceJobTimestamp().isEmpty());
            assertTrue(result.expectedTimestampUpdateTimestamp().isEmpty());
        }

        @Test
        @DisplayName("does not propagate fields from a deleted cached list")
        void deletedCachedList() {
            var cached = list(t0, t1, t0, t0, true);
            var result = DeviceExpectedTsUtils.computeExpectedTimestampForDeviceRecord(t1, cached, t0);
            assertEquals(t1, result.expectedTimestamp().orElseThrow());
            assertEquals(t0, result.expectedTimestampLastDeviceJobTimestamp().orElseThrow());
            assertTrue(result.expectedTimestampUpdateTimestamp().isPresent());
        }
    }

    @Nested
    @DisplayName("computeNewExpectedTimestamp")
    class ComputeNewExpectedTimestamp {
        private final Instant t0 = Instant.parse("2026-01-01T00:00:00Z");
        private final Instant t1 = t0.plus(Duration.ofMinutes(5));

        @Test
        @DisplayName("preserves current values when the current timestamp already meets the incoming one")
        void currentMeetsIncoming() {
            var result = DeviceExpectedTsUtils.computeNewExpectedTimestamp(t0, t1, t0, t1, t0, t0);
            assertEquals(t1, result.expectedTimestamp().orElseThrow());
            assertEquals(t0, result.expectedTimestampLastDeviceJobTimestamp().orElseThrow());
            assertEquals(t0, result.expectedTimestampUpdateTimestamp().orElseThrow());
        }

        @Test
        @DisplayName("preserves current values when the current expected already meets the incoming one")
        void currentExpectedMeetsIncoming() {
            var result = DeviceExpectedTsUtils.computeNewExpectedTimestamp(t1, t0, t0, t1, t0, t0);
            assertEquals(t1, result.expectedTimestamp().orElseThrow());
            assertEquals(t0, result.expectedTimestampLastDeviceJobTimestamp().orElseThrow());
            assertEquals(t0, result.expectedTimestampUpdateTimestamp().orElseThrow());
        }

        @Test
        @DisplayName("sets a new expectation when no current one exists")
        void freshExpectation() {
            var result = DeviceExpectedTsUtils.computeNewExpectedTimestamp(t1, t0, t0, null, null, null);
            assertEquals(t1, result.expectedTimestamp().orElseThrow());
            assertEquals(t0, result.expectedTimestampLastDeviceJobTimestamp().orElseThrow());
            assertTrue(result.expectedTimestampUpdateTimestamp().isPresent());
        }
    }

    @Nested
    @DisplayName("isDeviceListStale")
    class IsDeviceListStale {
        private final Instant now = Instant.parse("2026-01-02T00:00:00Z");
        private final Duration twoHours = Duration.ofHours(2);

        @Test
        @DisplayName("returns true when the list is older than the expiry threshold")
        void olderThanExpiry() {
            var stamp = now.minus(Duration.ofHours(3));
            var dl = list(stamp, null, null, null, false);
            assertTrue(DeviceExpectedTsUtils.isDeviceListStale(dl, now, twoHours, null));
        }

        @Test
        @DisplayName("returns false when the list is within the expiry threshold and has no expected-update")
        void withinExpiry() {
            var stamp = now.minus(Duration.ofMinutes(30));
            var dl = list(stamp, null, null, null, false);
            assertFalse(DeviceExpectedTsUtils.isDeviceListStale(dl, now, twoHours, null));
        }

        @Test
        @DisplayName("returns false when the expected-update is fresh (< 25 hours)")
        void freshExpectedUpdate() {
            var stamp = now.minus(Duration.ofMinutes(30));
            var lastUpdate = now.minus(Duration.ofHours(1));
            var dl = list(stamp, stamp.plus(Duration.ofMinutes(1)), null, lastUpdate, false);
            assertFalse(DeviceExpectedTsUtils.isDeviceListStale(dl, now, twoHours, null));
        }

        @Test
        @DisplayName("returns true when expected-update is old AND the last-job timestamp differs from the ADV check time")
        void staleExpectedUpdate() {
            var stamp = now.minus(Duration.ofMinutes(30));
            var lastUpdate = now.minus(Duration.ofHours(26));
            var lastJob = Instant.parse("2025-12-30T00:00:00Z");
            var dl = list(stamp, stamp.plus(Duration.ofMinutes(1)), lastJob, lastUpdate, false);
            var advCheckTime = Instant.parse("2026-01-01T12:00:00Z");
            assertTrue(DeviceExpectedTsUtils.isDeviceListStale(dl, now, twoHours, advCheckTime));
        }

        @Test
        @DisplayName("returns false when expected-update is old but the last-job timestamp matches the ADV check time")
        void staleExpectedUpdateMatchingJob() {
            var stamp = now.minus(Duration.ofMinutes(30));
            var lastUpdate = now.minus(Duration.ofHours(26));
            var advCheckTime = Instant.parse("2026-01-01T12:00:00Z");
            var dl = list(stamp, stamp.plus(Duration.ofMinutes(1)), advCheckTime, lastUpdate, false);
            assertFalse(DeviceExpectedTsUtils.isDeviceListStale(dl, now, twoHours, advCheckTime));
        }
    }

    @Nested
    @DisplayName("isDeviceListCloseToExpiration")
    class IsDeviceListCloseToExpiration {
        private final Instant now = Instant.parse("2026-01-02T00:00:00Z");
        private final Duration oneHour = Duration.ofHours(1);

        @Test
        @DisplayName("returns true when the list is older than the warning threshold")
        void olderThanWarning() {
            var stamp = now.minus(Duration.ofHours(2));
            var dl = list(stamp, null, null, null, false);
            assertTrue(DeviceExpectedTsUtils.isDeviceListCloseToExpiration(dl, now, oneHour));
        }

        @Test
        @DisplayName("returns true when an expected timestamp is set ahead of the list timestamp")
        void hasFutureExpectation() {
            var stamp = now.minus(Duration.ofMinutes(15));
            var dl = list(stamp, stamp.plus(Duration.ofMinutes(1)), null, null, false);
            assertTrue(DeviceExpectedTsUtils.isDeviceListCloseToExpiration(dl, now, oneHour));
        }

        @Test
        @DisplayName("returns false when neither condition holds")
        void neitherCondition() {
            var stamp = now.minus(Duration.ofMinutes(15));
            var dl = list(stamp, null, null, null, false);
            assertFalse(DeviceExpectedTsUtils.isDeviceListCloseToExpiration(dl, now, oneHour));
        }
    }
}
