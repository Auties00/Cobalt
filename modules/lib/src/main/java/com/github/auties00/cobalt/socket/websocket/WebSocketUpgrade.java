package com.github.auties00.cobalt.socket.websocket;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.util.DataUtils;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Performs the RFC 6455 section 1.3 WebSocket upgrade handshake on an
 * already-connected {@link Socket}.
 *
 * <p>The handshake runs after the TLS handshake completes, so the WA
 * Web bundle's {@code /ws/chat} endpoint can start delivering binary
 * frames. The leftover {@link ByteBuffer} returned by
 * {@link #upgrade(Socket, String, String, int, String)} captures any
 * bytes the server piggybacked on the upgrade response (the first
 * WebSocket frame's leading bytes) so the caller can feed them through
 * {@link WebSocketFrameInputStream} before touching the underlying
 * stream again.
 *
 * @implNote This implementation assembles the request into a single
 * pre-sized {@code byte[]} via {@link System#arraycopy} on pre-encoded
 * static fragments, so no {@link StringBuilder} or charset encoder runs
 * on the hot path. The response is parsed in place inside an 8 KiB
 * scratch buffer (grown on demand up to {@link #MAX_RESPONSE_SIZE})
 * using a sliding 32-bit window to locate the {@code CRLF CRLF}
 * terminator and inline byte-literal checks for the mandatory headers.
 */
public final class WebSocketUpgrade {

    /**
     * The logger for {@link WebSocketUpgrade}.
     */
    private static final System.Logger LOGGER = Log.get(WebSocketUpgrade.class);

    /**
     * Holds the WebSocket protocol GUID used when computing the
     * {@code Sec-WebSocket-Accept} hash as defined by
     * <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-1.3">RFC 6455 section 1.3</a>.
     */
    private static final byte[] WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
            .getBytes(StandardCharsets.US_ASCII);

    /**
     * Holds the length of the random {@code Sec-WebSocket-Key} in raw
     * bytes before Base64 encoding.
     */
    private static final int WEBSOCKET_KEY_BYTES = 16;

    /**
     * Holds the expected HTTP status code for a successful upgrade.
     */
    private static final int EXPECTED_STATUS_CODE = 101;

    /**
     * Holds the maximum total size of the response headers (including
     * the status line and the terminating {@code CRLF CRLF}); sized
     * well above any realistic upgrade response.
     */
    private static final int MAX_RESPONSE_SIZE = 65536;

    /**
     * Holds the default size of the recyclable response read buffer.
     */
    private static final int INITIAL_RESPONSE_BUFFER = 8192;

    /**
     * Holds the packed bytes representing {@code "\r\n\r\n"}, used to
     * locate the end of the response headers with a single sliding
     * 32-bit comparison.
     */
    private static final int CRLF_CRLF = 0x0D0A0D0A;

    /**
     * Holds the ASCII carriage-return byte.
     */
    private static final byte CR = '\r';

    /**
     * Holds the ASCII line-feed byte.
     */
    private static final byte LF = '\n';

    /**
     * Holds the ASCII space byte (HTTP optional whitespace).
     */
    private static final byte SP = ' ';

    /**
     * Holds the ASCII comma byte (HTTP list separator).
     */
    private static final byte COMMA = ',';

    /**
     * Holds the pre-encoded HTTP request line prefix: {@code "GET "}.
     */
    private static final byte[] REQ_LINE_PREFIX = "GET ".getBytes(StandardCharsets.US_ASCII);

    /**
     * Holds the pre-encoded HTTP version and Host header prefix:
     * {@code " HTTP/1.1\r\nHost: "}.
     */
    private static final byte[] REQ_HOST_HEADER = " HTTP/1.1\r\nHost: ".getBytes(StandardCharsets.US_ASCII);

    /**
     * Holds the pre-encoded static headers block from the CRLF after
     * Host through the {@code Sec-WebSocket-Key} value prefix.
     */
    private static final byte[] REQ_STATIC_BLOCK = (
            "\r\nUpgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            "Sec-WebSocket-Version: 13\r\n" +
            "Sec-WebSocket-Key: "
    ).getBytes(StandardCharsets.US_ASCII);

    /**
     * Holds the pre-encoded Origin header prefix including leading
     * CRLF: {@code "\r\nOrigin: https://"}.
     */
    private static final byte[] REQ_ORIGIN = "\r\nOrigin: https://".getBytes(StandardCharsets.US_ASCII);

    /**
     * Holds the pre-encoded User-Agent header prefix including leading
     * CRLF: {@code "\r\nUser-Agent: "}.
     */
    private static final byte[] REQ_USER_AGENT_PREFIX = "\r\nUser-Agent: ".getBytes(StandardCharsets.US_ASCII);

    /**
     * Holds the pre-encoded request terminator: {@code "\r\n\r\n"}.
     */
    private static final byte[] REQ_END = "\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

    /**
     * Holds the lowercase ASCII bytes of the {@code Upgrade} header
     * name, used for case-insensitive comparison.
     */
    private static final byte[] HEADER_UPGRADE = "upgrade".getBytes(StandardCharsets.US_ASCII);

    /**
     * Holds the lowercase ASCII bytes of the {@code Connection} header
     * name.
     */
    private static final byte[] HEADER_CONNECTION = "connection".getBytes(StandardCharsets.US_ASCII);

    /**
     * Holds the lowercase ASCII bytes of the
     * {@code Sec-WebSocket-Accept} header name.
     */
    private static final byte[] HEADER_ACCEPT = "sec-websocket-accept".getBytes(StandardCharsets.US_ASCII);

    /**
     * Holds the lowercase ASCII bytes of the expected {@code Upgrade}
     * header value ({@code "websocket"}).
     */
    private static final byte[] VALUE_WEBSOCKET = "websocket".getBytes(StandardCharsets.US_ASCII);

    /**
     * Holds the lowercase ASCII bytes of the expected token in the
     * {@code Connection} header value ({@code "upgrade"}).
     */
    private static final byte[] VALUE_UPGRADE = "upgrade".getBytes(StandardCharsets.US_ASCII);

    /**
     * Prevents instantiation of this utility holder.
     */
    private WebSocketUpgrade() {

    }

    /**
     * Performs the WebSocket upgrade handshake on {@code socket}.
     *
     * <p>The handshake runs once per WebSocket transport open, after
     * the TLS handshake has completed. It generates the random key,
     * computes the expected accept value, writes the request and
     * validates the response. The returned leftover bytes (if any) must
     * be fed into {@link WebSocketFrameInputStream} so the server's
     * piggybacked first frame is not lost.
     *
     * @param socket    the already-connected (and, for TLS endpoints,
     *                  already-TLS-handshaken) socket
     * @param path      the WebSocket endpoint path (e.g.
     *                  {@code "/ws/chat"})
     * @param host      the target host for the {@code Host} and
     *                  {@code Origin} headers
     * @param port      the target port; the {@code Host} header omits
     *                  the port when it equals {@code 443}
     * @param userAgent the {@code User-Agent} value
     * @return a {@link ByteBuffer} containing any bytes the server
     *         sent past the {@code CRLF CRLF} terminator (the first
     *         WebSocket frame's leading bytes), or {@code null} when
     *         the response ended exactly at the terminator
     * @throws IOException if the request cannot be written, the
     *                     response is truncated, the status code is
     *                     not {@code 101}, a mandatory header is
     *                     missing or invalid, or the response
     *                     exceeds {@link #MAX_RESPONSE_SIZE} bytes
     */
    public static ByteBuffer upgrade(Socket socket, String path, String host, int port, String userAgent) throws IOException {
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "starting websocket upgrade to {0}:{1}{2}", host, port, path);
        }
        var key = createWebSocketKey();
        var expectedAccept = computeExpectedAccept(key);
        sendUpgradeRequest(socket, path, host, port, key, userAgent);
        var leftover = readUpgradeResponse(socket, expectedAccept);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "websocket upgrade succeeded, leftover={0} byte(s)",
                    leftover != null ? leftover.remaining() : 0);
        }
        return leftover;
    }

    /**
     * Generates a random 16-byte WebSocket key, Base64-encoded as a
     * {@code byte[]} to avoid an intermediate {@link String}.
     *
     * @return the Base64-encoded key as ASCII bytes
     */
    private static byte[] createWebSocketKey() {
        var raw = DataUtils.randomByteArray(WEBSOCKET_KEY_BYTES);
        return Base64.getEncoder().encode(raw);
    }

    /**
     * Computes the expected {@code Sec-WebSocket-Accept} value by
     * hashing the key concatenated with the WebSocket GUID using
     * SHA-1.
     *
     * @param key the client-generated key as ASCII bytes
     * @return the expected Base64-encoded accept value as ASCII
     *         bytes
     * @throws IOException if SHA-1 is not available
     */
    private static byte[] computeExpectedAccept(byte[] key) throws IOException {
        try {
            var digest = MessageDigest.getInstance("SHA-1");
            digest.update(key);
            digest.update(WEBSOCKET_GUID);
            return Base64.getEncoder().encode(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException("Cannot compute WebSocket accept key", exception);
        }
    }

    /**
     * Sends the HTTP/1.1 WebSocket upgrade request as a single
     * {@code byte[]} write, using pre-encoded fragments and
     * {@link System#arraycopy}.
     *
     * @param socket    the connected socket
     * @param path      the WebSocket endpoint path
     * @param host      the target host
     * @param port      the target port
     * @param key       the Base64-encoded WebSocket key
     * @param userAgent the User-Agent value
     * @throws IOException if writing fails
     */
    private static void sendUpgradeRequest(Socket socket, String path, String host, int port, byte[] key, String userAgent) throws IOException {
        var hostBytes = host.getBytes(StandardCharsets.US_ASCII);
        var pathBytes = path.getBytes(StandardCharsets.US_ASCII);
        var portBytes = port != 443 ? Integer.toString(port).getBytes(StandardCharsets.US_ASCII) : null;
        var userAgentBytes = userAgent.getBytes(StandardCharsets.US_ASCII);

        var size = REQ_LINE_PREFIX.length + pathBytes.length + REQ_HOST_HEADER.length
                + hostBytes.length + (portBytes != null ? 1 + portBytes.length : 0)
                + REQ_STATIC_BLOCK.length + key.length
                + REQ_USER_AGENT_PREFIX.length + userAgentBytes.length
                + REQ_ORIGIN.length + hostBytes.length
                + REQ_END.length;

        var request = new byte[size];
        var pos = 0;

        System.arraycopy(REQ_LINE_PREFIX, 0, request, pos, REQ_LINE_PREFIX.length);
        pos += REQ_LINE_PREFIX.length;

        System.arraycopy(pathBytes, 0, request, pos, pathBytes.length);
        pos += pathBytes.length;

        System.arraycopy(REQ_HOST_HEADER, 0, request, pos, REQ_HOST_HEADER.length);
        pos += REQ_HOST_HEADER.length;

        System.arraycopy(hostBytes, 0, request, pos, hostBytes.length);
        pos += hostBytes.length;

        if (portBytes != null) {
            request[pos++] = ':';
            System.arraycopy(portBytes, 0, request, pos, portBytes.length);
            pos += portBytes.length;
        }

        System.arraycopy(REQ_STATIC_BLOCK, 0, request, pos, REQ_STATIC_BLOCK.length);
        pos += REQ_STATIC_BLOCK.length;

        System.arraycopy(key, 0, request, pos, key.length);
        pos += key.length;

        System.arraycopy(REQ_USER_AGENT_PREFIX, 0, request, pos, REQ_USER_AGENT_PREFIX.length);
        pos += REQ_USER_AGENT_PREFIX.length;

        System.arraycopy(userAgentBytes, 0, request, pos, userAgentBytes.length);
        pos += userAgentBytes.length;

        System.arraycopy(REQ_ORIGIN, 0, request, pos, REQ_ORIGIN.length);
        pos += REQ_ORIGIN.length;

        System.arraycopy(hostBytes, 0, request, pos, hostBytes.length);
        pos += hostBytes.length;

        System.arraycopy(REQ_END, 0, request, pos, REQ_END.length);

        var out = socket.getOutputStream();
        out.write(request);
        out.flush();
    }

    /**
     * Reads the upgrade response into a growable byte buffer,
     * locates the {@code CRLF CRLF} terminator via a sliding 32-bit
     * window, validates the status code and mandatory headers, and
     * returns any leftover bytes past the terminator.
     *
     * @param socket         the connected socket
     * @param expectedAccept the expected {@code Sec-WebSocket-Accept}
     *                       value
     * @return any bytes past {@code CRLF CRLF}, or {@code null} if
     *         none
     * @throws IOException on truncation, status mismatch, missing
     *                     header, or oversized response
     */
    private static ByteBuffer readUpgradeResponse(Socket socket, byte[] expectedAccept) throws IOException {
        var in = socket.getInputStream();
        var buffer = new byte[INITIAL_RESPONSE_BUFFER];
        var filled = 0;
        var headerEnd = -1;
        var last4 = 0;

        while (headerEnd < 0) {
            if (filled == buffer.length) {
                if (buffer.length >= MAX_RESPONSE_SIZE) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "websocket upgrade response exceeds {0} bytes", MAX_RESPONSE_SIZE);
                    }
                    throw new IOException("WebSocket upgrade response exceeds " + MAX_RESPONSE_SIZE + " bytes");
                }
                var grown = new byte[Math.min(buffer.length * 2, MAX_RESPONSE_SIZE)];
                System.arraycopy(buffer, 0, grown, 0, filled);
                buffer = grown;
            }
            var n = in.read(buffer, filled, buffer.length - filled);
            if (n < 0) {
                throw new IOException("Unexpected end of stream while reading WebSocket upgrade response");
            }
            for (var i = filled; i < filled + n; i++) {
                last4 = (last4 << 8) | (buffer[i] & 0xFF);
                if (last4 == CRLF_CRLF) {
                    headerEnd = i + 1;
                    break;
                }
            }
            filled += n;
        }

        validateStatusLine(buffer, headerEnd);
        validateHeaders(buffer, headerEnd, expectedAccept);

        if (headerEnd == filled) {
            return null;
        }
        var leftover = ByteBuffer.allocate(filled - headerEnd);
        leftover.put(buffer, headerEnd, filled - headerEnd);
        leftover.flip();
        return leftover;
    }

    /**
     * Validates that the response status line begins with
     * {@code "HTTP/1.x 101"}.
     *
     * @param buffer    the response buffer
     * @param headerEnd the offset (exclusive) of the end of the
     *                  header block
     * @throws IOException if the status line is malformed or the
     *                     status code is not {@code 101}
     */
    private static void validateStatusLine(byte[] buffer, int headerEnd) throws IOException {
        var lineEnd = indexOfCrlf(buffer, 0, headerEnd);
        if (lineEnd < 0) {
            throw new IOException("Malformed WebSocket upgrade response: no status line");
        }
        if (lineEnd < 12) {
            throw new IOException("Malformed WebSocket upgrade status line: " + previewLine(buffer, 0, lineEnd));
        }
        if (buffer[0] != 'H' || buffer[1] != 'T' || buffer[2] != 'T' || buffer[3] != 'P' || buffer[4] != '/') {
            throw new IOException("Malformed WebSocket upgrade status line: " + previewLine(buffer, 0, lineEnd));
        }
        var pos = 5;
        if (!isDigit(buffer[pos])) {
            throw new IOException("Malformed WebSocket upgrade status line: " + previewLine(buffer, 0, lineEnd));
        }
        pos++;
        if (buffer[pos] == '.') {
            pos++;
            if (!isDigit(buffer[pos])) {
                throw new IOException("Malformed WebSocket upgrade status line: " + previewLine(buffer, 0, lineEnd));
            }
            pos++;
        }
        if (buffer[pos] != SP) {
            throw new IOException("Malformed WebSocket upgrade status line: " + previewLine(buffer, 0, lineEnd));
        }
        pos++;

        if (pos + 3 > lineEnd) {
            throw new IOException("Malformed WebSocket upgrade status line: " + previewLine(buffer, 0, lineEnd));
        }
        var d1 = buffer[pos];
        var d2 = buffer[pos + 1];
        var d3 = buffer[pos + 2];
        if (!isDigit(d1) || !isDigit(d2) || !isDigit(d3)) {
            throw new IOException("Malformed WebSocket upgrade status line: " + previewLine(buffer, 0, lineEnd));
        }
        var statusCode = (d1 - '0') * 100 + (d2 - '0') * 10 + (d3 - '0');
        if (statusCode != EXPECTED_STATUS_CODE) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "websocket upgrade failed with status code {0}", statusCode);
            }
            throw new IOException("WebSocket upgrade failed with status code " + statusCode);
        }
    }

    /**
     * Walks the header block line by line and validates the three
     * mandatory headers: {@code Upgrade: websocket},
     * {@code Connection: upgrade} and {@code Sec-WebSocket-Accept}.
     *
     * @param buffer         the response buffer
     * @param headerEnd      the offset (exclusive) of the end of the
     *                       header block
     * @param expectedAccept the expected accept value as ASCII bytes
     * @throws IOException if any mandatory header is missing or
     *                     invalid
     */
    private static void validateHeaders(byte[] buffer, int headerEnd, byte[] expectedAccept) throws IOException {
        var pos = indexOfCrlf(buffer, 0, headerEnd) + 2;

        var foundUpgrade = false;
        var foundConnection = false;
        var foundAccept = false;

        while (pos < headerEnd - 2) {
            var lineEnd = indexOfCrlf(buffer, pos, headerEnd);
            if (lineEnd < 0 || lineEnd == pos) {
                break;
            }

            var colon = indexOf(buffer, pos, lineEnd, ':');
            if (colon < 0) {
                pos = lineEnd + 2;
                continue;
            }

            if (!foundUpgrade && nameMatchesIgnoreCase(buffer, pos, colon, HEADER_UPGRADE)) {
                if (valueEqualsIgnoreCase(buffer, colon + 1, lineEnd, VALUE_WEBSOCKET)) {
                    foundUpgrade = true;
                }
            } else if (!foundConnection && nameMatchesIgnoreCase(buffer, pos, colon, HEADER_CONNECTION)) {
                if (valueContainsTokenIgnoreCase(buffer, colon + 1, lineEnd, VALUE_UPGRADE)) {
                    foundConnection = true;
                }
            } else if (!foundAccept && nameMatchesIgnoreCase(buffer, pos, colon, HEADER_ACCEPT)) {
                if (valueEqualsExact(buffer, colon + 1, lineEnd, expectedAccept)) {
                    foundAccept = true;
                }
            }

            pos = lineEnd + 2;
        }

        if (!foundUpgrade || !foundConnection) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "websocket upgrade response is missing mandatory headers, upgrade={0} connection={1}",
                        foundUpgrade, foundConnection);
            }
            throw new IOException("WebSocket upgrade response is missing mandatory headers");
        }
        if (!foundAccept) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "websocket upgrade failed: invalid sec-websocket-accept");
            }
            throw new IOException("WebSocket upgrade failed: invalid Sec-WebSocket-Accept");
        }
    }

    /**
     * Returns the index of the next CRLF starting at {@code from},
     * or {@code -1} if none exists before {@code limit}.
     *
     * @param buffer the byte array to scan
     * @param from   the inclusive start offset
     * @param limit  the exclusive limit
     * @return the index of the {@code \r} of the CRLF, or {@code -1}
     */
    private static int indexOfCrlf(byte[] buffer, int from, int limit) {
        for (var i = from; i + 1 < limit; i++) {
            if (buffer[i] == CR && buffer[i + 1] == LF) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the index of the first occurrence of {@code target}
     * between {@code from} and {@code limit}, or {@code -1}.
     *
     * @param buffer the byte array to scan
     * @param from   the inclusive start offset
     * @param limit  the exclusive limit
     * @param target the target byte value
     * @return the index, or {@code -1}
     */
    private static int indexOf(byte[] buffer, int from, int limit, int target) {
        for (var i = from; i < limit; i++) {
            if (buffer[i] == target) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns whether the bytes between {@code start} and {@code end}
     * match the supplied lowercase ASCII header name
     * case-insensitively, with no trailing characters before the
     * colon.
     *
     * @param buffer the byte array
     * @param start  the inclusive start of the candidate name
     * @param end    the exclusive end of the candidate name (the
     *               colon offset)
     * @param name   the lowercase ASCII expected name
     * @return {@code true} if the bytes match the name
     */
    private static boolean nameMatchesIgnoreCase(byte[] buffer, int start, int end, byte[] name) {
        if (end - start != name.length) {
            return false;
        }
        for (var i = 0; i < name.length; i++) {
            var actual = buffer[start + i];
            if ((actual | 0x20) != name[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether the header value between {@code start} and
     * {@code end} equals {@code expected} case-insensitively, after
     * trimming leading and trailing optional whitespace.
     *
     * @param buffer   the byte array
     * @param start    the inclusive start of the value (one past the
     *                 colon)
     * @param end      the exclusive end of the value (the CR of the
     *                 line's CRLF)
     * @param expected the expected value in lowercase ASCII
     * @return {@code true} if the value matches
     */
    private static boolean valueEqualsIgnoreCase(byte[] buffer, int start, int end, byte[] expected) {
        var lo = trimLeading(buffer, start, end);
        var hi = trimTrailing(buffer, lo, end);
        if (hi - lo != expected.length) {
            return false;
        }
        for (var i = 0; i < expected.length; i++) {
            if ((buffer[lo + i] | 0x20) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether the header value between {@code start} and
     * {@code end} equals {@code expected} exactly (case-sensitive),
     * after trimming leading and trailing optional whitespace.
     *
     * @param buffer   the byte array
     * @param start    the inclusive start of the value
     * @param end      the exclusive end of the value
     * @param expected the expected value as ASCII bytes
     * @return {@code true} if the value matches
     */
    private static boolean valueEqualsExact(byte[] buffer, int start, int end, byte[] expected) {
        var lo = trimLeading(buffer, start, end);
        var hi = trimTrailing(buffer, lo, end);
        if (hi - lo != expected.length) {
            return false;
        }
        for (var i = 0; i < expected.length; i++) {
            if (buffer[lo + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether a comma-separated header value between
     * {@code start} and {@code end} contains the supplied
     * lowercase-ASCII token as a case-insensitive whole-token match
     * (RFC 7230 token list semantics).
     *
     * @param buffer the byte array
     * @param start  the inclusive start of the value
     * @param end    the exclusive end of the value
     * @param token  the token in lowercase ASCII
     * @return {@code true} if the token is present
     */
    private static boolean valueContainsTokenIgnoreCase(byte[] buffer, int start, int end, byte[] token) {
        var pos = start;
        while (pos < end) {
            pos = trimLeading(buffer, pos, end);
            var tokenStart = pos;
            while (pos < end && buffer[pos] != COMMA) {
                pos++;
            }
            var tokenEnd = trimTrailing(buffer, tokenStart, pos);
            if (tokenEnd - tokenStart == token.length) {
                var matched = true;
                for (var i = 0; i < token.length; i++) {
                    if ((buffer[tokenStart + i] | 0x20) != token[i]) {
                        matched = false;
                        break;
                    }
                }
                if (matched) {
                    return true;
                }
            }
            if (pos < end) {
                pos++;
            }
        }
        return false;
    }

    /**
     * Returns the offset of the first non-whitespace byte between
     * {@code start} and {@code limit}.
     *
     * @param buffer the byte array
     * @param start  the inclusive start offset
     * @param limit  the exclusive limit
     * @return the trimmed start offset
     */
    private static int trimLeading(byte[] buffer, int start, int limit) {
        var pos = start;
        while (pos < limit && (buffer[pos] == SP || buffer[pos] == '\t')) {
            pos++;
        }
        return pos;
    }

    /**
     * Returns the offset of the byte after the last non-whitespace
     * byte between {@code start} and {@code limit}.
     *
     * @param buffer the byte array
     * @param start  the inclusive start offset
     * @param limit  the exclusive limit
     * @return the trimmed end offset
     */
    private static int trimTrailing(byte[] buffer, int start, int limit) {
        var pos = limit;
        while (pos > start && (buffer[pos - 1] == SP || buffer[pos - 1] == '\t' || buffer[pos - 1] == CR)) {
            pos--;
        }
        return pos;
    }

    /**
     * Returns whether the given byte is an ASCII digit.
     *
     * @param b the byte to test
     * @return {@code true} if {@code b} is in {@code '0'..'9'}
     */
    private static boolean isDigit(byte b) {
        return b >= '0' && b <= '9';
    }

    /**
     * Returns a human-readable preview of a line for use in error
     * messages.
     *
     * @param buffer the byte array
     * @param start  the inclusive start offset
     * @param end    the exclusive end offset
     * @return the line content as a string
     */
    private static String previewLine(byte[] buffer, int start, int end) {
        return new String(buffer, start, end - start, StandardCharsets.US_ASCII);
    }
}
