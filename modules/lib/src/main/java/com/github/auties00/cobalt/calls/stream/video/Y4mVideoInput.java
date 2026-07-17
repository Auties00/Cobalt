package com.github.auties00.cobalt.calls.stream.video;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import com.github.auties00.cobalt.calls.stream.AudioFrame;
import com.github.auties00.cobalt.calls.stream.AudioInput;
import com.github.auties00.cobalt.calls.stream.VideoFrame;
import com.github.auties00.cobalt.calls.stream.VideoInput;
import com.github.auties00.cobalt.calls.stream.VideoPixelFormat;
import com.github.auties00.cobalt.telemetry.log.Log;

/**
 * Records the remote video of a call to a raw YUV4MPEG2 (Y4M) file.
 *
 * <p>This is the file backed {@link VideoInput} returned by {@link VideoInput#toY4m(Path)}. It writes each
 * received {@link VideoFrame} as uncompressed I420 into the Y4M container, so the recording involves no
 * encoder and no FFmpeg dependency; a player such as {@code ffplay} or {@code mpv} reads it back directly.
 * The stream header carries the resolution of the first frame; the frame rate is written as a nominal
 * {@value #NOMINAL_FPS} because Y4M has no per frame timing, so the running presentation timestamps the
 * engine attaches are not preserved.
 *
 * <p>Y4M is a fixed geometry container, so the dimensions are locked to the first frame and a later frame
 * of a different resolution (which a call may produce as the codec follows the peer's bandwidth) is dropped
 * rather than corrupting the file. A frame delivered in {@link VideoPixelFormat#NV12} is deinterleaved to
 * I420 before it is written.
 *
 * <p>As a {@link VideoInput} it is also an {@link AudioInput}; the inbound audio is discarded, since Y4M
 * records only video. Pass a separate audio sink through the input-only call overloads to record audio too.
 *
 * @implNote This implementation writes the frames raw and uncompressed, so the file grows at roughly
 * {@code width*height*1.5} bytes per frame; it is a diagnostic and bridging sink, not a space efficient
 * recording format.
 */
public final class Y4mVideoInput implements VideoInput {
    /**
     * The logger for {@link Y4mVideoInput}.
     */
    private static final System.Logger LOGGER = Log.get(Y4mVideoInput.class);

    /**
     * The frame rate written into the Y4M header, since the container carries no per frame timing and the
     * engine's presentation timestamps are not preserved.
     */
    private static final int NOMINAL_FPS = 30;

    /**
     * The per frame separator that precedes each frame's raw plane bytes in the Y4M body.
     */
    private static final byte[] FRAME_MARKER = "FRAME\n".getBytes(StandardCharsets.US_ASCII);

    /**
     * Guards {@link #shutdown()} so the file is finalized at most once.
     */
    private final Object lock = new Object();

    /**
     * The buffered output over the recording file, coalescing the many per frame writes.
     */
    private final OutputStream out;

    /**
     * Whether the sink has been ended, after which a frame is dropped and {@link #readVideo()} returns.
     */
    private boolean closed;

    /**
     * The locked frame width, or {@code -1} until the first frame fixes the stream geometry.
     */
    private int width = -1;

    /**
     * The locked frame height, or {@code -1} until the first frame fixes the stream geometry.
     */
    private int height;

    /**
     * The reusable scratch that an {@link VideoPixelFormat#NV12} frame is deinterleaved into before it is
     * written as I420, allocated on the first such frame.
     */
    private byte[] i420Scratch;

    /**
     * Opens the given path for writing, truncating any existing file.
     *
     * <p>The Y4M header is deferred to the first {@link #offerVideo(VideoFrame)}, since the stream geometry is
     * only known once a frame arrives.
     *
     * @param path the output file path; never {@code null}
     * @throws NullPointerException  if {@code path} is {@code null}
     * @throws IllegalStateException if the file cannot be opened
     */
    public Y4mVideoInput(Path path) {
        Objects.requireNonNull(path, "path cannot be null");
        try {
            this.out = new BufferedOutputStream(Files.newOutputStream(path,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE));
        } catch (IOException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to open Y4M sink", e);
            throw new IllegalStateException("failed to open Y4M sink at " + path, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Writes the stream header on the first frame, fixing the geometry, then appends this frame's I420
     * planes. A frame whose dimensions differ from the locked geometry, or offered after
     * {@link #shutdown()}, is dropped.
     *
     * @param frame {@inheritDoc}
     * @throws NullPointerException if {@code frame} is {@code null}
     * @throws UncheckedIOException if the underlying write fails
     */
    @Override
    public void offerVideo(VideoFrame frame) {
        Objects.requireNonNull(frame, "frame cannot be null");
        synchronized (lock) {
            if (closed) {
                return;
            }
            try {
                if (width < 0) {
                    width = frame.width();
                    height = frame.height();
                    out.write(header(width, height));
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "Y4M sink locked geometry {0}x{1}", width, height);
                } else if (frame.width() != width || frame.height() != height) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "dropping Y4M frame {0}x{1}, locked geometry is {2}x{3}",
                                frame.width(), frame.height(), width, height);
                    }
                    return;
                }
                out.write(FRAME_MARKER);
                out.write(i420(frame));
            } catch (IOException e) {
                if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to write Y4M frame", e);
                throw new UncheckedIOException("failed to write Y4M frame", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>A file backed sink is not read from: this blocks until {@link #shutdown()} and then returns
     * {@code null}, so a consumer that drains it in a loop terminates cleanly at end of call.
     *
     * @return {@code null} once the sink has been ended
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    @Override
    public VideoFrame readVideo() throws InterruptedException {
        synchronized (lock) {
            while (!closed) {
                lock.wait();
            }
            return null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Drops the frame: this sink records only video, so the inbound audio is discarded.
     *
     * @param frame {@inheritDoc}
     * @throws NullPointerException if {@code frame} is {@code null}
     */
    @Override
    public void offerAudio(AudioFrame frame) {
        Objects.requireNonNull(frame, "frame cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * <p>The audio side is not read from: this blocks until {@link #shutdown()} and then returns
     * {@code null}, sharing the video side's lock.
     *
     * @return {@code null} once the sink has been ended
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    @Override
    public AudioFrame readAudio() throws InterruptedException {
        synchronized (lock) {
            while (!closed) {
                lock.wait();
            }
            return null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Marks the sink ended, flushes and closes the file, and wakes a pending {@link #readVideo()}. Idempotent.
     *
     * @throws UncheckedIOException if flushing or closing the file fails
     */
    @Override
    public void shutdown() {
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "finalizing Y4M sink");
            lock.notifyAll();
            try {
                out.close();
            } catch (IOException e) {
                if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to finalise Y4M file", e);
                throw new UncheckedIOException("failed to finalise Y4M file", e);
            }
        }
    }

    /**
     * Builds the Y4M stream header for the given geometry.
     *
     * @param width  the locked frame width
     * @param height the locked frame height
     * @return the header bytes, terminated by a newline
     */
    private static byte[] header(int width, int height) {
        var header = "YUV4MPEG2 W" + width + " H" + height + " F" + NOMINAL_FPS + ":1 Ip A1:1 C420\n";
        return header.getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * Returns the frame's pixels as I420, deinterleaving an {@link VideoPixelFormat#NV12} chroma plane
     * into a reused scratch buffer.
     *
     * @param frame the frame to render as I420
     * @return the I420 plane bytes, either the frame's own buffer or the scratch
     */
    private byte[] i420(VideoFrame frame) {
        var pixels = frame.pixels();
        if (frame.format() == VideoPixelFormat.I420) {
            return pixels;
        }
        var lumaBytes = width * height;
        var chromaSamples = (width / 2) * (height / 2);
        var scratch = i420Scratch;
        if (scratch == null || scratch.length != pixels.length) {
            scratch = new byte[pixels.length];
            i420Scratch = scratch;
            if (Log.TRACE) LOGGER.log(Level.TRACE, "reallocated Y4M scratch buffer, size {0}", pixels.length);
        }
        System.arraycopy(pixels, 0, scratch, 0, lumaBytes);
        for (var i = 0; i < chromaSamples; i++) {
            scratch[lumaBytes + i] = pixels[lumaBytes + 2 * i];
            scratch[lumaBytes + chromaSamples + i] = pixels[lumaBytes + 2 * i + 1];
        }
        return scratch;
    }
}
