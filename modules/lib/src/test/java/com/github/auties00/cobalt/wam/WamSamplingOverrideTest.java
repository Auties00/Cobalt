package com.github.auties00.cobalt.wam;

import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.wire.wam.event.PsIdUpdateEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WamClientErrorsEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.PsIdAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behavioural tests for {@link WamSamplingOverride}, the runtime override
 * map that {@link WamService} consults before falling back to an event's
 * static {@code @WamEvent(releaseWeight)} annotation.
 *
 * <p>Covers the four properties callers depend on: {@code put} then
 * {@code get} returns the inserted weight; {@code remove} clears the
 * override; {@code replaceAll} swaps the entire override set, discarding
 * previous keys; and concurrent writes from many virtual threads do not
 * lose updates.
 */
@DisplayName("WamSamplingOverride")
class WamSamplingOverrideTest {
    @Test
    @DisplayName("get returns empty for unknown event ids")
    void getMissingReturnsEmpty() {
        var overrides = new WamSamplingOverride();
        assertTrue(overrides.get(1234).isEmpty());
        assertTrue(overrides.get(0).isEmpty());
        assertTrue(overrides.get(Integer.MAX_VALUE).isEmpty());
    }

    @Test
    @DisplayName("put then get returns the inserted weight")
    void putThenGet() {
        var overrides = new WamSamplingOverride();
        overrides.put(2862, 10);
        assertEquals(10, overrides.get(2862).orElseThrow());
        assertTrue(overrides.get(2861).isEmpty(), "neighbouring id stays empty");
    }

    @Test
    @DisplayName("second put for the same id overrides the first")
    void putOverwrites() {
        var overrides = new WamSamplingOverride();
        overrides.put(2862, 10);
        overrides.put(2862, 50);
        assertEquals(50, overrides.get(2862).orElseThrow());
    }

    @Test
    @DisplayName("remove clears the override")
    void removeClears() {
        var overrides = new WamSamplingOverride();
        overrides.put(2862, 10);
        overrides.remove(2862);
        assertTrue(overrides.get(2862).isEmpty());
    }

    @Test
    @DisplayName("replaceAll swaps the entire override set")
    void replaceAllSwapsState() {
        var overrides = new WamSamplingOverride();
        overrides.put(100, 1);
        overrides.put(200, 2);
        overrides.put(300, 3);

        var next = new HashMap<Integer, Integer>();
        next.put(400, 4);
        next.put(500, 5);
        overrides.replaceAll(next);

        assertTrue(overrides.get(100).isEmpty(), "old key 100 dropped");
        assertTrue(overrides.get(200).isEmpty(), "old key 200 dropped");
        assertTrue(overrides.get(300).isEmpty(), "old key 300 dropped");
        assertEquals(4, overrides.get(400).orElseThrow());
        assertEquals(5, overrides.get(500).orElseThrow());
    }

    @Test
    @DisplayName("replaceAll with an empty map clears all overrides")
    void replaceAllWithEmptyClears() {
        var overrides = new WamSamplingOverride();
        overrides.put(100, 1);
        overrides.put(200, 2);
        overrides.replaceAll(Map.of());
        assertTrue(overrides.get(100).isEmpty());
        assertTrue(overrides.get(200).isEmpty());
    }

    @Test
    @DisplayName("concurrent puts to distinct keys all land")
    void concurrentDistinctKeyPuts() throws InterruptedException {
        var overrides = new WamSamplingOverride();
        var threadCount = 64;
        var startGate = new CountDownLatch(1);
        var done = new CountDownLatch(threadCount);
        var observed = new AtomicInteger();

        for (var i = 0; i < threadCount; i++) {
            final var key = i;
            Thread.ofVirtual().start(() -> {
                try {
                    startGate.await();
                    overrides.put(key, key * 10);
                    observed.incrementAndGet();
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS), "all writer threads must complete within 5 seconds");
        assertEquals(threadCount, observed.get(), "every writer thread must have completed its put");

        IntStream.range(0, threadCount).forEach(i ->
                assertEquals(i * 10, overrides.get(i).orElseThrow(),
                        () -> "concurrent put for key " + i + " was lost"));
    }

    // Cross-checks the fallback target on real Cobalt events; the annotation
    // weights are not the contract, only that they are positive integers, the
    // documented invariant for any WAM sampling weight.
    @Test
    @DisplayName("removed override surfaces empty so callers fall back to static @WamEvent weight")
    void emptyAfterRemoveSupportsFallback() {
        var overrides = new WamSamplingOverride();
        overrides.put(2862, 100);
        overrides.remove(2862);
        assertTrue(overrides.get(2862).isEmpty(),
                "after remove, get() must return empty so WamService falls back to releaseWeight()");

        var psIdUpdate = new PsIdUpdateEventBuilder()
                .psIdAction(PsIdAction.CREATED)
                .psIdKey(1)
                .psIdRotationFrequence(7)
                .build();
        var clientErrors = new WamClientErrorsEventBuilder()
                .wamClientBufferDropErrorCount(1)
                .build();
        assertTrue(psIdUpdate.releaseWeight() > 0,
                "PsIdUpdateEvent's static releaseWeight must be positive");
        assertTrue(clientErrors.releaseWeight() > 0,
                "WamClientErrorsEvent's static releaseWeight must be positive");
    }

    @Test
    @DisplayName("replaceAll(props.samplingConfigs()) picks up AB-props refreshes")
    void abPropsRefreshConverges() {
        var overrides = new WamSamplingOverride();
        var props = TestABPropsService.builder().build();

        overrides.replaceAll(props.samplingConfigs());
        assertTrue(overrides.get(2862).isEmpty(),
                "no AB-props config -> no override");

        props.updateEventSamplingConfigs(Map.of(2862, 5));
        overrides.replaceAll(props.samplingConfigs());
        assertEquals(5, overrides.get(2862).orElseThrow(),
                "first AB-props refresh must install the weight");

        props.updateEventSamplingConfigs(Map.of(2862, 25));
        overrides.replaceAll(props.samplingConfigs());
        assertEquals(25, overrides.get(2862).orElseThrow(),
                "subsequent AB-props refresh must update the weight, not append");
    }

    // Either the final put landed or the final remove did; both are acceptable,
    // the assertion is only that the result is not garbage.
    @Test
    @DisplayName("interleaved put/remove on the same key reaches a consistent state")
    void interleavedPutRemove() throws InterruptedException {
        var overrides = new WamSamplingOverride();
        var key = 42;
        var rounds = 1_000;
        var startGate = new CountDownLatch(1);
        var done = new CountDownLatch(2);

        Thread.ofVirtual().start(() -> {
            try {
                startGate.await();
                for (var i = 0; i < rounds; i++) overrides.put(key, i);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        });
        Thread.ofVirtual().start(() -> {
            try {
                startGate.await();
                for (var i = 0; i < rounds; i++) overrides.remove(key);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        });

        startGate.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        var result = overrides.get(key);
        assertTrue(result.isEmpty() || result.getAsInt() >= 0,
                "final state must be either empty or a valid weight, was " + result);
    }
}
