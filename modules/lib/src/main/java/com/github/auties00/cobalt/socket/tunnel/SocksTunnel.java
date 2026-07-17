package com.github.auties00.cobalt.socket.tunnel;

import com.github.auties00.cobalt.client.WhatsAppClientProxy;
import com.github.auties00.cobalt.client.WhatsAppClientProxyAuthenticator;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.util.DataUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger.Level;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Establishes a TCP tunnel through a SOCKS proxy on an already-connected {@link Socket}.
 *
 * <p>This is the transport-layer hop used when a WhatsApp connection must traverse a
 * {@link WhatsAppClientProxy.Socks} proxy. It supports SOCKS4 (local DNS, IPv4 only), SOCKS4a (remote DNS
 * via the {@code 0.0.0.x} sentinel IP), SOCKS5 with local DNS resolution (RFC 1928) and SOCKS5h
 * with remote DNS resolution; SOCKS5 authentication uses the RFC 1929 username and password
 * sub-negotiation. The tunnel runs entirely on the supplied socket's
 * {@link InputStream}/{@link OutputStream}, with no intermediate {@link java.nio.ByteBuffer}
 * wrappers and no layer plumbing. On successful return the socket is positioned past the SOCKS
 * reply, ready for the next protocol layer.
 *
 * @implNote
 * This implementation relies on the {@link WhatsAppClientProxy.Socks} variant to decide whether the
 * destination address is resolved locally and sent as raw bytes (V4 or V5 Local) or passed to the
 * proxy as a domain name for remote resolution (V4a or V5h Remote); the sealed-class shape carries
 * that choice rather than a separate flag.
 */
public final class SocksTunnel {

    /**
     * The logger for {@link SocksTunnel}.
     */
    private static final System.Logger LOGGER = Log.get(SocksTunnel.class);

    /**
     * Holds the SOCKS protocol version 4 byte.
     */
    private static final byte SOCKS_VERSION_4 = 0x04;

    /**
     * Holds the SOCKS protocol version 5 byte.
     */
    private static final byte SOCKS_VERSION_5 = 0x05;

    /**
     * Holds the SOCKS {@code CONNECT} command byte.
     */
    private static final byte CMD_CONNECT = 0x01;

    /**
     * Holds the SOCKS5 method byte indicating "no authentication required".
     */
    private static final byte METHOD_NO_AUTH = 0x00;

    /**
     * Holds the SOCKS5 marker indicating that the proxy refused every offered method.
     */
    private static final int METHOD_NO_ACCEPTABLE = 0xFF;

    /**
     * Holds the SOCKS5 address type byte for IPv4.
     */
    private static final byte ADDR_TYPE_IPV4 = 0x01;

    /**
     * Holds the SOCKS5 address type byte for a domain name.
     */
    private static final byte ADDR_TYPE_DOMAIN = 0x03;

    /**
     * Holds the SOCKS5 address type byte for IPv6.
     */
    private static final byte ADDR_TYPE_IPV6 = 0x04;

    /**
     * Holds the RFC 1929 sub-negotiation version 1 byte.
     */
    private static final byte AUTH_VERSION_1 = 0x01;

    /**
     * Holds the RFC 1929 success byte.
     */
    private static final byte AUTH_SUCCESS = 0x00;

    /**
     * Holds the SOCKS5 reply byte for a successful {@code CONNECT}.
     */
    private static final byte SOCKS5_REPLY_SUCCESS = 0x00;

    /**
     * Holds the SOCKS5 reply byte for a general server failure.
     */
    private static final int SOCKS5_REPLY_GENERAL_FAILURE = 0x01;

    /**
     * Holds the SOCKS5 reply byte for "connection not allowed by ruleset".
     */
    private static final int SOCKS5_REPLY_CONNECTION_NOT_ALLOWED = 0x02;

    /**
     * Holds the SOCKS5 reply byte for "network unreachable".
     */
    private static final int SOCKS5_REPLY_NETWORK_UNREACHABLE = 0x03;

    /**
     * Holds the SOCKS5 reply byte for "host unreachable".
     */
    private static final int SOCKS5_REPLY_HOST_UNREACHABLE = 0x04;

    /**
     * Holds the SOCKS5 reply byte for "connection refused".
     */
    private static final int SOCKS5_REPLY_CONNECTION_REFUSED = 0x05;

    /**
     * Holds the SOCKS5 reply byte for "TTL expired".
     */
    private static final int SOCKS5_REPLY_TTL_EXPIRED = 0x06;

    /**
     * Holds the SOCKS5 reply byte for "command not supported".
     */
    private static final int SOCKS5_REPLY_COMMAND_NOT_SUPPORTED = 0x07;

    /**
     * Holds the SOCKS5 reply byte for "address type not supported".
     */
    private static final int SOCKS5_REPLY_ADDRESS_TYPE_UNSUPPORTED = 0x08;

    /**
     * Holds the SOCKS5 reserved byte that always carries a zero value on the wire.
     */
    private static final byte SOCKS5_RESERVED = 0x00;

    /**
     * Holds the SOCKS4 reply version byte, which is always zero per the spec.
     */
    private static final int SOCKS4_REPLY_VERSION = 0x00;

    /**
     * Holds the SOCKS4 status code for "request granted".
     */
    private static final int SOCKS4_REQUEST_GRANTED = 0x5A;

    /**
     * Holds the SOCKS4 status code for "request rejected or failed".
     */
    private static final int SOCKS4_REQUEST_REJECTED = 0x5B;

    /**
     * Holds the SOCKS4 status code for "identd unreachable".
     */
    private static final int SOCKS4_REQUEST_NO_IDENTD = 0x5C;

    /**
     * Holds the SOCKS4 status code for "identd reported a different user id".
     */
    private static final int SOCKS4_REQUEST_IDENTD_MISMATCH = 0x5D;

    /**
     * Holds the null byte used to terminate SOCKS4 user-id and domain fields.
     */
    private static final byte SOCKS4_NULL_TERMINATOR = 0x00;

    /**
     * Holds the sentinel IP {@code 0.0.0.1} that signals to a SOCKS4a proxy that the destination is
     * supplied as a domain name instead of an IPv4 address.
     */
    private static final byte[] SOCKS4A_SENTINEL_IP = {0x00, 0x00, 0x00, 0x01};

    /**
     * Holds the length of an IPv4 address in bytes.
     */
    private static final int IPV4_ADDR_LENGTH = 4;

    /**
     * Holds the length of an IPv6 address in bytes.
     */
    private static final int IPV6_ADDR_LENGTH = 16;

    /**
     * Holds the length of the SOCKS port field in bytes.
     */
    private static final int PORT_LENGTH = 2;

    /**
     * Holds the maximum domain length permitted by SOCKS5.
     */
    private static final int MAX_DOMAIN_LENGTH = 255;

    /**
     * Prevents instantiation of this utility holder.
     */
    private SocksTunnel() {

    }

    /**
     * Establishes a SOCKS tunnel through {@code proxy} on the already-connected socket.
     *
     * <p>Dispatches to the handshake matching the SOCKS variant of {@code proxy} (V4, V4a, V5 or
     * V5h) and returns once the proxy has signalled that the tunnel to
     * {@code targetHost:targetPort} is open. On return the socket streams are ready for the next
     * protocol layer. The same {@code raw} socket is returned to mirror the
     * {@link HttpTunnel#tunnel} signature.
     *
     * @param raw        the already-connected proxy socket
     * @param targetHost the host the tunnel is meant to reach
     * @param targetPort the port the tunnel is meant to reach
     * @param proxy      the SOCKS proxy configuration
     * @return the connected socket, which is the same {@code raw} that was passed in
     * @throws IOException if any phase of the handshake fails or the proxy rejects the request
     */
    public static Socket tunnel(Socket raw, String targetHost, int targetPort,
                                WhatsAppClientProxy.Socks proxy) throws IOException {
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "opening socks tunnel to {0}:{1} via {2}",
                    targetHost, targetPort, proxy.getClass().getSimpleName());
        }
        var in = raw.getInputStream();
        var out = raw.getOutputStream();
        switch (proxy) {
            case WhatsAppClientProxy.Socks.V4.Local v4 ->
                    performSocks4Handshake(in, out, v4, targetHost, targetPort);
            case WhatsAppClientProxy.Socks.V4.Remote v4a ->
                    performSocks4aHandshake(in, out, v4a, targetHost, targetPort);
            case WhatsAppClientProxy.Socks.V5.Local v5 ->
                    performSocks5Handshake(in, out, v5, targetHost, targetPort);
            case WhatsAppClientProxy.Socks.V5.Remote v5h ->
                    performSocks5Handshake(in, out, v5h, targetHost, targetPort);
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "socks tunnel established to {0}:{1}", targetHost, targetPort);
        }
        return raw;
    }

    /**
     * Performs a SOCKS4 handshake by resolving the host locally to an IPv4 address, sending the
     * {@code CONNECT} request with the optional user id, and validating the reply.
     *
     * <p>The host is resolved through {@link InetAddress#getByName(String)} and the request is
     * rejected unless the result is an {@link Inet4Address}, since SOCKS4 carries only IPv4
     * destinations. The optional user id from the {@link WhatsAppClientProxy.Socks.V4} authenticator is
     * ISO 8859-1 encoded and appended before the null terminator.
     *
     * @param in   the proxy input stream
     * @param out  the proxy output stream
     * @param v4   the SOCKS4 proxy configuration
     * @param host the target host
     * @param port the target port
     * @throws IOException if DNS resolution fails, the host is not IPv4, or the proxy rejects the
     *         request
     */
    private static void performSocks4Handshake(InputStream in, OutputStream out,
                                               WhatsAppClientProxy.Socks.V4.Local v4,
                                               String host, int port) throws IOException {
        var address = InetAddress.getByName(host);
        if (!(address instanceof Inet4Address)) {
            throw new IOException("SOCKS4 only supports IPv4 addresses, but resolved " + host + " to " + address.getHostAddress());
        }

        var ipBytes = address.getAddress();
        var userIdBytes = v4.authenticator()
                .map(auth -> auth.userId().getBytes(StandardCharsets.ISO_8859_1))
                .orElse(DataUtils.EMPTY_BYTE_ARRAY);
        var request = new byte[9 + userIdBytes.length];
        var pos = 0;
        request[pos++] = SOCKS_VERSION_4;
        request[pos++] = CMD_CONNECT;
        request[pos++] = (byte) (port >>> 8);
        request[pos++] = (byte) port;
        System.arraycopy(ipBytes, 0, request, pos, 4);
        pos += 4;
        System.arraycopy(userIdBytes, 0, request, pos, userIdBytes.length);
        pos += userIdBytes.length;
        request[pos] = SOCKS4_NULL_TERMINATOR;
        out.write(request);
        out.flush();

        handleSocks4Reply(in);
    }

    /**
     * Performs a SOCKS4a handshake by sending a {@code CONNECT} request with the sentinel IP and
     * the domain name for remote DNS resolution.
     *
     * <p>The request carries {@link #SOCKS4A_SENTINEL_IP} in place of an IPv4 address, instructing
     * the proxy to resolve the trailing null-terminated domain name itself. The optional user id
     * from the {@link WhatsAppClientProxy.Socks.V4} authenticator is ISO 8859-1 encoded and inserted
     * between the sentinel IP and the domain name.
     *
     * @param in   the proxy input stream
     * @param out  the proxy output stream
     * @param v4a  the SOCKS4a proxy configuration
     * @param host the target host, a domain name
     * @param port the target port
     * @throws IOException if the proxy rejects the request
     */
    private static void performSocks4aHandshake(InputStream in, OutputStream out,
                                                WhatsAppClientProxy.Socks.V4.Remote v4a,
                                                String host, int port) throws IOException {
        var userIdBytes = v4a.authenticator()
                .map(auth -> auth.userId().getBytes(StandardCharsets.ISO_8859_1))
                .orElse(DataUtils.EMPTY_BYTE_ARRAY);
        var domainBytes = host.getBytes(StandardCharsets.ISO_8859_1);
        var request = new byte[10 + userIdBytes.length + domainBytes.length];
        var pos = 0;
        request[pos++] = SOCKS_VERSION_4;
        request[pos++] = CMD_CONNECT;
        request[pos++] = (byte) (port >>> 8);
        request[pos++] = (byte) port;
        System.arraycopy(SOCKS4A_SENTINEL_IP, 0, request, pos, SOCKS4A_SENTINEL_IP.length);
        pos += SOCKS4A_SENTINEL_IP.length;
        System.arraycopy(userIdBytes, 0, request, pos, userIdBytes.length);
        pos += userIdBytes.length;
        request[pos++] = SOCKS4_NULL_TERMINATOR;
        System.arraycopy(domainBytes, 0, request, pos, domainBytes.length);
        pos += domainBytes.length;
        request[pos] = SOCKS4_NULL_TERMINATOR;
        out.write(request);
        out.flush();

        handleSocks4Reply(in);
    }

    /**
     * Reads and validates the eight-byte SOCKS4 or SOCKS4a reply.
     *
     * <p>The reply version byte must be {@link #SOCKS4_REPLY_VERSION} and the status byte must be
     * {@link #SOCKS4_REQUEST_GRANTED}; any other status is mapped to a reason string by
     * {@link #getSocks4ReplyReason(int)} and reported as a failure carrying the raw code.
     *
     * @param in the proxy input stream
     * @throws IOException if the reply version is invalid or the request was rejected
     */
    private static void handleSocks4Reply(InputStream in) throws IOException {
        var reply = readNBytes(in, 8);

        var replyVersion = reply[0] & 0xFF;
        if (replyVersion != SOCKS4_REPLY_VERSION) {
            throw new IOException("Invalid SOCKS4 reply version: " + replyVersion);
        }

        var status = reply[1] & 0xFF;
        if (status != SOCKS4_REQUEST_GRANTED) {
            var reason = getSocks4ReplyReason(status);
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "socks4 proxy request failed: {0} (code=0x{1})",
                        reason, Integer.toHexString(status));
            }
            throw new IOException("SOCKS4 proxy request failed: " + reason + " (Code: 0x" + Integer.toHexString(status) + ")");
        }
    }

    /**
     * Maps a SOCKS4 reply status code to a human-readable reason string.
     *
     * <p>Recognised codes are translated to their RFC 1928 descriptions; an unrecognised code
     * yields {@code "Unknown failure"}.
     *
     * @param status the SOCKS4 reply status byte, unsigned
     * @return a description of the failure reason
     */
    private static String getSocks4ReplyReason(int status) {
        return switch (status) {
            case SOCKS4_REQUEST_REJECTED -> "Request rejected or failed";
            case SOCKS4_REQUEST_NO_IDENTD -> "Request rejected because SOCKS server cannot connect to identd on the client";
            case SOCKS4_REQUEST_IDENTD_MISMATCH -> "Request rejected because the client program and identd report different user-ids";
            default -> "Unknown failure";
        };
    }

    /**
     * Performs the full SOCKS5 handshake comprising method negotiation, optional RFC 1929
     * authentication sub-negotiation, and the {@code CONNECT} request.
     *
     * <p>The greeting advertises "no authentication" and, when an authenticator is configured, that
     * authenticator's method id. If the proxy selects a method other than "no authentication" the
     * configured credentials must be present and match the selected method, after which the
     * username and password sub-negotiation runs. The destination is then encoded for the
     * {@code CONNECT} request: for {@link WhatsAppClientProxy.Socks.V5.Local} the host is resolved locally
     * and sent as an IPv4 or IPv6 address, while for {@link WhatsAppClientProxy.Socks.V5.Remote} the
     * domain name is sent for remote resolution by the proxy.
     *
     * @param in    the proxy input stream
     * @param out   the proxy output stream
     * @param socks the SOCKS5 proxy configuration
     * @param host  the target host
     * @param port  the target port
     * @throws IOException if negotiation, authentication, or the {@code CONNECT} request fails
     */
    private static void performSocks5Handshake(InputStream in, OutputStream out,
                                               WhatsAppClientProxy.Socks.V5 socks,
                                               String host, int port) throws IOException {
        var authenticatorOpt = socks.authenticator();
        var greeting = authenticatorOpt.isPresent()
                ? new byte[]{SOCKS_VERSION_5, 2, METHOD_NO_AUTH, (byte) authenticatorOpt.get().methodId()}
                : new byte[]{SOCKS_VERSION_5, 1, METHOD_NO_AUTH};
        out.write(greeting);
        out.flush();

        var serverChoice = readNBytes(in, 2);
        var version = serverChoice[0];
        if (version != SOCKS_VERSION_5) {
            throw new IOException("Unsupported socks version: " + (version & 0xFF));
        }

        var chosenMethod = serverChoice[1] & 0xFF;
        if (chosenMethod == METHOD_NO_ACCEPTABLE) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "socks5 proxy rejected all offered authentication methods");
            }
            throw new IOException("SOCKS5 proxy rejected all offered authentication methods");
        }
        if (chosenMethod != METHOD_NO_AUTH) {
            if (authenticatorOpt.isEmpty()) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "socks5 proxy requires authentication but no credentials configured");
                }
                throw new IOException("Missing credentials for authentication: please provide them in the proxy configuration");
            }
            var authenticator = authenticatorOpt.get();
            if (authenticator.methodId() != chosenMethod) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "socks5 proxy selected unsupported authentication method {0}", chosenMethod);
                }
                throw new IOException("Proxy selected unsupported authentication method: " + chosenMethod);
            }
            switch (authenticator) {
                case WhatsAppClientProxyAuthenticator.Socks.V5.UserPassword credentials -> {
                    var userBytes = credentials.username().getBytes(StandardCharsets.ISO_8859_1);
                    var passBytes = credentials.password().getBytes(StandardCharsets.ISO_8859_1);

                    var authRequest = new byte[3 + userBytes.length + passBytes.length];
                    var pos = 0;
                    authRequest[pos++] = AUTH_VERSION_1;
                    authRequest[pos++] = (byte) userBytes.length;
                    System.arraycopy(userBytes, 0, authRequest, pos, userBytes.length);
                    pos += userBytes.length;
                    authRequest[pos++] = (byte) passBytes.length;
                    System.arraycopy(passBytes, 0, authRequest, pos, passBytes.length);
                    out.write(authRequest);
                    out.flush();

                    var authResponse = readNBytes(in, 2);
                    var authVersion = authResponse[0] & 0xFF;
                    if (authVersion != AUTH_VERSION_1) {
                        throw new IOException("Unsupported SOCKS5 auth sub-negotiation version: " + authVersion);
                    }
                    if (authResponse[1] != AUTH_SUCCESS) {
                        if (Log.WARNING) {
                            LOGGER.log(Level.WARNING, "socks5 proxy authentication failed");
                        }
                        throw new IOException("SOCKS proxy authentication failed.");
                    }
                }
            }
        }

        var connRequest = switch (socks) {
            case WhatsAppClientProxy.Socks.V5.Local _ -> buildResolvedConnectRequest(host, port);
            case WhatsAppClientProxy.Socks.V5.Remote _ -> buildDomainConnectRequest(host, port);
        };
        out.write(connRequest);
        out.flush();

        handleSocks5Reply(in);
    }

    /**
     * Builds a SOCKS5 {@code CONNECT} request that carries the destination as a domain name for
     * remote DNS resolution.
     *
     * <p>The domain name is ISO 8859-1 encoded, length-prefixed, and tagged with
     * {@link #ADDR_TYPE_DOMAIN}; names longer than {@link #MAX_DOMAIN_LENGTH} bytes cannot be
     * represented and are rejected.
     *
     * @param host the target host, a domain name
     * @param port the target port
     * @return the serialized {@code CONNECT} request, ready to send
     * @throws IOException if the domain name exceeds {@value #MAX_DOMAIN_LENGTH} bytes
     */
    private static byte[] buildDomainConnectRequest(String host, int port) throws IOException {
        var destHostBytes = host.getBytes(StandardCharsets.ISO_8859_1);
        if (destHostBytes.length > MAX_DOMAIN_LENGTH) {
            throw new IOException("SOCKS5 domain name too long: " + destHostBytes.length + " bytes (max " + MAX_DOMAIN_LENGTH + ")");
        }
        var request = new byte[4 + 1 + destHostBytes.length + 2];
        var pos = 0;
        request[pos++] = SOCKS_VERSION_5;
        request[pos++] = CMD_CONNECT;
        request[pos++] = SOCKS5_RESERVED;
        request[pos++] = ADDR_TYPE_DOMAIN;
        request[pos++] = (byte) destHostBytes.length;
        System.arraycopy(destHostBytes, 0, request, pos, destHostBytes.length);
        pos += destHostBytes.length;
        request[pos++] = (byte) (port >>> 8);
        request[pos] = (byte) port;
        return request;
    }

    /**
     * Builds a SOCKS5 {@code CONNECT} request that carries the destination as a locally-resolved IP
     * address.
     *
     * <p>The host is resolved through {@link InetAddress#getByName(String)} and tagged with
     * {@link #ADDR_TYPE_IPV4} or {@link #ADDR_TYPE_IPV6} according to the resolved address family;
     * any other address class is unsupported and rejected.
     *
     * @param host the target host, resolved locally
     * @param port the target port
     * @return the serialized {@code CONNECT} request, ready to send
     * @throws IOException if DNS resolution fails or yields an unsupported address type
     */
    private static byte[] buildResolvedConnectRequest(String host, int port) throws IOException {
        var address = InetAddress.getByName(host);
        var ipBytes = address.getAddress();
        var addrType = switch (address) {
            case Inet4Address _ -> ADDR_TYPE_IPV4;
            case Inet6Address _ -> ADDR_TYPE_IPV6;
            default -> throw new IOException("Unsupported address type: " + address.getClass().getName());
        };
        var request = new byte[4 + ipBytes.length + 2];
        var pos = 0;
        request[pos++] = SOCKS_VERSION_5;
        request[pos++] = CMD_CONNECT;
        request[pos++] = SOCKS5_RESERVED;
        request[pos++] = addrType;
        System.arraycopy(ipBytes, 0, request, pos, ipBytes.length);
        pos += ipBytes.length;
        request[pos++] = (byte) (port >>> 8);
        request[pos] = (byte) port;
        return request;
    }

    /**
     * Reads and validates the SOCKS5 reply, then drains the variable-length bound address and port
     * fields.
     *
     * <p>The reply version must be {@link #SOCKS_VERSION_5} and the reply code must be
     * {@link #SOCKS5_REPLY_SUCCESS}; any other code is mapped to a reason string by
     * {@link #getSocks5ReplyReason(int)} and reported as a failure. The trailing bound address is
     * consumed according to its address type byte (IPv4, IPv6, or a length-prefixed domain) so the
     * stream is left positioned for the next protocol layer.
     *
     * @param in the proxy input stream
     * @throws IOException if the reply version is invalid or the request was rejected
     */
    private static void handleSocks5Reply(InputStream in) throws IOException {
        var replyInfo = readNBytes(in, 2);

        if (replyInfo[0] != SOCKS_VERSION_5) {
            throw new IOException("Invalid SOCKS version in server reply.");
        }
        var replyCode = replyInfo[1];
        if (replyCode != SOCKS5_REPLY_SUCCESS) {
            var reason = getSocks5ReplyReason(replyCode);
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "socks5 proxy request failed: {0} (code={1})", reason, (int) replyCode);
            }
            throw new IOException("SOCKS proxy request failed: " + reason + " (Code: " + replyCode + ")");
        }

        var addrInfo = readNBytes(in, 2);

        var addrType = addrInfo[1];
        int remainingBytes;
        switch (addrType) {
            case ADDR_TYPE_IPV4 -> remainingBytes = IPV4_ADDR_LENGTH + PORT_LENGTH;
            case ADDR_TYPE_DOMAIN -> {
                var lenBuf = readNBytes(in, 1);
                remainingBytes = (lenBuf[0] & 0xFF) + PORT_LENGTH;
            }
            case ADDR_TYPE_IPV6 -> remainingBytes = IPV6_ADDR_LENGTH + PORT_LENGTH;
            default -> throw new IOException("Proxy returned an unsupported address type in reply: " + addrType);
        }
        readNBytes(in, remainingBytes);
    }

    /**
     * Maps a SOCKS5 reply code to a human-readable reason string.
     *
     * <p>Recognised codes are translated to their RFC 1928 descriptions; an unrecognised code
     * yields {@code "Unknown failure"}.
     *
     * @param replyCode the SOCKS5 reply code byte, unsigned
     * @return a description of the failure reason
     */
    private static String getSocks5ReplyReason(int replyCode) {
        return switch (replyCode) {
            case SOCKS5_REPLY_GENERAL_FAILURE -> "General SOCKS server failure";
            case SOCKS5_REPLY_CONNECTION_NOT_ALLOWED -> "Connection not allowed by ruleset";
            case SOCKS5_REPLY_NETWORK_UNREACHABLE -> "Network unreachable";
            case SOCKS5_REPLY_HOST_UNREACHABLE -> "Host unreachable";
            case SOCKS5_REPLY_CONNECTION_REFUSED -> "Connection refused";
            case SOCKS5_REPLY_TTL_EXPIRED -> "TTL expired";
            case SOCKS5_REPLY_COMMAND_NOT_SUPPORTED -> "Command not supported";
            case SOCKS5_REPLY_ADDRESS_TYPE_UNSUPPORTED -> "Address type not supported";
            default -> "Unknown failure";
        };
    }

    /**
     * Reads exactly {@code n} bytes from {@code in} and returns them as a fresh array.
     *
     * <p>Delegates to {@link InputStream#readNBytes(int)} and treats a short read, which signals an
     * early end of stream, as a handshake failure.
     *
     * @param in the input stream
     * @param n  the exact number of bytes to read
     * @return a fresh {@code byte[n]} holding the read bytes
     * @throws IOException if the stream ends before {@code n} bytes have been read
     */
    private static byte[] readNBytes(InputStream in, int n) throws IOException {
        var buffer = in.readNBytes(n);
        if (buffer.length < n) {
            throw new IOException("Unexpected end of stream during SOCKS handshake");
        }
        return buffer;
    }
}
