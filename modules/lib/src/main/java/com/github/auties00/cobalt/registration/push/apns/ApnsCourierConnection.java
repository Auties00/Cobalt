package com.github.auties00.cobalt.registration.push.apns;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.registration.push.apns.courier.ApnsBag;
import com.github.auties00.cobalt.registration.push.apns.courier.ApnsCourierCrypto;
import com.github.auties00.cobalt.registration.push.apns.courier.ApnsPacket;
import com.github.auties00.cobalt.registration.push.apns.courier.ApnsPayloadTag;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * Owns the long-lived APNS courier TLS stream the {@link ApnsClient} holds open after activation
 * completes.
 *
 * <p>Responsibilities, in start-up order:
 * <ul>
 *   <li>fetch the courier bag and pick a replica via {@link ApnsBag};</li>
 *   <li>negotiate ALPN {@code apns-security-v3} on a TLSv1.3 socket and run the {@code CONNECT} /
 *       {@code READY} / {@code STATE} / {@code FILTER} handshake;</li>
 *   <li>pump incoming frames on a virtual-thread read loop;</li>
 *   <li>send {@link ApnsPayloadTag#KEEP_ALIVE_SEND} every five seconds;</li>
 *   <li>ack delivered notifications and surface {@code regcode} payloads to
 *       {@link ApnsPushCode};</li>
 *   <li>multiplex in-flight request/response exchanges over the single socket via
 *       {@link #exchange}.</li>
 * </ul>
 *
 * <p>Two virtual threads run at steady state: the read pump (started once the TLS handshake
 * completes) and the keep-alive sender (started once the courier {@code READY} arrives). All
 * outbound writes route through {@link #writeLock} so frames cannot interleave on the wire.
 */
final class ApnsCourierConnection {
    /**
     * The logger for {@link ApnsCourierConnection}.
     */
    private static final System.Logger LOGGER = Log.get(ApnsCourierConnection.class);

    /**
     * Holds the URL of the courier bag.
     *
     * <p>The bag is the JSON-in-plist directory listing Apple uses to advertise the active courier
     * replicas. HTTP, not HTTPS; the file is unauthenticated.
     */
    private static final String BAG_URL = "http://init-p01st.push.apple.com/bag";

    /**
     * Holds the standard HTTPS port the courier replicas listen on.
     */
    private static final int APNS_PORT = 443;

    /**
     * Holds the keep-alive cadence in seconds.
     *
     * @implNote This implementation uses five seconds to match the value the native {@code apsd} uses
     *           to keep middlebox NAT entries alive on cellular networks; a higher value risks the
     *           courier dropping the connection silently.
     */
    private static final long KEEP_ALIVE_INTERVAL_SECONDS = 5L;

    /**
     * Holds the wall-clock timeout in milliseconds applied to every {@link #exchange} caller.
     *
     * @implNote This implementation picks 30 seconds, lower than the keep-alive cadence multiplied by
     *           a few iterations, so a stalled request fails before the next keep-alive masks the
     *           underlying problem.
     */
    private static final long REQUEST_TIMEOUT_MS = 30_000L;

    /**
     * Holds the connect and request timeout applied to the bag HTTP call.
     */
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Holds the trust-everything {@link X509TrustManager} used for the courier socket.
     *
     * @implNote This implementation skips host PKI verification because the APNS protocol
     *           authenticates the device with its FairPlay-signed certificate in-band rather than via
     *           the TLS PKI chain, and the courier endpoint is a moving target ({@code 1-courier...}
     *           to {@code 50-courier...}) so a single pinned certificate is not viable. This matches
     *           the trade-off Apple's own {@code apsd} makes; the protocol is still authenticated by
     *           the ALPN string {@code apns-security-v3} and the FairPlay nonce signature on
     *           {@link ApnsPayloadTag#CONNECT}.
     */
    private static final TrustManager[] TRUST_ALL = {
            new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }
            }
    };

    /**
     * Holds the field id that, in a {@link ApnsPayloadTag#READY}, carries the auth token the courier
     * has assigned to this connection.
     */
    private static final int FIELD_AUTH_TOKEN_READY = 0x03;

    /**
     * Holds the field id that, in a {@link ApnsPayloadTag#READY}, carries a single status byte
     * ({@code 0x00} for success).
     */
    private static final int FIELD_STATUS = 0x01;

    /**
     * Holds the field id that, in a {@link ApnsPayloadTag#NOTIFICATION}, carries the server-assigned
     * id the client must echo back in the matching {@link ApnsPayloadTag#ACK}.
     */
    private static final int FIELD_NOTIFICATION_ID = 0x04;

    /**
     * Holds the field id that, in a {@link ApnsPayloadTag#NOTIFICATION}, carries the application
     * payload (typically a JSON object).
     */
    private static final int FIELD_PAYLOAD = 0x03;

    /**
     * Holds the field id that, in a {@link ApnsPayloadTag#TOKEN_RESPONSE}, carries the push token
     * bytes.
     */
    private static final int FIELD_TOKEN = 0x02;

    /**
     * Holds the field id that, in a {@link ApnsPayloadTag#ACK}, carries a single status byte.
     */
    private static final int FIELD_ACK_STATUS = 0x08;

    /**
     * Holds the field id that, in a {@link ApnsPayloadTag#GET_TOKEN}, carries the connection auth
     * token.
     *
     * <p>Distinct from {@link #FIELD_AUTH_TOKEN_READY}, which sits at field id {@code 0x03} in the
     * {@link ApnsPayloadTag#READY} response; field ids are scoped to the enclosing tag.
     */
    private static final int FIELD_GET_TOKEN_AUTH = 0x01;

    /**
     * Holds the field id that, in a {@link ApnsPayloadTag#GET_TOKEN}, carries the SHA-1 hash of the
     * bundle id whose token is being requested.
     */
    private static final int FIELD_GET_TOKEN_TOPIC = 0x02;

    /**
     * Holds the field id that, in a {@link ApnsPayloadTag#GET_TOKEN}, carries a two-byte zero suffix
     * the apsd protocol expects.
     */
    private static final int FIELD_GET_TOKEN_PADDING = 0x03;

    /**
     * Holds the field id that, in a {@link ApnsPayloadTag#TOKEN_RESPONSE}, echoes the topic hash back
     * so callers can correlate the response against their original request.
     */
    private static final int FIELD_TOKEN_RESPONSE_TOPIC = 0x03;

    /**
     * Holds the session whose keypair, device certificate and topic list drive the courier handshake.
     */
    private final ApnsSession session;

    /**
     * Holds the sink for verification codes extracted from incoming {@link ApnsPayloadTag#NOTIFICATION}
     * payloads.
     */
    private final ApnsPushCode pushCode;

    /**
     * Holds the HTTP client used for the single bag fetch.
     *
     * <p>Built once with the configured proxy and reused if {@link #start} is called more than once
     * across reconnects.
     */
    private final HttpClient http;

    /**
     * Holds the source of randomness used to pick a courier replica index and seed the SSL context.
     */
    private final SecureRandom random;

    /**
     * Holds the map of pending {@link #exchange} requests awaiting a matching response frame.
     *
     * <p>Keyed by sequential id so the read pump's iteration order is deterministic.
     *
     * @implNote This implementation uses a {@link ConcurrentHashMap} rather than a list so concurrent
     *           registrations do not require snapshot iteration.
     */
    private final Map<Long, Pending> pending;

    /**
     * Holds the monotonically increasing id generator for {@link #pending} keys.
     *
     * @implNote This implementation uses an {@link AtomicLong} to sidestep the fragile
     *           {@code threadHash << 32 | random} synthetic id approach used before this refactor,
     *           which could collide under thread reuse.
     */
    private final AtomicLong nextPendingId;

    /**
     * Holds the lock that serialises every outbound write on the courier socket.
     *
     * <p>Three potential concurrent writers, the read pump's ack path, public-API threads driving
     * {@link #requestToken}, and the keep-alive thread, need this so their frames cannot interleave on
     * the wire.
     */
    private final Object writeLock;

    /**
     * Holds the currently-attached TLS socket.
     *
     * <p>{@code null} between {@link #start} and the first successful handshake.
     *
     * @implNote This implementation marks the field {@code volatile} so {@link #close} can read a
     *           fresh value from any thread.
     */
    private volatile SSLSocket socket;

    /**
     * Holds the cached output stream of {@link #socket}.
     *
     * <p>Cached so the write path does not hit the {@code SSLSocket.getOutputStream} guard on every
     * frame.
     */
    private volatile OutputStream socketOut;

    /**
     * Holds the read pump virtual thread.
     *
     * <p>Started after the TLS handshake.
     *
     * @implNote This implementation holds the reference {@code volatile} so {@link #close} can
     *           interrupt it from any thread.
     */
    private volatile Thread readPumpThread;

    /**
     * Holds the keep-alive virtual thread.
     *
     * <p>Started after the courier {@code READY} arrives.
     *
     * @implNote This implementation holds the reference {@code volatile} for the same reason as
     *           {@link #readPumpThread}.
     */
    private volatile Thread keepAliveThread;

    /**
     * Holds the auth token assigned by the courier in {@link ApnsPayloadTag#READY}.
     *
     * <p>Echoed back in every subsequent {@link ApnsPayloadTag#FILTER}, {@link ApnsPayloadTag#GET_TOKEN}
     * and {@link ApnsPayloadTag#ACK} so the courier can attribute the request to the right session.
     */
    private volatile byte[] authToken;

    /**
     * Holds the stop flag flipped by {@link #close}.
     *
     * <p>Read by the read pump and the keep-alive loop so they can exit promptly.
     */
    private volatile boolean stopped;

    /**
     * Constructs a courier connection bound to a session and a push-code sink.
     *
     * <p>Does not open a socket; the caller must invoke {@link #start} after construction.
     *
     * @param session  the session that supplies the keypair, device certificate and topic list
     * @param pushCode the holder where regcodes from incoming notifications are delivered
     * @param proxy    proxy URI used for the bag HTTP fetch, or {@code null} for direct
     */
    ApnsCourierConnection(ApnsSession session, ApnsPushCode pushCode, URI proxy) {
        this.session = session;
        this.pushCode = pushCode;
        this.http = newHttpClient(proxy);
        this.random = new SecureRandom();
        this.pending = new ConcurrentHashMap<>();
        this.nextPendingId = new AtomicLong();
        this.writeLock = new Object();
    }

    /**
     * Fetches the bag, opens the TLS socket, runs the courier handshake and spawns the steady-state
     * threads.
     *
     * <p>Blocks until the handshake completes; throws on any step that fails. After a successful
     * return the read pump and the keep-alive thread are running and the connection is ready to
     * service {@link #requestToken} calls.
     *
     * @implNote This implementation negotiates TLSv1.3 with ALPN {@code apns-security-v3}, sends the
     *           {@link ApnsPayloadTag#CONNECT} packet carrying the device certificate plus the
     *           FairPlay nonce signature, validates the status byte of the matching
     *           {@link ApnsPayloadTag#READY}, captures the auth token, then sends the
     *           {@link ApnsPayloadTag#STATE} and topic-list {@link ApnsPayloadTag#FILTER} packets
     *           before starting the keep-alive thread.
     *
     * @throws IOException on any HTTP, TLS, or protocol failure
     */
    void start() throws IOException {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "apns bag fetch starting");
        var bag = fetchBag();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "apns courier handshake -> {0}, hosts={1}", bag.hostname(), bag.hostCount());

        var courierIndex = 1 + random.nextInt(Math.max(1, bag.hostCount() - 1));
        var host = courierIndex + "-" + bag.hostname();
        try {
            var sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(null, TRUST_ALL, random);
            var sock = (SSLSocket) sslContext.getSocketFactory().createSocket(host, APNS_PORT);
            sock.setTcpNoDelay(true);
            sock.setKeepAlive(true);
            var params = sock.getSSLParameters();
            params.setApplicationProtocols(new String[]{"apns-security-v3"});
            sock.setSSLParameters(params);
            sock.startHandshake();
            this.socket = sock;
            this.socketOut = sock.getOutputStream();
        } catch (GeneralSecurityException e) {
            throw new IOException("APNS TLS setup failed", e);
        }

        startReadPump(socket.getInputStream());

        var keyPair = ApnsCourierCrypto.restoreKeyPair(session.publicKeyDer(), session.privateKeyDer());
        var nonce = ApnsCourierCrypto.createNonce(random);
        var signature = ApnsCourierCrypto.signNonce(keyPair, nonce);
        byte[] certDer;
        try {
            certDer = ApnsCourierCrypto.reencodeDeviceCertificate(session.deviceCertificate());
        } catch (Exception e) {
            throw new IOException("device certificate is invalid", e);
        }
        var ready = exchange(
                ApnsPayloadTag.CONNECT,
                Map.of(
                        0x02, new byte[]{0x01},
                        0x05, new byte[]{0x00, 0x00, 0x00, 0x41},
                        0x0C, certDer,
                        0x0D, nonce,
                        0x0E, signature),
                p -> p.tag() == ApnsPayloadTag.READY);

        var statusBytes = ready.fields().get(FIELD_STATUS);
        if (statusBytes == null || statusBytes.length == 0 || statusBytes[0] != 0) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "apns courier rejected connect, status={0}",
                        statusBytes == null ? -1 : Byte.toUnsignedInt(statusBytes[0]));
            }
            throw new IOException("APNS courier rejected CONNECT: status="
                    + (statusBytes == null ? "null" : Byte.toUnsignedInt(statusBytes[0])));
        }
        var token = ready.fields().get(FIELD_AUTH_TOKEN_READY);
        if (token == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "apns ready missing auth token");
            throw new IOException("APNS READY missing auth token");
        }
        this.authToken = token;
        if (Log.INFO) LOGGER.log(Level.INFO, "apns courier authenticated");

        send(ApnsPayloadTag.STATE, Map.of(
                0x01, new byte[]{0x01},
                0x02, new byte[]{0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}));
        sendFilter();
        startKeepAlive();
    }

    /**
     * Sends a {@link ApnsPayloadTag#GET_TOKEN} for a topic and returns the hex-encoded push token.
     *
     * <p>Called by {@link ApnsClient#getPushToken()} for the first topic of the bound configuration;
     * blocks on the matching {@link ApnsPayloadTag#TOKEN_RESPONSE} until either it arrives, the
     * request times out, or the connection is torn down.
     *
     * @implNote This implementation correlates the response by matching both the response tag and the
     *           echoed topic hash; if a second concurrent caller requests a different topic the read
     *           pump still routes each response to its matching pending entry.
     *
     * @param topic the bundle id whose token to fetch
     * @return the hex-encoded push token
     * @throws IOException if the courier connection is not ready, the send fails, the response times
     *                     out, or the response omits the token field
     */
    String requestToken(String topic) throws IOException {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "apns push token requested for topic {0}", topic);
        var topicHash = ApnsCourierCrypto.sha1(topic);
        var packet = exchange(
                ApnsPayloadTag.GET_TOKEN,
                Map.of(
                        FIELD_GET_TOKEN_AUTH, authToken,
                        FIELD_GET_TOKEN_TOPIC, topicHash,
                        FIELD_GET_TOKEN_PADDING, new byte[]{0x00, 0x00}),
                p -> p.tag() == ApnsPayloadTag.TOKEN_RESPONSE
                        && Arrays.equals(p.fields().get(FIELD_TOKEN_RESPONSE_TOPIC), topicHash));
        var bytes = packet.fields().get(FIELD_TOKEN);
        if (bytes == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "apns token response missing token field for topic {0}", topic);
            throw new IOException("TOKEN_RESPONSE missing token field");
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "apns push token received, len={0}", bytes.length);
        return HexFormat.of().formatHex(bytes);
    }

    /**
     * Tears down the read pump, the keep-alive thread, and the TLS socket.
     *
     * <p>Fails every still-pending request with an {@link IOException} so blocked callers unblock with
     * a useful error rather than waiting forever. Idempotent.
     */
    void close() {
        if (stopped) {
            return;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "apns courier closing");
        stopped = true;
        var s = socket;
        if (s != null) {
            try {
                s.close();
            } catch (IOException _) {
            }
        }
        var pump = readPumpThread;
        if (pump != null) {
            pump.interrupt();
        }
        var hb = keepAliveThread;
        if (hb != null) {
            hb.interrupt();
        }
        var disconnect = new IOException("APNS client closed");
        for (var p : pending.values()) {
            p.fail(disconnect);
        }
        pending.clear();
    }

    /**
     * Performs the bag GET and parses the resulting plist into the structured {@link ApnsBag} record.
     *
     * <p>Called once at the start of {@link #start}; the bag tells Cobalt which courier hostname
     * suffix to dial and how many replicas to pick a random index from.
     *
     * @return the parsed bag
     * @throws IOException on transport failure or malformed plist
     */
    private ApnsBag fetchBag() throws IOException {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(BAG_URL))
                .timeout(HTTP_TIMEOUT)
                .GET()
                .build();
        return ApnsBag.ofPlist(sendBytes(request));
    }

    /**
     * Sends the topic-subscription {@link ApnsPayloadTag#FILTER} packet so the courier delivers only
     * pushes for the configured bundle ids.
     *
     * <p>Called once at the end of {@link #start}; the topic list is sourced from
     * {@code session.config().topics()} and hashed through {@link ApnsCourierCrypto#sha1(String)}
     * before being sent.
     *
     * @throws IOException if the write fails
     */
    private void sendFilter() throws IOException {
        var topics = session.config().topics();
        var hashes = new byte[topics.size()][];
        for (var i = 0; i < topics.size(); i++) {
            hashes[i] = ApnsCourierCrypto.sha1(topics.get(i));
        }
        send(ApnsPayloadTag.FILTER, Map.of(
                0x01, authToken,
                0x02, hashes));
    }

    /**
     * Spawns the read pump virtual thread.
     *
     * <p>Called once during {@link #start} after the TLS handshake; the thread terminates when
     * {@link #close} flips the stop flag or when the socket dies.
     *
     * @param in the courier socket input stream
     */
    private void startReadPump(InputStream in) {
        readPumpThread = Thread.startVirtualThread(() -> runReadPump(in));
    }

    /**
     * Runs the read pump body.
     *
     * <p>Decodes one frame at a time and dispatches it via {@link #dispatchPacket}; on any I/O error
     * fails every still-pending request unless the connection is closing.
     *
     * @implNote This implementation distinguishes the close path (no logging) from the
     *           unexpected-failure path (warning log plus fail-all-pending broadcast) by inspecting
     *           the {@link #stopped} flag before reacting.
     *
     * @param in the courier socket input stream
     */
    private void runReadPump(InputStream in) {
        try {
            while (!stopped) {
                var packet = readPacket(in);
                dispatchPacket(packet);
            }
        } catch (IOException e) {
            if (!stopped) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "apns read pump terminated", e);
                var disconnect = new IOException("APNS connection lost", e);
                for (var p : pending.values()) {
                    p.fail(disconnect);
                }
            }
        }
    }

    /**
     * Routes one decoded packet.
     *
     * <p>Notifications are auto-acked and any embedded {@code regcode} is delivered to
     * {@link #pushCode}; the packet is then offered to every pending request in registration order and
     * the first whose filter matches consumes it.
     *
     * @param packet the just-decoded packet
     */
    private void dispatchPacket(ApnsPacket packet) {
        if (Log.TRACE) LOGGER.log(Level.TRACE, "apns packet received, tag={0}", packet.tag());
        if (packet.tag() == ApnsPayloadTag.NOTIFICATION) {
            sendAckSafe(packet.fields().get(FIELD_NOTIFICATION_ID));
            deliverPushCode(packet);
        }
        for (var entry : pending.entrySet()) {
            var p = entry.getValue();
            if (p.filter.test(packet)) {
                pending.remove(entry.getKey());
                p.deliver(packet);
                return;
            }
        }
    }

    /**
     * Extracts the {@code regcode} string from a notification payload and hands it to
     * {@link #pushCode}.
     *
     * <p>Silently ignores notifications without a payload, with a non-JSON payload, or without a
     * {@code regcode} field; those are not registration codes and are still acked by the caller for
     * protocol correctness.
     *
     * @param packet the notification packet
     */
    private void deliverPushCode(ApnsPacket packet) {
        var payload = packet.fields().get(FIELD_PAYLOAD);
        if (payload == null) {
            return;
        }
        try {
            var json = JSON.parseObject(payload);
            if (json != null) {
                var regcode = json.getString("regcode");
                if (Log.DEBUG && regcode != null) {
                    LOGGER.log(Level.DEBUG, "apns push code received {0}", new LogRedactable.Code(regcode));
                }
                pushCode.deliver(regcode);
            }
        } catch (Exception e) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "apns notification payload not json or missing regcode", e);
        }
    }

    /**
     * Sends an {@link ApnsPayloadTag#ACK} for the given notification id.
     *
     * <p>Logs and swallows any I/O failure so the read pump can continue handling subsequent frames;
     * ignores a {@code null} notification id because the source notification did not carry one.
     *
     * @param notificationId the {@link #FIELD_NOTIFICATION_ID} bytes of the notification being acked,
     *                       or {@code null} when absent
     */
    private void sendAckSafe(byte[] notificationId) {
        if (notificationId == null) {
            return;
        }
        try {
            send(ApnsPayloadTag.ACK, Map.of(
                    0x01, authToken,
                    FIELD_NOTIFICATION_ID, notificationId,
                    FIELD_ACK_STATUS, new byte[]{0x00}));
        } catch (IOException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "apns ack failed", e);
        }
    }

    /**
     * Sends a packet and blocks on the first incoming packet whose filter matches.
     *
     * <p>The single request/response correlation primitive used by {@link #start} and
     * {@link #requestToken}; the filter determines which inbound packet completes the wait.
     *
     * @implNote This implementation registers the pending entry before sending so a fast response that
     *           lands between the {@code put} and the {@code await} is still captured.
     *
     * @param tag    the tag of the outbound packet
     * @param fields the TLV fields of the outbound packet (values are {@code byte[]} or
     *               {@code byte[][]})
     * @param filter the predicate used to identify the matching response
     * @return the matched response packet
     * @throws IOException on send failure, response timeout, or if the connection is torn down before
     *                     the response arrives
     */
    private ApnsPacket exchange(ApnsPayloadTag tag, Map<Integer, ?> fields,
                                Predicate<ApnsPacket> filter) throws IOException {
        var pendingId = nextPendingId.getAndIncrement();
        var entry = new Pending(filter);
        this.pending.put(pendingId, entry);
        try {
            send(tag, fields);
            return entry.await(REQUEST_TIMEOUT_MS);
        } catch (IOException | RuntimeException e) {
            this.pending.remove(pendingId);
            throw e;
        }
    }

    /**
     * Spawns the keep-alive virtual thread.
     *
     * <p>The thread emits a {@link ApnsPayloadTag#KEEP_ALIVE_SEND} every
     * {@link #KEEP_ALIVE_INTERVAL_SECONDS} seconds; quietly returns on socket death because the read
     * pump will surface the underlying error.
     */
    private void startKeepAlive() {
        keepAliveThread = Thread.startVirtualThread(() -> {
            while (!stopped) {
                try {
                    Thread.sleep(Duration.ofSeconds(KEEP_ALIVE_INTERVAL_SECONDS));
                    send(ApnsPayloadTag.KEEP_ALIVE_SEND, Map.of());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (IOException e) {
                    return;
                }
            }
        });
    }

    /**
     * Encodes and writes one APNS frame.
     *
     * <p>The frame layout is one tag byte, four big-endian length bytes, then the TLV-encoded payload.
     * The write is performed under {@link #writeLock} so concurrent senders cannot interleave frames
     * on the wire.
     *
     * @param tag    the frame tag
     * @param fields the TLV fields (values are {@code byte[]} or {@code byte[][]})
     * @throws IOException if the socket is not connected or the write fails
     */
    private void send(ApnsPayloadTag tag, Map<Integer, ?> fields) throws IOException {
        var out = socketOut;
        if (out == null) {
            throw new IOException("APNS socket not connected");
        }
        var payload = encodePayload(fields);
        var frame = new byte[1 + 4 + payload.length];
        frame[0] = (byte) tag.value();
        frame[1] = (byte) ((payload.length >>> 24) & 0xFF);
        frame[2] = (byte) ((payload.length >>> 16) & 0xFF);
        frame[3] = (byte) ((payload.length >>> 8) & 0xFF);
        frame[4] = (byte) (payload.length & 0xFF);
        System.arraycopy(payload, 0, frame, 5, payload.length);
        synchronized (writeLock) {
            out.write(frame);
            out.flush();
        }
    }

    /**
     * Encodes the TLV payload portion of an APNS frame.
     *
     * <p>Each entry's value must be either a single {@code byte[]} (one TLV record) or a
     * {@code byte[][]} (one record per element, all sharing the same field id); any other type is
     * rejected with {@link IllegalArgumentException}.
     *
     * @param fields the fields to encode
     * @return the encoded payload bytes
     * @throws IOException              if the underlying writer fails (impossible for an in-memory
     *                                  sink)
     * @throws IllegalArgumentException if any value is neither {@code byte[]} nor {@code byte[][]}
     */
    private static byte[] encodePayload(Map<Integer, ?> fields) throws IOException {
        var buf = new ByteArrayOutputStream();
        try (var dos = new DataOutputStream(buf)) {
            for (var entry : fields.entrySet()) {
                var value = entry.getValue();
                if (value == null) {
                    continue;
                }
                switch (value) {
                    case byte[] bytes -> {
                        dos.writeByte(entry.getKey().byteValue());
                        dos.writeShort(bytes.length);
                        dos.write(bytes);
                    }
                    case byte[][] groups -> {
                        dos.writeByte(entry.getKey().byteValue());
                        for (var g : groups) {
                            dos.writeShort(g.length);
                            dos.write(g);
                        }
                    }
                    default -> throw new IllegalArgumentException(
                            "unsupported APNS field type: " + value.getClass());
                }
            }
        }
        return buf.toByteArray();
    }

    /**
     * Reads one APNS frame and decodes its payload into a {@code field-id -> bytes} map.
     *
     * <p>Tags outside the {@link ApnsPayloadTag} range are logged at {@code DEBUG} and surface as a
     * packet with {@code null} tag; downstream code never matches such packets against a pending
     * filter so they are effectively dropped after logging.
     *
     * @param in the courier socket input stream
     * @return the decoded packet
     * @throws IOException if the stream closes mid-frame, the length is negative, or a field length
     *                     exceeds the remaining payload
     */
    private static ApnsPacket readPacket(InputStream in) throws IOException {
        var rawTag = readUnsignedByte(in);
        var length = readBigEndianInt(in);
        if (length < 0) {
            throw new IOException("APNS frame size negative: " + length);
        }
        var payload = new byte[length];
        var read = 0;
        while (read < length) {
            var n = in.read(payload, read, length - read);
            if (n < 0) {
                throw new IOException("APNS stream truncated");
            }
            read += n;
        }
        var fields = new LinkedHashMap<Integer, byte[]>();
        var bb = ByteBuffer.wrap(payload);
        while (bb.remaining() >= 3) {
            var fieldId = Byte.toUnsignedInt(bb.get());
            var fieldLen = Short.toUnsignedInt(bb.getShort());
            if (fieldLen > bb.remaining()) {
                throw new IOException("APNS field length exceeds frame");
            }
            var value = new byte[fieldLen];
            bb.get(value);
            fields.put(fieldId, value);
        }
        var tag = ApnsPayloadTag.of(rawTag);
        if (tag == null && Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "apns unknown tag {0} ({1} b)", rawTag, length);
        }
        return new ApnsPacket(tag, fields);
    }

    /**
     * Reads exactly one unsigned byte from a stream.
     *
     * <p>Throws when the stream has been closed; used by the frame decoder to surface unexpected EOF
     * as {@link IOException} rather than the silent {@code -1} {@link InputStream#read()} returns.
     *
     * @param in the source stream
     * @return the unsigned byte value in {@code [0, 255]}
     * @throws IOException if the stream is at EOF
     */
    private static int readUnsignedByte(InputStream in) throws IOException {
        var b = in.read();
        if (b < 0) {
            throw new IOException("APNS stream closed");
        }
        return b;
    }

    /**
     * Reads a 4-byte big-endian integer from a stream.
     *
     * @param in the source stream
     * @return the decoded {@code int}
     * @throws IOException if the stream closes before all four bytes are read
     */
    private static int readBigEndianInt(InputStream in) throws IOException {
        var b1 = readUnsignedByte(in);
        var b2 = readUnsignedByte(in);
        var b3 = readUnsignedByte(in);
        var b4 = readUnsignedByte(in);
        return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
    }

    /**
     * Sends a prepared HTTP request synchronously and returns the body bytes on success.
     *
     * <p>Used for the single bag fetch in {@link #fetchBag}; non-2xx responses and interruptions both
     * surface as {@link IOException} so callers can treat them uniformly as transport failures.
     *
     * @param request the prepared HTTP request
     * @return the raw response body bytes
     * @throws IOException on any non-2xx status, transport failure, or interruption during the call
     */
    private byte[] sendBytes(HttpRequest request) throws IOException {
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "apns bag fetch -> status {0}", response.statusCode());
            if (response.statusCode() / 100 != 2) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "apns bag http failed, status={0}", response.statusCode());
                throw new IOException("APNS bag HTTP " + response.statusCode() + ": "
                        + new String(response.body(), StandardCharsets.UTF_8));
            }
            return response.body();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            if (Log.WARNING) LOGGER.log(Level.WARNING, "apns bag fetch interrupted", ie);
            throw new IOException("APNS bag interrupted", ie);
        }
    }

    /**
     * Builds an {@link HttpClient} configured with the timeout and the optional proxy.
     *
     * <p>When the proxy URI omits an explicit port the default {@code 8080} is used, matching the
     * convention most HTTP proxies advertise.
     *
     * @param proxy proxy URI, or {@code null} for direct
     * @return a configured HTTP client
     */
    private static HttpClient newHttpClient(URI proxy) {
        var builder = HttpClient.newBuilder()
                .connectTimeout(HTTP_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL);
        if (proxy != null && proxy.getHost() != null) {
            var port = proxy.getPort() == -1 ? 8080 : proxy.getPort();
            builder.proxy(ProxySelector.of(new InetSocketAddress(proxy.getHost(), port)));
        }
        return builder.build();
    }

    /**
     * The one-shot synchronisation primitive for an {@link #exchange} caller.
     *
     * <p>Holds the filter the read pump consults to identify the matching response, plus a single-slot
     * mailbox the producer fills with either a delivered packet or an error.
     *
     * @implNote This implementation uses an {@link ArrayBlockingQueue} of size 1 so the dispatcher's
     *           deliver/fail methods never block waiting for the consumer to be ready, and a fast
     *           response that arrives between {@code pending.put} and the consumer's {@code poll} is
     *           buffered rather than lost.
     */
    private static final class Pending {
        /**
         * Holds the predicate the read pump consults when routing each incoming frame.
         *
         * <p>The first pending entry whose filter matches consumes the packet.
         */
        final Predicate<ApnsPacket> filter;

        /**
         * Holds the single-slot mailbox.
         *
         * <p>Carries either an {@link ApnsPacket} (on delivery) or a {@link Throwable} (on failure or
         * close); the consumer disambiguates via {@code instanceof}.
         */
        final ArrayBlockingQueue<Object> slot = new ArrayBlockingQueue<>(1);

        /**
         * Constructs a pending entry waiting for a packet matching a filter.
         *
         * @param filter the response-matching predicate
         */
        Pending(Predicate<ApnsPacket> filter) {
            this.filter = filter;
        }

        /**
         * Delivers a successful response to the awaiting consumer.
         *
         * <p>Non-blocking; subsequent calls are silently dropped.
         *
         * @param packet the matched response packet
         */
        void deliver(ApnsPacket packet) {
            slot.offer(packet);
        }

        /**
         * Delivers an error to the awaiting consumer so it surfaces via {@link #await(long)} as an
         * {@link IOException}.
         *
         * @param error the error to surface
         */
        void fail(Throwable error) {
            slot.offer(error);
        }

        /**
         * Blocks the calling thread until either a packet, an error, or the timeout arrives.
         *
         * <p>Wraps a {@link TimeoutException} into the cause chain of the surfaced {@link IOException}
         * so callers retain enough information to distinguish a timeout from a transport failure.
         *
         * @param timeoutMs the wall-clock timeout in milliseconds
         * @return the delivered packet
         * @throws IOException if the timeout fires, the call is interrupted, or {@link #fail} was
         *                     invoked
         */
        ApnsPacket await(long timeoutMs) throws IOException {
            try {
                var taken = slot.poll(timeoutMs, TimeUnit.MILLISECONDS);
                if (taken == null) {
                    throw new IOException(new TimeoutException("APNS request timed out"));
                }
                if (taken instanceof Throwable t) {
                    if (t instanceof IOException io) throw io;
                    throw new IOException(t);
                }
                return (ApnsPacket) taken;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IOException("APNS request interrupted", ie);
            }
        }
    }
}
