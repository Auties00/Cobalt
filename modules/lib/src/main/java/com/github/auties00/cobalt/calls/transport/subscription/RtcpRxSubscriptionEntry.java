package com.github.auties00.cobalt.calls.transport.subscription;

import java.util.Objects;

/**
 * One RTCP feedback subscription toward the relay.
 *
 * <p>A client tells the selective forwarding unit which RTCP feedback it wants forwarded
 * for a given media stream by registering an entry in the hop by hop feedback table. Each
 * entry binds the {@linkplain #mediaSsrc() subscribed media SSRC} to the
 * {@linkplain #peerSsrc() owning peer SSRC} and a bitmask of the {@linkplain #flags()
 * feedback kinds} the subscriber wants relayed, drawn from {@link #FLAG_NACK},
 * {@link #FLAG_PLI}, and {@link #FLAG_FIR}. The relay then forwards only the selected
 * feedback packets between the two SSRCs instead of every report.
 *
 * @param peerSsrc  the SSRC of the peer that owns the feedback relationship
 * @param mediaSsrc the media SSRC the feedback applies to
 * @param flags     the bitmask of requested feedback kinds
 * @implNote This implementation models one slot of the relay feedback table with the
 * following layout, where each slot spans {@code 0x60} bytes:
 * {@snippet lang="text" :
 * offset 0  : occupancy flag (whether the slot is in use)
 * offset 4  : peer SSRC
 * offset 8  : media SSRC
 * offset 12 : feedback flags
 * }
 * The occupancy flag is not carried on this record because table occupancy is tracked by
 * {@link RtcpRxSubscriptionTable} through the presence of an entry rather than an in band
 * byte.
 */
public record RtcpRxSubscriptionEntry(int peerSsrc, int mediaSsrc, int flags) {
    // TODO: confirm the exact bit position of each feedback flag against the relay; the
    //  distinct single bits below preserve the ordering but may not match the wire values.
    /**
     * Feedback flag selecting negative acknowledgement (NACK) packets.
     *
     * <p>When set in {@link #flags()} the relay forwards NACK feedback so the sender can
     * retransmit the lost packets the subscriber reports missing.
     */
    public static final int FLAG_NACK = 0x01;

    /**
     * Feedback flag selecting picture loss indication (PLI) packets.
     *
     * <p>When set in {@link #flags()} the relay forwards PLI feedback so the video sender
     * emits a fresh key frame when the subscriber's decoder loses reference frames.
     */
    public static final int FLAG_PLI = 0x02;

    /**
     * Feedback flag selecting full intra request (FIR) packets.
     *
     * <p>When set in {@link #flags()} the relay forwards FIR feedback so the video sender
     * produces a decoder refresh point on demand.
     */
    public static final int FLAG_FIR = 0x04;

    /**
     * Returns whether NACK feedback is requested by this entry.
     *
     * @return {@code true} when {@link #FLAG_NACK} is set in {@link #flags()}
     */
    public boolean wantsNack() {
        return (flags & FLAG_NACK) != 0;
    }

    /**
     * Returns whether PLI feedback is requested by this entry.
     *
     * @return {@code true} when {@link #FLAG_PLI} is set in {@link #flags()}
     */
    public boolean wantsPli() {
        return (flags & FLAG_PLI) != 0;
    }

    /**
     * Returns whether FIR feedback is requested by this entry.
     *
     * @return {@code true} when {@link #FLAG_FIR} is set in {@link #flags()}
     */
    public boolean wantsFir() {
        return (flags & FLAG_FIR) != 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(peerSsrc, mediaSsrc, flags);
    }

    @Override
    public String toString() {
        return "RtcpRxSubscriptionEntry[peerSsrc=" + peerSsrc
                + ", mediaSsrc=" + mediaSsrc
                + ", flags=0x" + Integer.toHexString(flags) + ']';
    }
}
