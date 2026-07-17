package com.github.auties00.cobalt.calls.transport.datachannel;

import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.sctp.Message;
import com.github.auties00.sctp.PayloadProtocolId;
import com.github.auties00.sctp.SctpImplementation;
import com.github.auties00.sctp.SctpSocket;
import com.github.auties00.sctp.StreamId;
import com.github.auties00.sctp.exception.AssociationAbortedException;
import com.github.auties00.sctp.exception.SendException;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import com.github.auties00.cobalt.calls.transport.LiveRelayTransport;
import com.github.auties00.cobalt.calls.transport.dtls.DtlsByteChannel;
import com.github.auties00.cobalt.calls.transport.dtls.VoipDtlsCertificates;
import com.github.auties00.cobalt.calls.transport.dtls.VoipDtlsTransport;

/**
 * The relay path {@link LiveRelayTransport.DataChannel}: it runs the DTLS handshake against a Meta edgeray
 * relay, connects one SCTP association over the resulting DTLS transport, and carries every media and
 * control message as SCTP DATA on a single pre negotiated data channel.
 *
 * <p>The WhatsApp Web relay transport is an {@code RTCPeerConnection} whose synthesized remote answer points
 * a single host ICE candidate at the relay and pins one well known DTLS certificate; once ICE selects that
 * pair the client runs DTLS as the client (the relay is the DTLS server unless the relay block sets
 * {@code enable_edgeray_dtls_active_mode}, which flips the client to the active role), opens one SCTP
 * association on port {@value #SCTP_PORT}, and creates one data channel that is pre negotiated
 * ({@code negotiated=true, id=0, ordered=false, maxRetransmits=0}) so no RFC 8832 DCEP open exchange is
 * sent. The engine multiplexes RTP, RTCP, application STUN, WARP, and the subscription over that one channel
 * as SCTP DATA; this backend is the opaque pipe that carries those bytes verbatim.
 *
 * <p>Outbound SCTP packets the association produces are encrypted into DTLS application data records by the
 * {@link DtlsByteChannel} the SCTP socket owns as its transport and written to the host UDP egress through a
 * {@link RelayDatagramTransport}; inbound DTLS records arrive through {@link #feedDtlsRecord(byte[])}, are
 * queued to that same datagram transport, decrypted, and read back into the SCTP stack, whose decoded
 * application messages reach the consumer registered through {@link #onMessage(Consumer)}. The certificate
 * the relay presents is not signaled per call; the handshake pins the fixed relay fingerprint
 * {@link #RELAY_CERT_FINGERPRINT_SHA256}.
 *
 * @implNote This implementation wires the JDK pure Java DTLS record layer (the relay path is low volume and
 *           never needs a native DTLS binding), driven by {@link VoipDtlsTransport}, to the pure Java
 *           {@code com.github.auties00.sctp} {@code SctpSocket} through a {@link DtlsByteChannel} that moves
 *           one SCTP record per read and write. It reproduces the {@code WAWebVoipSctpConnectionManager}
 *           create offer, synthesize answer, and open pre negotiated channel flow of WhatsApp Web restricted
 *           to the relay endpoint. The DTLS handshake and the SCTP connect both run on the calling bring up
 *           thread inside {@link #connect()}, which blocks until the channel is up or throws. The SCTP socket
 *           announces the {@link SctpImplementation#USRSCTP} implementation identity, requests
 *           {@value #WEBRTC_NUM_STREAMS} streams in each direction, enables the partial reliability
 *           extension, and sends the single pre negotiated channel's messages on stream
 *           {@value #CHANNEL_STREAM_ID} under PPID {@value #PPID_BINARY} unordered with
 *           {@code maxRetransmissions=0} (best effort once), matching the relay peer's expectations exactly.
 */
public final class RelayDataChannel implements LiveRelayTransport.DataChannel {
    /**
     * The SCTP port WebRTC data channels use at both ends (RFC 8831).
     */
    public static final int SCTP_PORT = 5000;

    /**
     * The pre negotiated data channel's stream id.
     *
     * @implNote This implementation uses {@code 0}, the id WhatsApp Web hardcodes when it passes
     * {@code createDataChannel("pre negotiated", {negotiated:true, id:0, ...})}. Because the channel is
     * pre negotiated, the id is fixed at {@code 0} regardless of which side takes the DTLS client role, so it
     * does not depend on {@link #relayActiveMode}.
     */
    public static final int CHANNEL_STREAM_ID = 0;

    /**
     * The SCTP Payload Protocol Identifier for a binary WebRTC data channel message (RFC 8831).
     *
     * @implNote This implementation uses {@code 53} ("WebRTC Binary"): the engine ships every multiplexed
     * datagram as opaque binary application data, so each {@link #send(byte[])} payload travels under this
     * PPID and the receive path delivers any message carrying it to the registered consumer.
     */
    public static final int PPID_BINARY = 53;

    /**
     * The pinned SHA-256 fingerprint of the relay's DTLS certificate, as the 32 raw digest bytes.
     *
     * <p>The relay path does not signal a per call {@code <certificate>}; the relay presents a fixed,
     * well known certificate and the client pins this fingerprint. It corresponds to the colon separated
     * form {@code F9:CA:0C:98:A3:CC:71:D6:42:CE:5A:E2:53:D2:15:20:D3:1B:BA:D8:57:A4:F0:AF:BE:0B:FB:F3:6B:0C:A0:68}.
     *
     * @implNote This implementation pins the fingerprint WhatsApp Web hardcodes into its synthesized relay
     * answer SDP ({@code a=fingerprint:sha-256 F9:CA:...:A0:68}). The relay certificate may rotate, so this
     * constant is extracted afresh per WA Web snapshot rather than computed.
     */
    public static final byte[] RELAY_CERT_FINGERPRINT_SHA256 = {
            (byte) 0xF9, (byte) 0xCA, (byte) 0x0C, (byte) 0x98, (byte) 0xA3, (byte) 0xCC, (byte) 0x71, (byte) 0xD6,
            (byte) 0x42, (byte) 0xCE, (byte) 0x5A, (byte) 0xE2, (byte) 0x53, (byte) 0xD2, (byte) 0x15, (byte) 0x20,
            (byte) 0xD3, (byte) 0x1B, (byte) 0xBA, (byte) 0xD8, (byte) 0x57, (byte) 0xA4, (byte) 0xF0, (byte) 0xAF,
            (byte) 0xBE, (byte) 0x0B, (byte) 0xFB, (byte) 0xF3, (byte) 0x6B, (byte) 0x0C, (byte) 0xA0, (byte) 0x68
    };

    /**
     * The SCTP transmission unit (maximum SCTP packet size, in bytes) the association is built with.
     *
     * @implNote This implementation uses {@code 1191}, the {@code com.github.auties00.sctp} default derived
     * from the minimum IPv6 MTU ({@code 1280}) minus conservative DTLS, UDP, and IP header estimates. Each
     * SCTP packet travels inside a DTLS record whose own maximum is bounded by the path MTU, so the SCTP MTU
     * must leave room for the DTLS record overhead (header, explicit nonce, AEAD tag). Setting it to the full
     * {@code 1500} path MTU makes a near MTU SCTP packet exceed the DTLS record budget, so the DTLS engine
     * splits it across two datagrams and the peer reads a truncated SCTP packet it cannot parse.
     */
    public static final int SCTP_MTU = 1191;

    /**
     * The RFC 8831 default count of inbound and outbound SCTP streams for a WebRTC DataChannel association.
     *
     * @implNote This implementation announces {@code 1024} streams in each direction, the value RFC 8831
     * specifies for WebRTC DataChannels.
     */
    private static final int WEBRTC_NUM_STREAMS = 1024;

    /**
     * The single binary payload protocol identifier every outbound and inbound application message uses.
     */
    private static final PayloadProtocolId BINARY_PPID = PayloadProtocolId.of(PPID_BINARY);

    /**
     * The single pre negotiated channel's stream selector.
     */
    private static final StreamId CHANNEL_STREAM = StreamId.of(CHANNEL_STREAM_ID);

    /**
     * The maximum time the SCTP handshake is allowed to take before {@link #connect()} fails, in seconds.
     *
     * @implNote This implementation bounds the {@code SctpSocket} connect wait so a relay that completes DTLS
     * but never answers the SCTP INIT fails the bring up rather than blocking the bring up thread forever.
     */
    private static final int SCTP_CONNECT_TIMEOUT_SECONDS = 10;

    /**
     * The maximum time the DTLS handshake is allowed to take before {@link #connect()} fails, in milliseconds.
     *
     * @implNote This implementation bounds the {@link VoipDtlsTransport#handshake(long)} wait so a relay that
     * never completes DTLS fails the bring up rather than retransmitting forever.
     */
    private static final long DTLS_HANDSHAKE_TIMEOUT_MILLIS = 10_000L;

    /**
     * The bounded depth of the inbound DTLS record queue.
     *
     * @implNote This implementation bounds the queue so a relay flooding records cannot grow it without
     * limit; a record offered to a full queue is dropped, which the DTLS retransmission recovers.
     */
    private static final int INBOUND_QUEUE_CAPACITY = 64;

    /**
     * The host UDP egress the DTLS records leave through, addressed to {@link #relayAddress}.
     */
    private final LiveRelayTransport.Egress egress;

    /**
     * The relay transport address the DTLS records are sent to.
     */
    private final SocketAddress relayAddress;

    /**
     * Whether the relay enabled DTLS active mode, flipping the relay to the DTLS client role.
     */
    private final boolean relayActiveMode;

    /**
     * The pinned relay certificate SHA-256 fingerprint the handshake verifies the server certificate against.
     */
    private final byte[] pinnedFingerprint;

    /**
     * The bounded inbound queue the DTLS datagram transport reads records from.
     *
     * <p>{@link #feedDtlsRecord(byte[])} offers inbound DTLS records here; the {@link RelayDatagramTransport}
     * polls it with the receive timeout so a closed channel can unblock the pump with a poison record.
     */
    private final LinkedBlockingQueue<byte[]> inbound = new LinkedBlockingQueue<>(INBOUND_QUEUE_CAPACITY);

    /**
     * The consumer that receives each inbound SCTP DATA application message, set through
     * {@link #onMessage(Consumer)}.
     */
    private final AtomicReference<Consumer<byte[]>> messageConsumer = new AtomicReference<>();

    /**
     * Whether the data channel has completed bring up and can carry application data.
     */
    private final AtomicBoolean ready = new AtomicBoolean();

    /**
     * Whether the channel has been closed, guarding the bring up and the teardown against running twice.
     */
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * The SCTP socket connected over the DTLS transport, or {@code null} until {@link #connect()} runs.
     */
    private volatile SctpSocket socket;

    /**
     * The single pre negotiated channel's outgoing stream handle, or {@code null} until {@link #connect()}
     * runs; the blocking {@link SctpSocket.Stream} refinement so its send requests carry the non blocking
     * {@code trySend} terminal.
     */
    private volatile SctpSocket.Stream stream;

    /**
     * The daemon virtual thread that pumps inbound SCTP messages from the socket to
     * {@link #onInboundMessage(int, int, byte[])}, or {@code null} until {@link #connect()} runs.
     */
    private volatile Thread receivePump;

    /**
     * The poison record offered to {@link #inbound} on close so a blocked datagram read returns at once.
     */
    private static final byte[] POISON = new byte[0];

    /**
     * The logger for {@link RelayDataChannel}.
     */
    private static final System.Logger LOGGER = Log.get(RelayDataChannel.class);

    /**
     * Guards the one time log of the first outbound DTLS record so the handshake's first ClientHello is
     * recorded without logging every retransmission.
     */
    private final AtomicBoolean firstOutboundDtlsLogged = new AtomicBoolean();

    /**
     * Guards the one time log of the first inbound DTLS record so a relay that answers the handshake is
     * recorded without logging every record.
     */
    private final AtomicBoolean firstInboundDtlsLogged = new AtomicBoolean();

    /**
     * Counts every inbound data channel message so the trace shows whether the subscription success
     * ({@code 0x0103}) and inbound media ever arrive, or only the {@code 0x0802} keepalive pong.
     */
    private final AtomicLong inboundMessages = new AtomicLong();

    /**
     * Counts outbound media sends so the trace samples them (every hundredth) instead of logging every RTP
     * packet, while every control message is logged in full.
     */
    private final AtomicLong outboundMedia = new AtomicLong();

    /**
     * Constructs the relay data channel over the host egress, relay address, DTLS role, and pinned
     * fingerprint.
     *
     * @param egress            the host UDP egress the DTLS records leave through
     * @param relayAddress      the relay transport address the DTLS records are sent to
     * @param relayActiveMode   whether the relay enabled DTLS active mode (the relay is then the DTLS client
     *                          and this side is the DTLS server)
     * @param pinnedFingerprint the SHA-256 fingerprint the relay certificate is pinned to, 32 raw digest
     *                          bytes
     * @throws NullPointerException     if {@code egress}, {@code relayAddress}, or {@code pinnedFingerprint}
     *                                  is {@code null}
     * @throws IllegalArgumentException if {@code pinnedFingerprint} is not exactly 32 bytes
     */
    public RelayDataChannel(LiveRelayTransport.Egress egress,
                            SocketAddress relayAddress,
                            boolean relayActiveMode,
                            byte[] pinnedFingerprint) {
        this.egress = Objects.requireNonNull(egress, "egress cannot be null");
        this.relayAddress = Objects.requireNonNull(relayAddress, "relayAddress cannot be null");
        this.relayActiveMode = relayActiveMode;
        Objects.requireNonNull(pinnedFingerprint, "pinnedFingerprint cannot be null");
        if (pinnedFingerprint.length != VoipDtlsCertificates.SHA256_FINGERPRINT_LENGTH) {
            throw new IllegalArgumentException("pinned fingerprint must be "
                    + VoipDtlsCertificates.SHA256_FINGERPRINT_LENGTH + " bytes, got " + pinnedFingerprint.length);
        }
        this.pinnedFingerprint = pinnedFingerprint.clone();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation runs the JDK DTLS client handshake against the relay over a
     *           {@link RelayDatagramTransport} wired to the host egress and the inbound queue, verifying the
     *           server certificate against {@link #pinnedFingerprint}; on success it wraps the established
     *           DTLS transport in a {@link DtlsByteChannel} and connects an {@code SctpSocket} over it to
     *           {@value #SCTP_PORT} at both ends, selects the pre negotiated stream
     *           {@value #CHANNEL_STREAM_ID}, starts the inbound receive pump, and marks the channel ready.
     *           The pre negotiated channel needs no DCEP open, so the association is usable for application
     *           data the moment SCTP connects. Any failure releases the partially built state and is
     *           surfaced as a {@link WhatsAppCallException.DataChannel}.
     */
    @Override
    public void connect() {
        if (closed.get()) {
            throw new WhatsAppCallException.DataChannel("relay data channel is closed");
        }
        if (ready.get()) {
            return;
        }
        var datagramTransport = new RelayDatagramTransport();
        SctpSocket localSocket = null;
        try {
            if (Log.INFO) {
                LOGGER.log(Level.INFO, "relay data channel connect: starting dtls handshake to {0}", relayAddress);
            }
            var dtls = handshake(datagramTransport);
            if (Log.INFO) {
                LOGGER.log(Level.INFO, "relay dtls handshake complete, connecting sctp on port {0}", SCTP_PORT);
            }
            var channel = new DtlsByteChannel(dtls);
            localSocket = SctpSocket.builder(channel)
                    .name("calls-relay-sctp")
                    .localPort(SCTP_PORT)
                    .remotePort(SCTP_PORT)
                    .maxTransmissionUnit(SCTP_MTU)
                    .announcedMaximumIncomingStreams(WEBRTC_NUM_STREAMS)
                    .announcedMaximumOutgoingStreams(WEBRTC_NUM_STREAMS)
                    .partialReliability(true)
                    .implementation(SctpImplementation.USRSCTP)
                    .connect(Duration.ofSeconds(SCTP_CONNECT_TIMEOUT_SECONDS));
            this.socket = localSocket;
            this.stream = localSocket.selectStream(CHANNEL_STREAM);
            startReceivePump(localSocket);
            ready.set(true);
            if (Log.INFO) {
                LOGGER.log(Level.INFO, "relay data channel ready, sctp connected");
            }
        } catch (WhatsAppCallException exception) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "relay data channel connect failed", exception);
            }
            closeSocketQuietly(localSocket);
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "relay data channel bring-up interrupted", exception);
            }
            closeSocketQuietly(localSocket);
            throw new WhatsAppCallException.DataChannel("relay data channel bring-up interrupted", exception);
        } catch (IOException | AssociationAbortedException | RuntimeException exception) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "relay data channel bring-up failed", exception);
            }
            closeSocketQuietly(localSocket);
            throw new WhatsAppCallException.DataChannel("relay data channel bring-up failed", exception);
        }
    }

    /**
     * Runs the DTLS handshake over the datagram transport and returns the established DTLS transport.
     *
     * <p>The role is chosen by {@link #relayActiveMode}: in the common passive mode the relay is the DTLS
     * server and this side is the DTLS client; when the relay block set {@code enable_edgeray_dtls_active_mode}
     * the relay is the DTLS client and this side is the DTLS server instead. Both roles present a freshly
     * generated self signed ECDSA P-256 certificate and pin the relay's certificate to
     * {@link #pinnedFingerprint} through {@link VoipDtlsCertificates#createEngine(boolean, byte[])}; the
     * established transport is identical whichever role ran it, so everything after this method (the SCTP
     * association and the pre negotiated channel) does not branch on the role.
     *
     * @param datagramTransport the datagram transport wired to the host egress and the inbound queue
     * @return the established DTLS application data transport
     * @throws WhatsAppCallException.DataChannel if the handshake fails or the relay certificate does not pin
     * @implNote This implementation runs the active mode server path as a defensive branch: every live relay
     *           answer carries {@code a=setup:passive}, so the role where the relay is the DTLS client and
     *           this side is the DTLS server has never run against a real relay, but the protocol defines it,
     *           so it is implemented rather than rejected. The DTLS record and handshake layer is the JDK's
     *           own {@code DTLSv1.2} {@link javax.net.ssl.SSLEngine} driven by {@link VoipDtlsTransport}, not
     *           a third party provider.
     */
    private VoipDtlsTransport handshake(RelayDatagramTransport datagramTransport) {
        try {
            var engine = VoipDtlsCertificates.createEngine(!relayActiveMode, pinnedFingerprint);
            var transport = new VoipDtlsTransport(engine, datagramTransport);
            transport.handshake(DTLS_HANDSHAKE_TIMEOUT_MILLIS);
            return transport;
        } catch (GeneralSecurityException | IOException exception) {
            throw new WhatsAppCallException.DataChannel("relay DTLS handshake failed", exception);
        }
    }

    /**
     * Starts the daemon virtual thread that pumps inbound SCTP messages from the socket to the consumer.
     *
     * <p>The pump blocks in {@code SctpSocket.receive()}; each message's payload is copied out of the
     * socket's lent read only buffer before the next receive, then demultiplexed by
     * {@link #onInboundMessage(int, int, byte[])}. The pump exits cleanly when the association dies (its
     * {@code receive()} throws {@link AssociationAbortedException}, which {@link #close()} triggers by
     * closing the socket) or when the thread is interrupted.
     *
     * @param localSocket the connected socket the pump receives from
     */
    private void startReceivePump(SctpSocket localSocket) {
        this.receivePump = Thread.ofVirtual()
                .name("calls-sctp-rx")
                .start(() -> runReceivePump(localSocket));
    }

    /**
     * The inbound receive pump body: copies each received message's payload out of the socket's lent buffer
     * and hands it to the demux until the association dies or the thread is interrupted.
     *
     * @param localSocket the connected socket to receive from
     */
    private void runReceivePump(SctpSocket localSocket) {
        while (!closed.get()) {
            Message message;
            try {
                message = localSocket.receive();
            } catch (AssociationAbortedException exception) {
                // The association died (peer ABORT, transport failure, or a local close()); stop the pump.
                return;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
            // The payload buffer is lent only until the next receive(), so copy it out immediately.
            var payload = message.payload();
            var bytes = new byte[payload.remaining()];
            payload.get(bytes);
            onInboundMessage(message.streamId().value(), (int) message.payloadProtocol().value(), bytes);
        }
    }

    /**
     * Delivers one inbound SCTP DATA application message to the registered consumer.
     *
     * <p>Only application data is forwarded; the DCEP control PPID is dropped because the channel is
     * pre negotiated and never exchanges a DCEP open or ack. A message that arrives after {@link #close()},
     * or before a consumer is registered, is dropped.
     *
     * @param streamId the SCTP stream the message arrived on
     * @param ppid     the SCTP Payload Protocol Identifier of the message
     * @param payload  the decoded payload bytes, already copied out of the socket's lent buffer
     */
    private void onInboundMessage(int streamId, int ppid, byte[] payload) {
        if (closed.get() || ppid == DcepMessage.PPID_DCEP) {
            return;
        }
        var count = inboundMessages.incrementAndGet();
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "relay data-channel in #{0}: type=0x{1} len={2} ppid={3} stream={4}",
                    count, messageType(payload), payload.length, ppid, streamId);
        }
        var consumer = messageConsumer.get();
        if (consumer != null) {
            consumer.accept(payload);
        }
    }

    /**
     * Formats the leading two bytes of a data channel message as a lowercase hex type tag for the message
     * trace ({@code 0003} subscription, {@code 0103} subscription ack, {@code 0801} keepalive, {@code 0802}
     * pong, {@code 90..} media).
     *
     * @param payload the message bytes
     * @return the two byte type tag, or a short marker for empty or single byte messages
     */
    private static String messageType(byte[] payload) {
        if (payload.length >= 2) {
            return String.format("%02x%02x", payload[0], payload[1]);
        }
        if (payload.length == 1) {
            return String.format("%02x", payload[0]);
        }
        return "empty";
    }

    /**
     * {@inheritDoc}
     *
     * @param message {@inheritDoc}
     * @return {@code true} when SCTP accepted the message for transmission, {@code false} when the channel is
     * not open, is closed, the send buffer had no space, or SCTP rejected it
     * @throws NullPointerException if {@code message} is {@code null}
     */
    @Override
    public boolean send(byte[] message) {
        Objects.requireNonNull(message, "message cannot be null");
        var localStream = stream;
        if (closed.get() || !ready.get() || localStream == null) {
            return false;
        }
        try {
            // Best effort once: unordered, no retransmissions, and never block the producing thread on a
            // congested association (trySend drops instead of waiting when the send buffer is full).
            var accepted = localStream.prepareSend(BINARY_PPID)
                    .ordered(false)
                    .maxRetransmissions(0)
                    .trySend(ByteBuffer.wrap(message));
            var rtp = message.length > 0 && (message[0] & 0xC0) == 0x80;
            if (!rtp) {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "relay data-channel out ctrl: type=0x{0} len={1} accepted={2} buffered={3}",
                            messageType(message), message.length, accepted, bufferedAmount());
                }
            } else {
                var count = outboundMedia.incrementAndGet();
                if ((count == 1 || count % 100 == 0) && Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "relay data-channel out media #{0}: len={1} accepted={2} buffered={3}",
                            count, message.length, accepted, bufferedAmount());
                }
            }
            return accepted;
        } catch (SendException | AssociationAbortedException exception) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "relay data-channel send failed", exception);
            }
            return false;
        }
    }

    /**
     * Returns the number of payload bytes buffered on the pre negotiated stream and not yet handed to the
     * transmission machinery.
     *
     * @return the buffered send amount, in bytes, or {@code 0} when the channel is not open or is closed
     */
    @Override
    public long bufferedAmount() {
        var localStream = stream;
        if (localStream == null || closed.get() || !ready.get()) {
            return 0L;
        }
        try {
            return localStream.bufferedAmount();
        } catch (RuntimeException exception) {
            return 0L;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param consumer {@inheritDoc}
     * @throws NullPointerException if {@code consumer} is {@code null}
     */
    @Override
    public void onMessage(Consumer<byte[]> consumer) {
        this.messageConsumer.set(Objects.requireNonNull(consumer, "consumer cannot be null"));
    }

    /**
     * Feeds one inbound DTLS record from the host socket into the DTLS datagram transport.
     *
     * <p>The record is offered to the inbound queue the {@link RelayDatagramTransport} reads from, so the
     * DTLS receive path can decrypt it. Before {@link #connect()} the handshake itself reads from the same
     * queue, so a record that arrives during the handshake is consumed by it; after the handshake the SCTP
     * socket's reader thread reads from it through the {@link DtlsByteChannel}. After {@link #close()} the
     * record is dropped.
     *
     * @param record the inbound DTLS record bytes
     * @throws NullPointerException if {@code record} is {@code null}
     */
    @Override
    public void feedDtlsRecord(byte[] record) {
        Objects.requireNonNull(record, "record cannot be null");
        if (closed.get()) {
            return;
        }
        var firstInbound = firstInboundDtlsLogged.compareAndSet(false, true);
        if (firstInbound && Log.INFO) {
            LOGGER.log(Level.INFO, "relay first inbound dtls record from relay: {0} bytes", record.length);
        }
        // A non blocking offer keeps the socket reader thread from stalling; a record dropped because the
        // bounded queue is momentarily full is recovered by the DTLS retransmission.
        inbound.offer(record);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} once the DTLS handshake and SCTP connect have completed
     */
    @Override
    public boolean isReady() {
        return ready.get() && !closed.get();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Idempotent: a second call returns at once. It marks the channel closed, wakes any blocked datagram
     * read with a poison record, closes the SCTP socket (which closes the {@link DtlsByteChannel} and with it
     * the DTLS transport, and unblocks the receive pump), and interrupts the receive pump.
     */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        ready.set(false);
        inbound.offer(POISON);
        closeSocketQuietly(socket);
        var pump = receivePump;
        if (pump != null) {
            pump.interrupt();
        }
    }

    /**
     * Closes an SCTP socket, swallowing any teardown fault.
     *
     * @param target the socket to close, or {@code null}
     */
    private static void closeSocketQuietly(SctpSocket target) {
        if (target == null) {
            return;
        }
        try {
            target.close();
        } catch (RuntimeException exception) {
            // The socket teardown is best effort on a failed bring up; a residual fault must not mask the
            // original bring up exception.
        }
    }

    /**
     * Bridges the JDK DTLS record layer to the host UDP egress and the inbound DTLS record queue.
     *
     * <p>{@link VoipDtlsTransport} drives DTLS over this {@link VoipDtlsTransport.Datagrams} seam: it writes one
     * DTLS record per {@link #send(byte[])} and reads one per {@link #receive(int)}. {@link #send(byte[])}
     * forwards the record to {@link RelayDataChannel#egress} addressed to the relay; {@link #receive(int)} polls
     * {@link RelayDataChannel#inbound}, which {@link RelayDataChannel#feedDtlsRecord(byte[])} fills from the host
     * socket reader. The poison record offered on close returns {@code null} so the DTLS layer observes the
     * closed transport.
     */
    private final class RelayDatagramTransport implements VoipDtlsTransport.Datagrams {
        /**
         * {@inheritDoc}
         *
         * <p>Writes one DTLS record to the host egress addressed to the relay. A failed egress send is a
         * best effort loss the DTLS retransmission recovers, so it does not raise.
         *
         * @param record {@inheritDoc}
         */
        @Override
        public void send(byte[] record) {
            if (closed.get()) {
                return;
            }
            var sent = egress.send(record, relayAddress);
            var firstOutbound = firstOutboundDtlsLogged.compareAndSet(false, true);
            if (firstOutbound && Log.INFO) {
                LOGGER.log(Level.INFO, "relay first outbound dtls record (clienthello) to {0}: {1} of {2} bytes accepted",
                        relayAddress, sent, record.length);
            }
        }

        /**
         * {@inheritDoc}
         *
         * <p>Polls the inbound DTLS record queue with the given timeout; a timeout, a closed transport (the
         * poison record), or an empty record returns {@code null} so the DTLS layer retries or observes the
         * close.
         *
         * @param waitMillis {@inheritDoc}
         * @return {@inheritDoc}
         */
        @Override
        public byte[] receive(int waitMillis) {
            if (closed.get()) {
                return null;
            }
            byte[] record;
            try {
                record = inbound.poll(Math.max(1, waitMillis), TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return null;
            }
            if (record == null || record == POISON || record.length == 0) {
                return null;
            }
            return record;
        }
    }
}
