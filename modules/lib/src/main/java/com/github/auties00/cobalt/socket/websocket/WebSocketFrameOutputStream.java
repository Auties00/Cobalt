package com.github.auties00.cobalt.socket.websocket;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static com.github.auties00.cobalt.socket.websocket.WebSocketFrameConstants.*;

/**
 * Wraps an {@link OutputStream} and emits RFC 6455 binary frames with
 * client-to-server masking.
 *
 * <p>The stream supports two modes, selected automatically based on
 * whether the caller invokes {@link #beginFrame(int)} before writing:
 *
 * <ul>
 *   <li><strong>Streaming mode</strong>: the caller declares the
 *       payload size up front via {@link #beginFrame(int)}, and the
 *       frame header is emitted immediately. Subsequent {@code write}
 *       calls are masked <strong>in place</strong> in the caller's
 *       array and forwarded straight to the underlying stream; no
 *       intermediate buffer. The frame ends implicitly once
 *       {@code payloadSize} bytes have been written.</li>
 *   <li><strong>One-shot mode</strong>: when {@code beginFrame} is not
 *       called, each individual {@link #write(byte[], int, int)} or
 *       {@link #write(int)} call is treated as one complete frame; the
 *       stream picks a fresh mask key, builds the header, masks the
 *       payload in place, and writes header plus payload as a single
 *       frame. Used for control frames (PONG, CLOSE) issued from the
 *       paired {@link WebSocketFrameInputStream}.</li>
 * </ul>
 *
 * <p>In both modes the mask is applied <strong>in place</strong> on the
 * caller's array through the {@link WebSocketMasker} selected at load
 * time, which prefers a SIMD implementation when the Vector API is
 * available and falls back to a scalar one otherwise. The mutation
 * contract is intentional and load-bearing for zero-copy: the caller's
 * buffer must not be reused or read after the call returns.
 *
 * <p>All public state-mutating methods are serialized on {@code this}
 * so the input thread's auto-PONG (which goes through
 * {@link #writeControlFrame(byte, byte[], int)}) cannot race the writer
 * thread's streaming-mode frame.
 */
public final class WebSocketFrameOutputStream extends FilterOutputStream {

    /**
     * The logger for {@link WebSocketFrameOutputStream}.
     */
    private static final System.Logger LOGGER = Log.get(WebSocketFrameOutputStream.class);

    /**
     * Holds the masker selected once at class-load time, shared by every
     * instance to mask payloads in place.
     */
    private static final WebSocketMasker MASKER = WebSocketMasker.INSTANCE;

    /**
     * Holds the maximum frame header size: two base bytes plus eight
     * extended-length bytes plus four mask bytes.
     */
    private static final int MAX_HEADER_SIZE = 14;

    /**
     * Holds the reusable scratch buffer for the frame header; private
     * to this instance and only touched under the {@code synchronized}
     * block around the actual write.
     */
    private final byte[] header = new byte[MAX_HEADER_SIZE];

    /**
     * Holds the number of payload bytes still expected for the
     * streaming-mode frame currently in flight, or {@code 0} when no
     * streaming-mode frame is open (the stream is in one-shot mode).
     */
    private int streamingRemaining;

    /**
     * Holds the mask key for the currently-open streaming-mode frame;
     * reset on every {@link #beginFrame(int)} call.
     */
    private int streamingMaskKey;

    /**
     * Holds the mask-cycle offset for the currently-open streaming-mode
     * frame; advanced as payload bytes are masked across multiple
     * {@code write} calls inside the same frame.
     */
    private int streamingMaskOffset;

    /**
     * Holds the reusable scratch buffer for a control frame deferred
     * while a streaming-mode frame is in flight; lazily allocated on
     * first use and sized at the RFC 6455 control-payload maximum so it
     * never needs to grow.
     */
    private byte[] pendingControlPayload;

    /**
     * Holds the opcode of the deferred control frame
     * ({@link WebSocketFrameConstants#OPCODE_PONG} or
     * {@link WebSocketFrameConstants#OPCODE_CLOSE}), or {@code 0} when
     * no control frame is pending.
     */
    private byte pendingControlOpcode;

    /**
     * Holds the number of valid bytes in {@link #pendingControlPayload}.
     */
    private int pendingControlLength;

    /**
     * Wraps an output stream that the caller has already opened.
     *
     * <p>The frame layer sits directly above the TLS-encrypted byte
     * stream, between WhatsApp's datagram stream and the TLS socket.
     *
     * @param out the underlying output stream
     * @throws NullPointerException if {@code out} is {@code null}
     */
    public WebSocketFrameOutputStream(OutputStream out) {
        super(Objects.requireNonNull(out, "out"));
    }

    /**
     * Begins one WebSocket binary frame whose payload will be the next
     * {@code payloadSize} bytes written to this stream.
     *
     * <p>The caller invokes this exactly once before the payload's
     * first {@code write} of each frame, then writes exactly
     * {@code payloadSize} bytes, then calls {@link #flush()} to push the
     * bytes downstream. The companion datagram stream
     * ({@link com.github.auties00.cobalt.socket.datagram.WhatsAppDatagramOutputStream})
     * calls this directly when its downstream is this class so the frame
     * header is emitted before the first byte of ciphertext.
     *
     * @implNote This implementation generates a fresh mask key, builds
     * the frame header and forwards it to the underlying stream
     * immediately, then transitions to streaming mode. Subsequent
     * {@link #write(byte[], int, int)} and {@link #write(int)} calls
     * mask {@code payloadSize} bytes in place and forward them straight
     * through; the frame ends implicitly once {@code payloadSize} bytes
     * have been written.
     *
     * @param payloadSize the exact number of body bytes the caller is
     *                    about to write; must be non-negative
     * @throws IOException              if writing the frame header fails
     * @throws IllegalArgumentException if {@code payloadSize} is
     *                                  negative
     * @throws IllegalStateException    if a streaming-mode frame is
     *                                  already in flight
     */
    public synchronized void beginFrame(int payloadSize) throws IOException {
        if (payloadSize < 0) {
            throw new IllegalArgumentException("payloadSize must be non-negative: " + payloadSize);
        }
        if (streamingRemaining != 0) {
            throw new IllegalStateException("Cannot begin a new message while a streaming frame "
                    + "is already in flight (" + streamingRemaining + " bytes remaining)");
        }
        var maskKey = ThreadLocalRandom.current().nextInt();
        var headerLen = buildHeader(OPCODE_BINARY, payloadSize, maskKey);
        out.write(header, 0, headerLen);
        streamingRemaining = payloadSize;
        streamingMaskKey = maskKey;
        streamingMaskOffset = 0;
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "begin streaming websocket frame, payloadSize={0}", payloadSize);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote In streaming mode this implementation masks the byte
     * using the current frame's key and offset and forwards it directly
     * (the streaming frame is one byte closer to completion), then
     * drains any deferred control frame if the current frame just
     * ended; in one-shot mode the byte becomes a one-byte binary frame
     * on its own.
     */
    @Override
    public synchronized void write(int b) throws IOException {
        if (streamingRemaining > 0) {
            var masked = b ^ (maskByte(streamingMaskKey, streamingMaskOffset) & 0xFF);
            out.write(masked & 0xFF);
            streamingMaskOffset++;
            streamingRemaining--;
            if (streamingRemaining == 0) {
                drainPendingControlFrame();
            }
        } else {
            write(new byte[]{(byte) b}, 0, 1);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>The {@code src} array is masked in place before forwarding, so
     * the caller must not reuse the supplied range after the call
     * returns.
     *
     * @implNote In streaming mode this implementation masks the bytes in
     * place against the current frame's key and offset, then forwards
     * them (the streaming frame's remaining-byte counter is decremented;
     * writing more bytes than declared by {@link #beginFrame(int)} is
     * rejected). In one-shot mode the call produces a complete one-frame
     * write (fresh mask key, header, payload) used for control frames
     * such as PONG and CLOSE.
     */
    @Override
    public synchronized void write(byte[] src, int off, int len) throws IOException {
        if (streamingRemaining > 0) {
            if (len > streamingRemaining) {
                throw new IOException("Streaming frame overflow: tried to write " + len
                        + " bytes but only " + streamingRemaining + " remain");
            }
            if (len > 0) {
                MASKER.applyMask(src, off, len, streamingMaskKey, streamingMaskOffset);
                out.write(src, off, len);
                streamingMaskOffset += len;
                streamingRemaining -= len;
            }
            if (streamingRemaining == 0) {
                drainPendingControlFrame();
            }
            return;
        }
        writeFrame(OPCODE_BINARY, src, off, len);
    }

    /**
     * Writes a control frame (ping, pong, or close) carrying the
     * supplied payload bytes.
     *
     * <p>This is called by {@link WebSocketFrameInputStream} when it
     * auto-responds to a peer-initiated PING with a matching PONG, or
     * sends a CLOSE on shutdown. The payload is masked in place inside
     * {@code payload}; the input stream's small control-frame scratch
     * buffer is the only caller and the mutation is safe. When a
     * streaming-mode binary frame is in flight the control frame is
     * deferred until the frame completes so the wire stream is not
     * corrupted.
     *
     * @param opcode  the control opcode
     *                ({@link WebSocketFrameConstants#OPCODE_PING},
     *                {@link WebSocketFrameConstants#OPCODE_PONG} or
     *                {@link WebSocketFrameConstants#OPCODE_CLOSE})
     * @param payload the control payload bytes
     * @param length  the number of valid bytes in {@code payload}, at
     *                most
     *                {@value WebSocketFrameConstants#CONTROL_PAYLOAD_MAX_LENGTH}
     * @throws IOException              if writing fails
     * @throws IllegalArgumentException if {@code length} is negative or
     *                                  exceeds the control payload
     *                                  maximum
     */
    public synchronized void writeControlFrame(byte opcode, byte[] payload, int length) throws IOException {
        if (length < 0 || length > CONTROL_PAYLOAD_MAX_LENGTH) {
            throw new IllegalArgumentException("Invalid control payload length: " + length);
        }
        if (streamingRemaining > 0) {
            if (pendingControlPayload == null) {
                pendingControlPayload = new byte[CONTROL_PAYLOAD_MAX_LENGTH];
            }
            if (length > 0) {
                System.arraycopy(payload, 0, pendingControlPayload, 0, length);
            }
            pendingControlOpcode = opcode;
            pendingControlLength = length;
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "deferring websocket control frame opcode=0x{0} until streaming frame completes",
                        Integer.toHexString(opcode));
            }
            return;
        }
        writeFrame(opcode, payload, 0, length);
    }

    /**
     * Emits any control frame that was deferred during a streaming-mode
     * write.
     *
     * <p>The streaming-mode write paths call this once
     * {@link #streamingRemaining} reaches zero, under the same
     * {@code synchronized} block that mutated the streaming counter.
     *
     * @throws IOException if the deferred frame's emit fails
     */
    private void drainPendingControlFrame() throws IOException {
        if (pendingControlOpcode == 0) {
            return;
        }
        var opcode = pendingControlOpcode;
        var length = pendingControlLength;
        pendingControlOpcode = 0;
        pendingControlLength = 0;
        writeFrame(opcode, pendingControlPayload, 0, length);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation forwards the flush to the underlying
     * stream under the same monitor as {@link #write(byte[], int, int)}
     * and {@link #writeControlFrame(byte, byte[], int)} so the input
     * thread's auto-PONG cannot race with the writer thread's flush.
     */
    @Override
    public synchronized void flush() throws IOException {
        out.flush();
    }

    /**
     * Builds the frame header, masks the payload in place, and writes
     * the header and payload to the underlying stream under a single
     * synchronized block so concurrent senders are serialized.
     *
     * @param opcode the frame opcode
     * @param src    the payload byte array (mutated in place)
     * @param off    the offset within {@code src}
     * @param len    the number of payload bytes
     * @throws IOException if writing fails
     */
    private synchronized void writeFrame(byte opcode, byte[] src, int off, int len) throws IOException {
        var maskKey = ThreadLocalRandom.current().nextInt();
        var headerLen = buildHeader(opcode, len, maskKey);
        if (len > 0) {
            MASKER.applyMask(src, off, len, maskKey, 0);
        }
        out.write(header, 0, headerLen);
        if (len > 0) {
            out.write(src, off, len);
        }
        out.flush();
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "wrote websocket frame opcode=0x{0} length={1}", Integer.toHexString(opcode), len);
        }
    }

    /**
     * Builds the frame header into {@link #header} and returns its total
     * length in bytes.
     *
     * <p>The header encodes the FIN bit, the opcode, the masked payload
     * length (with optional 16-bit or 64-bit extension) and the
     * four-byte mask key, per RFC 6455 section 5.2.
     *
     * @param opcode        the frame opcode
     * @param payloadLength the payload length in bytes
     * @param maskKey       the four-byte masking key
     * @return the number of bytes written to {@link #header}
     */
    private int buildHeader(byte opcode, int payloadLength, int maskKey) {
        header[0] = (byte) (0x80 | (opcode & 0x0F));
        int pos;
        if (payloadLength <= SMALL_PAYLOAD_LIMIT) {
            header[1] = (byte) (0x80 | payloadLength);
            pos = 2;
        } else if (payloadLength <= 0xFFFF) {
            header[1] = (byte) (0x80 | EXTENDED_16_PAYLOAD_MARKER);
            header[2] = (byte) (payloadLength >>> 8);
            header[3] = (byte) payloadLength;
            pos = 4;
        } else {
            header[1] = (byte) (0x80 | EXTENDED_64_PAYLOAD_MARKER);
            // The high four bytes are always zero because payloadLength
            // is an int; they are written for protocol conformance.
            header[2] = 0;
            header[3] = 0;
            header[4] = 0;
            header[5] = 0;
            header[6] = (byte) (payloadLength >>> 24);
            header[7] = (byte) (payloadLength >>> 16);
            header[8] = (byte) (payloadLength >>> 8);
            header[9] = (byte) payloadLength;
            pos = 10;
        }
        header[pos] = (byte) (maskKey >>> 24);
        header[pos + 1] = (byte) (maskKey >>> 16);
        header[pos + 2] = (byte) (maskKey >>> 8);
        header[pos + 3] = (byte) maskKey;
        return pos + 4;
    }
}
