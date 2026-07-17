package com.github.auties00.cobalt.socket.tunnel;

import com.github.auties00.cobalt.client.WhatsAppClientProxy;
import com.github.auties00.cobalt.client.WhatsAppClientProxyAuthenticator;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.socket.WhatsAppSslContextFactory;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Establishes an HTTP {@code CONNECT} tunnel through an HTTP or HTTPS proxy on an already-connected
 * {@link Socket}.
 *
 * <p>This is the transport-layer hop used when a WhatsApp connection must traverse a
 * {@link WhatsAppClientProxy.Http} proxy. The two proxy flavours are handled in one entry point:
 * {@link WhatsAppClientProxy.Http.Plain} sends the {@code CONNECT} on the raw socket in cleartext, while
 * {@link WhatsAppClientProxy.Http.Secure} wraps the raw socket in an {@link SSLSocket} obtained from the
 * supplied {@link WhatsAppSslContextFactory}, drives the TLS handshake, and sends the
 * {@code CONNECT} inside the TLS tunnel. When the proxy carries a
 * {@link WhatsAppClientProxyAuthenticator.Http} the matching {@code Proxy-Authorization} header is added
 * to the request. On successful return the socket's input stream is positioned past the response
 * header block, ready for the next protocol layer (typically the end-to-end TLS handshake to the
 * target).
 *
 * @implNote
 * This implementation exists because the JDK offers no usable path for this hop: the
 * {@code Socket(Proxy)} constructor rejects {@link java.net.Proxy.Type#HTTP} (only {@code SOCKS}
 * and {@code DIRECT} are accepted) and {@link java.net.http.HttpClient} cannot speak TLS to a
 * proxy, so the {@code CONNECT} is assembled directly on the raw socket. The response parser is
 * deliberately minimal: it confirms the status line is {@code HTTP/X[.X] 2xx} and scans the byte
 * stream for the {@code CRLF CRLF} header terminator without parsing individual header fields.
 * Header blocks larger than the read buffer are accepted by recycling the buffer in place, and any
 * bytes received after {@code CRLF CRLF} are rejected because the raw socket
 * {@link java.io.InputStream} provides no push-back to the next protocol layer. No {@code 407}
 * retry is attempted; authentication is preemptive when a {@link WhatsAppClientProxyAuthenticator.Http}
 * is supplied and the call fails if the proxy rejects the first attempt. Hosts containing a colon
 * are bracketed as IPv6 literals in both the request-target and the {@code Host} header. On a
 * malformed status line the buffered response is embedded verbatim in the thrown
 * {@link IOException} to ease proxy debugging.
 */
public final class HttpTunnel {

    /**
     * The logger for {@link HttpTunnel}.
     */
    private static final System.Logger LOGGER = Log.get(HttpTunnel.class);

    /**
     * Holds the size in bytes of the reusable read buffer used to drain the {@code CONNECT}
     * response.
     */
    private static final int READ_BUFFER_SIZE = 8192;

    /**
     * Holds the minimum length of an HTTP response status line, namely {@code "HTTP/X.X NNN"}
     * without the trailing {@code CRLF}.
     */
    private static final int STATUS_LINE_MIN_LENGTH = 12;

    /**
     * Holds the packed bytes representing {@code "\r\n\r\n"}.
     *
     * @implNote
     * This implementation packs the four terminator bytes into a single {@code int} so the header
     * block end can be detected with one sliding 32-bit comparison rather than a per-byte state
     * machine.
     */
    private static final int CRLF_CRLF = 0x0D0A0D0A;

    /**
     * Prevents instantiation of this utility holder.
     */
    private HttpTunnel() {

    }

    /**
     * Establishes an HTTP {@code CONNECT} tunnel through {@code proxy} and returns the socket
     * positioned for the target's protocol layer.
     *
     * <p>For {@link WhatsAppClientProxy.Http.Secure} the raw socket is TLS-wrapped first using
     * {@code ssl} (with SNI set to {@code proxy.host()}), the TLS handshake is driven, and the
     * {@code CONNECT} is sent inside the TLS tunnel; the returned socket is the {@link SSLSocket}
     * layer and the underlying raw socket auto-closes when that SSL socket is closed. For
     * {@link WhatsAppClientProxy.Http.Plain} the {@code CONNECT} is sent on the raw socket in cleartext
     * and {@code raw} itself is returned. If TLS wrapping fails the raw socket is closed before the
     * failure propagates.
     *
     * @param raw        the already-connected proxy socket
     * @param targetHost the host the tunnel is meant to reach
     * @param targetPort the port the tunnel is meant to reach
     * @param proxy      the proxy configuration
     * @param ssl        the SSL context factory used to TLS-wrap the proxy hop when {@code proxy}
     *                   is {@link WhatsAppClientProxy.Http.Secure}
     * @return the connected socket; {@code raw} itself for {@link WhatsAppClientProxy.Http.Plain}, or the
     *         {@link SSLSocket} wrapping {@code raw} for {@link WhatsAppClientProxy.Http.Secure}
     * @throws IOException if the TLS handshake, the {@code CONNECT} request, or the response parsing
     *         fails
     */
    public static Socket tunnel(Socket raw, String targetHost, int targetPort,
                                WhatsAppClientProxy.Http proxy, WhatsAppSslContextFactory ssl) throws IOException {
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "opening http connect tunnel to {0}:{1} via {2} proxy",
                    targetHost, targetPort, proxy.getClass().getSimpleName());
        }
        var transport = raw;
        if (proxy instanceof WhatsAppClientProxy.Http.Secure) {
            try {
                var sslSocket = (SSLSocket) ssl.sslContext().getSocketFactory()
                        .createSocket(raw, proxy.host(), proxy.port(), true);
                sslSocket.setSSLParameters(ssl.sslParameters());
                sslSocket.startHandshake();
                transport = sslSocket;
            } catch (IOException e) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "tls handshake with http proxy failed", e);
                }
                try {
                    raw.close();
                } catch (IOException _) {
                }
                throw e;
            }
        }

        sendConnect(transport, targetHost, targetPort, proxy.authenticator().orElse(null));
        readConnectResponse(transport);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "http connect tunnel established to {0}:{1}", targetHost, targetPort);
        }
        return transport;
    }

    /**
     * Writes the {@code CONNECT} request and the optional {@code Proxy-Authorization} header to the
     * socket.
     *
     * <p>The request line and the {@code Host} header both carry the target authority produced by
     * {@link #appendAuthority(StringBuilder, String, int)}. When {@code auth} is non-null its
     * {@link WhatsAppClientProxyAuthenticator.Http#authorization()} value is emitted as a
     * {@code Proxy-Authorization} header. The assembled request is encoded as US-ASCII and flushed.
     *
     * @param socket     the proxy socket
     * @param targetHost the target host
     * @param targetPort the target port
     * @param auth       the authenticator, or {@code null} when the proxy is anonymous
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
     * Appends an HTTP authority of the form {@code host:port} to {@code sb}, bracketing the host if
     * it is an IPv6 literal.
     *
     * <p>An IPv6 literal is detected by the presence of a colon together with the absence of a
     * leading bracket; an already-bracketed literal, a registered name, or an IPv4 literal is
     * appended verbatim. The port is always appended after a colon separator.
     *
     * @param sb   the destination buffer
     * @param host the host, either a registered name, an IPv4 literal, or an unbracketed IPv6
     *             literal
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
     * Reads the {@code CONNECT} response status line, verifies it reports a {@code 2xx} code, and
     * consumes the header block up to and including the {@code CRLF CRLF} terminator.
     *
     * <p>The method first reads at least {@link #STATUS_LINE_MIN_LENGTH} bytes so the status line
     * can be validated, then scans forward for the header terminator. A premature end of stream, a
     * malformed status line, a non-{@code 2xx} code, or any bytes trailing the terminator are all
     * reported as failures.
     *
     * @implNote
     * This implementation slides a 32-bit window through the response bytes looking for
     * {@link #CRLF_CRLF} so the header block can be located without parsing individual header
     * fields. When the header block exceeds {@link #READ_BUFFER_SIZE} the buffer is recycled in
     * place since the only state that must survive a refill lives in the sliding window itself.
     *
     * @param socket the proxy socket
     * @throws IOException if the response is incomplete, malformed, or reports a non-{@code 2xx}
     *         status
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
     * Validates that the first bytes of {@code buffer} match the {@code "HTTP/X.X 2NN"} status-line
     * prefix.
     *
     * <p>The version token, the single space separator, and the three-digit status code are checked
     * in order; any deviation, or a leading digit other than {@code '2'}, is rejected. A code in
     * the {@code 2xx} range is the only accepted outcome.
     *
     * @param buffer the response buffer
     * @param filled the number of bytes currently in {@code buffer}
     * @throws IOException if the prefix is malformed or the status code is not {@code 2xx}
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
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "http proxy refused connect, status={0}", status);
            }
            throw new IOException("Proxy refused CONNECT: " + status);
        }
    }

    /**
     * Returns whether the given byte is an ASCII digit.
     *
     * @param b the byte to test
     * @return {@code true} if {@code b} is between {@code '0'} and {@code '9'} inclusive
     */
    private static boolean isDigit(byte b) {
        return b >= '0' && b <= '9';
    }

    /**
     * Builds an {@link IOException} carrying the buffered raw response bytes up to the point of
     * detection.
     *
     * <p>The buffered response is decoded as US-ASCII and embedded in the exception message so a
     * misbehaving proxy that returns an HTML error page rather than an RFC-compliant status line
     * can be diagnosed from the failure alone.
     *
     * @param buffer the response buffer
     * @param filled the number of bytes currently in {@code buffer}
     * @return a new {@link IOException} suitable for throwing
     */
    private static IOException invalidResponse(byte[] buffer, int filled) {
        return new IOException("Invalid HTTP CONNECT response: "
                + new String(buffer, 0, filled, StandardCharsets.US_ASCII));
    }
}
