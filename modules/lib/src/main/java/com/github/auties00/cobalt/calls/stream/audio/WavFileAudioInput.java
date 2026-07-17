package com.github.auties00.cobalt.calls.stream.audio;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.util.DataUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import com.github.auties00.cobalt.calls.stream.AudioFrame;
import com.github.auties00.cobalt.calls.stream.AudioInput;

/**
 * Records the remote audio of a call to a WAV file.
 *
 * <p>This is the device backed {@link AudioInput} returned by {@link AudioInput#toWav(Path)}. Its
 * constructor records incoming frames as a canonical RIFF/WAVE PCM file: the header of 44 bytes is emitted
 * up front with its two size fields zeroed. Each {@link #offerAudio(AudioFrame)} appends the frame's samples
 * little endian to the body, and {@link #shutdown()} seeks back to patch the size fields with the final
 * byte counts and closes the file. Because the call media format is already 16 kHz mono signed 16 bit PCM,
 * the samples map directly onto the WAVE payload and no FFmpeg dependency or re encoding is involved.
 *
 * <p>The {@link AudioFrame#ptsMicros()} timestamp the engine attaches to each frame is ignored; the WAVE
 * body carries raw samples in arrival order.
 *
 * @implNote This implementation finalises the header only on {@link #shutdown()}, so a recording whose
 * process is killed before the call ends leaves the size fields zeroed and most players treat the file as
 * empty.
 */
public final class WavFileAudioInput implements AudioInput {
    /**
     * The logger for {@link WavFileAudioInput}.
     */
    private static final System.Logger LOGGER = Log.get(WavFileAudioInput.class);

    /**
     * Names the sample rate written into the WAV header, matching the call media rate of 16 kHz.
     */
    private static final int SAMPLE_RATE = 16_000;

    /**
     * Guards {@link #shutdown()} so the file is finalized at most once.
     */
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * Released by {@link #shutdown()} so a pending {@link #readAudio()} returns.
     */
    private final CountDownLatch done = new CountDownLatch(1);

    /**
     * Names the channel count written into the WAV header, matching the call media layout of mono.
     */
    private static final int CHANNELS = 1;

    /**
     * Names the bit depth written into the WAV header, matching the signed 16 bit call media samples.
     */
    private static final int BITS_PER_SAMPLE = 16;

    /**
     * Holds the underlying file as a {@link RandomAccessFile} so the header size fields can be patched by
     * seeking back on shutdown.
     */
    private final RandomAccessFile raf;

    /**
     * Holds a buffered stream over the writable channel view of {@link #raf} used for the sample appends.
     *
     * @implNote This implementation wraps the channel in a {@link BufferedOutputStream} so the many small
     * per frame writes coalesce into larger block writes.
     */
    private final OutputStream out;

    /**
     * Counts the audio sample bytes written so far, used to compute the final WAV size fields on shutdown.
     */
    private long sampleBytes;

    /**
     * Holds the reusable little endian byte scratch that one frame's samples are encoded into before being
     * appended to the WAV body, grown on demand when a larger frame arrives.
     *
     * <p>Confined to the single call render thread that drives {@link #offerAudio(AudioFrame)}, so reuse across
     * frames does not race.
     */
    private byte[] scratch;

    /**
     * Holds the {@link ShortBuffer} view over {@link #scratch}, rebuilt whenever {@link #scratch} is grown,
     * so each frame encodes by rewinding this view instead of allocating a fresh buffer.
     */
    private ShortBuffer scratchView;

    /**
     * Opens the given path for writing and emits the WAV header with placeholder size fields.
     *
     * <p>Truncates any existing file at the path, opens it for reading and writing so the header can later
     * be patched, and writes the header of 44 bytes with its size fields zeroed.
     *
     * @param path the output file path; never {@code null}
     * @throws NullPointerException  if {@code path} is {@code null}
     * @throws IllegalStateException if the file cannot be opened or the header cannot be written
     */
    public WavFileAudioInput(Path path) {
        Objects.requireNonNull(path, "path cannot be null");
        try {
            Files.write(path, DataUtils.EMPTY_BYTE_ARRAY,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            this.raf = new RandomAccessFile(path.toFile(), "rw");
            WritableByteChannel channel = raf.getChannel();
            this.out = new BufferedOutputStream(Channels.newOutputStream(channel));
            writeHeaderPlaceholder();
        } catch (IOException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to open WAV sink", e);
            throw new IllegalStateException("failed to open WAV sink at " + path, e);
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "WAV recording started");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Appends the frame's samples little endian to the WAV body and advances the running byte count
     * used to finalise the header on shutdown. A frame offered after {@link #shutdown()} is dropped.
     *
     * @param frame {@inheritDoc}
     * @throws NullPointerException  if {@code frame} is {@code null}
     * @throws UncheckedIOException  if the underlying write fails
     */
    @Override
    public void offerAudio(AudioFrame frame) {
        Objects.requireNonNull(frame, "frame cannot be null");
        if (closed.get()) {
            return;
        }
        var pcm = frame.pcm();
        var needed = pcm.length * Short.BYTES;
        var buf = scratch;
        if (buf == null || buf.length < needed) {
            buf = new byte[needed];
            scratch = buf;
            scratchView = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        }
        scratchView.rewind();
        scratchView.put(pcm);
        try {
            out.write(buf, 0, needed);
            sampleBytes += needed;
        } catch (IOException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to write WAV samples", e);
            throw new UncheckedIOException("failed to write WAV samples", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>A file backed sink is not read from: this blocks until {@link #shutdown()} and then returns
     * {@code null}.
     *
     * @return {@code null} once the sink has been ended
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    @Override
    public AudioFrame readAudio() throws InterruptedException {
        done.await();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Marks the sink ended, wakes a pending {@link #readAudio()}, flushes the buffered body, rewrites the two
     * placeholder size fields with the final byte counts, and closes the file even if the patch fails.
     * Idempotent.
     *
     * @throws UncheckedIOException if flushing or patching the header fails
     */
    @Override
    public void shutdown() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        done.countDown();
        try {
            out.flush();
            patchHeaderSizes();
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "WAV recording finalised: bytes={0}", sampleBytes);
        } catch (IOException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to finalise WAV file", e);
            throw new UncheckedIOException("failed to finalise WAV file", e);
        } finally {
            try {
                raf.close();
            } catch (IOException e) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to close WAV file", e);
            }
        }
    }

    /**
     * Writes the RIFF/WAVE PCM header of 44 bytes with its size fields zeroed.
     *
     * <p>Emits the {@code RIFF} chunk, the {@code WAVE} form type, the {@code fmt } chunk of 16 bytes
     * carrying PCM format code 1 with the configured channel count, sample rate, byte rate, block align,
     * and bit depth, and the {@code data} chunk header. The RIFF size and {@code data} size fields are left
     * zero and are filled in by {@link #patchHeaderSizes()} on shutdown.
     *
     * @throws IOException if writing the header fails
     */
    private void writeHeaderPlaceholder() throws IOException {
        var header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.put((byte) 'R').put((byte) 'I').put((byte) 'F').put((byte) 'F');
        header.putInt(0);
        header.put((byte) 'W').put((byte) 'A').put((byte) 'V').put((byte) 'E');
        header.put((byte) 'f').put((byte) 'm').put((byte) 't').put((byte) ' ');
        header.putInt(16);
        header.putShort((short) 1);
        header.putShort((short) CHANNELS);
        header.putInt(SAMPLE_RATE);
        header.putInt(SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8);
        header.putShort((short) (CHANNELS * BITS_PER_SAMPLE / 8));
        header.putShort((short) BITS_PER_SAMPLE);
        header.put((byte) 'd').put((byte) 'a').put((byte) 't').put((byte) 'a');
        header.putInt(0);
        out.write(header.array());
    }

    /**
     * Patches the header's two size fields with the final byte counts.
     *
     * <p>Seeks to offset 4 and writes the RIFF chunk size, the total sample byte count plus the 36 bytes
     * of header that follow that field, then seeks to offset 40 and writes the {@code data} chunk size,
     * the total sample byte count. Both values are stored little endian.
     *
     * @throws IOException if seeking or writing fails
     */
    private void patchHeaderSizes() throws IOException {
        var total = sampleBytes;
        raf.seek(4L);
        raf.writeInt(Integer.reverseBytes((int) (36L + total)));
        raf.seek(40L);
        raf.writeInt(Integer.reverseBytes((int) total));
    }
}
