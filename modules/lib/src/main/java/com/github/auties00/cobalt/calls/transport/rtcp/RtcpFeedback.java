package com.github.auties00.cobalt.calls.transport.rtcp;

/**
 * Carries the congestion control feedback extracted from one inbound RTCP compound packet, the fused input
 * the sender side bandwidth estimator and rate control loop consume.
 *
 * <p>A single RTCP packet may contain a Receiver Report, a receiver estimated maximum bitrate (REMB)
 * message, a picture loss indication or full intra request, and other records;
 * {@link RtcpFeedbackParser#parse(byte[], int)} fuses the recognised ones into one instance. Each numeric
 * field uses a negative sentinel to mean that the feedback carried no such signal: {@code -1} for
 * {@link #fractionLost()}, {@link #cumulativeLost()}, {@link #rttNs()}, and {@link #remoteBweBps()};
 * {@link #keyFrameRequested()} is {@code false} when no key frame request was present. The rate control
 * loop that consumes this reads only the fields a given feedback actually populated and leaves the rest of
 * its state unchanged, and the media session arms the encoder when a key frame was requested.
 *
 * @implNote This implementation is a transport layer value object rather than a protobuf wire model; each
 *           instance fuses the recognised records of one inbound RTCP compound packet. The sender rate
 *           controller's minimum remote bitrate floor is deliberately not a field here: that floor is the
 *           minimum, taken across all connected participants, of each participant's reported remote
 *           bandwidth estimate, so it belongs to the participant roster layer rather than this per packet
 *           feedback. The single packet REMB estimate this record does carry is {@link #remoteBweBps()}.
 *
 * @param fractionLost  the fraction of packets lost since the previous report, in {@code [0, 1]}, or
 *                      {@code -1.0} when no report contributed it
 * @param cumulativeLost the signed cumulative number of packets lost over the session, or {@code -1} when
 *                      no report contributed it
 * @param rttNs         the round trip time in nanoseconds derived from the last sender report and the
 *                      delay since last sender report fields, or {@code -1} when no plausible estimate was
 *                      available
 * @param remoteBweBps  the remote receiver's estimated maximum bitrate in bits per second from a REMB
 *                      message, or {@code -1} when none was present
 * @param arrivalMs     the local monotonic arrival timestamp in milliseconds, captured when the packet was
 *                      parsed, used to age the feedback and detect a stale or absent feedback stream
 * @param keyFrameRequested whether the packet carried a picture loss indication (PSFB PLI) or full intra
 *                      request (PSFB FIR) asking the local encoder to emit a key frame
 */
public record RtcpFeedback(double fractionLost,
                           long cumulativeLost,
                           long rttNs,
                           long remoteBweBps,
                           long arrivalMs,
                           boolean keyFrameRequested) {
    /**
     * Returns whether a REMB remote bandwidth estimate is present in this feedback.
     *
     * @return {@code true} when {@link #remoteBweBps()} carries a non negative estimate
     */
    public boolean hasRemoteBwe() {
        return remoteBweBps >= 0;
    }

    /**
     * Returns whether a round trip time estimate is present in this feedback.
     *
     * @return {@code true} when {@link #rttNs()} carries a non negative estimate
     */
    public boolean hasRtt() {
        return rttNs >= 0;
    }

    /**
     * Returns whether a packet loss fraction is present in this feedback.
     *
     * @return {@code true} when {@link #fractionLost()} carries a non negative fraction
     */
    public boolean hasLoss() {
        return fractionLost >= 0;
    }
}
