package com.github.auties00.cobalt.socket.websocket;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.util.Objects;

import static com.github.auties00.cobalt.socket.websocket.WebSocketFrameConstants.*;

/**
 * Parses RFC 6455 frames from an underlying {@link InputStream} and
 * exposes the unmasked payload bytes as a continuous byte stream.
 *
 * <p>Frame payloads are read directly into the caller's {@code dst}
 * array and unmasked <strong>in place</strong> through the
 * {@link WebSocketMasker} selected at load time; no intermediate buffer
 * or extra copy. Control frames (PING, CLOSE,
 * PONG) are handled inline: PING triggers a matching PONG through the
 * paired {@link WebSocketFrameOutputStream}, CLOSE marks the stream
 * as closed (subsequent reads return {@code -1}), PONG is ignored.
 *
 * <p>Leftover bytes from the WebSocket upgrade response (when the
 * server piggybacks the first frame on the same TCP segment) are
 * supplied at construction time and drained transparently before
 * touching the underlying stream.
 *
 * <p>Instances are <strong>not</strong> thread-safe: one decoder is
 * intended to be owned by a single reader thread. The paired output
 * stream is itself synchronized so the input thread's auto-PONG
 * cannot race the writer thread's data frame.
 */
public final class WebSocketFrameInputStream extends FilterInputStream {

    /**
     * The logger for {@link WebSocketFrameInputStream}.
     */
    private static final System.Logger LOGGER = Log.get(WebSocketFrameInputStream.class);

    /**
     * Holds the masker selected once at class-load time, shared by every
     * instance to unmask payloads in place.
     */
    private static final WebSocketMasker MASKER = WebSocketMasker.INSTANCE;

    /**
     * Holds the paired output stream used to send PONG replies to PING
     * frames and CLOSE acknowledgements.
     */
    private final WebSocketFrameOutputStream pairedOutput;

    /**
     * Holds the reusable buffer for control-frame payloads, sized at
     * the RFC 6455 section 5.5 maximum of 125 bytes so it never needs
     * to grow.
     */
    private final byte[] controlPayload = new byte[CONTROL_PAYLOAD_MAX_LENGTH];

    /**
     * Holds the reusable single-byte buffer used by {@link #read()} to
     * delegate to the bulk-read code path.
     */
    private final byte[] oneByteBuf = new byte[1];

    /**
     * Holds the bytes carried over from the WebSocket upgrade response
     * (the first frame's bytes that arrived in the same TCP segment),
     * or {@code null} once they are fully drained.
     */
    private ByteBuffer leftover;

    /**
     * Holds the number of payload bytes still to be consumed from the
     * current frame.
     *
     * <p>The value is zero when no frame is in flight, in which case
     * the next read triggers a new header parse.
     */
    private long frameRemaining;

    /**
     * Holds the opcode of the current frame, set by
     * {@link #parseFrameHeader()}.
     */
    private byte currentOpcode;

    /**
     * Holds the four-byte masking key from the current frame's header.
     */
    private int maskKey;

    /**
     * Holds the running offset into the mask cycle.
     *
     * <p>The value is incremented as payload bytes are consumed so the
     * mask is applied continuously across multiple {@code read} calls
     * inside a single frame.
     */
    private int maskOffset;

    /**
     * Indicates whether the current frame's MASK bit is set.
     *
     * <p>WhatsApp's server never masks its frames, but the bit is
     * honoured for RFC conformance so a strict intermediary that masks
     * server-to-client frames would still decode correctly.
     */
    private boolean masked;

    /**
     * Indicates whether a CLOSE frame has been received; once set,
     * subsequent reads return {@code -1}.
     */
    private boolean closed;

    /**
     * Wraps an underlying input stream with the WebSocket frame parser.
     *
     * <p>The decoder sits directly above the TLS-decrypted byte stream,
     * with the {@code leftover} buffer carrying any bytes the server
     * piggybacked on the upgrade response so they are drained before
     * the underlying stream is touched.
     *
     * @param in           the underlying stream of TLS-decrypted bytes
     * @param leftover     any bytes already read past the upgrade
     *                     response's {@code CRLF CRLF}, or {@code null}
     *                     if the upgrade response was consumed exactly
     * @param pairedOutput the paired output stream that PONG replies
     *                     and CLOSE acknowledgements are sent through
     * @throws NullPointerException if {@code in} or {@code pairedOutput}
     *                              is {@code null}
     */
    public WebSocketFrameInputStream(InputStream in, ByteBuffer leftover, WebSocketFrameOutputStream pairedOutput) {
        super(Objects.requireNonNull(in, "in"));
        this.leftover = leftover != null && leftover.hasRemaining() ? leftover : null;
        this.pairedOutput = Objects.requireNonNull(pairedOutput, "pairedOutput");
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation delegates to
     * {@link #read(byte[], int, int)} through {@link #oneByteBuf} so
     * the frame parser sees a single code path.
     */
    @Override
    public int read() throws IOException {
        var n = read(oneByteBuf, 0, 1);
        return n < 0 ? -1 : oneByteBuf[0] & 0xFF;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation parses any pending frame header
     * inline (recursing into control-frame handling as needed) and then
     * reads payload bytes directly into {@code dst} with SIMD unmasking
     * applied in place; no intermediate copy. It returns {@code -1}
     * when the stream is closed or after a CLOSE frame is received.
     */
    @Override
    public int read(byte[] dst, int off, int len) throws IOException {
        if (closed) {
            return -1;
        }
        if (len == 0) {
            return 0;
        }

        while (frameRemaining == 0) {
            if (!parseFrameHeader()) {
                return -1;
            }
            if (isControlOpcode()) {
                handleControlFrame();
                if (closed) {
                    return -1;
                }
            }
        }

        var toRead = (int) Math.min(frameRemaining, len);
        var n = readFromSource(dst, off, toRead);
        if (n < 0) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "websocket stream ended mid-frame, {0} byte(s) still expected", frameRemaining);
            }
            throw new IOException("Unexpected end of stream mid-frame");
        }
        if (masked) {
            MASKER.applyMask(dst, off, n, maskKey, maskOffset);
            maskOffset += n;
        }
        frameRemaining -= n;
        return n;
    }

    /**
     * Reads bytes from {@link #leftover} (if any) before falling
     * through to the underlying stream.
     *
     * @param dst the destination byte array
     * @param off the offset within {@code dst}
     * @param len the maximum number of bytes to read
     * @return the number of bytes read, or {@code -1} on end-of-stream
     * @throws IOException if the underlying read fails
     */
    private int readFromSource(byte[] dst, int off, int len) throws IOException {
        if (leftover != null) {
            var n = Math.min(leftover.remaining(), len);
            leftover.get(dst, off, n);
            if (!leftover.hasRemaining()) {
                leftover = null;
            }
            return n;
        }
        return in.read(dst, off, len);
    }

    /**
     * Reads exactly one byte from {@link #leftover} or the underlying
     * stream, used by the header parser.
     *
     * <p>This variant treats end-of-stream as an error, since it is
     * only called mid-header where a clean close cannot occur; the
     * {@link #readByteOrEof()} variant tolerates end-of-stream at a
     * frame boundary instead.
     *
     * @return the byte in {@code 0..255}
     * @throws IOException on end-of-stream or read failure
     */
    private int readByte() throws IOException {
        if (leftover != null) {
            var b = leftover.get() & 0xFF;
            if (!leftover.hasRemaining()) {
                leftover = null;
            }
            return b;
        }
        var b = in.read();
        if (b < 0) {
            throw new IOException("Unexpected end of stream while reading WebSocket frame header");
        }
        return b;
    }

    /**
     * Reads exactly {@code count} bytes from {@link #leftover} or the
     * underlying stream into {@code dst} at {@code off}.
     *
     * @param dst   the destination byte array
     * @param off   the offset within {@code dst}
     * @param count the exact number of bytes to read
     * @throws IOException on end-of-stream or read failure
     */
    private void readFully(byte[] dst, int off, int count) throws IOException {
        var pos = 0;
        while (pos < count) {
            var n = readFromSource(dst, off + pos, count - pos);
            if (n < 0) {
                throw new IOException("Unexpected end of stream while reading WebSocket frame");
            }
            pos += n;
        }
    }

    /**
     * Reads the next frame header bytes (1 + 1 + 0/2/8 + 0/4) and
     * populates {@link #frameRemaining}, {@link #masked},
     * {@link #maskKey}, {@link #maskOffset} and the current opcode.
     *
     * <p>The method returns {@code false} only when end-of-stream is
     * reached cleanly at the boundary between frames; a partial header
     * mid-read raises {@link IOException} via {@link #readByte()}.
     * Reserved bits, an out-of-bounds length and an oversized control
     * frame are all rejected as protocol errors.
     *
     * @return {@code true} if a frame header was parsed, {@code false}
     *         on clean end-of-stream
     * @throws IOException if the header is malformed or truncated
     */
    private boolean parseFrameHeader() throws IOException {
        var first = readByteOrEof();
        if (first < 0) {
            return false;
        }
        var second = readByte();

        if ((first & 0x70) != 0) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "websocket frame has reserved bits set: 0x{0}", Integer.toHexString(first));
            }
            throw new IOException("WebSocket frame has reserved bits set: 0x"
                    + Integer.toHexString(first));
        }

        currentOpcode = (byte) (first & 0x0F);
        var maskedBit = (second & 0x80) != 0;
        var lengthField = second & 0x7F;

        long length;
        if (lengthField <= SMALL_PAYLOAD_LIMIT) {
            length = lengthField;
        } else if (lengthField == EXTENDED_16_PAYLOAD_MARKER) {
            length = ((long) readByte() << 8) | readByte();
        } else {
            length = 0;
            for (var i = 0; i < 8; i++) {
                length = (length << 8) | readByte();
            }
        }

        if (length < 0 || length > MAX_FRAME_LENGTH) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "websocket frame length out of bounds: {0}", length);
            }
            throw new IOException("WebSocket frame length out of bounds: " + length);
        }

        if (isControlOpcode() && length > CONTROL_PAYLOAD_MAX_LENGTH) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "websocket control frame too large: {0}", length);
            }
            throw new IOException("WebSocket control frame too large: " + length);
        }

        masked = maskedBit;
        maskOffset = 0;
        if (maskedBit) {
            maskKey = (readByte() << 24)
                    | (readByte() << 16)
                    | (readByte() << 8)
                    | readByte();
        } else {
            maskKey = 0;
        }

        frameRemaining = length;
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "websocket frame header opcode=0x{0} length={1} masked={2}",
                    Integer.toHexString(currentOpcode), length, masked);
        }
        return true;
    }

    /**
     * Reads one byte, returning {@code -1} on clean end-of-stream.
     *
     * <p>This is used when reading the first byte of a new frame header,
     * where a peer disconnect is a normal close rather than an error;
     * the mid-frame {@link #readByte()} variant raises
     * {@link IOException} instead.
     *
     * @return the byte in {@code 0..255}, or {@code -1} on
     *         end-of-stream
     * @throws IOException on read failure
     */
    private int readByteOrEof() throws IOException {
        if (leftover != null) {
            var b = leftover.get() & 0xFF;
            if (!leftover.hasRemaining()) {
                leftover = null;
            }
            return b;
        }
        return in.read();
    }

    /**
     * Returns whether the current frame's opcode identifies a control
     * frame (CLOSE, PING, PONG).
     *
     * @return {@code true} if the current opcode is a control opcode
     */
    private boolean isControlOpcode() {
        return currentOpcode == OPCODE_CLOSE
                || currentOpcode == OPCODE_PING
                || currentOpcode == OPCODE_PONG;
    }

    /**
     * Drains the current control frame's payload and dispatches on its
     * opcode.
     *
     * <p>The payload is read into {@link #controlPayload} (unmasked if
     * necessary). A {@link WebSocketFrameConstants#OPCODE_PING} is
     * answered with a matching PONG through the paired output stream;
     * a {@link WebSocketFrameConstants#OPCODE_CLOSE} is answered with a
     * best-effort CLOSE acknowledgement and marks the stream closed;
     * a {@link WebSocketFrameConstants#OPCODE_PONG} is silently dropped
     * since outstanding pings are not tracked.
     *
     * @throws IOException if reading or replying fails
     */
    private void handleControlFrame() throws IOException {
        var length = (int) frameRemaining;
        if (length > 0) {
            readFully(controlPayload, 0, length);
            if (masked) {
                MASKER.applyMask(controlPayload, 0, length, maskKey, maskOffset);
                maskOffset += length;
            }
        }
        frameRemaining = 0;

        switch (currentOpcode) {
            case OPCODE_PING -> {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "received websocket ping, sending pong, length={0}", length);
                }
                pairedOutput.writeControlFrame(OPCODE_PONG, controlPayload, length);
            }
            case OPCODE_CLOSE -> {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "received websocket close frame, acknowledging");
                }
                try {
                    pairedOutput.writeControlFrame(OPCODE_CLOSE, controlPayload, length);
                } catch (IOException e) {
                    // Peer may have torn down the socket before the
                    // acknowledgement could land; CLOSE is best effort.
                    if (Log.DEBUG) {
                        LOGGER.log(Level.DEBUG, "failed to send websocket close acknowledgement", e);
                    }
                }
                closed = true;
            }
            case OPCODE_PONG -> {
                if (Log.TRACE) {
                    LOGGER.log(Level.TRACE, "received websocket pong, ignoring");
                }
            }
            default -> {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "unexpected websocket control opcode: 0x{0}", Integer.toHexString(currentOpcode));
                }
                throw new IOException("Unexpected control opcode: 0x"
                        + Integer.toHexString(currentOpcode));
            }
        }
    }
}
