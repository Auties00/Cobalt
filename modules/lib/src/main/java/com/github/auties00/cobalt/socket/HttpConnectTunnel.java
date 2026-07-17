package com.github.auties00.cobalt.socket;

import com.github.auties00.cobalt.client.WhatsAppClientProxy;
import com.github.auties00.cobalt.client.WhatsAppClientProxyAuthenticator;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Establishes an HTTP {@code CONNECT} tunnel on an already-connected
 * {@link Socket}.
 *
 * <p>This holder is used when the WhatsApp connection must traverse an HTTP
 * proxy and the proxy hop is plain or already TLS-wrapped by the caller. The
 * JDK's {@code Socket(Proxy)} constructor rejects
 * {@link java.net.Proxy.Type#HTTP} (only {@code SOCKS} and {@code DIRECT}
 * are accepted), and {@link java.net.http.HttpClient} cannot be plugged into
 * a raw socket transport, so every form factor that needs HTTP-proxy
 * tunnelling negotiates the {@code CONNECT} itself.
 *
 * @implNote
 * This implementation keeps the parser deliberately minimal: it
 * confirms the response is {@code HTTP/X[.X] 2xx} (matching both the
 * {@code HTTP/1.x} and {@code HTTP/2} status-line shapes) and scans
 * the byte stream for the {@code CRLF CRLF} header terminator. Header
 * fields are not parsed, header blocks larger than the read buffer
 * are still accepted by recycling the buffer in place, and any bytes
 * received after {@code CRLF CRLF} are rejected because the raw
 * socket {@link java.io.InputStream} provides no push-back to the
 * next protocol layer. No {@code 407} retry is attempted;
 * authentication is preemptive when a
 * {@link WhatsAppClientProxyAuthenticator.Http.Basic} is supplied and the
 * call fails if the proxy rejects the first attempt. Hosts containing
 * a colon are bracketed as IPv6 literals in both the request-target
 * and the {@code Host} header. On a malformed status line the
 * buffered response is embedded verbatim in the thrown
 * {@link IOException} to ease proxy debugging.
 */
final class HttpConnectTunnel {

    /**
     * The logger for {@link HttpConnectTunnel}.
     */
    private static final System.Logger LOGGER = Log.get(HttpConnectTunnel.class);

    /**
     * The size of the reusable read buffer used to drain the
     * {@code CONNECT} response.
     */
    private static final int READ_BUFFER_SIZE = 8192;

    /**
     * The minimum length of an HTTP response status line, namely
     * {@code "HTTP/X.X NNN"} without the trailing {@code CRLF}.
     */
    private static final int STATUS_LINE_MIN_LENGTH = 12;

    /**
     * The packed bytes representing {@code "\r\n\r\n"}, used to detect
     * the end of the header block with a single sliding 32-bit
     * comparison.
     */
    private static final int CRLF_CRLF = 0x0D0A0D0A;

    /**
     * Prevents instantiation of this utility holder.
     */
    private HttpConnectTunnel() {

    }

    /**
     * Issues an HTTP {@code CONNECT} request over {@code socket} for
     * the given target endpoint and consumes the response.
     *
     * <p>The {@code socket} parameter is typed as {@link Socket} so the same
     * method covers both a plain socket (Mobile path) and an
     * {@link javax.net.ssl.SSLSocket} already wrapping a connection to a
     * {@link WhatsAppClientProxy.Http.Secure} proxy. On success the socket's input
     * stream is positioned past the response header block and ready for the
     * next protocol layer.
     *
     * @param socket     the already-connected proxy socket
     * @param targetHost the host the tunnel is meant to reach
     * @param targetPort the port the tunnel is meant to reach
     * @param auth       the proxy authentication strategy, or
     *                   {@code null} for anonymous proxies
     * @throws IOException if the request cannot be written, the
     *         response cannot be read in full, the status line is
     *         malformed, the status code is not {@code 2xx}, or any
     *         other I/O failure occurs
     */
    static void tunnel(Socket socket, String targetHost, int targetPort,
                       WhatsAppClientProxyAuthenticator.Http auth) throws IOException {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "opening http connect tunnel to {0}:{1}", targetHost, targetPort);
        sendConnect(socket, targetHost, targetPort, auth);
        readConnectResponse(socket);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "http connect tunnel established to {0}:{1}", targetHost, targetPort);
    }

    /**
     * Writes the {@code CONNECT} request and the optional
     * {@code Proxy-Authorization} header to the socket.
     *
     * @param socket     the proxy socket
     * @param targetHost the target host
     * @param targetPort the target port
     * @param auth       the optional authenticator, or {@code null}
     * @throws IOException if the underlying write fails
     */
    private static void sendConnect(Socket socket, String targetHost, int targetPort,
                                    WhatsAppClientProxyAuthenticator.Http auth) throws IOException {
        var request = new StringBuilder(160).append("CONNECT ");
        appendAuthority(request, targetHost, targetPort).append(" HTTP/1.1\r\nHost: ");
        appendAuthority(request, targetHost, targetPort).append("\r\n");
        if (auth != null) {
            request.append("Proxy-Authorization: ").append(auth.authorization()).append("\r\n");
        }
        request.append("\r\n");

        var out = socket.getOutputStream();
        out.write(request.toString().getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }

    /**
     * Appends an HTTP authority of the form {@code host:port} to
     * {@code sb}, bracketing the host if it is an IPv6 literal.
     *
     * <p>IPv6 is detected by the presence of a colon and the absence of a
     * leading bracket; an already-bracketed literal is passed through
     * verbatim.
     *
     * @param sb   the destination buffer
     * @param host the host, either a registered name, an IPv4
     *             literal, or an unbracketed IPv6 literal
     * @param port the port
     * @return the same {@link StringBuilder} for chaining
     */
    private static StringBuilder appendAuthority(StringBuilder sb, String host, int port) {
        if (!host.isEmpty() && host.charAt(0) != '[' && host.indexOf(':') >= 0) {
            sb.append('[').append(host).append(']');
        } else {
            sb.append(host);
        }
        return sb.append(':').append(port);
    }

    /**
     * Reads the {@code CONNECT} response status line, verifies it
     * reports a {@code 2xx} code, and consumes the header block up to
     * and including the {@code CRLF CRLF} terminator.
     *
     * @implNote
     * This implementation slides a 32-bit window through the response
     * bytes looking for {@link #CRLF_CRLF} so the header block can be
     * located without parsing individual header fields. When the
     * header block exceeds {@link #READ_BUFFER_SIZE} the buffer is
     * recycled in place since the only state that must be preserved
     * across refills lives in the sliding window itself.
     *
     * @param socket the proxy socket
     * @throws IOException if the response is incomplete, malformed,
     *         or reports a non-{@code 2xx} status
     */
    private static void readConnectResponse(Socket socket) throws IOException {
        var in = socket.getInputStream();
        var buffer = new byte[READ_BUFFER_SIZE];
        var filled = 0;

        while (filled < STATUS_LINE_MIN_LENGTH) {
            var n = in.read(buffer, filled, buffer.length - filled);
            if (n < 0) {
                throw new IOException("Unexpected end of stream before CONNECT status line");
            }
            filled += n;
        }

        verifyStatusLine(buffer, filled);

        var last4 = 0;
        var scanPos = 0;
        while (true) {
            while (scanPos < filled) {
                last4 = (last4 << 8) | (buffer[scanPos++] & 0xFF);
                if (last4 == CRLF_CRLF) {
                    if (scanPos != filled) {
                        throw new IOException(
                                "Proxy sent unexpected data after CONNECT response headers");
                    }
                    return;
                }
            }
            if (filled == buffer.length) {
                filled = 0;
                scanPos = 0;
            }
            var n = in.read(buffer, filled, buffer.length - filled);
            if (n < 0) {
                throw new IOException("Unexpected end of stream while reading CONNECT headers");
            }
            filled += n;
        }
    }

    /**
     * Validates that the first bytes of {@code buffer} match the
     * {@code "HTTP/X.X 2NN"} status-line prefix.
     *
     * @param buffer the response buffer
     * @param filled the number of bytes currently in {@code buffer}
     * @throws IOException if the prefix is malformed or the status
     *         code is not {@code 2xx}
     */
    private static void verifyStatusLine(byte[] buffer, int filled) throws IOException {
        if (buffer[0] != 'H' || buffer[1] != 'T' || buffer[2] != 'T' || buffer[3] != 'P' || buffer[4] != '/') {
            throw invalidResponse(buffer, filled);
        }

        var pos = 5;
        if (!isDigit(buffer[pos])) {
            throw invalidResponse(buffer, filled);
        }
        pos++;
        if (buffer[pos] == '.') {
            pos++;
            if (!isDigit(buffer[pos])) {
                throw invalidResponse(buffer, filled);
            }
            pos++;
        }
        if (buffer[pos] != ' ') {
            throw invalidResponse(buffer, filled);
        }
        pos++;

        if (pos + 3 > filled) {
            throw invalidResponse(buffer, filled);
        }
        var d1 = buffer[pos];
        var d2 = buffer[pos + 1];
        var d3 = buffer[pos + 2];
        if (!isDigit(d1) || !isDigit(d2) || !isDigit(d3)) {
            throw invalidResponse(buffer, filled);
        }
        if (d1 != '2') {
            var status = "" + (char) d1 + (char) d2 + (char) d3;
            if (Log.WARNING) LOGGER.log(Level.WARNING, "http proxy refused connect, status={0}", status);
            throw new IOException("Proxy refused CONNECT: " + status);
        }
    }

    /**
     * Returns whether the given byte is an ASCII digit.
     *
     * @param b the byte to test
     * @return {@code true} if {@code b} is between {@code '0'} and
     *         {@code '9'} inclusive
     */
    private static boolean isDigit(byte b) {
        return b >= '0' && b <= '9';
    }

    /**
     * Builds an {@link IOException} carrying the buffered raw
     * response bytes up to the point of detection.
     *
     * <p>Embedding the buffered response in the exception message is the
     * fastest path to diagnosing misbehaving proxies, which often return
     * HTML error pages rather than RFC-compliant status lines.
     *
     * @param buffer the response buffer
     * @param filled the number of bytes currently in {@code buffer}
     * @return a new {@link IOException} suitable for throwing
     */
    private static IOException invalidResponse(byte[] buffer, int filled) {
        if (Log.WARNING) LOGGER.log(Level.WARNING, "http connect tunnel received malformed response, {0} byte(s)", filled);
        return new IOException("Invalid HTTP CONNECT response: "
                + new String(buffer, 0, filled, StandardCharsets.US_ASCII));
    }
}
