package com.github.auties00.cobalt.wam;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.wam.binary.WamEventDecoder;
import com.github.auties00.cobalt.wam.binary.WamEventEncoder;
import com.github.auties00.cobalt.wam.binary.WamEventSizes;
import com.github.auties00.cobalt.wire.wam.event.PsIdUpdateEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WamClientErrorsEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WamEventRegistry;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wire.wam.type.PsIdAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Exercises the behavioural state machine of {@link WamService} through a
 * {@link TestableWamService} subclass that stubs the four protected
 * timing/scheduling hooks, validating commit dispatch and channel routing,
 * the sampling-override surface, the {@code WAWebWamInitQueue} pre-init
 * fallback, close idempotency, per-channel sequence-number wrap, retry and
 * backoff, connectivity waiting, and buffer rotation and drop. Channel
 * pinning is verified dynamically against the captured live event registry
 * under {@code wam-event-definitions.json}.
 *
 * <p>The subclass never schedules a real recurring tick or calls
 * {@link Thread#sleep(long)}; it records every scheduler and sleep request
 * so the test bodies can drive ticks deterministically. The retry/backoff
 * and connectivity tests use a second-tier {@link RealisticHarness} that
 * wires a {@link TestWhatsAppClient} with an injectable connectivity flag
 * and a queue of canned IQ responses.
 */
@DisplayName("WamService behavioural state machine")
class WamServiceTest {
    @Nested
    @DisplayName("constructor")
    class ConstructorTests {
        @Test
        @DisplayName("LiveWamService(client, abPropsService) builds without throwing")
        void defaultBuilds() {
            var props = TestABPropsService.builder().build();
            var client = TestWhatsAppClient.create();
            var service = assertDoesNotThrow(() -> new LiveWamService(client, props));
            assertNotNull(service);
        }

        @Test
        @DisplayName("protected ctor accepts a substituted WamBeaconing")
        void protectedCtorAcceptsCustomBeaconing() {
            var props = TestABPropsService.builder().build();
            var client = TestWhatsAppClient.create();
            var beaconing = (WamBeaconingService) _ -> OptionalLong.empty();
            var service = assertDoesNotThrow(() -> new TestableWamService(client, props, beaconing));
            assertNotNull(service);
        }
    }

    @Nested
    @DisplayName("setSamplingOverride / removeSamplingOverride / replaceSamplingOverrides")
    class SamplingOverrideSurface {
        @Test
        @DisplayName("set / remove / replaceAll are no-throw")
        void apiSurface() {
            var service = newService();
            assertDoesNotThrow(() -> service.setSamplingOverride(2862, 100));
            assertDoesNotThrow(() -> service.removeSamplingOverride(2862));
            assertDoesNotThrow(() -> service.replaceSamplingOverrides(Map.of(1144, 50)));
        }

        // Only the deterministic weight=1 case is asserted; the weight>1 branch
        // is probabilistic and would flake on a small sample.
        @Test
        @DisplayName("weight=1 override never samples out")
        void weightOneOverrideAlwaysKeeps() {
            var service = newService();
            service.markInitializedForTesting();
            service.setSamplingOverride(2862, 1);
            service.commit(samplePsIdEvent());
            assertEquals(1, service.pendingCount(WamChannel.REGULAR),
                    "weight=1 sampling override must let every commit through");
        }
    }

    /**
     * Commit-dispatch tests covering {@link WamChannel#REGULAR} routing
     * after {@link LiveWamService#markInitializedForTesting()} flips the
     * service into the post-init phase; the pre-init deferral path is
     * covered by {@link InitQueueReplay}.
     */
    @Nested
    @DisplayName("commit dispatch (post-init)")
    class CommitDispatch {
        @Test
        @DisplayName("fresh service has zero pending events per channel")
        void freshHasEmptyPending() {
            var service = newService();
            for (var channel : WamChannel.values()) {
                assertEquals(0, service.pendingCount(channel),
                        () -> "fresh service should have zero pending events for " + channel);
            }
        }

        @Test
        @DisplayName("REGULAR commit lands in the REGULAR pending list")
        void regularCommitLands() {
            var service = newService();
            service.markInitializedForTesting();
            service.setSamplingOverride(2862, 1);
            service.commit(samplePsIdEvent());
            assertEquals(1, service.pendingCount(WamChannel.REGULAR));
            assertEquals(0, service.pendingCount(WamChannel.REALTIME));
            assertEquals(0, service.pendingCount(WamChannel.PRIVATE));
        }

        // A second markCommitted() returns false and the event is dropped with
        // a logged warning, matching WA Web's redundant-commit guard.
        @Test
        @DisplayName("redundant commit of the same event leaves pending unchanged")
        void redundantCommitIsNoop() {
            var service = newService();
            service.markInitializedForTesting();
            service.setSamplingOverride(2862, 1);
            var event = samplePsIdEvent();
            service.commit(event);
            assertEquals(1, service.pendingCount(WamChannel.REGULAR));
            service.commit(event);
            assertEquals(1, service.pendingCount(WamChannel.REGULAR),
                    "second commit of the same instance should be dropped by markCommitted()");
        }

        @Test
        @DisplayName("distinct event instances both land")
        void twoInstancesLand() {
            var service = newService();
            service.markInitializedForTesting();
            service.setSamplingOverride(2862, 1);
            service.commit(samplePsIdEvent());
            service.commit(samplePsIdEvent());
            assertEquals(2, service.pendingCount(WamChannel.REGULAR));
        }

        @Test
        @DisplayName("commitAndWaitForFlush returns a future and queues the event")
        void commitAndWaitForFlushFuture() {
            var service = newService();
            service.markInitializedForTesting();
            service.setSamplingOverride(2862, 1);
            var future = service.commitAndWaitForFlush(samplePsIdEvent());
            assertNotNull(future, "commitAndWaitForFlush must always return a non-null future");
            assertEquals(1, service.pendingCount(WamChannel.REGULAR));
        }

        @Test
        @DisplayName("WamClientErrorsEvent commit lands in REGULAR")
        void wamClientErrorsCommit() {
            var service = newService();
            service.markInitializedForTesting();
            service.setSamplingOverride(1144, 1);
            var event = new WamClientErrorsEventBuilder()
                    .wamClientBufferDropErrorCount(1)
                    .build();
            service.commit(event);
            assertEquals(1, service.pendingCount(WamChannel.REGULAR));
        }
    }

    /**
     * Init-queue tests covering the {@code WAWebWamInitQueue} fallback
     * path: a commit that arrives before {@link WamService#initialize()}
     * (or {@link LiveWamService#markInitializedForTesting()}) has run is
     * deferred to the init queue and replayed when
     * {@link LiveWamService#drainInitQueue()} runs, so validation, sampling,
     * and dispatch all happen at drain time against the live runtime, not
     * at the original commit point.
     */
    @Nested
    @DisplayName("init queue (WAWebWamInitQueue fallback)")
    class InitQueueReplay {
        @Test
        @DisplayName("pre-init commit defers into the init queue, not pending")
        void preInitCommitDefersToInitQueue() {
            var service = newService();
            service.commit(samplePsIdEvent());
            assertEquals(0, service.pendingCount(WamChannel.REGULAR),
                    "pre-init commit must not land in pending");
            assertEquals(1, service.initQueueSize(),
                    "pre-init commit must land in the init queue");
        }

        @Test
        @DisplayName("multiple pre-init commits all queue")
        void multiplePreInitCommitsAllQueue() {
            var service = newService();
            service.commit(samplePsIdEvent());
            service.commit(samplePsIdEvent());
            service.commit(samplePsIdEvent());
            assertEquals(3, service.initQueueSize());
            assertEquals(0, service.pendingCount(WamChannel.REGULAR));
        }

        @Test
        @DisplayName("drain replays queued commits into pending and empties the queue")
        void drainReplaysIntoPending() {
            var service = newService();
            service.commit(samplePsIdEvent());
            service.commit(samplePsIdEvent());
            assertEquals(2, service.initQueueSize());

            service.markInitializedForTesting();
            service.setSamplingOverride(2862, 1);
            service.drainInitQueue();

            assertEquals(0, service.initQueueSize(),
                    "drain must empty the init queue");
            assertEquals(2, service.pendingCount(WamChannel.REGULAR),
                    "drained events must land in the REGULAR pending list");
        }

        // Models the production initialize() sequence: load AB-props sampling
        // configs, set initialized=true, then drain. The drained commit re-enters
        // the post-init dispatch path and consults WamSamplingOverride freshly,
        // so weights loaded mid-sequence apply to queued events.
        @Test
        @DisplayName("sampling override installed before drain applies to queued commits")
        void samplingOverrideAppliesToDrainedCommits() {
            var service = newService();
            var event = heavilySampledRegularEvent(8888, 1000);
            service.commit(event);
            assertEquals(1, service.initQueueSize(),
                    "pre-init commit deferred to queue");
            assertEquals(0, service.pendingCount(WamChannel.REGULAR),
                    "deferred commit not yet in pending");

            service.markInitializedForTesting();
            service.setSamplingOverride(8888, 1);
            service.drainInitQueue();

            assertEquals(1, service.pendingCount(WamChannel.REGULAR),
                    "weight=1 override installed before drain must keep the event");
        }

        // The future is not asserted to complete here (completion requires a
        // flush); this only exercises the whenComplete bridge the pre-init path
        // wires between the deferred and live futures.
        @Test
        @DisplayName("pre-init commitAndWaitForFlush queues and returns a non-null future")
        void preInitCommitAndWaitForFlushFuture() {
            var service = newService();
            var future = service.commitAndWaitForFlush(samplePsIdEvent());
            assertNotNull(future, "deferred commitAndWaitForFlush must still return a non-null future");
            assertEquals(1, service.initQueueSize());
            assertEquals(0, service.pendingCount(WamChannel.REGULAR));

            service.markInitializedForTesting();
            service.setSamplingOverride(2862, 1);
            service.drainInitQueue();

            assertEquals(0, service.initQueueSize());
            assertEquals(1, service.pendingCount(WamChannel.REGULAR),
                    "the deferred event must land in pending after the drain");
        }
    }

    @Nested
    @DisplayName("close")
    class CloseLifecycle {
        @Test
        @DisplayName("close on never-initialized service does not throw")
        void closeWithoutInitialize() {
            var service = newService();
            assertDoesNotThrow(service::close);
        }

        @Test
        @DisplayName("repeated close is idempotent")
        void repeatedClose() {
            var service = newService();
            service.close();
            assertDoesNotThrow(service::close);
        }

        @Test
        @DisplayName("close invokes cancelAllScheduled")
        void closeCancelsScheduled() {
            var service = newService();
            service.scheduledRunnables.add(new ScheduledCall(() -> {}, 5, 5));
            service.scheduledRunnables.add(new ScheduledCall(() -> {}, 120, 120));
            service.close();
            assertTrue(service.scheduledRunnables.isEmpty(),
                    "close() must drive cancelAllScheduled to empty the recurring task queue");
        }
    }

    /**
     * Tests covering {@link LiveWamService#checkMidCycleUpload()}, the
     * five-second serialize tick's threshold-driven early-flush behaviour.
     */
    @Nested
    @DisplayName("checkMidCycleUpload")
    class MidCycleEntry {
        private final Jid selfPn = Jid.of("19254863482@s.whatsapp.net");

        @Test
        @DisplayName("no-ops when not initialised, leaves init queue intact")
        void noopWhenNotInitialized() {
            var service = newService();
            service.commit(samplePsIdEvent());
            assertEquals(1, service.initQueueSize());
            assertDoesNotThrow(service::checkMidCycleUpload);
            assertEquals(1, service.initQueueSize(),
                    "checkMidCycleUpload pre-init must not drain the init queue");
            assertEquals(0, service.pendingCount(WamChannel.REGULAR),
                    "checkMidCycleUpload pre-init must not promote queued events");
        }

        @Test
        @DisplayName("below-threshold pending stays in place")
        void belowThresholdDoesNotFlush() {
            var harness = newRealisticHarness(selfPn);
            harness.alwaysSucceed = true;
            harness.responsesQueue.add(successResponse());
            harness.service.markInitializedForTesting();
            harness.service.setSamplingOverride(2862, 1);

            harness.service.commit(samplePsIdEvent());
            harness.service.commit(samplePsIdEvent());
            harness.service.commit(samplePsIdEvent());
            assertEquals(3, harness.service.pendingCount(WamChannel.REGULAR));

            harness.service.checkMidCycleUpload();

            assertEquals(0, harness.sendNodeCalls.get(),
                    "below-threshold pending should not trigger an early flush");
            assertEquals(3, harness.service.pendingCount(WamChannel.REGULAR),
                    "below-threshold pending must stay queued for the next cycle");
        }

        @Test
        @DisplayName("above-threshold REGULAR pending triggers early flush")
        void aboveThresholdTriggersFlush() {
            var harness = newRealisticHarness(selfPn);
            harness.alwaysSucceed = true;
            harness.responsesQueue.add(successResponse());
            harness.service.markInitializedForTesting();

            harness.service.commit(hugeRegularEvent(55_000));
            assertEquals(1, harness.service.pendingCount(WamChannel.REGULAR));

            harness.service.checkMidCycleUpload();

            assertTrue(harness.sendNodeCalls.get() >= 1,
                    "above-threshold REGULAR pending must trigger an early flush");
            assertEquals(0, harness.service.pendingCount(WamChannel.REGULAR),
                    "early flush must drain the REGULAR pending list");
        }

        // Asserting the skip via a queued REALTIME event would race the
        // immediate-flush virtual thread, so this drives a small REGULAR commit
        // and verifies the sweep completes without sending any stanza.
        @Test
        @DisplayName("REALTIME channel is skipped by the mid-cycle sweep")
        void realtimeIsSkipped() {
            var harness = newRealisticHarness(selfPn);
            harness.alwaysSucceed = true;
            harness.responsesQueue.add(successResponse());
            harness.service.markInitializedForTesting();
            harness.service.commit(samplePsIdEvent());
            assertDoesNotThrow(harness.service::checkMidCycleUpload);
            assertEquals(0, harness.sendNodeCalls.get(),
                    "small commits should not trigger mid-cycle flush");
        }
    }

    // For every event in wam-event-definitions.json that resolves to a Cobalt
    // WamEventSpec, the reported channel must equal the channel string from WA
    // Web's WAWebWamCodegenUtils.events (case-insensitive against WamChannel),
    // catching a Cobalt @WamEvent that defaults a channel (typically REGULAR)
    // for an event WA Web classifies as REALTIME or PRIVATE. Degrades to a
    // single skip-test when the corpus fixture is unavailable.
    @TestFactory
    @DisplayName("channel pinning vs live WAM registry")
    List<DynamicTest> channelPinningAgainstLiveRegistry() {
        if (!WamFixtures.isAvailable("wam-event-definitions.json")) {
            return List.of(dynamicTest(
                    "wam-event-definitions.json not available",
                    () -> { /* skip when corpus missing */ }));
        }
        var fixture = WamFixtures.loadOracle("wam-event-definitions");
        var events = fixture.getJSONArray("events");
        var tests = new ArrayList<DynamicTest>();
        for (var entry : events) {
            var event = (JSONObject) entry;
            var id = event.get("id");
            var liveChannel = event.getString("channel");
            if (id == null || liveChannel == null) {
                continue;
            }
            var label = event.getString("name") + "(id=" + id + ")";
            tests.add(dynamicTest(label, () -> assertChannelMatches(
                    event.getString("name"),
                    ((Number) id).intValue(),
                    liveChannel)));
        }
        return tests;
    }

    // The minimal marker carries no fields, so the registry must report the
    // spec on the event id alone.
    private static void assertChannelMatches(String eventName, int eventId, String liveChannel) {
        var buffer = new byte[8];
        var encoder = WamEventEncoder.toBytes(buffer);
        encoder.writeEventMarker(eventId, 0, false);
        var decoder = WamEventDecoder.fromBytes(buffer, 0, encoder.written());
        var spec = WamEventRegistry.decode(decoder);
        var expected = WamChannel.valueOf(liveChannel.toUpperCase(Locale.ROOT));
        assertSame(expected, spec.channel(),
                () -> "channel drift for " + eventName + " (id=" + eventId
                        + "): Cobalt=" + spec.channel() + " live=" + expected);
    }

    // Each call returns a new instance so the spec's markCommitted() guard does
    // not reject repeat commits in tests that push more than one event.
    private static WamEventSpec samplePsIdEvent() {
        return new PsIdUpdateEventBuilder()
                .psIdAction(PsIdAction.CREATED)
                .psIdKey(42)
                .psIdRotationFrequence(7)
                .build();
    }

    // For the unit-level state-machine tests that do not need a real store or
    // canned-response IQ pipeline; the flush/retry tests use newRealisticHarness.
    private static TestableWamService newService() {
        var props = TestABPropsService.builder().build();
        var client = TestWhatsAppClient.create().withAbPropsService(props);
        return new TestableWamService(client, props, new LiveWamBeaconingService());
    }

    /**
     * Test double for {@link WamService} that stubs the four protected
     * timing/scheduling hooks with deterministic capture lists: tests
     * inspect {@link #sleepRequests} and {@link #scheduledRunnables} after
     * exercising the service, and neither {@link #sleep(long)} nor
     * {@link #scheduleRecurring(Runnable, long, long)} actually blocks or
     * schedules. The virtual clock advances on each {@link #sleep(long)}
     * so the connectivity-wait deadline check terminates deterministically.
     */
    private static final class TestableWamService extends LiveWamService {
        private final AtomicReference<Instant> now = new AtomicReference<>(Instant.ofEpochSecond(1_747_000_000L));

        final List<Long> sleepRequests = new ArrayList<>();

        final Queue<ScheduledCall> scheduledRunnables = new ConcurrentLinkedQueue<>();

        TestableWamService(LinkedWhatsAppClient client, ABPropsService props, WamBeaconingService beaconing) {
            super(client, props, beaconing);
        }

        @Override
        protected Instant now() {
            return now.get();
        }

        @Override
        protected void sleep(long millis) {
            sleepRequests.add(millis);
            now.updateAndGet(current -> current.plusMillis(millis));
        }

        @Override
        protected void scheduleRecurring(Runnable task, long initialDelaySeconds, long periodSeconds) {
            scheduledRunnables.add(new ScheduledCall(task, initialDelaySeconds, periodSeconds));
        }

        @Override
        protected void cancelAllScheduled() {
            scheduledRunnables.clear();
        }
    }

    /**
     * One captured
     * {@link LiveWamService#scheduleRecurring(Runnable, long, long)}
     * invocation; tests pop entries off
     * {@link TestableWamService#scheduledRunnables} and assert the
     * captured task and timing arguments against the production call site.
     */
    private record ScheduledCall(Runnable task, long initialDelaySeconds, long periodSeconds) {
    }

    /**
     * Per-channel sequence-counter tests covering monotonic advancement,
     * wrap from {@code MAX_SEQUENCE_NUMBER} ({@code 0xFFFF}) back to
     * {@code 1}, and independence across the three channels.
     */
    @Nested
    @DisplayName("sequence numbers")
    class SequenceNumbers {
        private final Jid selfPn = Jid.of("19254863482@s.whatsapp.net");

        @Test
        @DisplayName("REGULAR flush advances the counter by 1 per buffer")
        void counterAdvancesPerFlush() {
            var harness = newRealisticHarness(selfPn);
            harness.alwaysSucceed = true;
            harness.responsesQueue.add(successResponse());
            harness.service.markInitializedForTesting();
            harness.service.setSamplingOverride(2862, 1);

            var before = harness.service.sequenceNumberFor(WamChannel.REGULAR);
            harness.service.commit(samplePsIdEvent());
            harness.service.flushChannel(WamChannel.REGULAR);
            var afterOne = harness.service.sequenceNumberFor(WamChannel.REGULAR);
            assertEquals(before + 1, afterOne, "first flush bumps the counter by 1");

            harness.service.setSamplingOverride(2862, 1);
            harness.service.commit(samplePsIdEvent());
            harness.service.flushChannel(WamChannel.REGULAR);
            assertEquals(afterOne + 1, harness.service.sequenceNumberFor(WamChannel.REGULAR),
                    "second flush bumps the counter by 1 again");
        }

        @Test
        @DisplayName("REGULAR / REALTIME / PRIVATE counters are independent")
        void countersAreIndependent() {
            var harness = newRealisticHarness(selfPn);
            harness.alwaysSucceed = true;
            harness.responsesQueue.add(successResponse());
            harness.service.markInitializedForTesting();
            harness.service.setSamplingOverride(2862, 1);

            var realtimeBefore = harness.service.sequenceNumberFor(WamChannel.REALTIME);
            var privateBefore = harness.service.sequenceNumberFor(WamChannel.PRIVATE);

            harness.service.commit(samplePsIdEvent());
            harness.service.flushChannel(WamChannel.REGULAR);

            assertEquals(realtimeBefore, harness.service.sequenceNumberFor(WamChannel.REALTIME),
                    "REGULAR flush must not touch the REALTIME counter");
            assertEquals(privateBefore, harness.service.sequenceNumberFor(WamChannel.PRIVATE),
                    "REGULAR flush must not touch the PRIVATE counter");
        }

        @Test
        @DisplayName("counter at MAX_SEQUENCE_NUMBER wraps to 1 on next flush")
        void counterWrapsToOne() {
            var harness = newRealisticHarness(selfPn);
            harness.alwaysSucceed = true;
            harness.responsesQueue.add(successResponse());
            harness.service.markInitializedForTesting();
            harness.service.setSamplingOverride(2862, 1);

            harness.service.setSequenceNumberForTesting(WamChannel.REGULAR, 0xFFFF);
            harness.service.commit(samplePsIdEvent());
            harness.service.flushChannel(WamChannel.REGULAR);

            assertEquals(1, harness.service.sequenceNumberFor(WamChannel.REGULAR),
                    "counter must wrap from 0xFFFF back to 1, not to 0 or 0x10000");
        }
    }

    /**
     * Channel-routing tests covering the divergent commit paths for
     * {@link WamChannel#REALTIME} (immediate virtual-thread flush) and
     * {@link WamChannel#REGULAR} (accumulate-until-flush).
     *
     * <p>{@link WamChannel#PRIVATE} bucketing is not exercised here because
     * private events upload through the HTTP-only
     * {@link com.github.auties00.cobalt.wam.privatestats.WamPrivateStatsUploader},
     * which does not surface through the test client's send handler.
     */
    @Nested
    @DisplayName("channel routing")
    class ChannelRouting {
        private final Jid selfPn = Jid.of("19254863482@s.whatsapp.net");

        @Test
        @DisplayName("REGULAR commit accumulates, no immediate flush")
        void regularAccumulatesUntilFlush() {
            var harness = newRealisticHarness(selfPn);
            harness.alwaysSucceed = true;
            harness.responsesQueue.add(successResponse());
            harness.service.markInitializedForTesting();
            harness.service.setSamplingOverride(2862, 1);

            harness.service.commit(samplePsIdEvent());

            assertEquals(0, harness.sendNodeCalls.get(),
                    "REGULAR commit must not trigger sendNode synchronously");
            assertEquals(1, harness.service.pendingCount(WamChannel.REGULAR));
        }

        @Test
        @DisplayName("REALTIME commit triggers immediate flush via virtual thread")
        void realtimeTriggersImmediateFlush() throws InterruptedException {
            var harness = newRealisticHarness(selfPn);
            var latch = new CountDownLatch(1);
            harness.client.withSendNodeHandler(builder -> {
                harness.sendNodeCalls.incrementAndGet();
                latch.countDown();
                return successResponse();
            });
            harness.service.markInitializedForTesting();

            harness.service.commit(realtimeEvent(9999));

            assertTrue(latch.await(5, TimeUnit.SECONDS),
                    "REALTIME commit must trigger sendNode within 5s via the virtual-thread flush spawn");
        }

        // Multiple commits within the same tick all batch into one upload, so
        // the assertion only requires sendNodeCalls >= 1 and pendingCount == 0:
        // each commit spawns a flushChannel(REALTIME) virtual thread, and the
        // first to reach the atomic drain takes every queued event while later
        // threads observe an empty list and return.
        @Test
        @DisplayName("three REALTIME commits all drain through the pipeline")
        void multipleRealtimeCommitsAllFlush() throws InterruptedException {
            var harness = newRealisticHarness(selfPn);
            var firstUpload = new CountDownLatch(1);
            harness.client.withSendNodeHandler(builder -> {
                harness.sendNodeCalls.incrementAndGet();
                firstUpload.countDown();
                return successResponse();
            });
            harness.service.markInitializedForTesting();

            harness.service.commit(realtimeEvent(9991));
            harness.service.commit(realtimeEvent(9992));
            harness.service.commit(realtimeEvent(9993));

            assertTrue(firstUpload.await(5, TimeUnit.SECONDS),
                    "at least one REALTIME upload must reach sendNode within 5s "
                            + "(WA Web's setTimeout(1ms)+atomic-drain batches 3 commits "
                            + "into a single forceRunNow tick)");
            Thread.sleep(200);
            assertEquals(0, harness.service.pendingCount(WamChannel.REALTIME),
                    "every committed REALTIME event must drain (no events left in pending)");
            assertTrue(harness.sendNodeCalls.get() >= 1,
                    "at least one sendNode call must have been made; got "
                            + harness.sendNodeCalls.get());
        }
    }

    // Drives the immediate-flush path on a known-realtime spec without a
    // generated realtime event class. markCommitted() permits one positive call:
    // the init-queue replay re-enters commit(), but the second markCommitted()
    // returns false and the replay branch logs the redundant commit without
    // re-enqueuing.
    private static WamEventSpec realtimeEvent(int eventId) {
        return new WamEventSpec() {
            private volatile boolean committed;

            @Override
            public int id() {
                return eventId;
            }

            @Override
            public WamChannel channel() {
                return WamChannel.REALTIME;
            }

            @Override
            public int alphaWeight() {
                return 1;
            }

            @Override
            public int betaWeight() {
                return 1;
            }

            @Override
            public int releaseWeight() {
                return 1;
            }

            @Override
            public int privateStatsId() {
                return -1;
            }

            @Override
            public boolean markCommitted() {
                if (committed) {
                    return false;
                }
                committed = true;
                return true;
            }

            @Override
            public int sizeOf(int weight) {
                return WamEventSizes.eventMarkerSize(eventId, weight);
            }

            @Override
            public void encode(WamEventEncoder encoder, int weight) {
                encoder.writeEventMarker(eventId, weight, false);
            }
        };
    }

    /**
     * Retry and backoff tests driving the full flush pipeline against an
     * in-memory store and a controllable response sequence; the captured
     * {@link TestableWamService#sleepRequests} are asserted against the
     * exponential-backoff delays computed by
     * {@link LiveWamService#computeBackoffDelay(int)}.
     */
    @Nested
    @DisplayName("retry / backoff")
    class RetryBackoff {
        private final Jid selfPn = Jid.of("19254863482@s.whatsapp.net");

        // Probes the three regions: low clamp (2^attempt < base), the unclamped
        // middle band, and high clamp (2^attempt > max).
        @Test
        @DisplayName("computeBackoffDelay clamps low/high and lies within the 10% jitter band")
        void backoffFormulaBounds() {
            for (var attempt = 0; attempt < 4; attempt++) {
                var actual = LiveWamService.computeBackoffDelay(attempt);
                assertTrue(actual >= 1_000 && actual <= 1_100,
                        "attempt=" + attempt + " should clamp to base [1000, 1100], was " + actual);
            }
            var mid = LiveWamService.computeBackoffDelay(10);
            assertTrue(mid >= 1_024 && mid <= (long) (1_024 * 1.1),
                    "attempt=10 should be in [1024, ~1126], was " + mid);
            var clamped = LiveWamService.computeBackoffDelay(20);
            assertTrue(clamped >= 120_000 && clamped <= (long) (120_000 * 1.1),
                    "attempt=20 should clamp to max [120_000, 132_000], was " + clamped);
        }

        @Test
        @DisplayName("two 5xx responses are retried with backoff, third success terminates")
        void retryUntilSuccess() {
            var harness = newRealisticHarness(selfPn);
            harness.responsesQueue.add(errorResponse(500));
            harness.responsesQueue.add(errorResponse(503));
            harness.responsesQueue.add(successResponse());

            harness.service.markInitializedForTesting();
            harness.service.setSamplingOverride(2862, 1);
            harness.service.commit(samplePsIdEvent());
            harness.service.flushChannel(WamChannel.REGULAR);

            assertEquals(3, harness.sendNodeCalls.get(),
                    "expected three sendNode invocations: two failures + one success");
            assertEquals(2, harness.service.sleepRequests.size(),
                    "expected two backoff sleeps before the successful send");
            for (var delay : harness.service.sleepRequests) {
                assertTrue(delay >= 1_000 && delay <= 132_000,
                        () -> "captured backoff delay " + delay + " outside [1000, 132000]");
            }
        }

        @Test
        @DisplayName("4xx response does not retry; buffer is dropped")
        void permanentErrorDropsBuffer() {
            var harness = newRealisticHarness(selfPn);
            harness.responsesQueue.add(errorResponse(400));
            harness.responsesQueue.add(successResponse());

            harness.service.markInitializedForTesting();
            harness.service.setSamplingOverride(2862, 1);
            harness.service.commit(samplePsIdEvent());
            harness.service.flushChannel(WamChannel.REGULAR);

            assertEquals(0, harness.service.sleepRequests.size(),
                    "4xx is permanent, no backoff sleep should be requested");
            assertTrue(harness.sendNodeCalls.get() >= 1,
                    "at least the original buffer upload must have been attempted");
        }
    }

    /**
     * Connectivity-wait tests covering {@link WamService}'s disconnected
     * poll loop, which checks the client in one-second steps up to the
     * 30-second timeout.
     */
    @Nested
    @DisplayName("connectivity wait")
    class Connectivity {
        private final Jid selfPn = Jid.of("19254863482@s.whatsapp.net");

        @Test
        @DisplayName("connected on entry: no sleeps recorded")
        void connectedOnEntry() {
            var harness = newRealisticHarness(selfPn);
            harness.responsesQueue.add(successResponse());

            harness.service.markInitializedForTesting();
            harness.service.setSamplingOverride(2862, 1);
            harness.service.commit(samplePsIdEvent());
            harness.service.flushChannel(WamChannel.REGULAR);

            assertEquals(0, harness.service.sleepRequests.size(),
                    "no waitIfDisconnected sleeps should fire when client is connected");
        }

        @Test
        @DisplayName("disconnected on entry: 1s sleeps until deadline, then upload proceeds")
        void disconnectedUntilDeadline() {
            var harness = newRealisticHarness(selfPn);
            harness.client.withIsConnected(false);
            harness.responsesQueue.add(successResponse());

            harness.service.markInitializedForTesting();
            harness.service.setSamplingOverride(2862, 1);
            harness.service.commit(samplePsIdEvent());
            harness.service.flushChannel(WamChannel.REGULAR);

            var sleeps = harness.service.sleepRequests;
            assertTrue(sleeps.size() >= 30,
                    () -> "expected >= 30 connectivity sleeps (one per second of the 30s timeout), got " + sleeps.size());
            for (var delay : sleeps) {
                assertEquals(1_000L, delay,
                        () -> "every connectivity-wait sleep must be exactly 1000ms, was " + delay);
            }
            assertEquals(1, harness.sendNodeCalls.get(),
                    "upload should be attempted once after the connectivity-wait deadline elapses");
        }
    }

    /**
     * Buffer rotation and drop tests driving the encoder pipeline with
     * enough payload to cross the rotation threshold
     * ({@code MAX_BUFFER_SIZE = 50_000}) and the upload-drop threshold
     * ({@code MAX_UPLOAD_SIZE = 64_000}).
     */
    @Nested
    @DisplayName("buffer rotation / drop")
    class BufferRotation {
        private final Jid selfPn = Jid.of("19254863482@s.whatsapp.net");

        @Test
        @DisplayName("flush across MAX_BUFFER_SIZE splits into multiple sendNode calls")
        void multipleBuffersAcrossMaxBufferSize() {
            var harness = newRealisticHarness(selfPn);
            harness.responsesQueue.add(successResponse());
            harness.alwaysSucceed = true;

            harness.service.markInitializedForTesting();
            harness.service.setSamplingOverride(1144, 1);

            for (var i = 0; i < 8000; i++) {
                var event = new WamClientErrorsEventBuilder()
                        .wamClientBufferDropErrorCount(1)
                        .build();
                harness.service.commit(event);
            }
            harness.service.flushChannel(WamChannel.REGULAR);

            assertTrue(harness.sendNodeCalls.get() >= 2,
                    "aggregate payload above MAX_BUFFER_SIZE must split into >= 2 sendNode calls, was " + harness.sendNodeCalls.get());
            assertEquals(0, harness.service.pendingCount(WamChannel.REGULAR),
                    "all committed events should be drained after flush");
        }

        @Test
        @DisplayName("flush drains pending to zero on success")
        void flushDrainsPending() {
            var harness = newRealisticHarness(selfPn);
            harness.alwaysSucceed = true;
            harness.responsesQueue.add(successResponse());

            harness.service.markInitializedForTesting();
            harness.service.setSamplingOverride(2862, 1);
            harness.service.commit(samplePsIdEvent());
            harness.service.commit(samplePsIdEvent());
            harness.service.commit(samplePsIdEvent());

            assertEquals(3, harness.service.pendingCount(WamChannel.REGULAR));
            harness.service.flushChannel(WamChannel.REGULAR);
            assertEquals(0, harness.service.pendingCount(WamChannel.REGULAR),
                    "successful flush must drain pending to zero");
        }

        // The aggregate exceeds MAX_BUFFER_SIZE so one buffer is built; inside,
        // its precomputed size exceeds MAX_UPLOAD_SIZE so it is dropped before
        // any send and the drop-counter event is re-committed for the next cycle.
        @Test
        @DisplayName("buffer >64KB is dropped, drop-counter event re-committed")
        void oversizedBufferDropsAndRecommitsCounter() {
            var harness = newRealisticHarness(selfPn);
            harness.alwaysSucceed = true;
            harness.responsesQueue.add(successResponse());

            harness.service.markInitializedForTesting();
            harness.service.setSamplingOverride(1144, 1);

            var huge = hugeRegularEvent(64_500);
            harness.service.commit(huge);
            assertEquals(1, harness.service.pendingCount(WamChannel.REGULAR));

            harness.service.flushChannel(WamChannel.REGULAR);

            assertEquals(0, harness.sendNodeCalls.get(),
                    "oversized buffer must be dropped before any sendNode call");
            assertEquals(1, harness.service.pendingCount(WamChannel.REGULAR),
                    "the drop-counter WamClientErrorsEvent should land in REGULAR pending after the drop");
        }
    }

    // A releaseWeight of 1 always keeps; higher values produce a probabilistic
    // drop that tests counteract with a weight=1 override on the same id.
    private static WamEventSpec heavilySampledRegularEvent(int eventId, int releaseWeight) {
        return new WamEventSpec() {
            private volatile boolean committed;

            @Override
            public int id() {
                return eventId;
            }

            @Override
            public WamChannel channel() {
                return WamChannel.REGULAR;
            }

            @Override
            public int alphaWeight() {
                return releaseWeight;
            }

            @Override
            public int betaWeight() {
                return releaseWeight;
            }

            @Override
            public int releaseWeight() {
                return releaseWeight;
            }

            @Override
            public int privateStatsId() {
                return -1;
            }

            @Override
            public boolean markCommitted() {
                if (committed) {
                    return false;
                }
                committed = true;
                return true;
            }

            @Override
            public int sizeOf(int weight) {
                return WamEventSizes.eventMarkerSize(eventId, weight);
            }

            @Override
            public void encode(WamEventEncoder encoder, int weight) {
                encoder.writeEventMarker(eventId, weight, false);
            }
        };
    }

    // For the buffer-rotation and oversized-buffer tests; the actual encoded
    // length is the string length plus the event marker and the str32 length
    // prefix (~10 bytes overhead).
    private static WamEventSpec hugeRegularEvent(int targetSize) {
        var payload = "a".repeat(targetSize);
        return new WamEventSpec() {
            private volatile boolean committed;

            @Override
            public int id() {
                return 9000;
            }

            @Override
            public WamChannel channel() {
                return WamChannel.REGULAR;
            }

            @Override
            public int alphaWeight() {
                return 1;
            }

            @Override
            public int betaWeight() {
                return 1;
            }

            @Override
            public int releaseWeight() {
                return 1;
            }

            @Override
            public int privateStatsId() {
                return -1;
            }

            @Override
            public boolean markCommitted() {
                if (committed) {
                    return false;
                }
                committed = true;
                return true;
            }

            @Override
            public int sizeOf(int weight) {
                return WamEventSizes.eventMarkerSize(9000, weight)
                        + WamEventSizes.stringFieldSize(1, payload);
            }

            @Override
            public void encode(WamEventEncoder encoder, int weight) {
                encoder.writeEventMarker(9000, weight, true);
                encoder.writeStringField(1, payload, false);
            }
        };
    }

    /**
     * Realistic-store harness used by the retry, rotation, and
     * connectivity tests; wires a real in-memory store and a controllable
     * canned-response IQ pipeline. When {@link #alwaysSucceed} is set, the
     * send handler returns a fresh success response once
     * {@link #responsesQueue} is empty; otherwise it throws.
     */
    private static final class RealisticHarness {
        final TestWhatsAppClient client;

        final TestableWamService service;

        final Deque<Stanza> responsesQueue;

        final AtomicInteger sendNodeCalls;

        boolean alwaysSucceed;

        RealisticHarness(
                TestWhatsAppClient client,
                TestableWamService service,
                Deque<Stanza> responsesQueue,
                AtomicInteger sendNodeCalls) {
            this.client = client;
            this.service = service;
            this.responsesQueue = responsesQueue;
            this.sendNodeCalls = sendNodeCalls;
        }
    }

    // The store lets the flush path's sequence-number and persisted-buffer
    // writes land in real backing storage, which the retry and rotation tests
    // rely on. The client is pre-wired with isConnected=true, so connectivity
    // tests that need the disconnected branch must call withIsConnected(false).
    private static RealisticHarness newRealisticHarness(Jid selfPn) {
        var props = TestABPropsService.builder().build();
        var responses = new ArrayDeque<Stanza>();
        var calls = new AtomicInteger();
        var harnessRef = new RealisticHarness[1];
        var store = DeviceFixtures.temporaryStore(selfPn, null);
        var client = TestWhatsAppClient.create()
                .withStore(store)
                .withAbPropsService(props)
                .withIsConnected(true)
                .withSendNodeHandler(builder -> {
                    calls.incrementAndGet();
                    var next = responses.poll();
                    if (next != null) {
                        return next;
                    }
                    if (harnessRef[0] != null && harnessRef[0].alwaysSucceed) {
                        return successResponse();
                    }
                    throw new IllegalStateException("realistic harness ran out of canned responses for sendNode");
                });
        var service = new TestableWamService(client, props, new LiveWamBeaconingService());
        harnessRef[0] = new RealisticHarness(client, service, responses, calls);
        return harnessRef[0];
    }

    private static Stanza successResponse() {
        return new StanzaBuilder()
                .description("iq")
                .attribute("type", "result")
                .build();
    }

    // The error code lands in an <error code="..."/> child; the flush path
    // retries for code >= 500 and permanently drops everything else.
    private static Stanza errorResponse(int code) {
        var error = new StanzaBuilder()
                .description("error")
                .attribute("code", String.valueOf(code))
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("type", "error")
                .content(error)
                .build();
    }
}
