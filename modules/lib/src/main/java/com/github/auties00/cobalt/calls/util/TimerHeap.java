package com.github.auties00.cobalt.calls.util;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

/**
 * A binary min heap scheduler that fires {@link TimerEntry} callbacks in
 * deadline order, breaking ties by schedule order, and cancels a scheduled entry
 * in constant time.
 *
 * <p>The heap orders entries by their absolute deadline first and, among entries
 * sharing an identical deadline, by the monotonically increasing sequence assigned
 * at {@link #schedule(long, Duration, Runnable)} time, so equal deadlines fire
 * first in first out. Deadlines live in a nanosecond timebase: a relative delay
 * passed to {@link #schedule(long, Duration, Runnable)} is added to the current time
 * supplied to that same call, and {@link #poll(long)} compares against the current
 * time supplied to it; all such readings are expected to come from a single monotonic
 * source such as {@link System#nanoTime()}. The heap performs no
 * timing of its own and starts no threads; a caller drives it by repeatedly invoking
 * {@link #poll(long)} with the current time and sleeping for the returned delay,
 * which keeps the scheduling policy deterministic and unit testable.
 *
 * <p>Each entry remembers its slot in the backing array, so {@link #cancel(TimerEntry)}
 * locates the entry without scanning, swaps it with the last live entry, shrinks the
 * heap, and reheapifies in {@code O(log n)}. The backing array grows by doubling
 * when full and never shrinks. This class is not thread safe: a single owner thread
 * is expected to perform all scheduling, cancelling, and polling, matching the
 * single timer thread model the call layer uses.
 *
 * @implNote This implementation folds the id to slot mapping directly into the
 * {@link TimerEntry#slot} field carried on each entry rather than maintaining a
 * separate index array from timer id to heap slot, because JVM object references
 * remove the need for the integer id indirection and its free list while preserving
 * the {@code O(1)} cancel. Parent and child navigation uses the standard array heap
 * arithmetic ({@code (i - 1) >> 1} for the parent, {@code 2i + 1} and {@code 2i + 2}
 * for the children), and the backing array grows by doubling on overflow and never
 * shrinks. An empty {@link #poll(long)} returns {@link Long#MAX_VALUE} nanoseconds,
 * and no per poll cap is applied on the number of due entries drained: the caller
 * fires every due entry on its own virtual thread. The deadline is paired with a
 * per entry {@link TimerEntry#sequence()} tie break key so insertion order is
 * preserved among equal deadlines.
 */
public final class TimerHeap {
    /**
     * The logger for {@link TimerHeap}.
     */
    private static final System.Logger LOGGER = Log.get(TimerHeap.class);

    /**
     * Initial capacity of the backing array before the first growth.
     *
     * <p>Chosen to hold the handful of concurrent timeouts a single call typically
     * arms (offer/answer, video upgrade, keepalive, reconnect) without an early
     * resize.
     */
    private static final int INITIAL_CAPACITY = 16;

    /**
     * Backing array holding the live entries in heap order, with the earliest
     * deadline at index {@code 0}.
     *
     * <p>Slots {@code [0, size)} hold live entries; slots from {@code size} to the
     * array length are unused and held {@code null}. The array grows by doubling and
     * never shrinks.
     */
    private TimerEntry[] heap;

    /**
     * Number of live entries currently in the heap.
     *
     * <p>Equal to the count of populated prefix slots in {@link #heap}; the
     * root entry occupies index {@code 0} when this is positive.
     */
    private int size;

    /**
     * Monotonically increasing counter handed to each scheduled entry as its
     * tie break sequence.
     *
     * <p>Incremented on every {@link #schedule(long, Duration, Runnable)} so that no two
     * entries ever share a sequence, which makes the deadline then sequence ordering
     * a total order and guarantees deterministic first in first out firing among
     * equal deadlines.
     */
    private long sequenceCounter;

    /**
     * Constructs an empty timer heap with the default initial capacity.
     *
     * <p>The heap starts with no entries; {@link #poll(long)} on a fresh heap returns
     * {@link Long#MAX_VALUE}.
     */
    public TimerHeap() {
        this.heap = new TimerEntry[INITIAL_CAPACITY];
        this.size = 0;
        this.sequenceCounter = 0;
    }

    /**
     * Schedules the callback to fire once the given delay elapses after the supplied
     * current time and returns its handle.
     *
     * <p>The entry's absolute deadline is {@code nowNanos} plus the delay in
     * nanoseconds, so a delay of zero or less yields a deadline at or before
     * {@code nowNanos} and the entry becomes due on the next {@link #poll(long)}.
     * Passing the current time explicitly keeps the heap driven by a single
     * caller owned clock shared with {@link #poll(long)} and {@link #pollDue(long)},
     * which makes scheduling deterministic and unit testable. The returned
     * {@link TimerEntry} can be retained to {@link TimerEntry#cancel()} the timeout
     * before it fires. Each call advances the internal sequence counter so the new
     * entry sorts after any equal deadline entry scheduled earlier.
     *
     * @apiNote Pass the same monotonic reading, such as {@link System#nanoTime()},
     *          that the polling loop uses; retain the returned handle to disarm the
     *          timeout when the awaited event arrives first, or discard it for a
     *          fire and forget timeout that must always run.
     * @param nowNanos the current time in the heap's nanosecond timebase from which the
     *                 delay is measured
     * @param delay    the delay before the callback fires; must not be {@code null}
     * @param callback the task to run when the entry fires; must not be {@code null}
     * @return the handle for the newly scheduled entry
     * @throws NullPointerException if {@code delay} or {@code callback} is {@code null}
     */
    public TimerEntry schedule(long nowNanos, Duration delay, Runnable callback) {
        Objects.requireNonNull(delay, "delay must not be null");
        Objects.requireNonNull(callback, "callback must not be null");
        var deadline = nowNanos + delay.toNanos();
        var entry = new TimerEntry(this, deadline, sequenceCounter++, callback);
        if (size == heap.length) {
            heap = Arrays.copyOf(heap, heap.length << 1);
        }
        heap[size] = entry;
        siftUp(size);
        size++;
        if (Log.TRACE) LOGGER.log(Level.TRACE, "timer scheduled seq={0} delayMs={1} size={2}", entry.sequence(), delay.toMillis(), size);
        return entry;
    }

    /**
     * Removes the next due entry and returns it, or returns {@code null} when no entry
     * is due at the given time.
     *
     * <p>An entry is due when its deadline is less than or equal to {@code now}.
     * Because the root holds the earliest deadline, this inspects only the root: if the
     * heap is empty or the root is not yet due it returns {@code null}; otherwise it
     * removes the root, reheapifies, marks the removed entry as no longer scheduled,
     * and returns it without running its callback. A caller fires due entries by
     * looping on this method until it returns {@code null}, then sleeps for
     * {@link #poll(long)}.
     *
     * @param now the current time in the heap's nanosecond timebase
     * @return the earliest due entry, or {@code null} if none is due
     */
    public TimerEntry pollDue(long now) {
        if (size == 0 || heap[0].deadline() > now) {
            return null;
        }
        var due = removeAt(0);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "timer fired seq={0} size={1}", due.sequence(), size);
        return due;
    }

    /**
     * Returns the time in nanoseconds until the earliest entry is due, or
     * {@link Long#MAX_VALUE} when the heap is empty.
     *
     * <p>The result is {@code root.deadline - now}, clamped to {@code 0} so an
     * already due or overdue root reports no wait. A caller treats {@code 0} as a
     * signal to drain due entries with {@link #pollDue(long)} immediately, and any
     * positive value as the maximum time it may sleep before the next entry must fire.
     * An empty heap reports {@link Long#MAX_VALUE} so the caller waits until a new
     * entry is scheduled.
     *
     * @param now the current time in the heap's nanosecond timebase
     * @return the nonnegative nanoseconds until the next deadline, or
     *         {@link Long#MAX_VALUE} if the heap is empty
     */
    public long poll(long now) {
        if (size == 0) {
            return Long.MAX_VALUE;
        }
        var remaining = heap[0].deadline() - now;
        return Math.max(remaining, 0L);
    }

    /**
     * Reports whether the heap currently holds no entries.
     *
     * @return {@code true} if there are no scheduled entries, {@code false} otherwise
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the number of entries currently scheduled.
     *
     * @return the live entry count
     */
    public int size() {
        return size;
    }

    /**
     * Removes the given entry from the heap in constant locate time and reheapifies.
     *
     * <p>Uses the entry's recorded {@link TimerEntry#slot} to find it without scanning,
     * then delegates to {@link #removeAt(int)}. Returns {@code false} without touching
     * the heap when the entry does not belong to this heap or is already inert, which
     * makes a double cancel safe. Callers reach this through {@link TimerEntry#cancel()}.
     *
     * @param entry the entry to remove; must not be {@code null}
     * @return {@code true} if a still scheduled entry was removed, {@code false} if it
     *         was already inert
     * @throws NullPointerException if {@code entry} is {@code null}
     */
    boolean cancel(TimerEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        var slot = entry.slot;
        if (slot == TimerEntry.NOT_SCHEDULED || slot >= size || heap[slot] != entry) {
            return false;
        }
        removeAt(slot);
        if (Log.TRACE) LOGGER.log(Level.TRACE, "timer cancelled seq={0} size={1}", entry.sequence(), size);
        return true;
    }

    /**
     * Removes the entry occupying the given slot, restores the heap invariant, and
     * marks the removed entry as no longer scheduled.
     *
     * <p>Swaps the target slot with the last live entry, clears the vacated tail slot,
     * shrinks {@link #size}, then restores order at the target slot by sifting it down
     * and, if it did not move, up. Sifting both directions is necessary because the
     * relocated last entry may belong either below or above the removed position. The
     * removed entry's {@link TimerEntry#slot} is set to {@link TimerEntry#NOT_SCHEDULED}
     * so it reports {@link TimerEntry#isScheduled()} as {@code false} and a later cancel
     * has no effect.
     *
     * @param slot the index of the entry to remove; must be in {@code [0, size)}
     * @return the entry that was removed from the heap
     */
    private TimerEntry removeAt(int slot) {
        var removed = heap[slot];
        var last = --size;
        var moved = heap[last];
        heap[last] = null;
        removed.slot = TimerEntry.NOT_SCHEDULED;
        if (slot != last) {
            heap[slot] = moved;
            moved.slot = slot;
            siftDown(slot);
            if (heap[slot] == moved) {
                siftUp(slot);
            }
        }
        return removed;
    }

    /**
     * Moves the entry at the given slot toward the root until its parent no longer
     * orders after it, restoring the min heap invariant after an insertion or removal.
     *
     * <p>At each step compares the entry with its parent at {@code (index - 1) >> 1};
     * if the entry orders strictly before the parent it swaps them and continues from
     * the parent slot, otherwise it stops. Slot fields are updated on every swap so the
     * {@link TimerEntry#slot} of each moved entry stays accurate.
     *
     * @param index the slot whose entry may need to rise; must be in {@code [0, size)}
     */
    private void siftUp(int index) {
        var entry = heap[index];
        while (index > 0) {
            var parentIndex = (index - 1) >> 1;
            var parent = heap[parentIndex];
            if (compare(entry, parent) >= 0) {
                break;
            }
            heap[index] = parent;
            parent.slot = index;
            index = parentIndex;
        }
        heap[index] = entry;
        entry.slot = index;
    }

    /**
     * Moves the entry at the given slot toward the leaves until neither child orders
     * before it, restoring the min heap invariant after a removal.
     *
     * <p>At each step selects the smaller ordering of the two children at {@code 2*index+1}
     * and {@code 2*index+2}; if that child orders strictly before the entry it swaps them
     * and continues from the child slot, otherwise it stops. Slot fields are updated on
     * every swap so the {@link TimerEntry#slot} of each moved entry stays accurate.
     *
     * @param index the slot whose entry may need to descend; must be in {@code [0, size)}
     */
    private void siftDown(int index) {
        var entry = heap[index];
        var half = size >> 1;
        while (index < half) {
            var childIndex = (index << 1) + 1;
            var child = heap[childIndex];
            var rightIndex = childIndex + 1;
            if (rightIndex < size && compare(heap[rightIndex], child) < 0) {
                childIndex = rightIndex;
                child = heap[rightIndex];
            }
            if (compare(entry, child) <= 0) {
                break;
            }
            heap[index] = child;
            child.slot = index;
            index = childIndex;
        }
        heap[index] = entry;
        entry.slot = index;
    }

    /**
     * Compares two entries by deadline, breaking ties by schedule sequence.
     *
     * <p>Returns a negative, zero, or positive value as the first entry orders before,
     * equal to, or after the second. Entries are first ordered by ascending
     * {@link TimerEntry#deadline()}; entries with an equal deadline are ordered by
     * ascending {@link TimerEntry#sequence()}, which is unique per entry, so the
     * comparison is a total order and yields first in first out firing among equal
     * deadlines.
     *
     * @param left  the first entry; must not be {@code null}
     * @param right the second entry; must not be {@code null}
     * @return a negative, zero, or positive value per the ordering described above
     */
    private static int compare(TimerEntry left, TimerEntry right) {
        var byDeadline = Long.compare(left.deadline(), right.deadline());
        if (byDeadline != 0) {
            return byDeadline;
        }
        return Long.compare(left.sequence(), right.sequence());
    }
}
