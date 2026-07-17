package com.github.auties00.cobalt.calls.transport.rtcp;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Accumulates per remote stream RFC 3550 reception statistics so the outbound Sender Report can carry one
 * reception report block per remote synchronization source the relay leg receives.
 *
 * <p>For each remote SSRC the tracker maintains the sequence number bookkeeping (the base and highest
 * sequence numbers and the rollover counter cycles), the received packet count, the running interarrival
 * jitter estimate, and the timestamp of the most recent Sender Report received from that source. From this
 * state {@link #reportBlocks(long)} computes, once per outbound report, the fraction lost over the interval
 * since the previous report, the signed cumulative loss, the rollover extended highest sequence number, the
 * interarrival jitter, and the last SR and delay since last SR fields the report block conveys.
 *
 * <p>The tracker is fed from the transport's inbound paths: {@link #onRtpReceived} is called for every
 * inbound media packet (whose RTP header rides in the clear ahead of the SRTP ciphertext), and
 * {@link #recordInboundSr} is called for every inbound Sender Report so the last SR and delay since last SR
 * round trip fields can be reflected back. Those paths run on the data channel receive thread while
 * {@link #reportBlocks(long)} runs on the report thread, so every method is synchronized on the instance.
 *
 * @implNote This implementation reproduces the receiver side statistics RFC 3550 specifies in its appendix
 *           sequence number validity, loss, and jitter routines, matching the report blocks WhatsApp's
 *           embedded RTCP stack emits. The interarrival jitter is computed in the stream's RTP timestamp
 *           units from the local arrival clock scaled to those units; the stream clock rate is supplied per
 *           packet by the caller, which knows it from the payload type. No provenance annotation is carried
 *           because the underlying RTCP stack is an upstream third party library rather than a WhatsApp Web
 *           or Mobile source export.
 */
public final class RtcpReceptionStats {
    /**
     * The logger for {@link RtcpReceptionStats}.
     */
    private static final System.Logger LOGGER = Log.get(RtcpReceptionStats.class);

    /**
     * Holds the number of distinct values a sixteen bit RTP sequence number takes, the wrap around modulus.
     */
    private static final long RTP_SEQ_MOD = 1L << 16;

    /**
     * Holds the forward sequence number jump beyond which a packet is treated as a large gap rather than an
     * in order advance.
     *
     * @implNote This implementation uses {@code 3000}, the RFC 3550 {@code MAX_DROPOUT} default.
     */
    private static final int MAX_DROPOUT = 3000;

    /**
     * Holds the backward sequence number reach within which a packet is treated as a duplicate or reorder
     * rather than a stream restart.
     *
     * @implNote This implementation uses {@code 100}, the RFC 3550 {@code MAX_MISORDER} default.
     */
    private static final int MAX_MISORDER = 100;

    /**
     * Holds the number of compact NTP units in one second, the delay since last SR scale.
     *
     * @implNote This implementation uses {@code 65536}: the delay since last SR field is expressed in units
     * of {@code 1/65536} seconds per RFC 3550.
     */
    private static final long DLSR_UNITS_PER_SECOND = 65536L;

    /**
     * Holds the per remote SSRC reception records in first seen order so the report blocks are emitted in a
     * stable order.
     */
    private final Map<Integer, Stream> streams = new LinkedHashMap<>();

    /**
     * Records one inbound RTP packet's header fields against its stream's reception statistics.
     *
     * <p>The first packet from a source seeds its sequence bookkeeping; later packets advance the highest
     * sequence number across the sixteen bit rollover, count the reception, and fold the packet into the
     * interarrival jitter estimate. The RTP header rides in the clear ahead of the SRTP ciphertext, so this
     * is fed the protected packet's header fields directly.
     *
     * @param ssrc         the stream synchronization source
     * @param sequence     the packet's sixteen bit RTP sequence number
     * @param rtpTimestamp the packet's thirty two bit RTP timestamp
     * @param arrivalNanos the local monotonic arrival time, in nanoseconds
     * @param clockRateHz  the stream's RTP timestamp clock rate, in hertz, used to scale the jitter estimate
     */
    public synchronized void onRtpReceived(int ssrc, int sequence, long rtpTimestamp, long arrivalNanos, int clockRateHz) {
        var newStream = !streams.containsKey(ssrc);
        var stream = streams.computeIfAbsent(ssrc, _ -> new Stream(ssrc, sequence & 0xFFFF, clockRateHz));
        if (newStream && Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "rtcp reception stats tracking new stream, ssrc={0} clockRateHz={1}",
                    ssrc, clockRateHz);
        }
        stream.onRtp(sequence & 0xFFFF, rtpTimestamp & 0xFFFFFFFFL, arrivalNanos);
    }

    /**
     * Records the timestamp of one inbound Sender Report so the next outbound report block can reflect its
     * last SR and delay since last SR round trip fields.
     *
     * <p>The last SR field is the middle thirty two bits of the report's sixty four bit NTP timestamp; the
     * arrival time is captured so the delay since the report can be measured when the outbound block is
     * built. A report from a source no RTP has yet been received from is ignored, since a report block is
     * only emitted for a stream the tracker has reception statistics for.
     *
     * @param ssrc         the synchronization source the inbound Sender Report came from
     * @param ntpTimestamp the sixty four bit NTP timestamp the inbound Sender Report carried
     * @param arrivalNanos the local monotonic arrival time of the report, in nanoseconds
     */
    public synchronized void recordInboundSr(int ssrc, long ntpTimestamp, long arrivalNanos) {
        var stream = streams.get(ssrc);
        if (stream != null) {
            stream.recordSr(ntpTimestamp, arrivalNanos);
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "rtcp inbound sender report recorded, ssrc={0}", ssrc);
            }
        } else if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "rtcp inbound sender report ignored, no rtp yet for ssrc={0}", ssrc);
        }
    }

    /**
     * Computes one reception report block per tracked remote stream as of the given report time.
     *
     * <p>Each block carries the fraction lost over the interval since this method last ran, the signed
     * cumulative loss, the rollover extended highest sequence number, the interarrival jitter, and the
     * last SR and delay since last SR fields. Because the fraction lost computation consumes the interval
     * since the previous report, this method advances each stream's report state and so must be called
     * exactly once per outbound report round.
     *
     * @param nowNanos the local monotonic report time, in nanoseconds, used to measure delay since last SR
     * @return the report blocks, one per tracked remote stream in first seen order
     */
    public synchronized List<RtcpReportBuilder.ReportBlock> reportBlocks(long nowNanos) {
        var blocks = new ArrayList<RtcpReportBuilder.ReportBlock>(streams.size());
        for (var stream : streams.values()) {
            blocks.add(stream.toBlock(nowNanos));
        }
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "rtcp report blocks computed, count={0}", blocks.size());
        }
        return blocks;
    }

    /**
     * Returns the number of remote streams the tracker has reception statistics for.
     *
     * @return the count of distinct remote synchronization sources received
     */
    synchronized int trackedStreamCount() {
        return streams.size();
    }

    /**
     * Holds one remote stream's mutable reception statistics.
     *
     * <p>This is the per SSRC accumulator advanced in place on the receive thread and snapshotted into an
     * immutable {@link RtcpReportBuilder.ReportBlock} when an outbound report is built.
     */
    private static final class Stream {
        /**
         * Holds the stream synchronization source.
         */
        private final int ssrc;

        /**
         * Holds the stream's RTP timestamp clock rate, in hertz, used to scale the jitter estimate.
         */
        private final int clockRateHz;

        /**
         * Holds the first sequence number received, the base from which the expected packet count is
         * measured.
         */
        private final int baseSeq;

        /**
         * Holds the highest sixteen bit sequence number received so far.
         */
        private int maxSeq;

        /**
         * Holds the accumulated count of sequence number rollovers, shifted into the high bits of the
         * extended sequence number.
         */
        private long cycles;

        /**
         * Holds the count of packets received from the stream.
         */
        private long received;

        /**
         * Holds the expected packet count at the previous report, the basis for the loss interval fraction.
         */
        private long expectedPrior;

        /**
         * Holds the received packet count at the previous report, the basis for the loss interval fraction.
         */
        private long receivedPrior;

        /**
         * Holds the running interarrival jitter estimate, in RTP timestamp units.
         */
        private double jitter;

        /**
         * Holds the previous packet's transit time, in RTP timestamp units, the basis for the jitter delta.
         */
        private long lastTransit;

        /**
         * Holds whether a transit time has been observed, so the first packet seeds the jitter rather than
         * folding a meaningless delta.
         */
        private boolean haveTransit;

        /**
         * Holds the middle thirty two bits of the most recent inbound Sender Report's NTP timestamp, or
         * {@code 0} when none has been received.
         */
        private long lastSr;

        /**
         * Holds the local monotonic arrival time of the most recent inbound Sender Report, in nanoseconds.
         */
        private long lastSrArrivalNanos;

        /**
         * Holds whether an inbound Sender Report has been received, so the last SR and delay fields are
         * emitted only when meaningful.
         */
        private boolean haveSr;

        /**
         * Constructs a reception record seeded by a stream's first packet.
         *
         * @param ssrc        the stream synchronization source
         * @param firstSeq    the first sixteen bit sequence number received
         * @param clockRateHz the stream's RTP timestamp clock rate, in hertz
         */
        private Stream(int ssrc, int firstSeq, int clockRateHz) {
            this.ssrc = ssrc;
            this.clockRateHz = clockRateHz;
            this.baseSeq = firstSeq;
            this.maxSeq = firstSeq;
        }

        /**
         * Folds one inbound packet into the sequence bookkeeping and the jitter estimate.
         *
         * @param sequence     the packet's sixteen bit sequence number
         * @param rtpTimestamp the packet's RTP timestamp
         * @param arrivalNanos the local monotonic arrival time, in nanoseconds
         */
        private void onRtp(int sequence, long rtpTimestamp, long arrivalNanos) {
            var delta = (sequence - maxSeq) & 0xFFFF;
            if (delta < MAX_DROPOUT) {
                if (sequence < maxSeq) {
                    cycles += RTP_SEQ_MOD;
                }
                maxSeq = sequence;
            } else if (delta > RTP_SEQ_MOD - MAX_MISORDER) {
                // A small backward step is a duplicate or reorder; it is still a reception but does not
                // advance the highest sequence number.
            }
            // A jump outside both windows is a source restart; it is counted as a reception so the loss
            // fraction self corrects on the following interval rather than reseeding the base sequence.
            received++;
            var arrivalRtpUnits = arrivalNanos * clockRateHz / 1_000_000_000L;
            var transit = arrivalRtpUnits - rtpTimestamp;
            if (haveTransit) {
                var d = transit - lastTransit;
                if (d < 0) {
                    d = -d;
                }
                jitter += (d - jitter) / 16.0;
            }
            lastTransit = transit;
            haveTransit = true;
        }

        /**
         * Records one inbound Sender Report's last SR timestamp and arrival.
         *
         * @param ntpTimestamp the sixty four bit NTP timestamp the report carried
         * @param arrivalNanos the local monotonic arrival time, in nanoseconds
         */
        private void recordSr(long ntpTimestamp, long arrivalNanos) {
            lastSr = (ntpTimestamp >>> 16) & 0xFFFFFFFFL;
            lastSrArrivalNanos = arrivalNanos;
            haveSr = true;
        }

        /**
         * Snapshots the current statistics into an immutable report block and advances the report state.
         *
         * @param nowNanos the local monotonic report time, in nanoseconds
         * @return the report block for this stream
         */
        private RtcpReportBuilder.ReportBlock toBlock(long nowNanos) {
            var extendedMax = cycles + maxSeq;
            var expected = extendedMax - baseSeq + 1;
            var lost = expected - received;
            if (lost > 0x7FFFFF) {
                lost = 0x7FFFFF;
            } else if (lost < -0x800000) {
                lost = -0x800000;
            }
            var expectedInterval = expected - expectedPrior;
            var receivedInterval = received - receivedPrior;
            expectedPrior = expected;
            receivedPrior = received;
            var lostInterval = expectedInterval - receivedInterval;
            int fraction;
            if (expectedInterval == 0 || lostInterval <= 0) {
                fraction = 0;
            } else {
                fraction = (int) ((lostInterval << 8) / expectedInterval);
            }
            long delaySinceLastSr = 0;
            if (haveSr) {
                var elapsedNanos = nowNanos - lastSrArrivalNanos;
                if (elapsedNanos < 0) {
                    elapsedNanos = 0;
                }
                delaySinceLastSr = elapsedNanos * DLSR_UNITS_PER_SECOND / 1_000_000_000L;
            }
            return new RtcpReportBuilder.ReportBlock(
                    ssrc, fraction & 0xFF, lost, extendedMax, (long) jitter, lastSr, delaySinceLastSr);
        }
    }
}
