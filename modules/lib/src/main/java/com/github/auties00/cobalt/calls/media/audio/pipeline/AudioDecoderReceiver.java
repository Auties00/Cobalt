package com.github.auties00.cobalt.calls.media.audio.pipeline;

import com.github.auties00.cobalt.calls.platform.audio.AudioWriterPump;
import com.github.auties00.cobalt.calls.stream.AudioFrame;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Drives the audio receive path: it feeds received packets into the NetEq jitter buffer and, on each
 * playback pull, asks the buffer to render one frame and delivers the rendered samples.
 *
 * <p>This is the receiver half of the audio media engine and the producer the playback pump pulls from,
 * which it satisfies by implementing {@link AudioWriterPump.AudioBlockSource}. Two flows meet here. On
 * receipt, a demultiplexed and (on the group path) SFrame opened audio payload is inserted into the
 * {@link NetEqAudioSource} through {@link #receivePacket(int, long, byte[])}, which buffers it and tracks its
 * arrival timing. On playback, the writer pump calls {@link #pull(short[], int)} once per render period; the
 * receiver has the NetEq buffer render one frame straight into the pump's array through
 * {@link NetEqAudioSource#getAudioInto(short[], int)}, recording the frame's voice activity verdict
 * ({@link NetEqAudioSource#lastFrameVoiceActive()}) for level metering and mixing.
 *
 * <p>The NetEq jitter buffer the receiver delegates to owns every render decision: it asks its decision logic
 * for the next operation, decodes the scheduled packet through its codec seam, reconstructs a lost packet
 * from the in band forward error correction copy carried in the following packet or conceals it through the
 * codec's packet loss concealment or the built in expander, time stretches the decoded audio to drain or fill
 * the buffer, and serves comfort noise or silence for a gap. The receiver adds no render logic of its own; it
 * is a thin adapter that has the buffer render the served frame directly into the pump's array. A codec packet
 * that decodes to more than one frame (the sixty millisecond MLow packet decodes to three frames, nine hundred
 * and sixty samples, in a single decode) is served one frame per pull by the NetEq buffer's output history, so
 * the receiver holds no remainder buffer.
 *
 * <p>The jitter buffer get period is fixed at twenty milliseconds, so one pull renders one twenty millisecond
 * frame, which at the call's sixteen kilohertz mono format is three hundred and twenty samples. The receiver
 * renders up to the pump's capacity into its array; a pull for fewer than a whole frame therefore returns a
 * truncated frame rather than splitting the decode, and the pump sizes its request to a whole frame in
 * practice.
 *
 * <p>The two flows run on different threads: {@link #receivePacket(int, long, byte[])} on the transport
 * receive path and {@link #pull(short[], int)} on the playback pump's virtual thread. The NetEq buffer the
 * receiver delegates to owns the concurrency between insert and pull, mirroring the WebRTC NetEq design where
 * the insert and the get audio entry points are separately locked; this receiver adds no lock of its own and
 * keeps only single writer scratch state on the pull thread.
 *
 * @implNote This implementation is pure orchestration of the per pull render into array cycle: the decode, the
 * in band forward error correction recovery, the codec packet loss concealment, the time stretch, and the
 * voice activity flag all run inside the NetEq buffer behind the {@link NetEqAudioSource} seam. The get period
 * of twenty milliseconds is kept as {@link #GET_PERIOD_MILLIS} and the rendered frame is delivered per the
 * demand driven render contract of {@link AudioWriterPump.AudioBlockSource}. The multi frame MLow remainder is
 * served from the NetEq buffer's own decoded output history rather than a parallel remainder buffer held by
 * the receiver.
 */
public final class AudioDecoderReceiver implements AudioWriterPump.AudioBlockSource {
    /**
     * The logger for {@link AudioDecoderReceiver}.
     */
    private static final System.Logger LOGGER = Log.get(AudioDecoderReceiver.class);

    /**
     * The jitter buffer get period, in milliseconds.
     *
     * <p>One playback pull renders one frame of this duration; the reference engine fixes it at twenty
     * milliseconds.
     */
    public static final int GET_PERIOD_MILLIS = 20;

    /**
     * The call audio sample rate, in hertz.
     *
     * <p>Fixed at sixteen kilohertz mono to match the public call audio format; combined with the get
     * period it sets the rendered frame's sample count.
     */
    public static final int SAMPLE_RATE_HZ = 16_000;

    /**
     * The number of samples in one rendered frame.
     *
     * <p>The get period times the sample rate: twenty milliseconds at sixteen kilohertz is three hundred
     * and twenty samples. Every decode, conceal, or comfort noise frame produces exactly this many
     * samples.
     */
    public static final int FRAME_SAMPLES = SAMPLE_RATE_HZ / 1000 * GET_PERIOD_MILLIS;

    /**
     * One decoded packet's samples and the voice activity verdict the codec read from it.
     *
     * <p>The {@link #samples()} array holds a whole number of frames, each {@link #FRAME_SAMPLES} samples
     * long: one frame for a 20 ms Opus packet, three for a 60 ms MLow packet. The {@link #voiceActive()} flag
     * is the receive side voice activity verdict the codec derives from the packet that produced these
     * samples, so a comfort noise or inactive packet reports inactive even when it decodes to non silent PCM;
     * the one verdict covers every frame the packet decoded to.
     *
     * @param samples     the decoded samples, a positive multiple of {@link #FRAME_SAMPLES} long; never
     *                    {@code null}
     * @param voiceActive whether the decoded packet was judged to carry active speech
     */
    public record DecodedFrame(short[] samples, boolean voiceActive) {
        /**
         * Validates the decoded frame, rejecting a {@code null} sample array.
         *
         * @throws NullPointerException if {@code samples} is {@code null}
         */
        public DecodedFrame {
            Objects.requireNonNull(samples, "samples cannot be null");
        }
    }

    /**
     * Decodes or conceals one audio frame through the codec, the seam the NetEq jitter buffer renders each
     * frame through.
     *
     * <p>Implemented over the codec's libopus or MLow binding and bound into the NetEq jitter buffer at
     * wiring time. The buffer calls {@link #decode(byte[], int, boolean)} for a normal decode or an in band
     * forward error correction reconstruction and {@link #conceal(int)} for a codec packet loss concealment
     * frame; the receiver never calls this seam directly, since the buffer owns the per frame decision.
     */
    public interface FrameDecoder {
        /**
         * Decodes one packet into one or more frames and reports its voice activity verdict.
         *
         * @implSpec Implementations must return a whole number of frames, each {@code frameSamples} samples
         * long: exactly {@code frameSamples} samples for a packet that spans one get period (the 20 ms Opus
         * packet), or a positive integer multiple of {@code frameSamples} for a packet that spans several (the
         * 60 ms MLow packet decodes to three frames, {@code 3 * frameSamples} samples, in one decode). The
         * NetEq buffer pushes the whole decode into its output history and serves one frame per pull, so a
         * multi frame decode must not be split across calls. Implementations must set the returned frame's
         * {@link DecodedFrame#voiceActive()} from the decoded packet's own voice activity classification (the
         * Opus per SILK frame VAD flags, or the MLow TOC voice activity bit), never a fixed value; the one
         * verdict covers every frame the packet decodes to. A forward error correction reconstruction of a
         * previous frame does not carry the next packet's verdict.
         *
         * @param payload      the codec packet to decode; never {@code null}
         * @param frameSamples the number of samples in one get period frame
         * @param fec          whether to reconstruct the previous frame from this packet's in band copy
         * @return the decoded frame carrying the samples and the packet's voice activity verdict; never
         * {@code null}
         */
        DecodedFrame decode(byte[] payload, int frameSamples, boolean fec);

        /**
         * The sample count written into a caller buffer paired with the packet's voice activity verdict, the
         * fill into caller counterpart of {@link DecodedFrame}.
         *
         * @param length      the number of samples written into the caller's buffer
         * @param voiceActive whether the decoded packet was judged to carry active speech
         */
        record DecodedInto(int length, boolean voiceActive) {
        }

        /**
         * Decodes one packet straight into a caller supplied buffer and reports its voice activity verdict,
         * the fill into caller form of {@link #decode(byte[], int, boolean)}.
         *
         * <p>Writes the decoded PCM into {@code destination} and returns the sample count paired with the
         * packet's voice activity verdict, sparing the fresh array {@link #decode(byte[], int, boolean)}
         * allocates and the jitter buffer then copies into its output history and discards.
         *
         * @implSpec The default implementation delegates to {@link #decode(byte[], int, boolean)} and copies
         * the decoded samples into {@code destination}, so it is byte identical to that path; an
         * implementation whose codec writes PCM into an owned buffer overrides this to decode straight into
         * {@code destination}. The caller sizes {@code destination} to the largest packet it will decode (a
         * 60 ms MLow packet decodes to three frames, each {@code frameSamples} samples long).
         *
         * @param payload      the codec packet to decode; never {@code null}
         * @param frameSamples the number of samples in one get period frame
         * @param fec          whether to reconstruct the previous frame from this packet's in band copy
         * @param destination  the buffer to write the decoded PCM into, at least the decoded sample count long
         * @return the number of samples written paired with the packet's voice activity verdict
         */
        default DecodedInto decodeInto(byte[] payload, int frameSamples, boolean fec, short[] destination) {
            var decoded = decode(payload, frameSamples, fec);
            var samples = decoded.samples();
            System.arraycopy(samples, 0, destination, 0, samples.length);
            return new DecodedInto(samples.length, decoded.voiceActive());
        }

        /**
         * Produces one concealment frame with no input packet.
         *
         * @param frameSamples the number of samples the concealment frame must hold
         * @return the concealed samples, {@code frameSamples} long; never {@code null}
         */
        short[] conceal(int frameSamples);
    }

    /**
     * The NetEq jitter buffer the receiver inserts packets into and pulls rendered frames from.
     *
     * <p>Implemented by the NetEq buffer in {@code media/audio/neteq}: it buffers inserted packets, schedules
     * their playout against measured arrival timing, and on each pull renders exactly one get period frame,
     * decoding through its own {@link FrameDecoder} codec seam and running the full WSOLA time stretch and
     * concealment over its decoded PCM history. The receiver treats it as the authority on what to render and
     * never reorders packets or runs codec logic itself.
     */
    public interface NetEqAudioSource {
        /**
         * Inserts one received audio packet into the jitter buffer.
         *
         * @param rtpSequence  the packet's 16 bit RTP sequence number
         * @param rtpTimestamp the packet's RTP timestamp
         * @param payload      the decoded transport audio payload to buffer; never {@code null}
         */
        void insert(int rtpSequence, long rtpTimestamp, byte[] payload);

        /**
         * Renders and returns the next get period audio frame.
         *
         * @implSpec Implementations must ask the decision logic for an operation, render exactly one
         * get period frame by decoding the scheduled packet, reconstructing a lost frame from the following
         * packet's in band forward error correction, concealing through the codec or the built in expander,
         * time stretching the decoded audio, or generating comfort noise, and return the frame with a
         * presentation timestamp. A codec packet that decodes to several get period frames must be served one
         * frame per call from the buffer's output history.
         *
         * @return the rendered audio frame; never {@code null}
         */
        AudioFrame getAudio();

        /**
         * Renders the next get period audio frame directly into the caller's buffer.
         *
         * <p>This is the zero copy playback pull the receiver drives on the hot path: rather than
         * allocating a frame and returning it for the caller to copy, the source writes the rendered
         * samples straight into {@code destination}, so a steady playback pull neither allocates a sample
         * array nor stamps a presentation timestamp the caller would only discard.
         *
         * @implSpec Implementations render exactly one get period frame with the same decide, decode,
         * conceal, time stretch, and comfort noise logic as {@link #getAudio()}, write it into
         * {@code destination} starting at index zero, and update the {@link #lastFrameVoiceActive()}
         * verdict identically. When {@code length} is at least one whole frame the whole frame is written
         * and the frame length is returned; when {@code length} is shorter than a whole frame the
         * implementation still consumes a whole frame from the buffer but hands out only the first
         * {@code length} samples, returning that truncated count, so a short pull never splits a decode.
         * The default implementation delegates to {@link #getAudio()} and copies the served samples,
         * preserving behaviour for sources that do not override it.
         *
         * @param destination the buffer to render the frame into, written from index zero; must hold at
         *                    least {@code length} samples
         * @param length      the maximum number of samples to write; always positive
         * @return the number of samples written, in {@code [0, length]}
         */
        default int getAudioInto(short[] destination, int length) {
            var pcm = getAudio().pcm();
            var copied = Math.min(length, Math.min(FRAME_SAMPLES, pcm.length));
            System.arraycopy(pcm, 0, destination, 0, copied);
            return copied;
        }

        /**
         * Returns whether the most recently rendered frame was judged voice active.
         *
         * @implSpec Implementations must report the voice activity verdict of the packet the last
         * {@link #getAudio()} frame was decoded from, reporting inactive for a concealment,
         * forward error correction, comfort noise, or silence frame, and reporting one multi frame packet's
         * single verdict for every get period frame served from it.
         *
         * @return {@code true} if the last rendered frame carried active speech
         */
        boolean lastFrameVoiceActive();
    }

    /**
     * The NetEq jitter buffer the receiver inserts into and pulls rendered frames from.
     */
    private final NetEqAudioSource netEq;

    /**
     * Whether the last rendered frame was judged voice active.
     *
     * <p>Set on each pull from the NetEq buffer's verdict (a concealment or silence frame is inactive) and
     * read by {@link #lastFrameVoiceActive()} for level metering and mixing. Written only on the pull thread.
     */
    private volatile boolean lastFrameVoiceActive;

    /**
     * Constructs an audio decoder receiver wrapping the NetEq jitter buffer.
     *
     * @param netEq the NetEq jitter buffer to insert into and pull rendered frames from; never {@code null}
     * @throws NullPointerException if {@code netEq} is {@code null}
     */
    public AudioDecoderReceiver(NetEqAudioSource netEq) {
        this.netEq = Objects.requireNonNull(netEq, "netEq cannot be null");
        this.lastFrameVoiceActive = false;
    }

    /**
     * Inserts one received audio packet into the jitter buffer.
     *
     * <p>The payload is the audio bytes after transport demultiplexing and, on the group path, SFrame
     * opening, so the jitter buffer and codec see plain codec packets regardless of the confidentiality
     * path. Called on the transport receive thread; the buffering and any reordering are the jitter
     * buffer's responsibility.
     *
     * @param rtpSequence  the packet's 16 bit RTP sequence number
     * @param rtpTimestamp the packet's RTP timestamp
     * @param payload      the decoded transport audio payload; never {@code null}
     * @throws NullPointerException if {@code payload} is {@code null}
     */
    public void receivePacket(int rtpSequence, long rtpTimestamp, byte[] payload) {
        Objects.requireNonNull(payload, "payload cannot be null");
        if (Log.TRACE) LOGGER.log(Level.TRACE, "audio receive packet seq={0} ts={1} len={2}", rtpSequence, rtpTimestamp, payload.length);
        netEq.insert(rtpSequence, rtpTimestamp, payload);
    }

    /**
     * Returns whether the most recently rendered frame was voice active.
     *
     * <p>Reflects the NetEq buffer's voice activity verdict for the last {@link #pull(short[], int)}; a
     * concealment or silence frame is reported inactive. Used for audio level metering and mixing
     * downstream.
     *
     * @return {@code true} if the last rendered frame carried active speech
     */
    public boolean lastFrameVoiceActive() {
        return lastFrameVoiceActive;
    }

    /**
     * Renders one frame by pulling it from the NetEq jitter buffer directly into the pump's array.
     *
     * <p>Asks the NetEq buffer to render one get period frame straight into {@code block} through
     * {@link NetEqAudioSource#getAudioInto(short[], int)}; the buffer decides the operation, decodes or
     * conceals, and time stretches, so this method runs no codec logic and, over the production NetEq
     * source, copies nothing of its own. A {@code length} shorter than a whole frame truncates the
     * handed out samples rather than splitting the decode, and the count written is returned. The
     * voice activity verdict the buffer reports is recorded for {@link #lastFrameVoiceActive()}; a
     * multi frame MLow packet's three frames each carry that packet's single verdict because the buffer
     * holds it across the served frames.
     *
     * @param block  the array to render the frame into; written only during this call
     * @param length the maximum number of samples to write
     * @return the number of samples written, in {@code [0, length]}
     */
    @Override
    public int pull(short[] block, int length) {
        if (length <= 0) {
            return 0;
        }
        var written = netEq.getAudioInto(block, length);
        lastFrameVoiceActive = netEq.lastFrameVoiceActive();
        if (Log.TRACE) LOGGER.log(Level.TRACE, "audio pull written={0} voiceActive={1}", written, lastFrameVoiceActive);
        return written;
    }
}
