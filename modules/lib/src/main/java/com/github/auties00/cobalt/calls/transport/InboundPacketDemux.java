package com.github.auties00.cobalt.calls.transport;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Classifies each inbound datagram by its leading byte and routes it to the STUN, DTLS, or media handler,
 * dropping anything that matches none of the recognised ranges.
 *
 * <p>A WhatsApp Web call multiplexes traffic at two layers, and this demux serves both. At the socket
 * layer it splits inbound UDP datagrams into STUN connectivity checks (routed to the ICE agent) and DTLS
 * records (routed to the DTLS/SCTP bridge). At the data channel layer, once the SCTP data channel is open,
 * each inbound SCTP DATA message is itself a packet classified by its leading byte (hop by hop SRTP media,
 * RTCP, or an application STUN keepalive), and the same classifier splits it again; inbound WARP control
 * rides piggybacked on the tail of an RTP packet and so arrives through the media class rather than a
 * leading byte of its own. The STUN handler is a {@link StunHandler} that also receives the datagram's
 * source {@link SocketAddress} so the ICE agent can answer a binding request; the DTLS, media, and RTCP
 * handlers take only the bytes. A source of {@code null} is passed when the bytes did not arrive on a
 * socket (the data channel layer).
 *
 * <p>The RTP/RTCP class ({@link PacketClass#RTP}, leading byte {@code 128..191}) is shared by media and
 * RTCP, which the leading byte alone cannot tell apart. After classifying a datagram as
 * {@link PacketClass#RTP} the demux inspects the second byte (the RTP payload type, or the RTCP payload
 * type when the version bits are set) and routes a value in {@value #RTCP_PT_MIN}{@code ..}
 * {@value #RTCP_PT_MAX} (the RTCP payload type range covering SR {@code 200}, RR {@code 201}, SDES
 * {@code 202}, BYE {@code 203}, APP {@code 204}, RTPFB {@code 205}, and PSFB {@code 206}, the last
 * carrying REMB) to the {@code rtcpHandler}; every other second byte is media and goes to the
 * {@code mediaHandler}. The {@code rtcpHandler} is an additive sink that defaults to doing nothing, so a
 * demux constructed without one drops RTCP and behaves identically for the media path.
 *
 * <p>The handlers are invoked synchronously on whichever thread delivers the datagram. Each invocation is
 * guarded so a throwing handler cannot stop the demux from classifying the next datagram, matching the
 * Cobalt invariant that one malformed packet never breaks the receive path. A {@code null} handler for a
 * class means datagrams of that class are dropped.
 *
 * @implNote This implementation classifies by the RFC 7983 leading byte ranges that WebRTC stacks share:
 * STUN {@code 0..3}, TURN ChannelData {@code 64..79}, DTLS {@code 20..63}, and RTP/RTCP {@code 128..191}.
 * STUN goes to the binding parser, a DTLS record is dispatched to the data channel controller, and media
 * is routed to the hop by hop SRTP unprotect path. The second byte RTP/RTCP split disambiguates the shared
 * media range once the leg is decrypted, where an RTCP payload type in {@code 200..213} routes to the
 * feedback path rather than the media depacketizer.
 */
public final class InboundPacketDemux {
    /**
     * The logger for {@link InboundPacketDemux}.
     */
    private static final System.Logger LOGGER = Log.get(InboundPacketDemux.class);

    /**
     * Receives one inbound STUN datagram together with the transport address it arrived from.
     *
     * <p>The source address is needed so the ICE agent can answer a binding request: the requester's
     * reflexive address is echoed in the {@code XOR-MAPPED-ADDRESS} and the response is sent back to it. A
     * {@code null} source is passed when the STUN message did not arrive on a socket (an application STUN
     * keepalive carried as SCTP DATA on the data channel).
     */
    @FunctionalInterface
    public interface StunHandler {
        /**
         * Handles one inbound STUN datagram.
         *
         * @param datagram the STUN message bytes
         * @param source   the transport address the datagram arrived from, or {@code null} when it did not
         *                 arrive on a socket
         */
        void accept(byte[] datagram, SocketAddress source);
    }

    /**
     * Holds the inclusive upper bound of the STUN leading byte range.
     *
     * @implNote This implementation uses {@code 3}: RFC 7983 assigns leading bytes {@code 0..3} to STUN
     * because a STUN message begins with two zero bits followed by the 14 bit message type.
     */
    private static final int STUN_MAX = 3;

    /**
     * Holds the inclusive lower bound of the TURN ChannelData leading byte range.
     *
     * @implNote This implementation uses {@code 64}, the start of the RFC 7983 TURN ChannelData range
     * {@code 64..79}; Cobalt routes ChannelData to the STUN handler because it is connectivity plane
     * traffic.
     */
    private static final int CHANNEL_DATA_MIN = 64;

    /**
     * Holds the inclusive upper bound of the TURN ChannelData leading byte range.
     *
     * @implNote This implementation uses {@code 79}, the end of the RFC 7983 TURN ChannelData range
     * {@code 64..79}.
     */
    private static final int CHANNEL_DATA_MAX = 79;

    /**
     * Holds the inclusive lower bound of the DTLS leading byte range.
     *
     * @implNote This implementation uses {@code 20}, the start of the RFC 7983 DTLS range {@code 20..63};
     * the first byte of a DTLS record is its {@code ContentType}, which falls in this window.
     */
    private static final int DTLS_MIN = 20;

    /**
     * Holds the inclusive upper bound of the DTLS leading byte range.
     *
     * @implNote This implementation uses {@code 63}, the end of the RFC 7983 DTLS range {@code 20..63}.
     */
    private static final int DTLS_MAX = 63;

    /**
     * Holds the inclusive lower bound of the RTP/RTCP leading byte range.
     *
     * @implNote This implementation uses {@code 128}, the start of the RFC 7983 media range
     * {@code 128..191}; a valid RTP version 2 header sets the top two bits of the first byte.
     */
    private static final int RTP_MIN = 128;

    /**
     * Holds the inclusive upper bound of the RTP/RTCP leading byte range.
     *
     * @implNote This implementation uses {@code 191}, the end of the RFC 7983 media range {@code 128..191}.
     */
    private static final int RTP_MAX = 191;

    /**
     * Holds the inclusive lower bound of the RTCP payload type range within the shared RTP/RTCP class.
     *
     * @implNote This implementation uses {@code 200}, the first RTCP payload type (Sender Report); a
     * datagram in the RTP/RTCP leading byte class whose second byte is below this is RTP media, since an
     * RTP payload type never reaches {@code 200} in a WhatsApp call.
     */
    private static final int RTCP_PT_MIN = 200;

    /**
     * Holds the inclusive upper bound of the RTCP payload type range within the shared RTP/RTCP class.
     *
     * @implNote This implementation uses {@code 213}, the top of the assigned RTCP payload type block;
     * the values actually used are SR {@code 200}, RR {@code 201}, SDES {@code 202}, BYE {@code 203}, APP
     * {@code 204}, RTPFB {@code 205}, and PSFB {@code 206}, but the full {@code 200..213} window is
     * accepted so any RTCP record type routes to the feedback path rather than the media depacketizer.
     */
    private static final int RTCP_PT_MAX = 213;

    /**
     * Holds the handler for STUN and TURN ChannelData datagrams, or {@code null} to drop them.
     */
    private final StunHandler stunHandler;

    /**
     * Holds the handler for DTLS and SCTP over DTLS datagrams, or {@code null} to drop them.
     */
    private final Consumer<byte[]> dtlsHandler;

    /**
     * Holds the handler for SRTP media datagrams (the RTP half of the shared class), or {@code null} to
     * drop them.
     */
    private final Consumer<byte[]> mediaHandler;

    /**
     * Holds the handler for SRTCP datagrams (the RTCP half of the shared class, second byte in
     * {@value #RTCP_PT_MIN}{@code ..}{@value #RTCP_PT_MAX}), never {@code null}; defaults to doing nothing
     * so RTCP is dropped when no handler was supplied.
     */
    private final Consumer<byte[]> rtcpHandler;

    /**
     * Constructs a demux wiring the STUN, DTLS, and media handlers, with RTCP datagrams dropped.
     *
     * <p>A datagram in the RTP/RTCP class whose second byte is an RTCP payload type is dropped. Use
     * {@link #InboundPacketDemux(StunHandler, Consumer, Consumer, Consumer)} to route RTCP to a feedback
     * handler.
     *
     * @param stunHandler  the handler for STUN and ChannelData datagrams, or {@code null} to drop them
     * @param dtlsHandler  the handler for DTLS and SCTP datagrams, or {@code null} to drop them
     * @param mediaHandler the handler for SRTP media datagrams, or {@code null} to drop them
     */
    public InboundPacketDemux(StunHandler stunHandler,
                              Consumer<byte[]> dtlsHandler,
                              Consumer<byte[]> mediaHandler) {
        this(stunHandler, dtlsHandler, mediaHandler, null);
    }

    /**
     * Constructs a demux wiring the STUN, DTLS, media, and RTCP handlers.
     *
     * <p>The {@code rtcpHandler} receives the RTCP half of the shared RTP/RTCP class (a datagram whose
     * second byte falls in {@value #RTCP_PT_MIN}{@code ..}{@value #RTCP_PT_MAX}); a {@code null}
     * {@code rtcpHandler} is treated as doing nothing so RTCP is dropped, leaving the media path unchanged.
     *
     * @param stunHandler  the handler for STUN and ChannelData datagrams, or {@code null} to drop them
     * @param dtlsHandler  the handler for DTLS and SCTP datagrams, or {@code null} to drop them
     * @param mediaHandler the handler for SRTP media datagrams, or {@code null} to drop them
     * @param rtcpHandler  the handler for SRTCP datagrams, or {@code null} to drop them
     */
    public InboundPacketDemux(StunHandler stunHandler,
                              Consumer<byte[]> dtlsHandler,
                              Consumer<byte[]> mediaHandler,
                              Consumer<byte[]> rtcpHandler) {
        this.stunHandler = stunHandler;
        this.dtlsHandler = dtlsHandler;
        this.mediaHandler = mediaHandler;
        this.rtcpHandler = rtcpHandler == null ? _ -> {
        } : rtcpHandler;
    }

    /**
     * Classifies one inbound datagram by its leading byte per the RFC 7983 ranges.
     *
     * <p>Returns {@link PacketClass#STUN} for a leading byte in {@code 0..3} or {@code 64..79},
     * {@link PacketClass#DTLS} for {@code 20..63}, {@link PacketClass#RTP} for {@code 128..191}, and
     * {@link PacketClass#UNKNOWN} for an empty datagram or any other leading byte.
     *
     * @param packet the datagram bytes
     * @return the class the datagram falls into
     * @throws NullPointerException if {@code packet} is {@code null}
     */
    public static PacketClass classify(byte[] packet) {
        Objects.requireNonNull(packet, "packet cannot be null");
        if (packet.length == 0) {
            return PacketClass.UNKNOWN;
        }
        var b0 = packet[0] & 0xFF;
        if (b0 >= RTP_MIN && b0 <= RTP_MAX) {
            return PacketClass.RTP;
        }
        if (b0 <= STUN_MAX || (b0 >= CHANNEL_DATA_MIN && b0 <= CHANNEL_DATA_MAX)) {
            return PacketClass.STUN;
        }
        if (b0 >= DTLS_MIN && b0 <= DTLS_MAX) {
            return PacketClass.DTLS;
        }
        return PacketClass.UNKNOWN;
    }

    /**
     * Routes one inbound datagram to the handler for its {@link PacketClass}, splitting the shared
     * RTP/RTCP class into media and RTCP by the datagram's second byte.
     *
     * <p>An empty datagram and a datagram that classifies as {@link PacketClass#UNKNOWN} are dropped. A
     * {@link PacketClass#STUN} datagram is handed to the STUN handler with its source address so the ICE
     * agent can answer a binding request. A {@link PacketClass#RTP} datagram is routed to the
     * {@code rtcpHandler} when its second byte falls in {@value #RTCP_PT_MIN}{@code ..}
     * {@value #RTCP_PT_MAX} and to the {@code mediaHandler} otherwise; a one byte RTP/RTCP datagram has no
     * second byte to inspect and is treated as media. Each handler call is guarded so a throwing callback
     * cannot break the demux.
     *
     * <p>The returned {@link PacketClass} is the leading byte class only: an RTCP datagram still returns
     * {@link PacketClass#RTP}, because the second byte split is a routing decision within that class and
     * does not change the RFC 7983 classification {@link #classify(byte[])} reports.
     *
     * @param packet the inbound datagram bytes
     * @param source the transport address the datagram arrived from, or {@code null} when it did not
     *               arrive on a socket
     * @return the class the datagram was routed as, for callers that want to observe the decision
     * @throws NullPointerException if {@code packet} is {@code null}
     */
    public PacketClass accept(byte[] packet, SocketAddress source) {
        var packetClass = classify(packet);
        if (Log.TRACE) LOGGER.log(Level.TRACE, "classified datagram as {0}, {1} bytes", packetClass, packet.length);
        switch (packetClass) {
            case STUN -> dispatchStun(packet, source);
            case DTLS -> dispatch(dtlsHandler, packet);
            case RTP -> {
                if (isRtcp(packet)) {
                    dispatch(rtcpHandler, packet);
                } else {
                    dispatch(mediaHandler, packet);
                }
            }
            case UNKNOWN -> {
            }
        }
        return packetClass;
    }

    /**
     * Returns whether an RTP/RTCP class datagram is RTCP rather than media, by its second byte.
     *
     * <p>A datagram already known to be in the {@link PacketClass#RTP} leading byte class is RTCP when it
     * has a second byte and that byte (the RTCP payload type) falls in {@value #RTCP_PT_MIN}{@code ..}
     * {@value #RTCP_PT_MAX}; a single byte datagram cannot be a valid RTCP record and is treated as media.
     *
     * @param packet the datagram bytes, already classified as {@link PacketClass#RTP}
     * @return {@code true} when the datagram is RTCP, {@code false} when it is media
     */
    private static boolean isRtcp(byte[] packet) {
        if (packet.length < 2) {
            return false;
        }
        var secondByte = packet[1] & 0xFF;
        return secondByte >= RTCP_PT_MIN && secondByte <= RTCP_PT_MAX;
    }

    /**
     * Delivers a STUN datagram to the STUN handler with its source, guarding against a throwing or
     * {@code null} handler.
     *
     * @param packet the datagram bytes
     * @param source the transport address the datagram arrived from, or {@code null}
     */
    private void dispatchStun(byte[] packet, SocketAddress source) {
        if (stunHandler == null) {
            return;
        }
        try {
            stunHandler.accept(packet, source);
        } catch (Throwable throwable) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "stun handler threw", throwable);
        }
    }

    /**
     * Delivers a datagram to a handler, guarding against a throwing handler and a {@code null} handler.
     *
     * @param handler the handler to invoke, or {@code null} to drop the datagram
     * @param packet  the datagram bytes
     */
    private static void dispatch(Consumer<byte[]> handler, byte[] packet) {
        if (handler == null) {
            return;
        }
        try {
            handler.accept(packet);
        } catch (Throwable throwable) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "packet handler threw", throwable);
        }
    }
}
