package com.github.auties00.cobalt.calls.util;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Accumulates timestamped samples into a sixty slot ring at one second granularity,
 * the rolling last minute window used for short horizon call telemetry.
 *
 * <p>The buffer holds one slot per second of the trailing minute. Each
 * {@link #insert(long, long)} either folds its sample into the current slot, when the
 * sample falls within the same one second bucket as the slot's timestamp, or opens a
 * new slot for a later bucket. Opening a new slot grows the live count until all sixty
 * slots are in use, after which the oldest slot is overwritten, so the buffer always
 * reflects at most the last sixty seconds of activity. Aggregates over the window, the
 * sum and the live sample count, are read through {@link #sum()} and {@link #count()};
 * the buffer is unit agnostic and simply totals whatever amounts are inserted.
 *
 * <p>Timestamps supplied to {@link #insert(long, long)} are milliseconds from a single
 * monotonic source and must not decrease across calls, since the buffer compares
 * each sample against the most recent slot's timestamp to decide whether to fold or
 * advance. A fresh buffer holds no slots; the first insert opens the first slot.
 * Instances are not thread safe; the owning statistics path drives one buffer from a
 * single thread.
 *
 * @implNote This implementation locates the current slot at {@code (head + count - 1) %
 * SLOT_COUNT}, folds the sample into that slot when the elapsed time since its timestamp
 * is below the {@link #BUCKET_MILLIS} bucket, and otherwise advances to a new slot,
 * growing the count up to {@link #SLOT_COUNT} before overwriting the oldest. Samples are
 * held as Java {@code long} totals rather than a fixed width record to avoid overflow on
 * large accumulations, and {@link #runningTotal} is maintained incrementally so
 * {@link #sum()} never rescans the ring.
 */
public final class LastMinuteBuffer {
    /**
     * The logger for {@link LastMinuteBuffer}.
     */
    private static final System.Logger LOGGER = Log.get(LastMinuteBuffer.class);

    /**
     * Number of slots in the ring, one per second of the trailing minute.
     */
    private static final int SLOT_COUNT = 60;

    /**
     * Duration of a single slot's bucket in milliseconds.
     *
     * <p>A sample whose elapsed time since the current slot's timestamp is below this
     * value folds into that slot; a later sample opens a new slot.
     */
    private static final long BUCKET_MILLIS = 1000L;

    /**
     * Accumulated amount held in each slot, indexed cyclically by the head cursor.
     *
     * <p>Slots within the live span carry the totals folded into each second; a slot is
     * reset to the opening sample when it is newly opened or overwritten.
     */
    private final long[] samples;

    /**
     * Opening timestamp in milliseconds of each slot's bucket, indexed cyclically.
     *
     * <p>Used to decide whether an incoming sample belongs to the slot's bucket or
     * requires a new slot.
     */
    private final long[] timestamps;

    /**
     * Index of the oldest live slot in the ring.
     *
     * <p>Advances modulo {@link #SLOT_COUNT} once the ring is full and the oldest slot
     * is overwritten; the live slots span from this index forward by {@link #count}.
     */
    private int head;

    /**
     * Number of live slots currently in the ring, never exceeding {@link #SLOT_COUNT}.
     *
     * <p>Grows as new slots open until the ring is full, after which it stays at
     * {@link #SLOT_COUNT} and the head advances instead.
     */
    private int count;

    /**
     * Running sum of the amounts held across the live slots.
     *
     * <p>Maintained incrementally as samples fold into a slot, a slot opens, or the oldest slot is
     * overwritten, so {@link #sum()} reads it directly rather than rescanning the ring; it always equals
     * the total the per slot scan would compute.
     */
    private long runningTotal;

    /**
     * Constructs an empty last minute buffer with no live slots.
     *
     * <p>The first {@link #insert(long, long)} opens the first slot at its sample's
     * timestamp.
     */
    public LastMinuteBuffer() {
        this.samples = new long[SLOT_COUNT];
        this.timestamps = new long[SLOT_COUNT];
        this.head = 0;
        this.count = 0;
    }

    /**
     * Inserts a sample at the given time, folding it into the current slot or opening a
     * new one.
     *
     * <p>When the buffer is empty, or the sample's time is at least one bucket past the
     * most recent slot's timestamp, a new slot opens carrying the sample and its
     * timestamp; the live count grows until the ring is full, after which the oldest
     * slot is overwritten and the head advances. Otherwise the sample folds into the
     * most recent slot, adding to its accumulated amount without changing its bucket
     * timestamp.
     *
     * @param nowMillis the sample's time in milliseconds; must not precede the previous
     *                  call's time
     * @param amount    the amount to record for this sample
     */
    public void insert(long nowMillis, long amount) {
        if (count == 0) {
            open(nowMillis, amount);
            return;
        }
        var current = (head + count - 1) % SLOT_COUNT;
        if (nowMillis - timestamps[current] < BUCKET_MILLIS) {
            samples[current] += amount;
            runningTotal += amount;
        } else {
            open(nowMillis, amount);
        }
    }

    /**
     * Returns the sum of the amounts held across the live slots.
     *
     * @return the total accumulated over the trailing window
     */
    public long sum() {
        return runningTotal;
    }

    /**
     * Returns the number of live slots currently in the ring.
     *
     * @return the live slot count, between {@code 0} and {@link #SLOT_COUNT}
     */
    public int count() {
        return count;
    }

    /**
     * Opens a new slot carrying the given sample, advancing the head once the ring is
     * full.
     *
     * <p>Writes the sample and its timestamp into the slot just past the current live
     * span. While the ring has free slots the live count grows; once full, the head
     * advances so the new slot overwrites the oldest, keeping the window bounded to
     * {@link #SLOT_COUNT} slots.
     *
     * @param nowMillis the opening timestamp of the new slot, in milliseconds
     * @param amount    the sample amount the new slot starts with
     */
    private void open(long nowMillis, long amount) {
        var slot = (head + count) % SLOT_COUNT;
        if (count < SLOT_COUNT) {
            runningTotal += amount;
            count++;
        } else {
            runningTotal += amount - samples[slot];
            head = (head + 1) % SLOT_COUNT;
            if (Log.TRACE) LOGGER.log(Level.TRACE, "last minute buffer wrapped, oldest slot overwritten");
        }
        samples[slot] = amount;
        timestamps[slot] = nowMillis;
    }
}
