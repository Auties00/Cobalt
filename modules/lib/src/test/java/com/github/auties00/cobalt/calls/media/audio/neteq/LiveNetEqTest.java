package com.github.auties00.cobalt.calls.media.audio.neteq;

import com.github.auties00.cobalt.calls.media.audio.pipeline.AudioDecoderReceiver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.auties00.cobalt.calls.media.video.jitter.AvSyncFeedback;

// Drives LiveNetEq through a hand-built FrameDecoder seam (no libopus) so the decide-render cycle, the
// SyncBuffer-backed multi-frame serving, the voice-activity verdict, the in-band FEC reconstruction, and the
// WSOLA time-stretch/concealment ops can be asserted on the real getAudio() pull path. The decode produces a
// periodic ramp so the time-stretch criterion can fire; conceal() returns a sentinel constant so a WSOLA
// frame is distinguishable from a plain codec conceal.
@DisplayName("LiveNetEq composed jitter buffer")
class LiveNetEqTest {
    private static final int FRAME_SAMPLES = 320;

    // The codec-conceal sentinel: a plain codec PLC frame is exactly this constant, so any frame that is not
    // all-CONCEAL_SENTINEL was produced by a real decode or a WSOLA op, never by FrameDecoder.conceal().
    private static final short CONCEAL_SENTINEL = 0x4321;

    // A FrameDecoder seam decoding each packet to a fixed periodic signal with a per-packet voice verdict read
    // from the payload's first byte (TOC-style VAD bit 0x40), recording the last fec flag so an in-band
    // recovery decode is observable. A 60 ms MLow packet (TOC bit 0x80) decodes to three frames.
    private static final class StubDecoder implements AudioDecoderReceiver.FrameDecoder {
        private boolean lastFec;
        private int decodeCalls;

        @Override
        public AudioDecoderReceiver.DecodedFrame decode(byte[] payload, int frameSamples, boolean fec) {
            lastFec = fec;
            decodeCalls++;
            var voiceActive = payload.length > 0 && (payload[0] & 0x40) != 0;
            var multiFrame = payload.length > 0 && (payload[0] & 0x80) != 0;
            var samples = multiFrame ? 3 * frameSamples : frameSamples;
            return new AudioDecoderReceiver.DecodedFrame(periodic(samples), voiceActive);
        }

        @Override
        public short[] conceal(int frameSamples) {
            var out = new short[frameSamples];
            java.util.Arrays.fill(out, CONCEAL_SENTINEL);
            return out;
        }
    }

    // A periodic 80-sample pitch ramp so the WSOLA lag search finds a strong period; the leading sample of
    // each MLow frame carries that frame's index so the served frames are distinguishable.
    private static short[] periodic(int length) {
        var out = new short[length];
        for (var i = 0; i < length; i++) {
            out[i] = (short) (4_000 * Math.sin(2 * Math.PI * (i % 80) / 80.0));
        }
        return out;
    }

    private static boolean allConceal(short[] frame) {
        for (var s : frame) {
            if (s != CONCEAL_SENTINEL) {
                return false;
            }
        }
        return true;
    }

    private static LiveNetEq newNetEq() {
        return new LiveNetEq(NetEqConfig.defaults(), new StubDecoder());
    }

    private static LiveNetEq newNetEq(NetEqConfig config) {
        return new LiveNetEq(config, new StubDecoder());
    }

    // A 20 ms Opus-style packet (single frame), voice-active flag in bit 0x40.
    private static RtpAudioPacket packet(int seq, long arrival, boolean voiceActive) {
        var toc = (byte) (voiceActive ? 0x40 : 0x00);
        return new RtpAudioPacket(seq, seq * 320L, LiveNetEq.SPEECH_PAYLOAD_TYPE, new byte[]{toc}, arrival);
    }

    private static RtpAudioPacket packet(int seq, long arrival) {
        return packet(seq, arrival, true);
    }

    // A 60 ms MLow-style packet (multi-frame, bit 0x80), voice flag in bit 0x40.
    private static RtpAudioPacket mlowPacket(int seq, long arrival, boolean voiceActive) {
        var toc = (byte) (0x80 | (voiceActive ? 0x40 : 0x00));
        return new RtpAudioPacket(seq, seq * 960L, LiveNetEq.SPEECH_PAYLOAD_TYPE, new byte[]{toc}, arrival);
    }

    @Test
    @DisplayName("renders a 20 ms frame with an advancing presentation timestamp")
    void rendersFrame() {
        var neteq = newNetEq();
        neteq.insertPacket(packet(1, 0));
        var first = neteq.getAudio();
        assertNotNull(first);
        assertEquals(FRAME_SAMPLES, first.pcm().length);
        assertEquals(0L, first.ptsMicros());
        neteq.insertPacket(packet(2, 20));
        var second = neteq.getAudio();
        assertEquals(20_000L, second.ptsMicros());
    }

    @Test
    @DisplayName("counts a normal decode of a buffered packet")
    void decodesBufferedPacket() {
        var neteq = newNetEq();
        neteq.insertPacket(packet(1, 0));
        neteq.getAudio();
        assertTrue(neteq.statistics().normalFrames() >= 1);
    }

    @Test
    @DisplayName("conceals into the expanded-frame count when no packet is buffered")
    void concealsOnEmpty() {
        var neteq = newNetEq();
        neteq.getAudio();
        // an empty buffer with codec PLC enabled conceals, counted as an expanded frame
        assertTrue(neteq.statistics().expandedFrames() >= 1);
    }

    @Test
    @DisplayName("carries the per-packet voice-activity verdict to the served frame")
    void voiceVerdictPerFrame() {
        var neteq = newNetEq();
        neteq.insertPacket(packet(1, 0, true));
        neteq.getAudio();
        assertTrue(neteq.lastFrameVoiceActive(), "a voice-active packet renders voice-active");
        neteq.insertPacket(packet(2, 20, false));
        neteq.getAudio();
        assertFalse(neteq.lastFrameVoiceActive(), "an inactive packet renders inactive");
    }

    @Test
    @DisplayName("counts rendered operations into the statistics")
    void statisticsAccumulate() {
        var neteq = newNetEq();
        for (var i = 1; i <= 10; i++) {
            neteq.insertPacket(packet(i, i * 20L));
        }
        for (var i = 0; i < 10; i++) {
            neteq.getAudio();
        }
        var stats = neteq.statistics();
        assertTrue(stats.normalFrames() >= 1);
        assertEquals(0, stats.packetsDiscarded());
    }

    @Test
    @DisplayName("flush drains the buffer so the next pull conceals")
    void flush() {
        var neteq = newNetEq();
        for (var i = 1; i <= 5; i++) {
            neteq.insertPacket(packet(i, i * 20L));
        }
        neteq.flush();
        var before = neteq.statistics().expandedFrames();
        neteq.getAudio();
        // after a flush the buffer is empty, so the pull conceals into the expanded count
        assertTrue(neteq.statistics().expandedFrames() > before);
    }

    @Test
    @DisplayName("reports the pending NACK list for a gap in the received stream")
    void nackList() {
        var neteq = newNetEq();
        neteq.insertPacket(packet(10, 0));
        neteq.insertPacket(packet(13, 0));
        var nacks = neteq.pendingNackList(1000);
        assertEquals(java.util.List.of(11, 12), nacks);
    }

    @Test
    @DisplayName("ingests an avsync correction through the feedback sink")
    void avSyncSink() {
        var neteq = newNetEq();
        // the sink rounds the correction to whole milliseconds; a no-op heartbeat must not throw
        neteq.applyAvSyncFeedback(new AvSyncFeedback(0.0, 0.0));
        neteq.applyAvSyncFeedback(new AvSyncFeedback(12.0, 8.4));
    }

    @Nested
    @DisplayName("60 ms MLow packet served from the SyncBuffer (not a remainder FIFO)")
    class Mlow {
        @Test
        @DisplayName("serves the 960-sample decode as three frames across three pulls from one decode")
        void servesThreeFramesFromOneDecode() {
            var decoder = new StubDecoder();
            var neteq = new LiveNetEq(NetEqConfig.defaults(), decoder);
            neteq.insertPacket(mlowPacket(1, 0, true));

            var expected = periodic(3 * FRAME_SAMPLES);
            var first = neteq.getAudio().pcm();
            var second = neteq.getAudio().pcm();
            var third = neteq.getAudio().pcm();

            for (var i = 0; i < FRAME_SAMPLES; i++) {
                assertEquals(expected[i], first[i], "first frame is samples [0, FRAME)");
                assertEquals(expected[FRAME_SAMPLES + i], second[i], "second frame is samples [FRAME, 2*FRAME)");
                assertEquals(expected[2 * FRAME_SAMPLES + i], third[i], "third frame is samples [2*FRAME, 3*FRAME)");
            }
            assertEquals(1, decoder.decodeCalls, "one decode serves all three frames; the SyncBuffer holds the rest");
        }

        @Test
        @DisplayName("carries the packet's voice verdict across all three served frames")
        void voiceVerdictCarries() {
            var neteq = newNetEq();
            neteq.insertPacket(mlowPacket(1, 0, true));
            neteq.getAudio();
            assertTrue(neteq.lastFrameVoiceActive(), "first frame reports the packet's verdict");
            neteq.getAudio();
            assertTrue(neteq.lastFrameVoiceActive(), "second served frame keeps the verdict");
            neteq.getAudio();
            assertTrue(neteq.lastFrameVoiceActive(), "third served frame keeps the verdict");
        }
    }

    @Nested
    @DisplayName("WSOLA operations on the live getAudio() pull path")
    class Wsola {
        // A config with codec PLC off so a gap chooses the built-in NetEqExpand (not the codec conceal), and
        // with acceleration enabled and the high-latency time-stretch gate off so a span-driven decision can
        // reach ACCELERATE / PREEMPTIVE_EXPAND deterministically.
        private NetEqConfig timeStretchConfig() {
            var d = NetEqConfig.defaults();
            return new NetEqConfig(d.minDelayMs(), d.maxDelayMs(), d.delayOffsetMs(), d.targetDelayMs(),
                    d.initMinE2eDelayMs(), d.dmHistorySizeMs(), d.dlHistorySizeMs(), d.maxHistoryMs(),
                    d.underrunQuantile(), d.underrunForgetFactor(), d.reorderForgetFactor(),
                    d.reorderStartForgetWeight(), d.enablePeakDetector(), d.smartBufferFlushEnabled(),
                    d.bufferFlushMaxLengthMs(), d.maxPacketsInBuffer(), d.use20msGetPeriod(),
                    d.numInitialPackets(),
                    d.audioJitbufBufferLowerLimitScalePercent(), d.audioJitbufBufferLimitsWindowSizeMs(),
                    d.highThresholdOffsetMs(), d.useMaxDelayInHighThreshold(),
                    true, // allowTimeStretchAcceleration
                    false, // allowTimeStretchForHighLatency (gate off)
                    d.allowTimeStretchThresholdMs(), d.enableCodecPlc(),
                    d.preexpandWithFilteredLevelPerc(), d.skipNackWithFec(), d.ladEnabledForNack(),
                    d.ladEnabledForFec(), d.ladNackExtraInsertTimeMs(), d.nackRttLimitMs(), d.maxNackListSize(),
                    d.audioNackMaxSeqReq(), d.enableSpeakerStatus());
        }

        // codec PLC off so an empty-buffer gap renders the built-in NetEqExpand instead of FrameDecoder.conceal.
        private NetEqConfig builtinExpandConfig() {
            var d = NetEqConfig.defaults();
            return new NetEqConfig(d.minDelayMs(), d.maxDelayMs(), d.delayOffsetMs(), d.targetDelayMs(),
                    d.initMinE2eDelayMs(), d.dmHistorySizeMs(), d.dlHistorySizeMs(), d.maxHistoryMs(),
                    d.underrunQuantile(), d.underrunForgetFactor(), d.reorderForgetFactor(),
                    d.reorderStartForgetWeight(), d.enablePeakDetector(), d.smartBufferFlushEnabled(),
                    d.bufferFlushMaxLengthMs(), d.maxPacketsInBuffer(), d.use20msGetPeriod(),
                    d.numInitialPackets(),
                    d.audioJitbufBufferLowerLimitScalePercent(), d.audioJitbufBufferLimitsWindowSizeMs(),
                    d.highThresholdOffsetMs(), d.useMaxDelayInHighThreshold(),
                    d.allowTimeStretchAcceleration(), d.allowTimeStretchForHighLatency(),
                    d.allowTimeStretchThresholdMs(),
                    false, // enableCodecPlc off -> EXPAND not CODEC_PLC
                    d.preexpandWithFilteredLevelPerc(), d.skipNackWithFec(), d.ladEnabledForNack(),
                    d.ladEnabledForFec(), d.ladNackExtraInsertTimeMs(), d.nackRttLimitMs(), d.maxNackListSize(),
                    d.audioNackMaxSeqReq(), d.enableSpeakerStatus());
        }

        // Pulls past the numInitialPackets warm-up window with a steady near-target buffer, building a real
        // periodic SyncBuffer history of decoded audio for the WSOLA ops to splice over.
        private void warmUp(LiveNetEq neteq, NetEqConfig config, int extraSeq) {
            var n = config.numInitialPackets();
            for (var i = 1; i <= n + extraSeq; i++) {
                neteq.insertPacket(packet(i, i * 20L));
            }
            for (var i = 0; i < n; i++) {
                neteq.getAudio();
            }
        }

        @Test
        @DisplayName("a gap renders the built-in NetEqExpand extrapolation, not a plain codec conceal")
        void expandRunsBuiltInExpander() {
            var config = builtinExpandConfig();
            var neteq = newNetEq(config);
            warmUp(neteq, config, 0);
            var expandedBefore = neteq.statistics().expandedFrames();

            // No further contiguous packet -> EXPAND. With codec PLC off the built-in NetEqExpand runs over the
            // periodic history, so the frame is real extrapolated audio, never the conceal sentinel.
            var frame = neteq.getAudio().pcm();
            assertEquals(FRAME_SAMPLES, frame.length);
            assertTrue(neteq.statistics().expandedFrames() > expandedBefore, "the EXPAND arm must run");
            assertFalse(allConceal(frame), "the built-in expander output must not be the codec-conceal sentinel");
        }

        @Test
        @DisplayName("the first packet after a concealment run cross-fades through NetEqMerge")
        void mergeRunsAfterConcealment() {
            var config = builtinExpandConfig();
            var neteq = newNetEq(config);
            var n = config.numInitialPackets();
            warmUp(neteq, config, 0);
            // The warm-up consumed seq 1..n, so the last decoded sequence is n. One EXPAND (empty buffer), then
            // the contiguous packet seq n+1 returns -> the first decode after concealment must MERGE.
            neteq.getAudio(); // EXPAND
            var mergedBefore = neteq.statistics().mergedFrames();
            neteq.insertPacket(packet(n + 1, (n + 1) * 20L));
            var frame = neteq.getAudio().pcm(); // MERGE
            assertEquals(FRAME_SAMPLES, frame.length);
            assertTrue(neteq.statistics().mergedFrames() > mergedBefore, "the MERGE arm must run on the first recovered packet");
            assertFalse(allConceal(frame), "a merged frame is decoded audio cross-faded with history, not a conceal");
        }

        @Test
        @DisplayName("a buffer below the low limit pre-emptively expands through NetEqTimeStretch")
        void preemptiveExpandRunsTimeStretch() {
            var config = timeStretchConfig();
            var neteq = newNetEq(config);
            // Pin a high target so the steady span sits below the low limit and the decision is PREEMPTIVE_EXPAND.
            neteq.setMinimumDelayMillis(200);
            neteq.setMaximumDelayMillis(400);
            warmUp(neteq, config, 1);
            var preemptiveBefore = neteq.statistics().preemptiveExpandedFrames();
            neteq.insertPacket(packet(900, 18_000L));
            var frame = neteq.getAudio().pcm();
            assertEquals(FRAME_SAMPLES, frame.length);
            assertTrue(neteq.statistics().preemptiveExpandedFrames() > preemptiveBefore,
                    "the PREEMPTIVE_EXPAND time-stretch arm must run, not a plain decode");
            assertFalse(allConceal(frame), "a time-stretched frame is real audio, not a conceal");
        }

        @Test
        @DisplayName("a grossly over-full buffer accelerates through NetEqTimeStretch")
        void accelerateRunsTimeStretch() {
            var config = timeStretchConfig();
            var neteq = newNetEq(config);
            // Pin a low target so the deep backlog sits above the high limit and the decision is ACCELERATE.
            neteq.setMinimumDelayMillis(0);
            neteq.setMaximumDelayMillis(40);
            var n = config.numInitialPackets();
            // A deep backlog: insert far more than the warm-up consumes so the span stays over-full after warm-up.
            for (var i = 1; i <= n + 60; i++) {
                neteq.insertPacket(packet(i, i * 20L));
            }
            for (var i = 0; i < n; i++) {
                neteq.getAudio();
            }
            var acceleratedBefore = neteq.statistics().acceleratedFrames();
            var frame = neteq.getAudio().pcm();
            assertEquals(FRAME_SAMPLES, frame.length);
            assertTrue(neteq.statistics().acceleratedFrames() > acceleratedBefore,
                    "the ACCELERATE time-stretch arm must run on a grossly over-full buffer");
            assertFalse(allConceal(frame), "an accelerated frame is real time-compressed audio, not a conceal");
        }
    }

    @Nested
    @DisplayName("in-band forward-error-correction reconstruction")
    class ForwardErrorCorrection {
        // skipNackWithFec=false so a concealment looks ahead for a following packet to reconstruct from.
        private NetEqConfig fecConfig() {
            var d = NetEqConfig.defaults();
            return new NetEqConfig(d.minDelayMs(), d.maxDelayMs(), d.delayOffsetMs(), d.targetDelayMs(),
                    d.initMinE2eDelayMs(), d.dmHistorySizeMs(), d.dlHistorySizeMs(), d.maxHistoryMs(),
                    d.underrunQuantile(), d.underrunForgetFactor(), d.reorderForgetFactor(),
                    d.reorderStartForgetWeight(), d.enablePeakDetector(), d.smartBufferFlushEnabled(),
                    d.bufferFlushMaxLengthMs(), d.maxPacketsInBuffer(), d.use20msGetPeriod(),
                    d.numInitialPackets(),
                    d.audioJitbufBufferLowerLimitScalePercent(), d.audioJitbufBufferLimitsWindowSizeMs(),
                    d.highThresholdOffsetMs(), d.useMaxDelayInHighThreshold(),
                    d.allowTimeStretchAcceleration(), d.allowTimeStretchForHighLatency(),
                    d.allowTimeStretchThresholdMs(),
                    false, // enableCodecPlc off so EXPAND/FEC arm is reached over codec conceal
                    d.preexpandWithFilteredLevelPerc(),
                    false, // skipNackWithFec=false -> FEC lookahead happens
                    d.ladEnabledForNack(), d.ladEnabledForFec(), d.ladNackExtraInsertTimeMs(),
                    d.nackRttLimitMs(), d.maxNackListSize(), d.audioNackMaxSeqReq(), d.enableSpeakerStatus());
        }

        @Test
        @DisplayName("a non-contiguous following packet drives a recovery decode with the fec flag set")
        void recoveryDecodeUsesFecFlag() {
            var config = fecConfig();
            var decoder = new StubDecoder();
            var neteq = new LiveNetEq(config, decoder);
            // A gap at the head (seq 10 then 13): the next contiguous packet is missing, but a following
            // packet (13) is buffered, so the concealment arm reconstructs from it with the recovery flag.
            neteq.insertPacket(packet(10, 0));
            neteq.insertPacket(packet(13, 0));
            // Pull seq 10 normally so the head advances and seq 11 becomes the awaited-but-missing packet.
            neteq.getAudio();
            decoder.lastFec = false;
            var frame = neteq.getAudio().pcm();
            assertEquals(FRAME_SAMPLES, frame.length);
            assertTrue(decoder.lastFec, "the recovery decode must pass the fec recovery flag");
            assertFalse(neteq.lastFrameVoiceActive(), "a reconstructed frame carries no voice verdict of its own");
        }
    }
}
