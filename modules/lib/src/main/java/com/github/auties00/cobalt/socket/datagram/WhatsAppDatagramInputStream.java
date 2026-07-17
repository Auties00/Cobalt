package com.github.auties00.cobalt.socket.datagram;

import com.github.auties00.cobalt.exception.linked.WhatsAppSessionException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.binary.StanzaReader;
import com.github.auties00.cobalt.util.AesGcmStreamCipher;
import com.github.auties00.cobalt.wire.core.util.DataUtils;

import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger.Level;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;
import java.util.Objects;

/**
 * Exposes the {@code int24}-prefixed, AES-GCM-encrypted WhatsApp datagram wire as a continuous plain byte stream.
 *
 * <p>The wire is a sequence of {@code int24}-prefixed datagrams. Each datagram is its own AES-GCM ciphertext: the
 * cipher is re-initialised with a fresh nonce at every datagram boundary, the ciphertext bytes are fed through
 * {@link AesGcmStreamCipher#update}, and {@link AesGcmStreamCipher#doFinal} verifies the authentication tag at the end
 * of each datagram. Datagram boundaries are invisible to the consumer: {@link #read()} yields plaintext bytes
 * continuously across datagram boundaries and signals {@code -1} when the underlying stream ends, whether at an exact
 * datagram boundary or partway through a datagram whose declared length is never fully delivered. A datagram left
 * incomplete by an end of stream is discarded rather than reported as an error, mirroring the way WA Web's
 * {@code WAFrameSocket} abandons a buffered partial frame when the transport closes; the WhatsApp server routinely ends
 * a stream by starting a final datagram it never finishes and then closing the socket.
 *
 * <p>This continuous mode is the natural fit for the post-handshake reader thread, which decodes one
 * {@link Stanza} per datagram via {@link StanzaReader}:
 * each {@code readNode()} call consumes exactly one stanza's worth of bytes (the wire pairs one stanza body to one
 * datagram), so the cursor naturally arrives at the next datagram boundary without the caller having to manage framing.
 *
 * <p>For the Noise XX handshake, where the consumer (a {@link it.auties.protobuf.stream.ProtobufInputStream}) needs an
 * {@link InputStream} that bounds reads to one handshake message, {@link #readDatagramLength()} exposes the plaintext
 * length of the next datagram so a bounded view can be assembled around the continuous {@link #read()}.
 *
 * <p>The stream owns two fixed-size buffers and nothing else: an 8 KiB {@link #ibuffer} for one batched read from the
 * underlying stream, and an 8 KiB {@link #plaintextChunk} that captures the plaintext released by one round of
 * {@link AesGcmStreamCipher#update}. Neither buffer grows with the datagram size.
 *
 * <p>Before the Noise handshake completes the stream is in pre-handshake mode: {@code int24}-framed bytes are still
 * consumed from the underlying stream, but no cipher is applied and the bytes are copied through {@link #ibuffer} into
 * {@link #plaintextChunk} verbatim. The caller flips the stream to encrypted mode by invoking
 * {@link #setReadKey(SecretKey)} once both Noise keys have been derived.
 *
 * <p>Instances are not thread-safe; one input stream is intended to be owned by a single reader thread.
 *
 * @implNote
 * This implementation drives an {@link AesGcmStreamCipher}, a streaming AES-GCM built on the JDK's own AES primitives,
 * rather than {@code javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")}. The packaged JDK GCM buffers the entire
 * ciphertext and releases its plaintext only at {@code doFinal}, whereas {@link AesGcmStreamCipher#update} streams
 * plaintext block by block (deferring only the tag check to {@link AesGcmStreamCipher#doFinal}), which lets the output
 * buffer stay at a fixed 8 KiB regardless of how large a single datagram is. The state machine keys off
 * {@link #textRemaining}: a value of {@link #NO_ACTIVE_DATAGRAM} means no active datagram and the next call must read
 * the next {@code int24} length; a positive value means more ciphertext must be fed through
 * {@link AesGcmStreamCipher#update}; zero is the transient state just before {@link AesGcmStreamCipher#doFinal} fires
 * and is then reset to {@link #NO_ACTIVE_DATAGRAM}.
 */
public final class WhatsAppDatagramInputStream extends FilterInputStream {

    /**
     * The logger for {@link WhatsAppDatagramInputStream}.
     */
    private static final System.Logger LOGGER = Log.get(WhatsAppDatagramInputStream.class);

    /**
     * Holds the size in bytes of the AES-GCM authentication tag.
     */
    private static final int GCM_TAG_BYTE_SIZE = 16;

    /**
     * Holds the maximum wire byte count encodable in the {@code int24} length prefix that frames every datagram.
     *
     * @implNote
     * This implementation mirrors the {@code 1 << 24} threshold guarded by {@code WAFrameSocket.sendFrame} on the WA
     * Web side.
     */
    private static final int MAX_DATAGRAM_LENGTH = 0xFF_FFFF;

    /**
     * Holds the minimum acceptable wire byte count for a pre-handshake datagram, requiring at least one plaintext byte
     * to follow the {@code int24} prefix.
     */
    private static final int MIN_PRE_HANDSHAKE_DATAGRAM_LENGTH = 1;

    /**
     * Holds the minimum acceptable wire byte count for a post-handshake datagram, requiring at least one plaintext byte
     * plus the {@value #GCM_TAG_BYTE_SIZE}-byte GCM tag.
     */
    private static final int MIN_POST_HANDSHAKE_DATAGRAM_LENGTH = GCM_TAG_BYTE_SIZE + 1;

    /**
     * Holds the size of the fixed read buffer; one read from the underlying stream draws at most this many bytes.
     */
    private static final int INPUT_BUFFER_SIZE = 8192;

    /**
     * Holds the sentinel value for {@link #textRemaining} meaning no datagram is currently in flight and the next call
     * must read the next {@code int24} length prefix.
     */
    private static final int NO_ACTIVE_DATAGRAM = -1;

    /**
     * Holds the fixed-size buffer used by {@link #produceMoreData()} to read one chunk from the underlying stream
     * before handing it to {@link AesGcmStreamCipher#update}.
     */
    private final byte[] ibuffer = new byte[INPUT_BUFFER_SIZE];

    /**
     * Holds the fixed-size buffer that captures the plaintext emitted by one {@link AesGcmStreamCipher#update} call.
     *
     * <p>The buffer is drained incrementally into the caller's destination by
     * {@link #drainPlaintextChunk(byte[], int, int)} across successive {@link #read(byte[], int, int)} calls so the
     * consumer receives bytes without seeing datagram boundaries.
     *
     * @implNote
     * This implementation sizes the buffer at exactly {@value #INPUT_BUFFER_SIZE} bytes because the streaming cipher
     * never releases more plaintext than the ciphertext fed in (decryption holds the trailing tag back rather than
     * overshooting), so one {@code update} over an {@link #ibuffer}-sized read can never overflow it and
     * {@code doFinal} releases no further plaintext.
     */
    private final byte[] plaintextChunk = new byte[INPUT_BUFFER_SIZE];

    /**
     * Holds the read cursor in {@link #plaintextChunk}; bytes from {@code plaintextChunkStart} (inclusive) to
     * {@link #plaintextChunkEnd} (exclusive) are valid plaintext waiting to be delivered.
     */
    private int plaintextChunkStart;

    /**
     * Holds the write cursor in {@link #plaintextChunk}; one past the last valid plaintext byte produced by the most
     * recent {@link #produceMoreData()} call.
     */
    private int plaintextChunkEnd;

    /**
     * Holds the reusable single-byte buffer used by {@link #read()} to delegate to the bulk-read code path without
     * per-call allocation.
     */
    private final byte[] oneByteBuf = new byte[1];

    /**
     * Holds the AES-GCM cipher, re-initialised on every datagram boundary with a fresh nonce derived from
     * {@link #readCounter}.
     */
    private final AesGcmStreamCipher cipher;

    /**
     * Holds the number of ciphertext bytes still to be fed through {@link AesGcmStreamCipher#update} for the current
     * datagram, or {@link #NO_ACTIVE_DATAGRAM} when the next call must read the length prefix.
     */
    private int textRemaining = NO_ACTIVE_DATAGRAM;

    /**
     * Holds the current AES-GCM read key, or {@code null} before the Noise handshake completes while the stream is in
     * pre-handshake passthrough mode.
     *
     * <p>The field is declared {@code volatile} so the handshake thread's {@link #setReadKey(SecretKey)} call is
     * visible to the reader thread that subsequently runs against this stream.
     */
    private volatile SecretKey readKey;

    /**
     * Holds the monotonic counter that seeds the AES-GCM nonce for the next datagram, incremented every time the cipher
     * is re-initialised.
     */
    private long readCounter;

    /**
     * Holds the sticky end-of-stream flag set when the underlying stream returned {@code -1} between datagrams (a clean
     * disconnect).
     */
    private boolean endOfStream;

    /**
     * Builds a datagram input stream wrapping {@code in}.
     *
     * <p>The stream starts in pre-handshake passthrough mode; bytes are {@code int24}-deframed but not decrypted until
     * {@link #setReadKey(SecretKey)} supplies a key. The expected caller is
     * {@link com.github.auties00.cobalt.socket.WhatsAppSocketClient}, which constructs the stream over the
     * transport-layer {@link InputStream} before running the Noise handshake on top.
     *
     * @param in the underlying byte stream
     * @throws IOException          if the platform cannot provide the AES-GCM cipher
     * @throws NullPointerException if {@code in} is {@code null}
     */
    public WhatsAppDatagramInputStream(InputStream in) throws IOException {
        super(Objects.requireNonNull(in, "in"));
        try {
            this.cipher = new AesGcmStreamCipher();
        } catch (GeneralSecurityException exception) {
            throw new IOException("Failed to initialise AES-GCM cipher", exception);
        }
    }

    /**
     * Installs the AES-GCM read key derived from the Noise handshake and resets the nonce counter to zero.
     *
     * <p>Subsequent datagrams are decrypted under {@code key} with monotonically increasing nonces. This method must be
     * called exactly once after the handshake completes and before starting the reader loop; calling it twice resets
     * the nonce counter and produces unreadable traffic.
     *
     * @param key the read key
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public void setReadKey(SecretKey key) {
        this.readKey = Objects.requireNonNull(key, "key");
        this.readCounter = 0;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "datagram input stream read key installed");
    }

    /**
     * Reads the next {@code int24} length prefix from the wire, initialises the AES-GCM cipher (in post-handshake mode)
     * with a fresh nonce, and returns the number of plaintext bytes that will follow on the stream.
     *
     * <p>This method combines three otherwise-separate steps (reading the {@code int24}, setting up the cipher state,
     * and exposing the plaintext length) into one entry point so a bounded {@link InputStream}, as used by the Noise XX
     * handshake to feed {@link it.auties.protobuf.stream.ProtobufInputStream#fromStream(InputStream)}, can be assembled
     * without duplicating any of the framing logic. The returned length is the plaintext byte count: the wire's
     * {@code int24} value in pre-handshake mode, and the wire's {@code int24} value minus the
     * {@value #GCM_TAG_BYTE_SIZE}-byte GCM tag in post-handshake mode. {@link #read()} yields exactly that many
     * plaintext bytes for the current datagram before the next call advances to the next one.
     *
     * @return the plaintext length of the next datagram, or {@code -1} on end-of-stream at the datagram boundary or
     *         inside an incomplete trailing length prefix
     * @throws IllegalStateException if a previous {@code read} call left the stream mid-datagram
     * @throws IOException           if the underlying read fails, the declared length is invalid, or the cipher cannot
     *                               be initialised
     */
    public int readDatagramLength() throws IOException {
        if (textRemaining != NO_ACTIVE_DATAGRAM) {
            throw new IllegalStateException("Cannot read a new datagram length: "
                    + textRemaining + " ciphertext byte(s) still pending in the current datagram");
        }
        if (plaintextChunkStart < plaintextChunkEnd) {
            throw new IllegalStateException("Cannot read a new datagram length: "
                    + (plaintextChunkEnd - plaintextChunkStart) + " plaintext byte(s) still buffered from the previous chunk");
        }
        if (endOfStream) {
            return -1;
        }
        var length = readWireLength();
        if (length < 0) {
            endOfStream = true;
            if (Log.TRACE) LOGGER.log(Level.TRACE, "datagram input stream reached end of stream");
            return -1;
        }
        var plaintextLength = startDatagram(length);
        if (Log.TRACE) LOGGER.log(Level.TRACE, "datagram length wire={0} plaintext={1}", length, plaintextLength);
        return plaintextLength;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation delegates to {@link #read(byte[], int, int)} through {@link #oneByteBuf} so the cipher-state
     * machine has a single code path; per-call allocation is avoided by reusing the one-byte scratch buffer.
     */
    @Override
    public int read() throws IOException {
        var n = read(oneByteBuf, 0, 1);
        return n < 0 ? -1 : oneByteBuf[0] & 0xFF;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation delivers plaintext from {@link #plaintextChunk}: if the chunk still has bytes from a previous
     * decrypt step they are drained first; otherwise {@link #produceMoreData()} runs one wire-chunk decrypt step into
     * {@link #plaintextChunk} and the resulting bytes are then drained. A single call returns at most
     * {@link #plaintextChunk}-worth of bytes; the caller invokes {@code read} repeatedly to consume a multi-chunk
     * datagram.
     */
    @Override
    public int read(byte[] dst, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        while (plaintextChunkStart >= plaintextChunkEnd) {
            if (endOfStream) {
                return -1;
            }
            if (!produceMoreData()) {
                return -1;
            }
        }
        return drainPlaintextChunk(dst, off, len);
    }

    /**
     * Copies the pending {@link #plaintextChunk} bytes into {@code dst}.
     *
     * <p>Copies at most {@code len} bytes from {@code plaintextChunkStart} into {@code dst} starting at {@code off} and
     * advances {@code plaintextChunkStart} by the number copied.
     *
     * @param dst the destination byte array
     * @param off the offset in {@code dst}
     * @param len the maximum number of bytes to copy
     * @return the number of bytes copied
     */
    private int drainPlaintextChunk(byte[] dst, int off, int len) {
        var n = Math.min(plaintextChunkEnd - plaintextChunkStart, len);
        System.arraycopy(plaintextChunk, plaintextChunkStart, dst, off, n);
        plaintextChunkStart += n;
        return n;
    }

    /**
     * Advances the cipher state machine by one wire chunk.
     *
     * <p>Starts a new datagram if needed, then either pumps one chunk of ciphertext through
     * {@link AesGcmStreamCipher#update} or finalises the current datagram with {@link AesGcmStreamCipher#doFinal}, in
     * both cases appending the resulting plaintext to {@link #plaintextChunk}. If the underlying stream ends while the
     * current datagram is still expecting ciphertext, the incomplete datagram is discarded and a clean end-of-stream is
     * reported rather than failing.
     *
     * @implNote
     * This implementation may return {@code true} with the chunk still empty: the decryption holds back a trailing
     * tag-sized window of input, so until more than a tag's worth of ciphertext has arrived no plaintext is released
     * (and a short partial read can leave a round entirely within that window). The {@code read} loop handles this by
     * re-invoking until the chunk is non-empty or end-of-stream is observed.
     *
     * <p>Discarding a trailing datagram whose declared length is never fully delivered mirrors WA Web's
     * {@code WAFrameSocket.convertBufferedToFrames}, which only emits a frame once its full {@code int24}-declared
     * length is buffered ({@code length <= buffer.size()}) and otherwise leaves the bytes queued as a "partial frame"
     * that is silently abandoned when the socket closes. The WhatsApp server ends a WebSocket stream by sending a
     * final short binary frame that begins a datagram it never finishes and then closing the transport; treating that
     * never-completed tail as a framing error would turn every orderly server-initiated close into a spurious failure
     * and reconnect.
     *
     * @return {@code true} if the cipher state machine advanced one step, {@code false} on clean or mid-datagram
     *         end-of-stream
     * @throws IOException if the underlying read fails, the cipher rejects the tag, or the datagram length is out of
     *                     bounds
     */
    private boolean produceMoreData() throws IOException {
        if (textRemaining == NO_ACTIVE_DATAGRAM) {
            var plaintextLen = readDatagramLength();
            if (plaintextLen < 0) {
                return false;
            }
        }
        plaintextChunkStart = 0;
        plaintextChunkEnd = 0;
        if (textRemaining > 0) {
            var toRead = Math.min(textRemaining, ibuffer.length);
            var n = in.read(ibuffer, 0, toRead);
            if (n < 0) {
                textRemaining = NO_ACTIVE_DATAGRAM;
                endOfStream = true;
                return false;
            }
            textRemaining -= n;
            decryptChunk(n);
        }
        if (textRemaining == 0) {
            finalizeDatagram();
        }
        return true;
    }

    /**
     * Reads the next {@code int24} length prefix from the underlying stream and returns the wire byte count.
     *
     * <p>The returned wire byte count is ciphertext plus GCM tag in post-handshake mode and plaintext length in
     * pre-handshake mode; the caller is responsible for converting to plaintext length when needed. An end of stream
     * reached anywhere inside the three-byte prefix discards the partial prefix and is reported as the same
     * {@code -1} as an end of stream observed exactly at the datagram boundary, because a length prefix that is
     * started but never completed is a trailing partial frame the server abandoned by closing the transport.
     *
     * @return the next datagram wire length, or {@code -1} if the underlying stream is exhausted at or inside the
     *         length prefix
     * @throws IOException if the underlying read fails
     */
    private int readWireLength() throws IOException {
        var b0 = in.read();
        if (b0 < 0) {
            return -1;
        }
        var b1 = in.read();
        var b2 = in.read();
        if (b1 < 0 || b2 < 0) {
            return -1;
        }
        return (b0 << 16) | (b1 << 8) | b2;
    }

    /**
     * Initialises {@link #cipher} for the next datagram, sets {@link #textRemaining} to the wire byte count and returns
     * the plaintext byte count that the consumer can expect to receive for this datagram.
     *
     * <p>In pre-handshake mode no cipher operation occurs; the datagram bytes are copied through {@link #ibuffer} into
     * {@link #plaintextChunk} verbatim by {@link #decryptChunk(int)} and the plaintext length equals the wire length.
     * In post-handshake mode the cipher is initialised with a fresh nonce and the plaintext length is the wire length
     * minus the {@value #GCM_TAG_BYTE_SIZE}-byte GCM tag.
     *
     * @param length the wire byte count read from the {@code int24} prefix
     * @return the plaintext byte count for this datagram
     * @throws IOException if the cipher cannot be initialised, the wire length falls below the mode-specific minimum
     *                     ({@value #MIN_PRE_HANDSHAKE_DATAGRAM_LENGTH} pre-handshake;
     *                     {@value #MIN_POST_HANDSHAKE_DATAGRAM_LENGTH} post-handshake), or the wire length exceeds
     *                     {@value #MAX_DATAGRAM_LENGTH}
     */
    private int startDatagram(int length) throws IOException {
        var key = readKey;
        var min = key == null ? MIN_PRE_HANDSHAKE_DATAGRAM_LENGTH : MIN_POST_HANDSHAKE_DATAGRAM_LENGTH;
        if (length < min) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "datagram length {0} below minimum {1}", length, min);
            throw new IOException("Datagram length " + length + " is below the minimum of " + min);
        }
        if (length > MAX_DATAGRAM_LENGTH) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "datagram length {0} exceeds maximum {1}", length, MAX_DATAGRAM_LENGTH);
            throw new IOException("Datagram length " + length + " exceeds the maximum of " + MAX_DATAGRAM_LENGTH);
        }
        if (key != null) {
            try {
                var nonce = new byte[12];
                DataUtils.putLong(nonce, 4, readCounter++, ByteOrder.BIG_ENDIAN);
                cipher.init(false, key, nonce);
            } catch (GeneralSecurityException exception) {
                throw new IOException("Failed to initialise AES-GCM cipher", exception);
            }
        }
        textRemaining = length;
        return key == null ? length : length - GCM_TAG_BYTE_SIZE;
    }

    /**
     * Feeds {@code n} bytes from {@link #ibuffer} through {@link AesGcmStreamCipher#update}, or copies them verbatim in
     * pre-handshake mode, appending the result to {@link #plaintextChunk}.
     *
     * @param n the number of bytes in {@link #ibuffer} to process
     * @throws IOException if the cipher operation fails or releases more bytes than the plaintext chunk buffer can hold
     */
    private void decryptChunk(int n) throws IOException {
        var key = readKey;
        if (key == null) {
            System.arraycopy(ibuffer, 0, plaintextChunk, plaintextChunkEnd, n);
            plaintextChunkEnd += n;
            return;
        }
        try {
            var produced = cipher.update(ibuffer, 0, n, plaintextChunk, plaintextChunkEnd);
            plaintextChunkEnd += produced;
        } catch (ShortBufferException exception) {
            throw new IOException("AES-GCM update produced more bytes than the plaintext chunk buffer allowed", exception);
        }
    }

    /**
     * Calls {@link AesGcmStreamCipher#doFinal} to verify the GCM tag for the current datagram and append any trailing
     * plaintext to {@link #plaintextChunk}.
     *
     * <p>In pre-handshake mode this method only resets {@link #textRemaining} to {@link #NO_ACTIVE_DATAGRAM}.
     *
     * @throws WhatsAppSessionException.BadMac if the GCM tag fails to authenticate; the Noise cipher state is
     *                                         unusable afterwards, so this is a session-level fault distinct from a
     *                                         structural stanza-decode error
     */
    private void finalizeDatagram() throws IOException {
        var key = readKey;
        textRemaining = NO_ACTIVE_DATAGRAM;
        if (key == null) {
            return;
        }
        try {
            var produced = cipher.doFinal(plaintextChunk, plaintextChunkEnd);
            plaintextChunkEnd += produced;
        } catch (GeneralSecurityException exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "datagram authentication tag verification failed", exception);
            throw new WhatsAppSessionException.BadMac("AES-GCM datagram authentication failed", exception);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation closes the underlying stream and holds no other resource that needs releasing.
     */
    @Override
    public void close() throws IOException {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "closing datagram input stream");
        in.close();
    }
}
