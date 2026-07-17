package com.github.auties00.cobalt.calls.util;

import java.time.Duration;

/**
 * A single scheduled task held by a {@link TimerHeap}, pairing an absolute fire
 * deadline with the callback to run when that deadline elapses.
 *
 * <p>An entry is created by {@link TimerHeap#schedule(long, Duration, Runnable)} and is
 * returned to the caller as an opaque handle whose only public capability is
 * {@link #cancel()}. The deadline is expressed in the same nanosecond timebase the
 * owning {@link TimerHeap} uses for {@link TimerHeap#poll(long)}; it is a monotonic
 * reading such as {@link System#nanoTime()}, not a wall clock instant, so it is
 * immune to clock adjustments. Among entries that share an identical deadline the
 * {@code sequence} assigned at schedule time provides a stable first in first out
 * ordering, so two timeouts armed for the same instant always fire in the order they
 * were scheduled.
 *
 * <p>An entry tracks its current slot in the owning heap's backing array so the heap
 * can relocate it in {@code O(log n)} during sift operations and remove it in
 * {@code O(1)} on {@link #cancel()} without scanning. A cancelled or already fired
 * entry holds {@link #NOT_SCHEDULED} as its slot and will never run; only
 * {@link TimerHeap} mutates the slot.
 *
 * @implNote This implementation collapses the absolute fire time onto a single
 * nanosecond {@link #deadline} rather than a split second/microsecond pair, since the
 * nanosecond timebase already carries full resolution. No use after free guard is
 * carried on the entry because JVM references make dangling access impossible. The
 * {@link #sequence} tie breaker preserves insertion order among entries that share an
 * identical deadline.
 */
public final class TimerEntry {
    /**
     * Slot sentinel marking an entry that is not currently present in any heap,
     * either because it has been cancelled or because its callback has already
     * fired.
     *
     * <p>An entry holding this value as its {@link #slot} is inert: a subsequent
     * {@link #cancel()} does nothing and the heap will never sift or fire it.
     */
    static final int NOT_SCHEDULED = -1;

    /**
     * Heap that owns this entry and against which {@link #cancel()} delegates.
     *
     * <p>Never {@code null}; an entry cannot exist without an owning heap because
     * the only constructor is invoked from within {@link TimerHeap#schedule(long, Duration, Runnable)}.
     */
    private final TimerHeap heap;

    /**
     * Absolute fire time of this entry, in the owning heap's nanosecond timebase.
     *
     * <p>An entry becomes due once {@link TimerHeap#poll(long)} is called with a
     * {@code now} that is greater than or equal to this value. The deadline is the
     * primary heap ordering key.
     */
    private final long deadline;

    /**
     * Monotonically increasing schedule order used as the secondary ordering key.
     *
     * <p>Assigned from the owning heap's schedule counter at construction, this
     * value breaks ties between entries with an equal {@link #deadline} so that the
     * earlier scheduled entry is ordered first, yielding first in first out firing
     * among equal deadlines.
     */
    private final long sequence;

    /**
     * Callback executed when this entry fires.
     *
     * <p>Invoked at most once, synchronously on the thread that calls
     * {@link TimerHeap#poll(long)}, and never after {@link #cancel()} has removed
     * the entry. Never {@code null}.
     */
    private final Runnable callback;

    /**
     * Index of this entry within the owning heap's backing array, or
     * {@link #NOT_SCHEDULED} once the entry has been cancelled or fired.
     *
     * <p>Maintained by {@link TimerHeap} as the entry is sifted up or down so that
     * {@link #cancel()} can locate and remove the entry in constant time. Only the
     * owning heap mutates this field.
     */
    int slot;

    /**
     * Constructs a scheduled entry bound to the given heap, deadline, sequence, and
     * callback.
     *
     * <p>The new entry starts with its {@link #slot} set to {@link #NOT_SCHEDULED};
     * the owning heap assigns the real slot when it inserts the entry into its
     * backing array. Entries are created exclusively by
     * {@link TimerHeap#schedule(long, Duration, Runnable)}.
     *
     * @param heap     the owning heap, must not be {@code null}
     * @param deadline the absolute fire time in the heap's nanosecond timebase
     * @param sequence the schedule order tie breaker for equal deadlines
     * @param callback the task to run when the entry fires, must not be {@code null}
     */
    TimerEntry(TimerHeap heap, long deadline, long sequence, Runnable callback) {
        this.heap = heap;
        this.deadline = deadline;
        this.sequence = sequence;
        this.callback = callback;
        this.slot = NOT_SCHEDULED;
    }

    /**
     * Returns the absolute fire time of this entry in the owning heap's nanosecond
     * timebase.
     *
     * @return the deadline against which {@link TimerHeap#poll(long)} compares
     *         {@code now}
     */
    long deadline() {
        return deadline;
    }

    /**
     * Returns the schedule order tie breaker assigned to this entry.
     *
     * @return the monotonically increasing sequence used to order entries with an
     *         equal {@link #deadline}
     */
    long sequence() {
        return sequence;
    }

    /**
     * Returns the callback this entry runs when it fires.
     *
     * @return the task supplied at schedule time
     */
    Runnable callback() {
        return callback;
    }

    /**
     * Reports whether this entry is currently present in its owning heap.
     *
     * <p>Returns {@code false} once the entry has been cancelled or its callback has
     * fired, in which case its {@link #slot} holds {@link #NOT_SCHEDULED}.
     *
     * @return {@code true} if the entry is still scheduled, {@code false} otherwise
     */
    public boolean isScheduled() {
        return slot != NOT_SCHEDULED;
    }

    /**
     * Cancels this entry so its callback never runs.
     *
     * <p>Removes the entry from the owning heap in constant time and reheapifies the
     * remaining entries. Cancelling an entry that has already fired or been cancelled
     * does nothing. After this call the entry reports {@link #isScheduled()} as
     * {@code false}.
     *
     * @apiNote Callers arm a timeout by retaining the {@link TimerEntry} returned
     *          from {@link TimerHeap#schedule(long, Duration, Runnable)} and invoke this
     *          method to disarm it when the awaited event arrives first, for example
     *          when a peer answers before the offer timeout elapses.
     * @return {@code true} if this call removed a still scheduled entry,
     *         {@code false} if it was already inert
     */
    public boolean cancel() {
        return heap.cancel(this);
    }
}
