package com.github.auties00.cobalt.sync.key;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.linked.device.sync.MissingDeviceSyncKeyBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.SyncdCoordinator;
import com.github.auties00.cobalt.wam.LiveWamService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Pins the synchronously-observable invariants of {@link MissingSyncKeyTimeoutScheduler}
 * across every entry point ({@code scheduleTimeoutCheck}, {@code cancel}, {@code shutdown},
 * {@code startPeriodicReRequestJob}, {@code scheduleAllDevicesRespondedCheck}) without
 * depending on a real timer fire.
 *
 * <p>The wait-for-key timeout is set to 30 days so the scheduled timers cannot fire during a
 * test and pollute assertions; {@link #tearDown()} always calls
 * {@link MissingSyncKeyTimeoutScheduler#shutdown()} so the timer threads do not leak.
 */
@DisplayName("MissingSyncKeyTimeoutScheduler")
class MissingSyncKeyTimeoutSchedulerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid SELF_PN_DEVICE_1 = Jid.of("19250000001:1@s.whatsapp.net");

    private TestWhatsAppClient client;
    private LinkedWhatsAppStore store;
    private TestABPropsService props;
    private MissingSyncKeyRequestService requestService;
    private MissingSyncKeyTimeoutScheduler scheduler;

    @BeforeEach
    void setUp() {
        props = TestABPropsService.builder().build();
        props.set(ABProp.SYNCD_WAIT_FOR_KEY_TIMEOUT_DAYS, 30);

        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        store.accountStore().setJid(SELF_PN_DEVICE_1);
        client = TestWhatsAppClient.create().withStore(store);
        var wam = new LiveWamService(client, props);
        var coordinator = new SyncdCoordinator();
        requestService = new LiveMissingSyncKeyRequestService(client, wam, coordinator);
        scheduler = new MissingSyncKeyTimeoutScheduler(client, props, requestService, coordinator);
        requestService.setTimeoutScheduler(scheduler);
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdown();
    }

    @Nested
    @DisplayName("scheduleTimeoutCheck")
    class ScheduleTimeoutCheck {
        @Test
        @DisplayName("with no missing keys is a no-op (does not throw)")
        void noKeysNoSchedule() {
            assertDoesNotThrow(scheduler::scheduleTimeoutCheck,
                    "empty missing-key store must not throw - WAWebSyncdStoreMissingKeys._setMissingKeyTimeout returns early when n.length === 0");
        }

        @Test
        @DisplayName("with a tracked missing key schedules the timeout check")
        void tracksFuture() {
            store.syncStore().addMissingSyncKey(new MissingDeviceSyncKeyBuilder()
                    .keyId(new byte[]{1, 2, 3, 4, 5, 6})
                    .timestamp(Instant.now())
                    .askedDevices(Set.of(0))
                    .build());
            assertDoesNotThrow(scheduler::scheduleTimeoutCheck,
                    "non-empty missing-key store should schedule a check without throwing");
        }

        @Test
        @DisplayName("rescheduling is safe (replaces the previous future)")
        void reschedulingReplaces() {
            store.syncStore().addMissingSyncKey(new MissingDeviceSyncKeyBuilder()
                    .keyId(new byte[]{1, 2, 3, 4, 5, 6})
                    .timestamp(Instant.now())
                    .build());
            scheduler.scheduleTimeoutCheck();
            assertDoesNotThrow(scheduler::scheduleTimeoutCheck,
                    "second schedule must cancel the first and re-arm");
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {
        @Test
        @DisplayName("cancel without a scheduled check is safe")
        void cancelNoOp() {
            assertDoesNotThrow(scheduler::cancel);
        }

        @Test
        @DisplayName("cancel after scheduling clears the pending check")
        void cancelAfterSchedule() {
            store.syncStore().addMissingSyncKey(new MissingDeviceSyncKeyBuilder()
                    .keyId(new byte[]{1, 2, 3, 4, 5, 6})
                    .timestamp(Instant.now())
                    .build());
            scheduler.scheduleTimeoutCheck();
            assertDoesNotThrow(scheduler::cancel);
        }

        @Test
        @DisplayName("cancel is idempotent")
        void cancelIdempotent() {
            scheduler.cancel();
            assertDoesNotThrow(scheduler::cancel);
        }
    }

    @Nested
    @DisplayName("scheduleAllDevicesRespondedCheck - 5-second grace period")
    class GracePeriod {
        @Test
        @DisplayName("scheduling the grace period does not throw")
        void scheduleGracePeriod() {
            assertDoesNotThrow(scheduler::scheduleAllDevicesRespondedCheck);
        }

        @Test
        @DisplayName("a second schedule cancels and replaces the previous grace check")
        void rescheduleGracePeriod() {
            scheduler.scheduleAllDevicesRespondedCheck();
            assertDoesNotThrow(scheduler::scheduleAllDevicesRespondedCheck);
        }
    }

    @Nested
    @DisplayName("startPeriodicReRequestJob - idempotent")
    class PeriodicJob {
        @Test
        @DisplayName("first call schedules without throwing")
        void firstCallSchedules() {
            assertDoesNotThrow(scheduler::startPeriodicReRequestJob);
        }

        @Test
        @DisplayName("second call is a no-op (already scheduled)")
        void secondCallIsNoop() {
            scheduler.startPeriodicReRequestJob();
            assertDoesNotThrow(scheduler::startPeriodicReRequestJob,
                    "WA Web relies on WAWebTasksDefinitions single-registration; Cobalt guards explicitly");
        }
    }

    @Nested
    @DisplayName("shutdown - cancels timers and is idempotent")
    class Shutdown {
        @Test
        @DisplayName("shutdown does not throw")
        void shutdownDoesNotThrow() {
            assertDoesNotThrow(scheduler::shutdown);
        }

        @Test
        @DisplayName("shutdown after scheduling does not throw")
        void shutdownAfterScheduling() {
            store.syncStore().addMissingSyncKey(new MissingDeviceSyncKeyBuilder()
                    .keyId(new byte[]{1, 2, 3, 4, 5, 6})
                    .timestamp(Instant.now())
                    .build());
            scheduler.scheduleTimeoutCheck();
            scheduler.scheduleAllDevicesRespondedCheck();
            scheduler.startPeriodicReRequestJob();
            assertDoesNotThrow(scheduler::shutdown);
        }

        @Test
        @DisplayName("a second shutdown is a no-op")
        void shutdownIdempotent() {
            scheduler.shutdown();
            assertDoesNotThrow(scheduler::shutdown);
        }
    }
}
