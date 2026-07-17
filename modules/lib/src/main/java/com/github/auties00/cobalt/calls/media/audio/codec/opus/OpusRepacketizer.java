package com.github.auties00.cobalt.calls.media.audio.codec.opus;

import com.github.auties00.cobalt.calls.media.audio.codec.opus.bindings.CobaltOpus;
import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.util.NativeLibLoader;

import java.lang.System.Logger.Level;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Combines several encoded Opus frames into one multi frame Opus packet and splits a multi frame
 * packet back into its constituent frames, wrapping the native libopus repacketizer.
 *
 * <p>Frames per packet aggregation buffers between one and six encoded Opus frames and emits them as a
 * single RTP payload, halving or thirding the per frame RTP overhead at the cost of added delay. The
 * {@linkplain #combine(List) combine} path feeds each frame to the native repacketizer with
 * {@code cobalt_opus_repacketizer_cat} and emits the combined packet with
 * {@code cobalt_opus_repacketizer_out_range}; the {@linkplain #split(byte[]) split} path runs the
 * inverse, recovering each frame as its own byte array. An instance owns one native repacketizer state,
 * held behind the {@link CobaltOpus} shim as an opaque handle, and a reusable output buffer from a
 * per instance arena, and is single writer: it must be driven from one thread. Closing destroys the
 * native state and releases the arena.
 *
 * @implNote This implementation allocates the repacketizer state once with
 * {@code cobalt_opus_repacketizer_create} and reinitializes it per operation with
 * {@code cobalt_opus_repacketizer_init}; the output buffer is sized for the worst case six frame packet.
 * The portable shim allocates the state from the libopus heap behind an opaque handle rather than
 * exposing {@code opus_repacketizer_get_size} for a caller allocated buffer, so the binding carries no
 * build dependent state size. The native code caps the frame count against an internal array bound and
 * errors on an invalid packet length; this implementation enforces the {@code 1..6} frame count before
 * the native call.
 */
public final class OpusRepacketizer implements AutoCloseable {
    static {
        NativeLibLoader.load("cobalt-native", Arena.global());
    }

    /**
     * Capacity, in bytes, of the reusable native output buffer for one combined packet.
     *
     * @implNote This implementation uses 6 times the 1276 byte RFC 6716 maximum single Opus frame plus
     * the per frame length prefixes, comfortably above the largest six frame aggregate a 20 ms voice
     * stream produces.
     */
    private static final int MAX_PACKET_BYTES = 6 * 1276 + 64;

    /**
     * The lowest frames per packet count the repacketizer accepts.
     */
    private static final int MIN_FRAMES = OpusCodecParams.MIN_FRAMES_PER_PACKET;

    /**
     * The highest frames per packet count the repacketizer accepts.
     */
    private static final int MAX_FRAMES = OpusCodecParams.MAX_FRAMES_PER_PACKET;

    /**
     * The logger for {@link OpusRepacketizer}.
     */
    private static final System.Logger LOGGER = Log.get(OpusRepacketizer.class);

    /**
     * Per instance arena owning the native repacketizer state and the reusable scratch buffers.
     */
    private final Arena arena;

    /**
     * Opaque handle to the native {@code OpusRepacketizer} state, allocated by
     * {@code cobalt_opus_repacketizer_create}.
     *
     * <p>Set to {@link MemorySegment#NULL} once the instance is closed.
     */
    private MemorySegment state;

    /**
     * Reusable native buffer the combined packet bytes are written into before being copied to the heap.
     */
    private final MemorySegment outBuf;

    /**
     * Reusable native buffer one input frame is copied into before being handed to the repacketizer.
     */
    private final MemorySegment inBuf;

    /**
     * Constructs a repacketizer, allocating the native state and scratch buffers from a fresh shared
     * arena.
     *
     * @throws WhatsAppCallException.Opus if the native repacketizer state cannot be allocated
     * @throws UnsatisfiedLinkError      if libopus cannot be loaded
     */
    public OpusRepacketizer() {
        this.arena = Arena.ofShared();
        try {
            var outHandle = arena.allocate(ValueLayout.ADDRESS);
            var rc = CobaltOpus.cobalt_opus_repacketizer_create(outHandle);
            this.state = outHandle.get(ValueLayout.ADDRESS, 0);
            if (rc != CobaltOpus.COBALT_OPUS_OK() || state.equals(MemorySegment.NULL)) {
                throw WhatsAppCallException.Opus.fromErr("cobalt_opus_repacketizer_create", rc);
            }
            this.outBuf = arena.allocate(MAX_PACKET_BYTES);
            this.inBuf = arena.allocate(MAX_PACKET_BYTES);
        } catch (RuntimeException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls opus repacketizer create failed", e);
            destroyState();
            arena.close();
            throw e;
        } catch (Throwable t) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls opus repacketizer allocation failed", t);
            destroyState();
            arena.close();
            throw new WhatsAppCallException.Opus("cobalt_opus_repacketizer allocation failed", t);
        }
    }

    /**
     * Combines the given encoded Opus frames into one multi frame Opus packet.
     *
     * <p>Reinitializes the native repacketizer, appends each frame with
     * {@code cobalt_opus_repacketizer_cat}, and emits the combined packet with
     * {@code cobalt_opus_repacketizer_out_range}.
     *
     * @param frames the encoded Opus frames to aggregate, in transmit order; size {@code 1..6}
     * @return a fresh {@code byte[]} holding the combined multi frame packet
     * @throws NullPointerException       if {@code frames} or any element is {@code null}
     * @throws IllegalStateException      if the repacketizer is closed
     * @throws IllegalArgumentException   if {@code frames} is empty or holds more than six frames
     * @throws WhatsAppCallException.Opus if a native repacketizer call fails
     */
    public byte[] combine(List<byte[]> frames) {
        Objects.requireNonNull(frames, "frames cannot be null");
        requireOpen();
        if (frames.size() < MIN_FRAMES || frames.size() > MAX_FRAMES) {
            throw new IllegalArgumentException("frames-per-packet must be in 1..6, got " + frames.size());
        }
        try {
            CobaltOpus.cobalt_opus_repacketizer_init(state);
            // cobalt_opus_repacketizer_cat does NOT copy its input: it retains a pointer that must stay
            // valid until cobalt_opus_repacketizer_out_range runs. Each frame therefore needs its own
            // region of inBuf; copying every frame to offset 0 would leave all cat'd frames pointing at the
            // last frame's bytes, corrupting every multi frame packet. The six frame worst case (6 * 1276)
            // fits inBuf.
            var offset = 0L;
            for (var i = 0; i < frames.size(); i++) {
                var frame = Objects.requireNonNull(frames.get(i), "frame cannot be null");
                var slice = inBuf.asSlice(offset, frame.length);
                MemorySegment.copy(frame, 0, slice, ValueLayout.JAVA_BYTE, 0, frame.length);
                var rc = CobaltOpus.cobalt_opus_repacketizer_cat(state, slice, frame.length);
                if (rc != CobaltOpus.COBALT_OPUS_OK()) {
                    throw WhatsAppCallException.Opus.fromErr("cobalt_opus_repacketizer_cat frame=" + i, rc);
                }
                offset += frame.length;
            }
            var written = CobaltOpus.cobalt_opus_repacketizer_out_range(state, 0, frames.size(), outBuf, MAX_PACKET_BYTES);
            if (written < 0) {
                throw WhatsAppCallException.Opus.fromErr("cobalt_opus_repacketizer_out_range", written);
            }
            var out = new byte[written];
            MemorySegment.copy(outBuf, ValueLayout.JAVA_BYTE, 0, out, 0, written);
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "calls opus repacketizer combine: frames={0} bytes={1}", frames.size(), written);
            }
            return out;
        } catch (WhatsAppCallException.Opus e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls opus repacketizer combine failed: frames=" + frames.size(), e);
            throw e;
        } catch (Throwable t) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls opus repacketizer combine failed: frames=" + frames.size(), t);
            throw new WhatsAppCallException.Opus("cobalt_opus_repacketizer combine failed", t);
        }
    }

    /**
     * Splits a multi frame Opus packet into its constituent encoded frames.
     *
     * <p>Reinitializes the native repacketizer over the packet, reads the frame count with
     * {@code cobalt_opus_repacketizer_get_nb_frames}, and emits each frame singly with
     * {@code cobalt_opus_repacketizer_out_range}. The inverse of {@link #combine(List)}.
     *
     * @param packet the multi frame Opus packet bytes
     * @return the constituent encoded frames, in packet order
     * @throws NullPointerException       if {@code packet} is {@code null}
     * @throws IllegalStateException      if the repacketizer is closed
     * @throws WhatsAppCallException.Opus if the packet is malformed or a native call fails
     */
    public List<byte[]> split(byte[] packet) {
        Objects.requireNonNull(packet, "packet cannot be null");
        requireOpen();
        try {
            MemorySegment.copy(packet, 0, inBuf, ValueLayout.JAVA_BYTE, 0, packet.length);
            CobaltOpus.cobalt_opus_repacketizer_init(state);
            var rc = CobaltOpus.cobalt_opus_repacketizer_cat(state, inBuf, packet.length);
            if (rc != CobaltOpus.COBALT_OPUS_OK()) {
                throw WhatsAppCallException.Opus.fromErr("cobalt_opus_repacketizer_cat", rc);
            }
            var count = CobaltOpus.cobalt_opus_repacketizer_get_nb_frames(state);
            if (count < 0) {
                throw WhatsAppCallException.Opus.fromErr("cobalt_opus_repacketizer_get_nb_frames", count);
            }
            var frames = new ArrayList<byte[]>(count);
            for (var i = 0; i < count; i++) {
                var written = CobaltOpus.cobalt_opus_repacketizer_out_range(state, i, i + 1, outBuf, MAX_PACKET_BYTES);
                if (written < 0) {
                    throw WhatsAppCallException.Opus.fromErr("cobalt_opus_repacketizer_out_range frame=" + i, written);
                }
                var frame = new byte[written];
                MemorySegment.copy(outBuf, ValueLayout.JAVA_BYTE, 0, frame, 0, written);
                frames.add(frame);
            }
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "calls opus repacketizer split: bytes={0} frames={1}", packet.length, frames.size());
            }
            return frames;
        } catch (WhatsAppCallException.Opus e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls opus repacketizer split failed: bytes=" + packet.length, e);
            throw e;
        } catch (Throwable t) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls opus repacketizer split failed: bytes=" + packet.length, t);
            throw new WhatsAppCallException.Opus("cobalt_opus_repacketizer split failed", t);
        }
    }

    /**
     * Verifies that the repacketizer is still open.
     *
     * @throws IllegalStateException if the repacketizer has been closed
     */
    private void requireOpen() {
        if (state == null || state.equals(MemorySegment.NULL)) {
            throw new IllegalStateException("OpusRepacketizer is closed");
        }
    }

    /**
     * Releases the native repacketizer state and the per instance arena.
     *
     * <p>Idempotent: a second call after closing returns without effect. The repacketizer state lives in
     * the libopus heap and is freed with {@code cobalt_opus_repacketizer_destroy} before the arena holding
     * the scratch buffers is closed.
     */
    @Override
    public void close() {
        if (state == null || state.equals(MemorySegment.NULL)) {
            return;
        }
        destroyState();
        arena.close();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "calls opus repacketizer closed");
    }

    /**
     * Destroys the native repacketizer state if it is still live.
     *
     * <p>Calls {@code cobalt_opus_repacketizer_destroy} and nulls the handle; any throwable from the
     * native call is swallowed so this can run safely from a failed constructor or from {@link #close()}.
     */
    private void destroyState() {
        if (state == null || state.equals(MemorySegment.NULL)) {
            return;
        }
        try {
            CobaltOpus.cobalt_opus_repacketizer_destroy(state);
        } catch (Throwable t) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "calls opus repacketizer destroy failed", t);
        }
        state = MemorySegment.NULL;
    }
}
