package com.github.auties00.cobalt.calls.media.audio.neteq;

import java.util.Objects;
import com.github.auties00.cobalt.calls.media.audio.neteq.decoder.AudioDecoder;

/**
 * Holds one received audio packet as it enters the {@link LiveNetEq} jitter buffer, after transport
 * demultiplexing and, on the group path, SFrame opening.
 *
 * <p>A packet carries its 16 bit RTP {@link #sequenceNumber()}, its RTP {@link #timestamp()}, and its 7 bit
 * RTP {@link #payloadType()} as read from the header, the codec {@link #payload()} bytes, and the local
 * {@link #arrivalMillis()} the transport stamped on receipt. The sequence number drives ordering and gap
 * detection in the {@link PacketBuffer} and {@link NackTracker}; the timestamp drives playout scheduling
 * through the {@link DelayManager}; the payload type resolves the buffered frame to a decoder or to the
 * comfort noise or telephone event role through the {@link LiveNetEq} payload type registry; the arrival
 * time drives the inter arrival histogram. The payload is the plain codec packet the {@link AudioDecoder}
 * consumes; a zero length payload denotes a packet whose discontinuous transmission frame produced no codec
 * bytes.
 *
 * <p>The sequence number is held as an {@code int} in the range {@code 0..65535}; arithmetic comparing two
 * sequence numbers must use the wraparound helper {@link NackTracker#isNewerSequenceNumber(int, int)}
 * rather than a plain integer comparison. The payload type is held as an {@code int} in the range
 * {@code 0..127}, the 7 bit field of the second RTP header byte. The payload array is referenced as supplied
 * and is not copied, so a caller transfers ownership and must not mutate it after constructing the packet.
 *
 * <p>The arrival time is the local clock the transport layer records on receipt, not a wire field. WhatsApp
 * negotiates a single audio payload type on the wire ({@code <audio enc="opus" rate="...">}, with no separate
 * comfort noise or telephone event line), so on a WhatsApp call this field carries the one Opus payload type
 * on every audio packet; it remains the genuine RTP header value the depacketizer reads.
 *
 * @param sequenceNumber the packet's 16 bit RTP sequence number, in {@code 0..65535}
 * @param timestamp      the packet's RTP timestamp, in the codec sample clock
 * @param payloadType    the packet's 7 bit RTP payload type, in {@code 0..127}
 * @param payload        the codec payload bytes; never {@code null}, possibly empty
 * @param arrivalMillis  the local monotonic time the packet arrived, in milliseconds
 */
public record RtpAudioPacket(int sequenceNumber, long timestamp, int payloadType, byte[] payload, long arrivalMillis) {
    /**
     * The inclusive upper bound of a 16 bit RTP sequence number, {@code 0xFFFF}.
     */
    public static final int MAX_SEQUENCE_NUMBER = 0xFFFF;

    /**
     * The inclusive upper bound of a 7 bit RTP payload type, {@code 0x7F}.
     */
    public static final int MAX_PAYLOAD_TYPE = 0x7F;

    /**
     * Validates the packet, rejecting an out of range sequence number or payload type or a {@code null}
     * payload.
     *
     * <p>The payload reference is retained as supplied and is neither copied nor cloned. No constraint is
     * placed on the payload length: an empty payload is a legal discontinuous transmission packet.
     *
     * @throws NullPointerException     if {@code payload} is {@code null}
     * @throws IllegalArgumentException if {@code sequenceNumber} is outside {@code 0..65535} or
     *                                  {@code payloadType} is outside {@code 0..127}
     */
    public RtpAudioPacket {
        Objects.requireNonNull(payload, "payload cannot be null");
        if (sequenceNumber < 0 || sequenceNumber > MAX_SEQUENCE_NUMBER) {
            throw new IllegalArgumentException("sequenceNumber must be in 0..65535, got " + sequenceNumber);
        }
        if (payloadType < 0 || payloadType > MAX_PAYLOAD_TYPE) {
            throw new IllegalArgumentException("payloadType must be in 0..127, got " + payloadType);
        }
    }
}
