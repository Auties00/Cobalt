package com.github.auties00.cobalt.registration.push.fcm;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.push.fcm.mcs.*;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns the long-lived MCS (Mobile Cloud Service) TLS stream the {@link FcmClient} maintains with
 * {@code mtalk.google.com:5228} after registration completes.
 *
 * <p>Drives the framed MCS protocol end-to-end: version preamble, login handshake, heartbeat ping/ack exchange,
 * periodic stream-ack iq, and incoming data-message stanzas. Reconnects with a fixed back-off after any transport
 * failure.
 *
 * <p>Two virtual threads are involved at steady state:
 * <ul>
 *   <li>the <em>reader thread</em>, started by {@link #start()}, owns the socket lifecycle and pumps incoming
 *       frames;</li>
 *   <li>the <em>heartbeat thread</em>, spawned per connection by the reader after login, writes a
 *       {@link FcmMcsHeartbeatPing} every {@link #HEARTBEAT_INTERVAL_SECONDS} seconds.</li>
 * </ul>
 *
 * <p>Both writers go through {@link #writeLock} so frames can never interleave on the wire. Persistent ids accumulated
 * from incoming data messages are mirrored into {@link FcmSession#persistentIds()} under {@link #sessionLock} so the
 * next login can replay them. Push-code values extracted from incoming {@code app_data} are handed to
 * {@link FcmPushCode#deliver(String)} so callers blocked in {@link FcmClient#getPushCode()} unblock immediately.
 */
final class FcmMcsConnection {
    /**
     * The logger for {@link FcmMcsConnection}.
     */
    private static final System.Logger LOGGER = Log.get(FcmMcsConnection.class);

    /**
     * MCS gateway hostname, identical for every Android device.
     */
    private static final String MCS_HOST = "mtalk.google.com";

    /**
     * MCS port; matches the value the native Play Services client uses.
     */
    private static final int MCS_PORT = 5228;

    /**
     * Single-byte version preamble sent before the first framed packet on every connection.
     */
    private static final byte MCS_VERSION = 41;

    /**
     * Frame tag carried by an inbound heartbeat ping.
     */
    private static final byte TAG_HEARTBEAT_PING = 0;

    /**
     * Frame tag carried by an outbound heartbeat ack.
     */
    private static final byte TAG_HEARTBEAT_ACK = 1;

    /**
     * Frame tag for the {@link FcmMcsLoginRequest} packet.
     */
    private static final byte TAG_LOGIN_REQUEST = 2;

    /**
     * Frame tag for the {@link FcmMcsLoginResponse} packet.
     */
    private static final byte TAG_LOGIN_RESPONSE = 3;

    /**
     * Frame tag the server uses to ask the client to disconnect.
     */
    private static final byte TAG_CLOSE = 4;

    /**
     * Frame tag for {@link FcmMcsIqStanza}.
     *
     * <p>Cobalt only emits the periodic stream-ack iq under this tag.
     */
    private static final byte TAG_IQ_STANZA = 7;

    /**
     * Frame tag for incoming {@link FcmMcsDataMessageStanza}, the silent push notifications.
     */
    private static final byte TAG_DATA_MESSAGE_STANZA = 8;

    /**
     * Heartbeat interval in seconds.
     *
     * @implNote
     * This implementation uses ten minutes to match the cadence the native client uses to keep middleboxes from
     * idle-timing out the TCP flow.
     */
    private static final long HEARTBEAT_INTERVAL_SECONDS = 10 * 60L;

    /**
     * Number of frames that must arrive before the client emits a cumulative stream-ack iq advancing the server's
     * retry cursor.
     */
    private static final int STREAM_ACK_EVERY = 10;

    /**
     * Maximum number of persistent ids the client retains for replay on the next login.
     *
     * @implNote
     * This implementation discards older ids beyond this cap so the {@link FcmSession} payload stays bounded across
     * long-lived sessions.
     */
    private static final int PERSISTENT_ID_BUFFER = 50;

    /**
     * Back-off in milliseconds between reconnect attempts after a transport failure.
     *
     * @implNote
     * This implementation uses ten seconds to match the value the native Play Services client settles on after the
     * first burst of fast retries.
     */
    private static final long RECONNECT_BACKOFF_MS = 10_000L;

    /**
     * Key under which WhatsApp's silent verification push carries the {@code /v2/code} value inside the FCM data
     * message's {@code app_data} map.
     */
    private static final String PUSH_CODE_APP_DATA_KEY = "registration_code";

    /**
     * Session whose credentials drive the login packet.
     *
     * <p>The session's {@link FcmSession#persistentIds()} list is mirrored from inbound stanzas.
     */
    private final FcmSession session;

    /**
     * Sink for verification codes extracted from incoming {@code app_data} entries.
     */
    private final FcmPushCode pushCode;

    /**
     * Serialises every outbound write on the MCS stream.
     *
     * @implNote
     * Both the reader thread (acks, heartbeat acks) and the heartbeat virtual thread (ten-minute ping) need to write
     * framed bytes; without this lock two writers could interleave inside one frame and corrupt the stream.
     */
    private final Object writeLock;

    /**
     * Lock guarding mutations and snapshots of {@link FcmSession#persistentIds()}.
     *
     * @implNote
     * This implementation uses a dedicated final monitor rather than {@code synchronized (session)} so lock identity
     * stays stable across the connection's lifetime even if {@link #session} were ever replaced.
     */
    private final Object sessionLock;

    /**
     * Currently-attached TLS socket, or {@code null} between connection attempts.
     *
     * @implNote
     * This implementation holds the reference {@code volatile} so {@link #close()} can read a fresh value from any
     * thread.
     */
    private volatile SSLSocket socket;

    /**
     * Reader virtual thread, started by {@link #start()}.
     *
     * @implNote
     * This implementation holds the reference {@code volatile} so {@link #close()} can interrupt it from any thread.
     */
    private volatile Thread listenerThread;

    /**
     * Heartbeat virtual thread for the current connection.
     *
     * <p>Replaced each time {@link #connectAndListen()} reconnects.
     */
    private volatile Thread heartbeatThread;

    /**
     * Stop flag flipped by {@link #close()}.
     *
     * <p>Read by both the listener loop and the heartbeat loop on every iteration so they exit promptly without
     * waiting for the next blocking I/O call to time out.
     */
    private volatile boolean stopped;

    /**
     * Last received stream id.
     *
     * @implNote
     * Written by the reader thread and read by both the reader (for iq acks) and the heartbeat thread (for the ping
     * cursor); this implementation marks it {@code volatile} so the heartbeat thread sees fresh values without a
     * happens-before edge per ping.
     */
    private volatile long streamId;

    /**
     * Highest stream id already advertised to the server via a stream-ack iq.
     *
     * <p>Lives only on the reader thread, so no synchronisation is required.
     */
    private long lastStreamIdReported;

    /**
     * Constructs a new connection bound to the given session and push-code sink.
     *
     * <p>Does not open a socket; the caller must invoke {@link #start()} after construction.
     *
     * @param session  the session that supplies login credentials and receives persistent-id updates
     * @param pushCode the holder where verification codes are delivered
     */
    FcmMcsConnection(FcmSession session, FcmPushCode pushCode) {
        this.session = session;
        this.pushCode = pushCode;
        this.writeLock = new Object();
        this.sessionLock = new Object();
        this.lastStreamIdReported = -1L;
    }

    /**
     * Spawns the reader virtual thread that owns the MCS connection.
     *
     * <p>Returns immediately; subsequent transport failures are swallowed and retried until {@link #close()} flips the
     * stop flag.
     */
    void start() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "starting mcs listener thread");
        this.listenerThread = Thread.startVirtualThread(this::listenLoop);
    }

    /**
     * Stops the reader and heartbeat threads, tears down the TLS socket, and lets {@link #listenLoop()} return.
     *
     * <p>Idempotent; called by {@link FcmClient#close()}.
     */
    void close() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "closing mcs connection");
        stopped = true;
        var s = socket;
        if (s != null) {
            try {
                s.close();
            } catch (IOException _) {
            }
        }
        var hb = heartbeatThread;
        if (hb != null) {
            hb.interrupt();
        }
        var lt = listenerThread;
        if (lt != null) {
            lt.interrupt();
        }
    }

    /**
     * Runs the reader virtual-thread loop.
     *
     * <p>Reconnects with a fixed back-off on every transport failure; exits cleanly when {@link #stopped} flips to
     * {@code true} or when the thread is interrupted by {@link #close()}.
     */
    private void listenLoop() {
        while (!stopped) {
            try {
                connectAndListen();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception ex) {
                if (stopped) {
                    return;
                }
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "mcs connection lost; reconnecting in " + RECONNECT_BACKOFF_MS + " ms", ex);
                }
                try {
                    Thread.sleep(RECONNECT_BACKOFF_MS);
                } catch (InterruptedException sleepInterrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * Performs one MCS connection cycle.
     *
     * <p>Opens the TLS socket, writes the version preamble plus the {@link FcmMcsLoginRequest}, checks the
     * {@link FcmMcsLoginResponse}, spins up the heartbeat thread, then loops reading frames until {@link #stopped}
     * flips or the stream errors out (the failure is caught by the outer {@link #listenLoop()}).
     *
     * @throws Exception on any transport failure or login refusal
     */
    private void connectAndListen() throws Exception {
        streamId = 0;
        lastStreamIdReported = -1;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "connecting to mcs host={0} port={1}", MCS_HOST, MCS_PORT);
        var sock = (SSLSocket) SSLSocketFactory.getDefault().createSocket(MCS_HOST, MCS_PORT);
        sock.setTcpNoDelay(true);
        sock.setKeepAlive(true);
        sock.startHandshake();
        socket = sock;

        var in = sock.getInputStream();
        var out = sock.getOutputStream();
        synchronized (writeLock) {
            out.write(new byte[]{MCS_VERSION});
            out.flush();
            writeFrame(out, TAG_LOGIN_REQUEST, FcmMcsLoginRequestSpec.encode(buildLoginRequest()));
        }

        var serverVersion = in.read();
        if (serverVersion < 0) {
            throw new IOException("MCS server closed before sending version");
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "mcs server version={0}", serverVersion);

        var loginPacket = readFrame(in);
        if (loginPacket.tag != TAG_LOGIN_RESPONSE) {
            throw new IOException("expected LoginResponse, got tag=" + loginPacket.tag);
        }
        var login = FcmMcsLoginResponseSpec.decode(loginPacket.payload);
        if (login.error() != null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "mcs login refused (code={0})", login.error().code());
            throw new IOException("MCS login refused: code="
                    + login.error().code() + " message=" + login.error().message());
        }
        if (Log.INFO) LOGGER.log(Level.INFO, "mcs logged in (server_ts={0})", login.serverTimestamp());
        streamId = 1;
        startHeartbeatLoop(out);

        while (!stopped) {
            var pkt = readFrame(in);
            streamId++;
            handleFrame(pkt, out);
            ackIfNeeded(out, false);
        }
    }

    /**
     * Routes one decoded frame to the matching protocol handler.
     *
     * <p>Unknown tags are logged and ignored. A {@link #TAG_CLOSE} from the server is converted to an
     * {@link IOException} so the outer {@link #listenLoop()} reconnects.
     *
     * @param frame the just-read frame
     * @param out   the connection's output stream, for writing acks
     * @throws IOException if the server requested close or a write fails
     */
    private void handleFrame(Frame frame, OutputStream out) throws IOException {
        switch (frame.tag) {
            case TAG_DATA_MESSAGE_STANZA -> dispatchDataMessage(frame.payload);
            case TAG_HEARTBEAT_PING -> {
                var ack = new FcmMcsHeartbeatAckBuilder()
                        .lastStreamIdReceived(streamId)
                        .status(0L)
                        .build();
                var encoded = FcmMcsHeartbeatAckSpec.encode(ack);
                synchronized (writeLock) {
                    writeFrame(out, TAG_HEARTBEAT_ACK, encoded);
                }
            }
            case TAG_HEARTBEAT_ACK, TAG_IQ_STANZA -> {
                if (Log.TRACE) LOGGER.log(Level.TRACE, "mcs ack/iq tag={0}", frame.tag);
            }
            case TAG_CLOSE -> throw new IOException("MCS server requested close");
            default -> {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "mcs unknown tag {0} ({1} b)", frame.tag, frame.payload.length);
                }
            }
        }
    }

    /**
     * Decodes an incoming data-message stanza, mirrors its {@code persistent_id} into
     * {@link FcmSession#persistentIds()}, and surfaces any {@code registration_code} entry to {@link #pushCode}.
     *
     * <p>The persistent-id list is trimmed to the most-recent {@link #PERSISTENT_ID_BUFFER} entries so a long-lived
     * session never accumulates an unbounded payload.
     *
     * @param payload the encoded stanza bytes
     */
    private void dispatchDataMessage(byte[] payload) {
        var stanza = FcmMcsDataMessageStanzaSpec.decode(payload);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "mcs data message received ({0} bytes)", payload.length);
        var pid = stanza.persistentId();
        if (pid != null && !pid.isEmpty()) {
            synchronized (sessionLock) {
                var ids = session.persistentIds();
                ids.add(pid);
                if (ids.size() > PERSISTENT_ID_BUFFER) {
                    ids.subList(0, ids.size() - PERSISTENT_ID_BUFFER).clear();
                }
            }
        }
        if (stanza.appData() == null) {
            return;
        }
        for (var entry : stanza.appData()) {
            if (PUSH_CODE_APP_DATA_KEY.equals(entry.key())) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "fcm push code extracted from mcs data message");
                pushCode.deliver(entry.value());
                return;
            }
        }
    }

    /**
     * Writes a stream-ack iq when either {@code force} is set or {@link #STREAM_ACK_EVERY} new frames have arrived
     * since the previous ack.
     *
     * <p>Updates {@link #lastStreamIdReported} on success so the next call uses the freshly advertised cursor as its
     * baseline.
     *
     * @param out   the connection's output stream
     * @param force if {@code true}, sends an iq even when fewer than {@link #STREAM_ACK_EVERY} frames have accumulated
     * @throws IOException if the write fails
     */
    private void ackIfNeeded(OutputStream out, boolean force) throws IOException {
        if (!force && streamId == lastStreamIdReported) return;
        var delta = streamId - Math.max(lastStreamIdReported, 0);
        if (force || delta >= STREAM_ACK_EVERY) {
            var extension = new FcmMcsIqStanzaExtensionBuilder()
                    .id(13L)
                    .data(new byte[0])
                    .build();
            var iq = new FcmMcsIqStanzaBuilder()
                    .type(1L)
                    .id("")
                    .extension(extension)
                    .lastStreamIdReceived(streamId)
                    .status(0L)
                    .build();
            var encoded = FcmMcsIqStanzaSpec.encode(iq);
            synchronized (writeLock) {
                writeFrame(out, TAG_IQ_STANZA, encoded);
            }
            if (Log.TRACE) LOGGER.log(Level.TRACE, "mcs stream-ack sent (stream_id={0})", streamId);
            lastStreamIdReported = streamId;
        }
    }

    /**
     * Builds the login request packet for the current connection.
     *
     * <p>Snapshots the persistent-id list under {@link #sessionLock} so it cannot mutate mid-encode while the reader
     * thread receives a new push.
     *
     * @return the populated login request
     */
    private FcmMcsLoginRequest buildLoginRequest() {
        var androidId = session.androidId();
        var newVcSetting = new FcmMcsLoginRequestSettingBuilder()
                .name("new_vc")
                .value("1")
                .build();
        List<String> persistentIdsCopy;
        synchronized (sessionLock) {
            persistentIdsCopy = new ArrayList<>(session.persistentIds());
        }
        return new FcmMcsLoginRequestBuilder()
                .id("android-30")
                .domain("mcs.android.com")
                .user(Long.toString(androidId))
                .resource(Long.toString(androidId))
                .authToken(Long.toString(session.securityToken()))
                .deviceId("android-" + Long.toHexString(androidId))
                .settings(List.of(newVcSetting))
                .persistentIds(persistentIdsCopy)
                .adaptiveHeartbeat(false)
                .useRmq2(true)
                .authService(2L)
                .networkType(1L)
                .build();
    }

    /**
     * Spawns the heartbeat virtual thread for the current connection.
     *
     * <p>Quietly returns when the socket dies; the outer reader loop notices the failure and reconnects, which spawns
     * a fresh heartbeat thread.
     *
     * @param out the connection's output stream
     */
    private void startHeartbeatLoop(OutputStream out) {
        heartbeatThread = Thread.startVirtualThread(() -> {
            while (!stopped) {
                try {
                    Thread.sleep(Duration.ofSeconds(HEARTBEAT_INTERVAL_SECONDS));
                    var ping = new FcmMcsHeartbeatPingBuilder()
                            .lastStreamIdReceived(streamId)
                            .build();
                    var encoded = FcmMcsHeartbeatPingSpec.encode(ping);
                    synchronized (writeLock) {
                        writeFrame(out, TAG_HEARTBEAT_PING, encoded);
                    }
                    if (Log.TRACE) LOGGER.log(Level.TRACE, "mcs heartbeat ping sent (stream_id={0})", streamId);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (IOException _) {
                    return;
                }
            }
        });
    }

    /**
     * One MCS frame: a one-byte tag, a varint length prefix, and a payload.
     *
     * <p>Used as the unit of transfer between {@link #readFrame(InputStream)} and
     * {@link #handleFrame(Frame, OutputStream)}.
     *
     * @param tag     the frame tag identifying the protocol message
     * @param payload the raw protobuf payload bytes
     */
    private record Frame(byte tag, byte[] payload) {
    }

    /**
     * Writes one MCS frame to {@code out}.
     *
     * <p>Writes the tag byte, the varint-encoded payload length, then the payload itself in a single buffered write to
     * keep the wire frame contiguous.
     *
     * @param out     the destination stream
     * @param tag     the frame tag byte
     * @param payload the raw payload bytes
     * @throws IOException if the underlying write fails
     */
    private static void writeFrame(OutputStream out, byte tag, byte[] payload) throws IOException {
        var lenBuf = encodeVarint(payload.length);
        var frame = new byte[1 + lenBuf.length + payload.length];
        frame[0] = tag;
        System.arraycopy(lenBuf, 0, frame, 1, lenBuf.length);
        System.arraycopy(payload, 0, frame, 1 + lenBuf.length, payload.length);
        out.write(frame);
        out.flush();
    }

    /**
     * Reads exactly one MCS frame from {@code in}.
     *
     * <p>Loops on {@link InputStream#read(byte[], int, int)} until the full payload has been read; a stream close
     * mid-payload surfaces as an {@link IOException} rather than returning a truncated payload.
     *
     * @param in the source stream
     * @return the decoded frame
     * @throws IOException if the stream closes mid-frame or the payload is truncated
     */
    private static Frame readFrame(InputStream in) throws IOException {
        var tag = in.read();
        if (tag < 0) {
            throw new IOException("MCS stream closed");
        }
        var size = readVarintInt(in);
        var payload = new byte[size];
        var read = 0;
        while (read < size) {
            var n = in.read(payload, read, size - read);
            if (n < 0) {
                throw new IOException("MCS stream truncated");
            }
            read += n;
        }
        return new Frame((byte) tag, payload);
    }

    /**
     * Encodes a non-negative {@code long} as protobuf varint bytes.
     *
     * @implNote
     * This implementation allocates a 10-byte scratch buffer (the maximum varint width for a 64-bit value), then trims
     * to the actual encoded length before returning.
     *
     * @param value the value to encode, treated as unsigned
     * @return the varint bytes
     */
    private static byte[] encodeVarint(long value) {
        var buf = new byte[10];
        var i = 0;
        while ((value & ~0x7FL) != 0) {
            buf[i++] = (byte) ((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buf[i++] = (byte) value;
        var out = new byte[i];
        System.arraycopy(buf, 0, out, 0, i);
        return out;
    }

    /**
     * Reads a varint-encoded payload length from {@code in}.
     *
     * <p>Rejects values that overflow {@link Integer#MAX_VALUE} or exceed the 64-bit varint width so a malicious peer
     * cannot trigger an unbounded allocation through the {@link #readFrame(InputStream)} payload buffer.
     *
     * @param in the source stream
     * @return the decoded length, in {@code [0, Integer.MAX_VALUE]}
     * @throws IOException if the stream closes mid-varint or the decoded length is out of range
     */
    private static int readVarintInt(InputStream in) throws IOException {
        var result = 0L;
        var shift = 0;
        while (true) {
            var b = in.read();
            if (b < 0) {
                throw new IOException("MCS stream closed mid-varint");
            }
            result |= ((long) (b & 0x7F)) << shift;
            if ((b & 0x80) == 0) {
                if (result < 0L || result > Integer.MAX_VALUE) {
                    throw new IOException("MCS frame size out of range: " + result);
                }
                return (int) result;
            }
            shift += 7;
            if (shift >= 64) {
                throw new IOException("varint too long");
            }
        }
    }
}
