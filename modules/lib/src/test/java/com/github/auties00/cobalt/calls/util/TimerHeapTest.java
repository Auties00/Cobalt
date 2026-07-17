package com.github.auties00.cobalt.calls.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TimerHeap scheduler")
class TimerHeapTest {
    private static Runnable noop() {
        return () -> {
        };
    }

    @Test
    @DisplayName("an empty heap reports Long.MAX_VALUE and no due entry")
    void emptyHeap() {
        var heap = new TimerHeap();
        assertTrue(heap.isEmpty());
        assertEquals(0, heap.size());
        assertEquals(Long.MAX_VALUE, heap.poll(0));
        assertNull(heap.pollDue(Long.MAX_VALUE));
    }

    @Test
    @DisplayName("drains entries in ascending deadline order regardless of insert order")
    void minOrder() {
        var heap = new TimerHeap();
        var order = new ArrayList<Integer>();
        heap.schedule(0, Duration.ofNanos(50), () -> order.add(50));
        heap.schedule(0, Duration.ofNanos(10), () -> order.add(10));
        heap.schedule(0, Duration.ofNanos(30), () -> order.add(30));
        heap.schedule(0, Duration.ofNanos(20), () -> order.add(20));
        heap.schedule(0, Duration.ofNanos(40), () -> order.add(40));

        TimerEntry due;
        while ((due = heap.pollDue(Long.MAX_VALUE)) != null) {
            due.callback().run();
        }
        assertEquals(List.of(10, 20, 30, 40, 50), order);
        assertTrue(heap.isEmpty());
    }

    @Test
    @DisplayName("breaks equal-deadline ties first-in-first-out")
    void fifoTieBreak() {
        var heap = new TimerHeap();
        var order = new ArrayList<Integer>();
        for (var i = 0; i < 8; i++) {
            var id = i;
            heap.schedule(0, Duration.ofNanos(100), () -> order.add(id));
        }
        TimerEntry due;
        while ((due = heap.pollDue(Long.MAX_VALUE)) != null) {
            due.callback().run();
        }
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7), order);
    }

    @Test
    @DisplayName("orders by deadline first then by schedule sequence")
    void deadlineThenSequence() {
        var heap = new TimerHeap();
        var order = new ArrayList<String>();
        heap.schedule(0, Duration.ofNanos(20), () -> order.add("b-late"));
        heap.schedule(0, Duration.ofNanos(10), () -> order.add("a1"));
        heap.schedule(0, Duration.ofNanos(10), () -> order.add("a2"));
        heap.schedule(0, Duration.ofNanos(10), () -> order.add("a3"));

        TimerEntry due;
        while ((due = heap.pollDue(Long.MAX_VALUE)) != null) {
            due.callback().run();
        }
        assertEquals(List.of("a1", "a2", "a3", "b-late"), order);
    }

    @Test
    @DisplayName("pollDue yields only entries due at the supplied time")
    void pollDueRespectsDeadline() {
        var heap = new TimerHeap();
        heap.schedule(0, Duration.ofNanos(10), noop());
        heap.schedule(0, Duration.ofNanos(100), noop());

        assertNull(heap.pollDue(5));
        assertEquals(10, heap.poll(0));

        var first = heap.pollDue(10);
        assertEquals(10, first.deadline());
        assertNull(heap.pollDue(10));
        assertEquals(90, heap.poll(10));
    }

    @Test
    @DisplayName("poll clamps an overdue deadline to zero")
    void pollClampsOverdue() {
        var heap = new TimerHeap();
        heap.schedule(0, Duration.ofNanos(10), noop());
        assertEquals(0, heap.poll(1000));
    }

    @Test
    @DisplayName("cancel removes an entry in place and preserves remaining order")
    void cancelByHandle() {
        var heap = new TimerHeap();
        var order = new ArrayList<Integer>();
        heap.schedule(0, Duration.ofNanos(10), () -> order.add(10));
        var middle = heap.schedule(0, Duration.ofNanos(20), () -> order.add(20));
        heap.schedule(0, Duration.ofNanos(30), () -> order.add(30));

        assertTrue(middle.isScheduled());
        assertTrue(middle.cancel());
        assertFalse(middle.isScheduled());
        assertEquals(2, heap.size());

        TimerEntry due;
        while ((due = heap.pollDue(Long.MAX_VALUE)) != null) {
            due.callback().run();
        }
        assertEquals(List.of(10, 30), order);
    }

    @Test
    @DisplayName("a double cancel is a no-op")
    void doubleCancelIsNoop() {
        var heap = new TimerHeap();
        var entry = heap.schedule(0, Duration.ofNanos(10), noop());
        assertTrue(entry.cancel());
        assertFalse(entry.cancel());
        assertTrue(heap.isEmpty());
    }

    @Test
    @DisplayName("a polled entry reports itself no longer scheduled")
    void polledEntryNotScheduled() {
        var heap = new TimerHeap();
        var entry = heap.schedule(0, Duration.ofNanos(10), noop());
        var polled = heap.pollDue(Long.MAX_VALUE);
        assertSame(entry, polled);
        assertFalse(polled.isScheduled());
        assertFalse(polled.cancel());
    }

    @Test
    @DisplayName("grows past its initial capacity and keeps total ordering")
    void growthKeepsOrder() {
        var heap = new TimerHeap();
        var count = 100;
        for (var i = count; i >= 1; i--) {
            heap.schedule(0, Duration.ofNanos(i), noop());
        }
        assertEquals(count, heap.size());
        var previous = Long.MIN_VALUE;
        TimerEntry due;
        var drained = 0;
        while ((due = heap.pollDue(Long.MAX_VALUE)) != null) {
            assertTrue(due.deadline() >= previous, "deadlines must be non-decreasing");
            previous = due.deadline();
            drained++;
        }
        assertEquals(count, drained);
    }
}
