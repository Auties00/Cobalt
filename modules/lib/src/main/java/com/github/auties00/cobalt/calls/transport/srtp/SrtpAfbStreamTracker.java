package com.github.auties00.cobalt.calls.transport.srtp;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SrtpAfbStreamInfo;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SrtpAfbStreamInfoBuilder;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SrtpAfbStreams;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SrtpAfbStreamsBuilder;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tracks, per SSRC, the highest RTP and RTCP packet indices a hop by hop relay context has processed
 * and renders them as the model {@link SrtpAfbStreams} authenticated feedback report.
 *
 * <p>SRTP authenticated feedback lets a receiver tell a sender which packet indices it has seen so the
 * two replay windows stay synchronised: each tracked stream contributes one
 * {@link SrtpAfbStreamInfo} carrying its SSRC, its highest rollover extended RTP packet index, and its
 * highest SRTCP index, flagged valid once the stream has feedback state. As the relay context protects
 * and unprotects packets it advances the indices through {@link #recordRtp(int, long)} and
 * {@link #recordRtcp(int, int)}; {@link #toReport()} snapshots the current indices into the report
 * that becomes the body of the authenticated feedback control message and the value of a WARP
 * {@code SRTP-AFB} attribute.
 *
 * <p>This tracker is mutable runtime state owned by a single {@link HbhSrtpRelay} and advanced on the
 * transport thread; it is not thread safe. Streams are reported in first seen order.
 *
 * @implNote This implementation holds the live per stream indices in a {@link Map} keyed by SSRC and
 *           materialises the model {@link SrtpAfbStreams} only on demand. Each index monotonically
 *           advances, mirroring the rollover extended RTP index watermark and the SRTCP index
 *           watermark of the corresponding stream.
 */
public final class SrtpAfbStreamTracker {
    /**
     * The logger for {@link SrtpAfbStreamTracker}.
     */
    private static final System.Logger LOGGER = Log.get(SrtpAfbStreamTracker.class);

    /**
     * Holds the per stream index watermarks keyed by SSRC, in first seen order.
     */
    private final Map<Integer, Watermark> streams;

    /**
     * Constructs an empty stream tracker.
     */
    public SrtpAfbStreamTracker() {
        this.streams = new LinkedHashMap<>();
    }

    /**
     * Advances the highest RTP packet index seen for a stream.
     *
     * <p>The index is the rollover counter extended RTP packet index, so it only ever moves forward; a
     * value not greater than the current watermark is ignored. The first record for an SSRC marks the
     * stream valid.
     *
     * @param ssrc     the stream's synchronization source identifier
     * @param rtpIndex the rollover extended RTP packet index
     */
    public void recordRtp(int ssrc, long rtpIndex) {
        if (Log.DEBUG && !streams.containsKey(ssrc)) {
            LOGGER.log(Level.DEBUG, "afb stream tracking started, ssrc={0}", ssrc);
        }
        var watermark = streams.computeIfAbsent(ssrc, _ -> new Watermark());
        if (rtpIndex > watermark.rtpIndex) {
            watermark.rtpIndex = rtpIndex;
        }
        watermark.valid = true;
    }

    /**
     * Advances the highest SRTCP index seen for a stream.
     *
     * <p>The SRTCP index only ever moves forward; a value not greater than the current watermark is
     * ignored. The first record for an SSRC marks the stream valid.
     *
     * @param ssrc      the stream's synchronization source identifier
     * @param rtcpIndex the SRTCP index, treated as an unsigned 31 bit value
     */
    public void recordRtcp(int ssrc, int rtcpIndex) {
        if (Log.DEBUG && !streams.containsKey(ssrc)) {
            LOGGER.log(Level.DEBUG, "afb stream tracking started, ssrc={0}", ssrc);
        }
        var watermark = streams.computeIfAbsent(ssrc, _ -> new Watermark());
        if (Integer.compareUnsigned(rtcpIndex, watermark.rtcpIndex) > 0) {
            watermark.rtcpIndex = rtcpIndex;
        }
        watermark.valid = true;
    }

    /**
     * Returns the number of tracked streams.
     *
     * @return the count of distinct SSRCs recorded
     */
    public int size() {
        return streams.size();
    }

    /**
     * Forgets every tracked stream.
     */
    public void clear() {
        if (Log.DEBUG && !streams.isEmpty()) {
            LOGGER.log(Level.DEBUG, "afb stream tracker cleared, {0} stream(s) forgotten", streams.size());
        }
        streams.clear();
    }

    /**
     * Renders the current per stream index watermarks as an authenticated feedback report.
     *
     * <p>The report carries one {@link SrtpAfbStreamInfo} per tracked SSRC, in first seen order, each
     * with its highest RTP and SRTCP indices and its validity flag. The returned report is an immutable
     * snapshot; later records do not change it.
     *
     * @return the {@link SrtpAfbStreams} report for every tracked stream; never {@code null}
     */
    public SrtpAfbStreams toReport() {
        var entries = new ArrayList<SrtpAfbStreamInfo>(streams.size());
        for (var entry : streams.entrySet()) {
            var watermark = entry.getValue();
            entries.add(new SrtpAfbStreamInfoBuilder()
                    .ssrc(entry.getKey())
                    .rtpIndex(watermark.rtpIndex)
                    .rtcpIndex(watermark.rtcpIndex)
                    .isValid(watermark.valid)
                    .build());
        }
        return new SrtpAfbStreamsBuilder()
                .srtpAfb(entries)
                .build();
    }

    /**
     * Holds one stream's mutable RTP and SRTCP index watermarks and its validity flag.
     *
     * <p>This is the per SSRC accumulator the tracker advances in place; it is materialised into the
     * immutable model {@link SrtpAfbStreamInfo} only when {@link #toReport()} runs.
     */
    private static final class Watermark {
        /**
         * Holds the highest rollover extended RTP packet index seen, initially zero.
         */
        private long rtpIndex;

        /**
         * Holds the highest SRTCP index seen, initially zero, compared as unsigned.
         */
        private int rtcpIndex;

        /**
         * Holds whether the stream has been recorded at least once and so has feedback state.
         */
        private boolean valid;
    }
}
