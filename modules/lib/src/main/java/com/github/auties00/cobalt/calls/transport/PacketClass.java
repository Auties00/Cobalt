package com.github.auties00.cobalt.calls.transport;

/**
 * Enumerates the four kinds an inbound transport datagram is classified into by its leading byte before
 * it is routed to the matching subsystem.
 *
 * <p>A single UDP flow toward a WhatsApp relay or a Web P2P peer multiplexes STUN connectivity checks,
 * DTLS handshake and application data records, and SRTP/SRTCP media on one port. The receiver tells them
 * apart by the first byte of each datagram, following the demultiplexing ranges defined by RFC 7983:
 * STUN goes to the binding parser, DTLS (or SCTP over DTLS) goes to the data channel controller, and
 * media goes to the SRTP unprotect path. This enum names those buckets;
 * {@link InboundPacketDemux#classify(byte[])} computes the mapping, and a value of {@link #UNKNOWN} marks
 * a datagram that falls in none of the ranges and is dropped.
 *
 * <p>The leading byte partitions the ranges as follows:
 * <ul>
 * <li>{@code 0..3} and {@code 64..79}: {@link #STUN}</li>
 * <li>{@code 20..63}: {@link #DTLS}</li>
 * <li>{@code 128..191}: {@link #RTP}</li>
 * <li>anything else: {@link #UNKNOWN}</li>
 * </ul>
 */
public enum PacketClass {
    /**
     * Marks a STUN message, recognised by a leading byte in {@code 0..3}, or a TURN ChannelData message in
     * {@code 64..79}.
     *
     * <p>Covers the RFC 5389 binding request or response, including the WhatsApp proprietary relay
     * attributes.
     */
    STUN,

    /**
     * Marks a DTLS record, whether a handshake or application data record, recognised by a leading byte in
     * {@code 20..63}.
     *
     * <p>On the Web P2P path the application data records carry the SCTP DataChannel traffic.
     */
    DTLS,

    /**
     * Marks an SRTP or SRTCP media packet, recognised by a leading byte in {@code 128..191} (a valid RTP
     * version 2 header), carrying audio or video and any piggybacked WARP control bytes.
     */
    RTP,

    /**
     * Marks a datagram whose leading byte falls in none of the recognised ranges.
     *
     * <p>Such datagrams are dropped rather than routed.
     */
    UNKNOWN
}
