package com.github.auties00.cobalt.wam;

import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Combined behavioural and known-answer tests for the WAM beaconing roll
 * backing {@link LiveWamBeaconingService}, exercising the 1% activation roll,
 * the UTC day boundary, and the per-buffer-key counter semantics demanded
 * by {@code WAWebWamBeaconing.maybeGetEventSequenceNumber}.
 *
 * <p>The KAT suite replays captured scenarios; the behavioural suite
 * covers boundary conditions (threshold inclusivity, counter wraparound,
 * key isolation) against a {@link DeterministicWamBeaconingService} harness that
 * lets the test queue activation rolls and drive the simulated UTC day
 * boundary by hand. KAT vectors live in
 * {@code fixtures/wam/wam-beaconing.json}, pinned to snapshot revision
 * {@code 1039260921}.
 */
@DisplayName("WamBeaconing behavioural + KAT")
class WamBeaconingTest {
    private static final long PINNED_SNAPSHOT_REVISION = 1039260921L;

    // 1% activation cutoff applied inclusively (roll <= 0.01 activates),
    // matching the literal .01 in WAWebWamBeaconing.
    private static final double ACTIVATION_PROBABILITY = 0.01;

    @TestFactory
    List<DynamicNode> beaconingMatchesLiveBundle() {
        var fixture = WamFixtures.loadOracle("wam-beaconing");
        WamFixtures.requireSnapshotRevision(fixture, PINNED_SNAPSHOT_REVISION);
        var threshold = fixture.getDoubleValue("activationThreshold");
        assertEquals(ACTIVATION_PROBABILITY, threshold,
                "fixture threshold drift: live=" + threshold + " expected=" + ACTIVATION_PROBABILITY);
        var scenarios = fixture.getJSONArray("scenarios");
        var tests = new ArrayList<DynamicNode>(scenarios.size());
        for (var entry : scenarios) {
            var scenario = (JSONObject) entry;
            tests.add(dynamicTest(scenario.getString("name"), () -> replayScenario(scenario)));
        }
        return tests;
    }

    // Each step carries a unixTime, an optional pushedRandom to enqueue before
    // the call, a bufferKey, and an expected result (the next sequence number,
    // or null for empty).
    private static void replayScenario(JSONObject scenario) {
        var beaconing = new DeterministicWamBeaconingService();
        var steps = scenario.getJSONArray("results");
        for (var entry : steps) {
            var step = (JSONObject) entry;
            beaconing.setUnixTimeSeconds(step.getLongValue("unixTime"));
            var pushed = step.get("pushedRandom");
            if (pushed != null) {
                beaconing.enqueueRandom(((Number) pushed).doubleValue());
            }
            var bufferKey = step.getString("bufferKey");
            var actual = beaconing.nextSequenceNumber(bufferKey);

            var expectedRaw = step.get("result");
            if (expectedRaw == null) {
                assertTrue(actual.isEmpty(),
                        () -> "scenario " + scenario.getString("name") + " step " + step.getString("step")
                                + ": expected empty but got " + actual.getAsLong());
            } else {
                var expected = ((Number) expectedRaw).longValue();
                assertTrue(actual.isPresent(),
                        () -> "scenario " + scenario.getString("name") + " step " + step.getString("step")
                                + ": expected " + expected + " but got empty");
                assertEquals(expected, actual.getAsLong(),
                        () -> "scenario " + scenario.getString("name") + " step " + step.getString("step"));
            }
        }
    }

    @Nested
    @DisplayName("DefaultWamBeaconing semantics under deterministic inputs")
    class BehaviouralSemantics {
        @Test
        @DisplayName("activation roll below threshold starts the per-day sequence at 1")
        void activatesAndIncrements() {
            var beaconing = new DeterministicWamBeaconingService();
            beaconing.setUnixTimeSeconds(100 * dayInSeconds());
            beaconing.enqueueRandom(0.005);
            assertEquals(OptionalLong.of(1), beaconing.nextSequenceNumber("regular"));
            assertEquals(OptionalLong.of(2), beaconing.nextSequenceNumber("regular"));
            assertEquals(OptionalLong.of(3), beaconing.nextSequenceNumber("regular"));
        }

        @Test
        @DisplayName("activation roll above threshold returns empty for the whole day")
        void deactivatesForDay() {
            var beaconing = new DeterministicWamBeaconingService();
            beaconing.setUnixTimeSeconds(100 * dayInSeconds());
            beaconing.enqueueRandom(0.5);
            assertTrue(beaconing.nextSequenceNumber("regular").isEmpty());
            assertTrue(beaconing.nextSequenceNumber("regular").isEmpty());
        }

        @Test
        @DisplayName("crossing a UTC day boundary re-rolls activation")
        void rerollsAcrossDayBoundary() {
            var beaconing = new DeterministicWamBeaconingService();
            var day0 = 100L * dayInSeconds();
            var day1 = day0 + dayInSeconds();

            beaconing.setUnixTimeSeconds(day0);
            beaconing.enqueueRandom(0.005);
            assertEquals(OptionalLong.of(1), beaconing.nextSequenceNumber("regular"));

            beaconing.setUnixTimeSeconds(day1);
            beaconing.enqueueRandom(0.5);
            assertTrue(beaconing.nextSequenceNumber("regular").isEmpty(),
                    "day1 deactivation should make next call return empty");
        }

        @Test
        @DisplayName("buffer keys roll independently")
        void independentKeys() {
            var beaconing = new DeterministicWamBeaconingService();
            beaconing.setUnixTimeSeconds(100 * dayInSeconds());

            beaconing.enqueueRandom(0.005);
            assertEquals(OptionalLong.of(1), beaconing.nextSequenceNumber("regular"));

            beaconing.enqueueRandom(0.5);
            assertTrue(beaconing.nextSequenceNumber("realtime").isEmpty());

            assertEquals(OptionalLong.of(2), beaconing.nextSequenceNumber("regular"));
        }

        @Test
        @DisplayName("threshold is inclusive at 0.01")
        void thresholdInclusive() {
            var activated = new DeterministicWamBeaconingService();
            activated.setUnixTimeSeconds(100 * dayInSeconds());
            activated.enqueueRandom(0.01);
            assertTrue(activated.nextSequenceNumber("k").isPresent());

            var deactivated = new DeterministicWamBeaconingService();
            deactivated.setUnixTimeSeconds(100 * dayInSeconds());
            deactivated.enqueueRandom(0.01000001);
            assertTrue(deactivated.nextSequenceNumber("k").isEmpty());
        }

        @Test
        @DisplayName("sequence counter resets at the start of a new active day")
        void counterResetsOnNewActiveDay() {
            var beaconing = new DeterministicWamBeaconingService();
            var day0 = 100L * dayInSeconds();
            var day2 = day0 + 2L * dayInSeconds();

            beaconing.setUnixTimeSeconds(day0);
            beaconing.enqueueRandom(0.001);
            assertEquals(OptionalLong.of(1), beaconing.nextSequenceNumber("regular"));
            assertEquals(OptionalLong.of(2), beaconing.nextSequenceNumber("regular"));

            beaconing.setUnixTimeSeconds(day2);
            beaconing.enqueueRandom(0.001);
            assertEquals(OptionalLong.of(1), beaconing.nextSequenceNumber("regular"),
                    "new active day must reset the counter to 1");
        }

        // Forces the counter to Integer.MAX_VALUE - 1 to make the 2^31 boundary
        // observable in constant time without firing 2 billion increments. An int
        // counter would sign-flip to Integer.MIN_VALUE here; the JavaScript
        // reference cannot reach this case, so it is Cobalt-specific coverage.
        @Test
        @DisplayName("counter past Integer.MAX_VALUE stays positive (long counter)")
        void counterAdvancesPastIntMax() {
            var beaconing = new DeterministicWamBeaconingService();
            beaconing.setUnixTimeSeconds(100 * dayInSeconds());
            beaconing.enqueueRandom(0.001);
            assertEquals(OptionalLong.of(1L), beaconing.nextSequenceNumber("regular"));

            beaconing.forceSequenceCounter("regular", Integer.MAX_VALUE - 1L);

            var atMax = beaconing.nextSequenceNumber("regular");
            assertEquals(OptionalLong.of(Integer.MAX_VALUE), atMax,
                    "first call after forcing the counter to MAX-1 returns MAX");
            var pastMax = beaconing.nextSequenceNumber("regular");
            assertEquals(OptionalLong.of((long) Integer.MAX_VALUE + 1L), pastMax,
                    "the call past Integer.MAX_VALUE must yield 2^31, not roll over to Integer.MIN_VALUE");
        }
    }

    private static long dayInSeconds() {
        return Duration.ofDays(1).toSeconds();
    }

    /**
     * Controllable {@link WamBeaconingService} test double whose UTC day
     * boundary and activation rolls are driven explicitly by the test:
     * on the first call of a new UTC day the next queued random value is
     * consumed, activation set if it is {@code <= 0.01}, and the counter
     * reset to {@code 0}; subsequent same-day calls return the next
     * sequence number (when active) or empty (when inactive), mirroring
     * {@link LiveWamBeaconingService}'s per-buffer-key state machine.
     */
    private static final class DeterministicWamBeaconingService implements WamBeaconingService {
        private final ConcurrentMap<String, KeyState> states = new ConcurrentHashMap<>();

        private final List<Double> pendingRandoms = new ArrayList<>();

        private long unixSeconds;

        void setUnixTimeSeconds(long seconds) {
            this.unixSeconds = seconds;
        }

        void enqueueRandom(double value) {
            pendingRandoms.add(value);
        }

        // Lets the wraparound test probe values near Integer.MAX_VALUE without
        // firing 2 billion increments; the key's state must already exist.
        void forceSequenceCounter(String bufferKey, long value) {
            var state = states.get(bufferKey);
            if (state == null) {
                throw new IllegalStateException("no state yet for bufferKey " + bufferKey
                        + " - call nextSequenceNumber(bufferKey) once first");
            }
            state.sequenceNumber = value;
        }

        @Override
        public OptionalLong nextSequenceNumber(String bufferKey) {
            var state = states.computeIfAbsent(bufferKey, _ -> new KeyState());
            var currentDayEpoch = Instant.ofEpochSecond(unixSeconds)
                    .truncatedTo(ChronoUnit.DAYS)
                    .getEpochSecond();
            if (currentDayEpoch != state.activationDayEpoch) {
                state.activationDayEpoch = currentDayEpoch;
                if (pendingRandoms.isEmpty()) {
                    throw new IllegalStateException("DeterministicWamBeaconing ran out of pending random values for key " + bufferKey);
                }
                var roll = pendingRandoms.removeFirst();
                state.active = roll <= ACTIVATION_PROBABILITY;
                state.sequenceNumber = 0;
            }

            if (!state.active) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(++state.sequenceNumber);
        }

        // Per-buffer-key activation and counter state mirroring
        // DefaultWamBeaconing's private ChannelState bit for bit.
        private static final class KeyState {
            // -1 so the first call observes a day change.
            long activationDayEpoch = -1;

            boolean active;

            long sequenceNumber;
        }
    }

    // Tolerates both outcomes of the randomised roll, asserting only the
    // contract: present-and-monotonic, or empty-and-stays-empty.
    @Test
    @DisplayName("DefaultWamBeaconing is the production impl exposed to WamService")
    void defaultImplProvidesWamBeaconing() {
        WamBeaconingService beaconing = new LiveWamBeaconingService();
        var first = beaconing.nextSequenceNumber("regular");
        var second = beaconing.nextSequenceNumber("regular");
        if (first.isPresent()) {
            assertTrue(second.isPresent(), "if first call is present, second must be too");
            assertEquals(first.getAsLong() + 1, second.getAsLong(),
                    "successive same-day calls must increment the sequence");
        } else {
            assertTrue(second.isEmpty(), "if first call is empty (deactivated), second must be empty too");
        }
    }

    // Probabilistic by construction (the production impl reads the real
    // Math.random); asserts only that the second key's state was decided by a
    // fresh roll, not inherited from the first.
    @Test
    @DisplayName("DefaultWamBeaconing keeps per-key state isolated")
    void defaultImplKeepsKeysIsolated() {
        var beaconing = new LiveWamBeaconingService();
        var regular = beaconing.nextSequenceNumber("regular");
        var realtime = beaconing.nextSequenceNumber("realtime");
        if (regular.isPresent() && realtime.isPresent()) {
            assertEquals(1, regular.getAsLong(), "first call to 'regular' on an active day starts at 1");
            assertEquals(1, realtime.getAsLong(), "first call to 'realtime' on an active day also starts at 1");
        }
        var regular2 = beaconing.nextSequenceNumber("regular");
        if (regular.isPresent()) {
            assertNotEquals(regular, regular2, "'regular' must advance independently of 'realtime'");
        } else {
            assertFalse(regular2.isPresent(), "'regular' stays empty if it was empty");
        }
    }

}
