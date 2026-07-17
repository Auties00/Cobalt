package com.github.auties00.cobalt.calls.media.audio.neteq;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Arrays;

/**
 * The decoded PCM output history the NetEq time stretch and concealment operations splice over, a faithful
 * single channel model of WhatsApp's WebRTC sync buffer.
 *
 * <p>The sync buffer is a fixed capacity sliding window of the most recently decoded and rendered call
 * audio. Decoded frames are pushed in at the back through {@link #pushBack(short[])}, which shifts the whole
 * window left by the inserted length so the buffer never grows past its capacity and always holds the same
 * span of recent samples. A read cursor, {@link #nextIndex()}, marks the boundary between samples already
 * handed to the playout and samples decoded but not yet played; {@link #getNextAudioInterleaved(short[], int)}
 * copies the next run of unplayed samples out and advances the cursor. The operations reach into the window
 * directly through {@link #at(int)} and {@link #set(int, short)} to read past output for a lag search and to
 * overwrite the splice region with time stretched or concealed audio.
 *
 * <p>The window holds the recent output so the accelerate, expand, and merge kernels can correlate against
 * and overlap add over real past samples. It composes with, rather than replaces, the receiver's decoded
 * remainder FIFO: the remainder FIFO serves the extra frames of a multi frame codec packet one render period
 * at a time, and each frame it serves is also pushed through this buffer so the history stays complete across
 * a multi frame packet.
 *
 * <p>The buffer is single writer: the playout pull thread owns it, pushing decoded frames and reading
 * rendered samples in one sequence with no concurrent access, matching the native NetEq get audio loop that
 * holds the engine lock around the whole decode and render cycle.
 *
 * @implNote This implementation models the single channel the call audio format uses (sixteen kilohertz
 * mono), so the multi channel interleave the native multi vector carries collapses to a flat array. The
 * native ring buffer the cross fade indexes is flattened to a left shifting linear window here because the
 * operations always splice over a contiguous recent span; the sample values the splice reads and writes are
 * identical, only the storage is linear rather than wrapped. The window is always full, so the left shift on
 * a push equals the inserted length.
 */
public final class NetEqSyncBuffer {
    /**
     * The logger for {@link NetEqSyncBuffer}.
     */
    private static final System.Logger LOGGER = Log.get(NetEqSyncBuffer.class);

    /**
     * The fixed sample capacity of the history window.
     *
     * <p>The window always holds exactly this many samples; a push that would overflow shifts the oldest
     * samples out first.
     */
    private final int capacity;

    /**
     * The sample storage, {@link #capacity} long, oldest sample at index zero.
     *
     * <p>The most recent samples sit at the high end; {@link #pushBack(short[])} shifts the contents left to
     * make room at the back.
     */
    private final short[] samples;

    /**
     * The read cursor, the index of the next sample not yet handed to the playout.
     *
     * <p>Samples below this index have been played; samples from here to the end of the window are decoded
     * but unplayed. {@link #getNextAudioInterleaved(short[], int)} advances it; {@link #pushBack(short[])}
     * shifts it left with the window.
     */
    private int nextIndex;

    /**
     * Constructs a sync buffer of the given sample capacity, initialized to silence with the read cursor at
     * the end.
     *
     * <p>The window starts full of zero samples with {@link #nextIndex()} at the capacity, so the buffer
     * reports no unplayed samples until the first frame is pushed.
     *
     * @param capacity the fixed sample capacity; must be positive
     * @throws IllegalArgumentException if {@code capacity} is not positive
     */
    NetEqSyncBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive, got " + capacity);
        }
        this.capacity = capacity;
        this.samples = new short[capacity];
        this.nextIndex = capacity;
    }

    /**
     * Returns the fixed sample capacity of the window.
     *
     * @return the capacity in samples
     */
    int capacity() {
        return capacity;
    }

    /**
     * Returns the read cursor, the index of the next unplayed sample.
     *
     * @return the read cursor, in {@code [0, capacity]}
     */
    int nextIndex() {
        return nextIndex;
    }

    /**
     * Returns the number of decoded but not yet played samples in the window.
     *
     * <p>The span from the read cursor to the end of the window, the samples a pull can still serve before
     * the buffer underruns.
     *
     * @return the unplayed sample count, {@code capacity - nextIndex}
     */
    int futureLength() {
        return capacity - nextIndex;
    }

    /**
     * Returns the sample at an absolute window index.
     *
     * @param index the window index, in {@code [0, capacity)}
     * @return the sample at that index
     */
    short at(int index) {
        return samples[index];
    }

    /**
     * Overwrites the sample at an absolute window index, the splice write the operations use.
     *
     * @param index the window index, in {@code [0, capacity)}
     * @param value the sample to store
     */
    void set(int index, short value) {
        samples[index] = value;
    }

    /**
     * Copies a contiguous run of samples out of the window into a destination array.
     *
     * <p>Copies {@code len} samples starting at absolute window index {@code fromIndex} into {@code dest}
     * beginning at {@code destOff}, the bulk equivalent of a run of {@link #at(int)} reads the operations use
     * to gather a recent span for a lag search or an overlap add. The copied sample values are identical to
     * reading {@code fromIndex + i} through {@link #at(int)} for each {@code i} in {@code [0, len)}.
     *
     * @param fromIndex the first window index to copy, in {@code [0, capacity)}
     * @param dest      the destination array; never {@code null}
     * @param destOff   the offset into {@code dest} at which the first copied sample lands
     * @param len       the number of samples to copy
     */
    void copyRange(int fromIndex, short[] dest, int destOff, int len) {
        System.arraycopy(samples, fromIndex, dest, destOff, len);
    }

    /**
     * Appends a decoded frame at the back of the window, shifting the window left to stay within capacity.
     *
     * <p>Drops the oldest samples so the inserted frame fits at the high end, shifts the surviving samples
     * and the read cursor down by the same amount, then copies the frame into the freed tail. After the
     * push the read cursor points one frame further from the end, so the freshly decoded samples are the
     * unplayed span a subsequent read returns.
     *
     * @implNote This implementation shifts the window left by the inserted length (the window is always
     * full), moves the cursor down by the same amount and clamps it at zero, then copies the frame into the
     * freed tail.
     *
     * @param frame the decoded samples to append; never {@code null}
     */
    void pushBack(short[] frame) {
        pushBack(frame, 0, frame.length);
    }

    /**
     * Appends a subrange of a decoded frame to the back of the window, the {@code (array, offset, length)}
     * form of {@link #pushBack(short[])}.
     *
     * <p>Behaves exactly as {@link #pushBack(short[])} for the {@code length} samples of {@code frame}
     * starting at {@code off}, so a caller that decoded into a longer reusable scratch buffer appends only
     * the produced prefix without slicing a fresh array first.
     *
     * @param frame  the buffer holding the decoded samples; never {@code null}
     * @param off    the index of the first sample to append
     * @param length the number of samples to append
     */
    void pushBack(short[] frame, int off, int length) {
        if (length >= capacity) {
            System.arraycopy(frame, off + length - capacity, samples, 0, capacity);
            nextIndex = 0;
            return;
        }
        System.arraycopy(samples, length, samples, 0, capacity - length);
        System.arraycopy(frame, off, samples, capacity - length, length);
        nextIndex = Math.max(0, nextIndex - length);
    }

    /**
     * Overwrites the tail of the window with a freshly produced frame without shifting, the in place splice
     * write.
     *
     * <p>Replaces the last {@code frame.length} samples of the window with {@code frame}, leaving the read
     * cursor untouched. The time stretch and merge operations use this to write their spliced output over
     * the region the plain decode would have occupied, after they have already correlated against the
     * existing history.
     *
     * @param frame the samples to write into the tail; never {@code null} and no longer than the capacity
     */
    void replaceTail(short[] frame) {
        var length = frame.length;
        System.arraycopy(frame, 0, samples, capacity - length, length);
    }

    /**
     * Copies the next run of unplayed samples out and advances the read cursor, the playout read.
     *
     * <p>Copies up to {@code requested} samples starting at the read cursor into {@code out}, returns the
     * number copied (fewer than requested only when the unplayed span is shorter), and advances the cursor
     * past the copied samples.
     *
     * @implNote This implementation starts the copy at the read cursor for the single channel, clamps it to
     * the unplayed span, and advances the cursor by the count returned.
     *
     * @param out       the destination array; never {@code null}
     * @param requested the maximum number of samples to copy
     * @return the number of samples copied, in {@code [0, requested]}
     */
    int getNextAudioInterleaved(short[] out, int requested) {
        var available = capacity - nextIndex;
        var count = Math.min(requested, available);
        System.arraycopy(samples, nextIndex, out, 0, count);
        nextIndex += count;
        return count;
    }

    /**
     * Resets the window to silence with the read cursor at the end, clearing the history across a
     * discontinuity.
     *
     * <p>Called when the stream is reconfigured so the operations never splice over samples from before a
     * codec switch or a flush.
     */
    void reset() {
        Arrays.fill(samples, (short) 0);
        nextIndex = capacity;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sync buffer reset, capacity={0}", capacity);
    }
}
