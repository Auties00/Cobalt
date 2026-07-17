package com.github.auties00.cobalt.calls.media.audio.pipeline;

import com.github.auties00.cobalt.calls.platform.audio.AudioReaderPump;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.github.auties00.cobalt.calls.media.audio.codec.EncodedAudioFrame;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.MLowRedPacker;

/**
 * Drives the audio send path: aggregates encoded frames into one packet per send, attaches the audio level
 * extension, applies the group end to end transform, retains the packet for redundancy, and hands it to the
 * transport.
 *
 * <p>This is the sender half of the audio media engine. Captured PCM arrives one fixed block at a time
 * from the {@link AudioReaderPump}, which this class consumes by implementing
 * {@link AudioReaderPump.AudioBlockSink}. Each block is encoded through the {@link FrameEncoder} seam into
 * an {@link EncodedAudioFrame}, then buffered: a configurable number of consecutive encoded frames (the
 * frames per packet count, {@code 1..6}) are packed into a single RTP payload through the
 * {@link FramePacker} seam before sending, which amortizes the per packet header and transport overhead
 * across several short Opus frames. Once the buffer holds a full group the sender combines the frames and
 * ships one packet. A discontinuous transmission silence frame (the encoder suppressed it to at most one
 * byte) is not buffered; it flushes any partial group so a trailing frame is not stranded, and unless the
 * call start window transmits it the sender drops it and notifies the {@link SilenceSink} so the outbound
 * RTP media clock still advances across the silence gap.
 *
 * <p>For each combined payload the sender derives an {@link AudioLevelRtpExtension} from the loudest frame
 * in the group and its voice activity verdict, so the packet advertises its level to a mixer or selective
 * forwarding unit. The level extension and the combined codec payload are assembled into the outbound media
 * payload, which then takes one of two confidentiality paths: a group call seals the payload with SFrame
 * through the {@link SFrameTransform} seam so the relay forwards opaque ciphertext, while a one to one call
 * leaves the payload to the shared key hop by hop SRTP applied downstream in the transport and supplies no
 * SFrame transform. The finished packet is retained in the {@link StreamPacketCache} keyed by its extended
 * sequence so the redundancy schemes can replay or protect it, and is handed to the {@link MediaPacketSink}
 * for transmission.
 *
 * <p>The sender runs entirely on the reader pump's virtual thread: {@link #accept(short[], int)} is invoked
 * once per captured block on that thread, and all buffering, combining, sealing, caching, and sending happen
 * inline before it returns. It is therefore single threaded and holds no internal lock; the collaborators it
 * calls own their own concurrency. A call's sender is created once and used for the call's lifetime.
 *
 * @implNote This implementation buffers already encoded frames rather than raw PCM: the encoder state, the
 * lazy encode cache, and the adaptive complexity controller live in the codec unit behind the
 * {@link FrameEncoder} seam, so this class is pure orchestration over narrow functional seams (encode, pack,
 * seal, send) bound at wiring time. The SFrame seam is supplied only for a group call; a one to one call
 * relies on shared key SRTP applied downstream and passes no transform. The redundancy schemes draw from the
 * {@link StreamPacketCache} this sender populates rather than from the send path here.
 */
public final class AudioEncoderSender implements AudioReaderPump.AudioBlockSink {
    /**
     * The logger for {@link AudioEncoderSender}.
     */
    private static final System.Logger LOGGER = Log.get(AudioEncoderSender.class);

    /**
     * Lowest legal frames per packet count.
     *
     * <p>A value of one sends every encoded frame immediately with no aggregation.
     */
    public static final int MIN_FRAMES_PER_PACKET = 1;

    /**
     * Highest legal frames per packet count.
     *
     * <p>The engine never packs more than six Opus frames into one payload.
     */
    public static final int MAX_FRAMES_PER_PACKET = 6;

    /**
     * Encodes one captured PCM block into a codec frame.
     *
     * <p>Implemented by the audio codec unit over its libopus binding; the sender hands each block from
     * the capture pump to this seam and aggregates the returned {@link EncodedAudioFrame}s. An
     * implementation owns the encoder state and the lazy encode and adaptive complexity behaviour; this
     * seam exposes only the per block encode the sender needs.
     */
    @FunctionalInterface
    public interface FrameEncoder {
        /**
         * Encodes one block of captured PCM samples into a codec frame.
         *
         * @param pcm    the captured samples; never {@code null}
         * @param length the number of valid samples at the start of {@code pcm}
         * @return the encoded frame, including its voice activity and discontinuity flags; never
         * {@code null}
         */
        EncodedAudioFrame encode(short[] pcm, int length);
    }

    /**
     * Combines a group of encoded frames into one aggregated codec payload.
     *
     * <p>Implemented by the codec unit over the Opus repacketizer; the sender passes the buffered
     * frames per packet group and receives the single combined payload that goes into one RTP packet. A
     * group of exactly one frame yields that frame's payload unchanged.
     */
    @FunctionalInterface
    public interface FramePacker {
        /**
         * Combines the encoded frames of one frames per packet group into a single payload.
         *
         * @param frames the encoded frames to combine, in send order; never {@code null} and never empty
         * @return the combined codec payload bytes; never {@code null}
         */
        byte[] pack(List<EncodedAudioFrame> frames);
    }

    /**
     * Seals one media payload with the group end to end SFrame transform.
     *
     * <p>Implemented over the {@code calls.media.sframe} secure frame transform; the sender invokes it
     * only on a group call, so a one to one call supplies no instance and the payload is left for
     * shared key SRTP downstream. The seam takes the assembled media payload and returns the SFrame
     * ciphertext and trailer bytes.
     */
    @FunctionalInterface
    public interface SFrameTransform {
        /**
         * Seals one media payload, returning the SFrame frame bytes.
         *
         * @param payload the plaintext media payload to seal; never {@code null}
         * @return the sealed SFrame frame; never {@code null}
         */
        byte[] seal(byte[] payload);
    }

    /**
     * Transmits one finished audio media packet over the active transport.
     *
     * <p>Implemented by the transport unit; the sender hands it the assembled payload (already SFrame
     * sealed on the group path) together with the packet's extended sequence and voice activity flag,
     * which the transport packetizes into RTP, applies hop by hop SRTP to, and sends. The sender does not
     * build the RTP header itself, since the SSRC, sequence wire form, and SRTP keys are transport state.
     */
    @FunctionalInterface
    public interface MediaPacketSink {
        /**
         * Sends one audio media payload as an RTP packet.
         *
         * @param payload          the media payload to send, SFrame sealed on the group path; never
         *                         {@code null}
         * @param extendedSequence the 32 bit RTP extended sequence number assigned to the packet
         * @param level            the audio level extension to attach; never {@code null}
         */
        void send(byte[] payload, long extendedSequence, AudioLevelRtpExtension level);
    }

    /**
     * Accounts one discontinuous transmission silence frame the sender suppressed rather than transmitted.
     *
     * <p>Implemented by the transport's audio packetizer; the sender invokes it once per silence frame it
     * drops so the downstream RTP media clock advances across the gap and the next transmitted packet
     * resumes a talkspurt. A pipeline that keeps no outbound timestamp state (a loopback harness) supplies
     * a no op.
     */
    @FunctionalInterface
    public interface SilenceSink {
        /**
         * Accounts one suppressed silence frame in the downstream media clock.
         */
        void onSilenceFrame();
    }

    /**
     * The codec encode seam each captured block is passed through.
     */
    private final FrameEncoder encoder;

    /**
     * The repacketizer seam that combines a frames per packet group into one payload.
     */
    private final FramePacker packer;

    /**
     * The group SFrame transform, or {@code null} on a one to one call.
     *
     * <p>When {@code null}, the assembled payload is sent in the clear for shared key SRTP to protect
     * downstream; when present, every payload is sealed before caching and sending.
     */
    private final SFrameTransform sframe;

    /**
     * The transport send seam each finished packet is handed to.
     */
    private final MediaPacketSink sink;

    /**
     * The seam notified once per suppressed silence frame so the downstream media clock spans the gap.
     */
    private final SilenceSink silenceSink;

    /**
     * The cache retaining each sent packet for the redundancy schemes.
     */
    private final StreamPacketCache packetCache;

    /**
     * The number of encoded frames packed into one outbound packet.
     */
    private final int framesPerPacket;

    /**
     * The encoded frames buffered toward the current frames per packet group.
     *
     * <p>Filled by successive {@link #accept(short[], int)} calls and drained when it reaches
     * {@link #framesPerPacket} or an empty frame forces an early flush. Represents the current frames per
     * packet group under construction.
     */
    private final List<EncodedAudioFrame> pending;

    /**
     * The next RTP extended sequence number to assign to an outbound packet.
     *
     * <p>Advanced by one per sent packet; widened to 32 bits so the redundancy windows order packets
     * across the 16 bit RTP sequence rollover.
     */
    private long nextExtendedSequence;

    /**
     * The number of discontinuous transmission frames to transmit at call start before reverting to
     * dropping them, or zero when the call start DTX window is off.
     *
     * <p>Derived by the caller from {@code options.enable_additional_dtx_frames_at_call_start_ms}: a
     * silence frame is normally not sent (the peer conceals the gap), but the first few are transmitted at
     * call start so the peer locks onto this client's stream before speech begins.
     */
    private final int additionalDtxFramesAtCallStart;

    /**
     * The count of frames accepted since call start, compared against
     * {@link #additionalDtxFramesAtCallStart} to bound the call start DTX transmission window.
     */
    private long callStartFrames;

    /**
     * Constructs an audio encoder sender wiring the codec, packer, optional SFrame transform, transport,
     * and packet cache.
     *
     * @param encoder         the codec encode seam; never {@code null}
     * @param packer          the repacketizer seam combining a frames per packet group; never {@code null}
     * @param sframe          the group SFrame transform, or {@code null} for a one to one call
     * @param sink            the transport send seam; never {@code null}
     * @param silenceSink     the seam notified once per suppressed silence frame so the downstream media
     *                        clock spans the discontinuous transmission gap; never {@code null}
     * @param packetCache     the cache retaining sent packets for redundancy; never {@code null}
     * @param framesPerPacket the number of encoded frames per packet, in {@code [1, 6]}
     * @param additionalDtxFramesAtCallStart the number of discontinuous transmission frames to transmit at
     *                                       call start before dropping them, zero to disable the window
     * @throws NullPointerException     if {@code encoder}, {@code packer}, {@code sink}, {@code silenceSink},
     *                                  or {@code packetCache} is {@code null}
     * @throws IllegalArgumentException if {@code framesPerPacket} is outside {@code [1, 6]}
     */
    public AudioEncoderSender(FrameEncoder encoder,
                              FramePacker packer,
                              SFrameTransform sframe,
                              MediaPacketSink sink,
                              SilenceSink silenceSink,
                              StreamPacketCache packetCache,
                              int framesPerPacket,
                              int additionalDtxFramesAtCallStart) {
        this.encoder = Objects.requireNonNull(encoder, "encoder cannot be null");
        this.packer = Objects.requireNonNull(packer, "packer cannot be null");
        this.sframe = sframe;
        this.sink = Objects.requireNonNull(sink, "sink cannot be null");
        this.silenceSink = Objects.requireNonNull(silenceSink, "silenceSink cannot be null");
        this.packetCache = Objects.requireNonNull(packetCache, "packetCache cannot be null");
        if (framesPerPacket < MIN_FRAMES_PER_PACKET || framesPerPacket > MAX_FRAMES_PER_PACKET) {
            throw new IllegalArgumentException(
                    "framesPerPacket must be in [" + MIN_FRAMES_PER_PACKET + ", " + MAX_FRAMES_PER_PACKET
                            + "]: " + framesPerPacket);
        }
        this.framesPerPacket = framesPerPacket;
        this.pending = new ArrayList<>(framesPerPacket);
        this.nextExtendedSequence = 0;
        this.additionalDtxFramesAtCallStart = Math.max(0, additionalDtxFramesAtCallStart);
    }

    /**
     * Returns the number of encoded frames packed into one outbound packet.
     *
     * @return the configured frames per packet count
     */
    public int framesPerPacket() {
        return framesPerPacket;
    }

    /**
     * Returns whether this sender seals payloads with the group SFrame transform.
     *
     * @return {@code true} on a group call with an SFrame transform, {@code false} on a one to one call
     */
    public boolean isGroupSecured() {
        return sframe != null;
    }

    /**
     * Encodes one captured PCM block and sends a packet once a full frames per packet group is buffered.
     *
     * <p>Encodes the block through the codec seam. A discontinuous transmission frame (a silence frame at
     * most one byte long) is not aggregated: it flushes any partial group and is transmitted only while the
     * call start DTX window is open (its first {@link #additionalDtxFramesAtCallStart} frames), then dropped.
     * A dropped silence frame still spans one packet of media time, so the {@link SilenceSink} is notified to
     * advance the outbound RTP media clock across the gap, keeping the next transmitted packet's timestamp
     * aligned with real time. Otherwise the frame is appended to the pending group, and when the group
     * reaches the frames per packet count it is combined, tagged with its level, sealed on the group path,
     * cached, and sent.
     *
     * @param block  the captured samples from the reader pump; never {@code null}
     * @param length the number of valid samples at the start of {@code block}
     */
    @Override
    public void accept(short[] block, int length) {
        var frame = encoder.encode(block, length);
        callStartFrames++;
        if (frame.discontinuous() && frame.payload().length < 2) {
            // A silence frame the encoder emitted as at most one byte is normally dropped, so flush any
            // buffered speech group. During the call start window the first additionalDtxFramesAtCallStart of
            // them are sent instead so the peer locks onto the stream before speech; a zero length frame
            // carries nothing to send. A dropped silence frame still spans one packet of media time, so the
            // silence seam is notified to advance the outbound clock; a sent one advances it through the
            // ordinary send path.
            flush();
            if (callStartFrames <= additionalDtxFramesAtCallStart && !frame.isEmpty()) {
                if (Log.TRACE) LOGGER.log(Level.TRACE, "audio dtx frame sent during call start window, count={0}", callStartFrames);
                pending.add(frame);
                flush();
            } else {
                if (Log.TRACE) LOGGER.log(Level.TRACE, "audio dtx frame dropped");
                silenceSink.onSilenceFrame();
            }
            return;
        }
        if (frame.isEmpty()) {
            flush();
            silenceSink.onSilenceFrame();
            return;
        }
        pending.add(frame);
        if (pending.size() >= framesPerPacket) {
            flush();
        }
    }

    /**
     * Combines, seals, caches, and sends the currently buffered frames per packet group.
     *
     * <p>Does nothing when no frames are buffered. Otherwise packs the buffered frames into one payload,
     * derives the audio level extension from the group, seals the payload on the group path, retains the
     * finished packet in the cache keyed by its extended sequence, hands it to the transport, advances the
     * sequence, and clears the buffer. Exposed so the call teardown can flush a trailing partial group
     * before the sender is discarded.
     */
    public void flush() {
        if (pending.isEmpty()) {
            return;
        }
        var combined = packer.pack(pending);
        var level = levelFor(pending);
        var payload = sframe == null ? combined : sframe.seal(combined);
        var sequence = nextExtendedSequence++;
        packetCache.store(sequence, payload, true);
        // TODO: wire MLowRedPacker - when enable_mlow_red is set, instantiate MLowRedPacker (from server pushed mlow_red_redundancy_level/stream_mtu/samplesPerFrame) and call pack(packetCache, payload, sequence) here in the group seal/send path, using its output as the RTP payload passed to sink.send
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "audio packet sent seq={0} frames={1} len={2} sealed={3} voiceActive={4}",
                    sequence, pending.size(), payload.length, sframe != null, level.voiceActive());
        }
        sink.send(payload, sequence, level);
        pending.clear();
    }

    /**
     * Derives the audio level extension for a frames per packet group from its loudest frame.
     *
     * <p>The packet advertises one level for the whole group, so the loudest frame governs: the level is
     * the minimum {@code -dBov} magnitude across the group (the loudest sample, since a smaller magnitude
     * is louder), taken from each frame's {@linkplain EncodedAudioFrame#levelDbov() measured level}, and
     * the voice activity flag is set when any frame in the group is voice active. A group every frame of
     * which measures silence reports {@link AudioLevelRtpExtension#SILENCE_LEVEL}.
     *
     * @implNote This implementation advertises one level for the whole group and selects it as the minimum
     * {@code -dBov} magnitude scanned across the per frame levels, which is the loudest sample. This is the
     * slice of the running audio level history that the outbound packet covers.
     *
     * @param frames the buffered frames per packet group; never empty
     * @return the audio level extension to attach to the combined packet
     */
    private AudioLevelRtpExtension levelFor(List<EncodedAudioFrame> frames) {
        // TODO: apply the audio_level_num_lsb_to_zero low-bit masking to the selected level when it is set.
        //  RE (re/calls): this is the WA voip param audio_level_num_lsb_to_zero, a privacy toggle that zeroes
        //  the low N bits of the 7-bit level in the value octet; WebRTC ships no such masking, so the baseline
        //  default is 0 (no masking) and the current unmasked level is faithful for that default. When a
        //  nonzero value is server-pushed, mask as level & ~((1 << n) - 1) here after selecting the group
        //  level; the param is not yet plumbed from the voip settings into this sender.
        var voiceActive = false;
        var level = AudioLevelRtpExtension.SILENCE_LEVEL;
        for (var frame : frames) {
            voiceActive |= frame.voiceActive();
            level = Math.min(level, frame.levelDbov());
        }
        return new AudioLevelRtpExtension(level, voiceActive);
    }
}
