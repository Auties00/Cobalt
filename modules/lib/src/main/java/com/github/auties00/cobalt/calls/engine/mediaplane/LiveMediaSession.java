package com.github.auties00.cobalt.calls.engine.mediaplane;

import com.github.auties00.cobalt.calls.capability.*;
import com.github.auties00.cobalt.calls.config.*;
import com.github.auties00.cobalt.calls.config.param.*;
import com.github.auties00.cobalt.calls.engine.participant.*;
import com.github.auties00.cobalt.calls.config.CallsFeatureGate;
import com.github.auties00.cobalt.calls.config.VoipSettings;
import com.github.auties00.cobalt.calls.engine.LifecycleController;
import com.github.auties00.cobalt.calls.crypto.CallE2eKeyDerivation;
import com.github.auties00.cobalt.calls.transport.srtp.E2eMediaSrtp;
import com.github.auties00.cobalt.calls.engine.participant.CallMembership;
import com.github.auties00.cobalt.calls.engine.participant.CallSecureSsrcGenerator;
import com.github.auties00.cobalt.calls.media.audio.neteq.*;
import com.github.auties00.cobalt.calls.media.audio.neteq.decoder.*;
import com.github.auties00.cobalt.calls.media.audio.neteq.*;
import com.github.auties00.cobalt.calls.media.video.jitter.*;
import com.github.auties00.cobalt.calls.media.audio.codec.*;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.*;
import com.github.auties00.cobalt.calls.media.audio.codec.opus.*;
import com.github.auties00.cobalt.calls.media.audio.pipeline.*;
import com.github.auties00.cobalt.calls.media.audio.processing.WebRtcAudioProcessor;
import com.github.auties00.cobalt.calls.media.sframe.SFrameKeyProvider;
import com.github.auties00.cobalt.calls.media.video.codec.EncodedVideoFrame;
import com.github.auties00.cobalt.calls.media.video.codec.VideoCodec;
import com.github.auties00.cobalt.calls.media.video.codec.VideoCodecParams;
import com.github.auties00.cobalt.calls.media.video.codec.VideoCodecRegistry;
import com.github.auties00.cobalt.calls.media.video.yuv.YuvConverter;
import com.github.auties00.cobalt.calls.transport.congestion.bwe.*;
import com.github.auties00.cobalt.calls.transport.congestion.bwe.combine.*;
import com.github.auties00.cobalt.calls.transport.congestion.bwe.delay.*;
import com.github.auties00.cobalt.calls.transport.congestion.bwe.ml.*;
import com.github.auties00.cobalt.calls.transport.congestion.bwe.shaping.*;
import com.github.auties00.cobalt.calls.transport.congestion.bwe.signal.*;
import com.github.auties00.cobalt.calls.transport.congestion.ratecontrol.AudioRateController;
import com.github.auties00.cobalt.calls.transport.congestion.ratecontrol.SctpBufferCongestionController;
import com.github.auties00.cobalt.calls.transport.congestion.ratecontrol.UnifiedAudioQualityControl;
import com.github.auties00.cobalt.calls.transport.congestion.ratecontrol.VideoRateController;
import com.github.auties00.cobalt.calls.transport.*;
import com.github.auties00.cobalt.calls.transport.congestion.bwe.signal.*;
import com.github.auties00.cobalt.calls.transport.datachannel.*;
import com.github.auties00.cobalt.calls.transport.dtls.*;
import com.github.auties00.cobalt.calls.transport.ice.*;
import com.github.auties00.cobalt.calls.transport.rtcp.*;
import com.github.auties00.cobalt.calls.transport.srtp.*;
import com.github.auties00.cobalt.calls.transport.stun.*;
import com.github.auties00.cobalt.calls.transport.subscription.*;
import com.github.auties00.cobalt.calls.transport.warp.*;
import com.github.auties00.cobalt.calls.platform.*;
import com.github.auties00.cobalt.calls.platform.audio.*;
import com.github.auties00.cobalt.calls.platform.video.*;
import com.github.auties00.cobalt.calls.platform.video.VideoCaptureDriver.VideoCaptureCapability;
import com.github.auties00.cobalt.calls.platform.video.VideoCaptureDriver.VideoSink;
import com.github.auties00.cobalt.calls.signaling.relay.RelayEndpoint;
import com.github.auties00.cobalt.calls.signaling.relay.RelayInfo;
import com.github.auties00.cobalt.calls.stream.*;
import com.github.auties00.cobalt.calls.stream.audio.*;
import com.github.auties00.cobalt.calls.stream.ffmpeg.*;
import com.github.auties00.cobalt.calls.stream.video.*;
import com.github.auties00.cobalt.calls.util.TimerHeap;
import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SenderSubscriptionExt;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SenderSubscriptionExtBuilder;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SenderSubscriptionExtPidTemporalLayerBuilder;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SenderSubscriptionExtSSrcsToPidAssignmentsBuilder;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SenderSubscriptions;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SenderSubscriptionsBuilder;
import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamDescriptor;
import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamDescriptorBuilder;
import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamDescriptors;
import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamDescriptorsBuilder;
import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamSubscriptions;
import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamSubscriptionsBuilder;
import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamSubscriptionsEntryBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppAccountStore;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import com.github.auties00.cobalt.calls.engine.state.CallLifecycleState;

/**
 * The live media plane for one call: the running audio and video pipeline that turns a connected call's
 * relay credentials and call key into flowing media.
 *
 * <p>This is the production backing of the {@link MediaPlane} seam the {@link LifecycleController}
 * drives once a call is answered. Its {@link LiveMediaPlane} factory brings up a {@link LiveMediaSession}
 * by wiring the already built units into the full two way media chain:
 * <ul>
 *   <li><b>Outbound audio:</b> the call's application audio capture source (an {@link AudioOutput} the
 *       embedder supplies, a microphone bound one capturing from its own device, or, when the call supplied
 *       none, a platform capture device started through the engine's {@link VoipDriverManager} and bridged
 *       to the reader pump by a {@link CaptureSourceBridge}) feeds a single producer
 *       single consumer {@link AudioCaptureRing} drained by an {@link AudioReaderPump} into the
 *       {@link AudioEncoderSender}, which Opus encodes through an {@link OpusAudioCodec}, packs a
 *       frames per packet group with an {@link OpusRepacketizer}, seals the payload with SFrame on a group
 *       call (the {@link SFrameKeyProvider} chain) or leaves it for shared key hop by hop SRTP on a
 *       one to one call, RTP packetizes it, and ships it through the {@link MediaTransport}, which
 *       hop by hop SRTP protects it and writes it as SCTP DATA on the one SCTP data channel.</li>
 *   <li><b>Inbound audio:</b> a socket receive loop hands each datagram, with its source address, to the
 *       {@link MediaTransport}, whose ICE/DTLS/SCTP demultiplex decrypts the data channel media and hands
 *       the cleartext RTP back; it is RTP depacketized, SFrame opened on a
 *       group call, inserted into the {@link LiveNetEq} adaptive jitter buffer, and rendered one frame at a
 *       time by the {@code AudioDecoderReceiver}, delivered either to the call's application audio playback
 *       sink (an {@link AudioInput} the embedder supplies, a speaker bound one rendering to its own device)
 *       by a fixed cadence render loop, or, when the call supplied none, into an {@link AudioPlaybackRing}
 *       fed by the demand driven {@link AudioWriterPump} and drained by a platform playback device started
 *       through the engine's {@link VoipDriverManager} ({@link LiveAudioPlaybackDriver}).</li>
 *   <li><b>Video:</b> when the local side participates with video, the negotiated {@link VideoCodec} from
 *       the {@link VideoCodecRegistry} drives an inbound {@link VideoJitterBuffer} decode and render
 *       loop that delivers each decoded picture to the call's application {@link VideoInput} sink when one
 *       was supplied (falling back to {@link VoipHostApi#renderVideoFrame}), and an outbound encode loop
 *       that drains the call's application {@link VideoOutput} capture source, encodes each picture, and
 *       ships the access unit over the same relay transport; when the call sends video but supplied no
 *       {@link VideoOutput}, the engine's {@link VoipDriverManager} starts a platform camera capture whose
 *       frames feed the encode loop through a {@link ManagerVideoSourceBridge}.</li>
 * </ul>
 *
 * <p>The session keys its media from the call key through {@link CallE2eKeyDerivation}: the relay
 * {@code <hbh_key>} keys the client to relay hop by hop SRTP, and on a group call the per participant
 * SFrame base key keys the end to end SFrame layer. Each pipeline runs on its own virtual thread per the
 * Cobalt threading model; {@link MediaPlane.Session#close()} stops every pump and thread, closes the codecs, the
 * transport, and the datagram socket, and is idempotent so a teardown that races the transport's own
 * close is safe. A bring up that cannot start the transport, open a codec, or bind the socket surfaces as
 * a non fatal {@link WhatsAppCallException.DataChannel}, which the controller isolates to this one call
 * rather than tearing the session down.
 *
 * @apiNote This is an internal engine collaborator, not a public surface; the lifecycle controller is its
 * only caller and embedders never construct it.
 * @implNote This implementation runs the media plane over a single {@link LiveRelayTransport}, the
 * {@code RTCPeerConnection}-equivalent that brings the call up over ICE and DTLS and carries every packet as
 * SCTP DATA on one SCTP data channel; the session owns the host UDP socket that backs that ICE/DTLS
 * transport. The HTTP session bootstrap is not driven from this media plane seam: it is a signaling side
 * transport step owned by the transport controller.
 */
public final class LiveMediaSession implements MediaPlane.Session {
    /**
     * The logger for {@link LiveMediaSession}.
     */
    private static final System.Logger LOGGER = Log.get(LiveMediaSession.class);

    /**
     * The call audio sample rate, in hertz, fixed at sixteen kilohertz mono to match the public call
     * audio format and the {@code AudioDecoderReceiver} frame geometry.
     */
    private static final int AUDIO_SAMPLE_RATE_HZ = 16_000;

    /**
     * The number of audio channels, fixed at one (mono) for the call audio format.
     */
    private static final int AUDIO_CHANNELS = 1;

    /**
     * The audio frame duration, in milliseconds, matching the NetEq twenty millisecond get period and the
     * default Opus ptime.
     */
    private static final int AUDIO_FRAME_MILLIS = 20;

    /**
     * The number of samples in one captured or rendered audio block: the frame duration times the sample
     * rate, three hundred and twenty samples for a twenty millisecond frame at sixteen kilohertz.
     */
    private static final int AUDIO_FRAME_SAMPLES = AUDIO_SAMPLE_RATE_HZ / 1000 * AUDIO_FRAME_MILLIS;

    /**
     * The number of frames packed into one outbound audio RTP packet.
     *
     * <p>One frame per packet is the negotiated initial value: the {@code <voip_settings>} document carries
     * {@code options.initial_fpp=1}, so the call starts sending every encoded frame immediately with no
     * aggregation. The negotiated packing ceiling is {@code rc.maxfpp=2} or
     * {@code encode.max_frames_per_packet=3} at a {@code 60 ms} frame ({@code encode.frame_ms=60}) under the
     * rate control rules.
     *
     * @implNote This implementation pins the initial frames per packet to the negotiated
     * {@code options.initial_fpp=1}, so the initial packing is faithful.
     */
    // TODO: adapt the frames per packet up to rc.maxfpp per round; the per round value the engine selects is
    //  driven by the same rate control condition catalogue that gates the audio reserve split (see
    //  audioShare), which is not yet modelled. Threading it requires that catalogue plus carrying the selected
    //  value through the capture pump cadence, the OutboundRtpPacketizer timestamp increment, and the
    //  FramePacker.
    private static final int FRAMES_PER_PACKET = 1;

    /**
     * The number of 20 ms frames MLow packs into one outbound packet, giving its 60 ms packetization time.
     *
     * <p>WhatsApp's MLow path ships a 60 ms packet (three 20 ms internal frames carried in one TOC led
     * range coded bitstream), unlike the 20 ms Opus packet {@link #FRAMES_PER_PACKET} produces. The capture
     * pump stays on the 20 ms cadence the audio processing front end requires, so the send path accumulates
     * this many captured blocks before the single 60 ms MLow encode rather than resizing the pump; the
     * outbound RTP timestamp then advances by this many frames per MLow packet.
     */
    private static final int MLOW_FRAMES_PER_PACKET = 3;

    /**
     * The number of 20 ms PCM blocks aggregated into one Opus frame, giving WhatsApp's 60 ms Opus frame
     * duration.
     *
     * @implNote This implementation aggregates {@code 3} captured 20 ms blocks into one 60 ms Opus encode
     * ({@code config 11}, SILK wideband 60 ms) so the wire frame matches WhatsApp's 60 ms ptime, the frame
     * length the peer's jitter buffer is sized for: the peer's call telemetry reports
     * {@code frameLengthMs = 60} and WhatsApp paces one packet per {@code 960}-sample (60 ms at 16 kHz) span.
     */
    private static final int OPUS_FRAME_BLOCKS = 3;

    /**
     * The {@code section.key} wire path of the per call voip parameter selecting the MLow audio codec
     * over Opus, the {@code encode} section field {@code use_mlow_codec_v1} (the engine's
     * {@code p->use_mlow_codec} struct field).
     *
     * <p>A boolean tunable resolved to its {@link VoipParamKey} through
     * {@link VoipParamKey#ofWirePath(String)} and read through {@link VoipParams#getBoolean(VoipParamKey)}:
     * a {@code "true"} (or {@code "1"}) value routes the call's audio encode and decode through the MLow
     * low bitrate speech codec ({@link MLowAudioCodec} and {@link MLowAudioDecoder}) instead of the
     * {@link OpusAudioCodec} and {@link OpusAudioDecoder}. The server delivers it inside the {@code encode}
     * section of the {@code <voip_settings>} document, so the catalogue addresses it by this area sectioned
     * wire path. Declared once here so the path the codec selector resolves is not literal duplicated
     * across the decoder and encoder construction sites.
     */
    private static final String USE_MLOW_CODEC_PARAM = "encode.use_mlow_codec_v1";

    /**
     * The wire path of the per call MLow LPC postfilter gate, the {@code decode} section field
     * {@code mlow_post_filter} (the native {@code p->mlow_enable_lpc_postfilter}).
     *
     * <p>The MLow decode postfilter chain always runs its harmonic and high pass postfilters as the live
     * decoder's default level lift and harmonic shaping; this integer tunable gates only the additional LPC
     * postfilter, which the native {@code LPC_postfilter_mode} default and the live client both leave off, so
     * an absent, zero, or non integer value leaves the LPC postfilter disabled.
     */
    private static final String MLOW_ENABLE_LPC_POSTFILTER_PARAM = "decode.mlow_post_filter";

    /**
     * The wire path of the relay DTLS active mode gate, the {@code options} section field
     * {@code enable_edgeray_dtls_active_mode} (the native {@code vp->enable_edgeray_dtls_active_mode}).
     *
     * <p>This integer voip param, not a {@code <relay>} element field, flips the relay leg's DTLS roles: when
     * it is zero the relay is the DTLS server and the web client is the DTLS client (the common, only observed
     * mode), and when it is non zero the relay is the DTLS client and the web client is the DTLS server. It is
     * delivered through the {@code <voip_settings>} channel, so it is read from the active voip param set
     * rather than from the parsed relay block.
     */
    private static final String ENABLE_EDGERAY_DTLS_ACTIVE_MODE_PARAM = "options.enable_edgeray_dtls_active_mode";

    /**
     * The wire path of the audio rate control maximum bandwidth estimate, the {@code rc} section field
     * {@code maxbwe} (the native {@code p->max_bwe}).
     *
     * <p>The {@code rc} section is the audio rate control area, so the audio sender bandwidth estimator reads
     * its ceiling from here; the {@code vid_rc} section carries the separate video ceiling. An absent value
     * leaves the audio loop on its compiled maximum bitrate default.
     */
    private static final String RC_MAXBWE_PARAM = "rc.maxbwe";

    /**
     * The wire attribute on a {@code <voip_settings>} stanza naming the settings bucket it belongs under.
     */
    private static final String VOIP_SETTINGS_TYPE_ATTRIBUTE = "type";

    /**
     * The wire attribute on a {@code <voip_settings>} stanza naming the device the bundle applies to.
     *
     * <p>The offer acknowledgement delivers one {@code jid}-tagged bundle per callee device; the
     * delivered offer carries a single bundle with no {@code jid} (the callee's own config).
     */
    private static final String VOIP_SETTINGS_JID_ATTRIBUTE = "jid";

    /**
     * The wire {@code type} value selecting the audio call voip settings overlay.
     */
    private static final String VOIP_SETTINGS_TYPE_AUDIO = "audio";

    /**
     * The wire {@code type} value selecting the video call voip settings overlay.
     */
    private static final String VOIP_SETTINGS_TYPE_VIDEO = "video";

    /**
     * The wire {@code type} value the default voip settings bundle carries, and the fallback assumed when a
     * {@code <voip_settings>} stanza omits the {@code type} attribute.
     *
     * <p>The default bundle maps onto {@link VoipSettingsType#NONE}, the mandatory baseline; the captured
     * offer acknowledgement delivers its single bundle with no {@code type} attribute at all, which this
     * fallback routes to the same baseline.
     */
    private static final String VOIP_SETTINGS_TYPE_DEFAULT = "default";

    /**
     * The capacity, in samples, of the capture and playback rings.
     *
     * <p>Sized to comfortably exceed both the block size and the startup seed so the producer side is not
     * immediately full; the rings round it up to a power of two.
     */
    private static final int RING_CAPACITY_SAMPLES = 8 * AUDIO_FRAME_SAMPLES;

    /**
     * The number of samples the capture ring buffers before the reader pump forwards the first block,
     * establishing the capture to render skew margin.
     */
    private static final int CAPTURE_STARTUP_SEED_SAMPLES = 2 * AUDIO_FRAME_SAMPLES;

    /**
     * The maximum size, in bytes, of an inbound datagram the receive loop reads.
     */
    private static final int MAX_DATAGRAM_BYTES = 1500 + 64;

    /**
     * The fixed RFC 8445 priority of the single relay host candidate the synthesized answer carries.
     *
     * @implNote This implementation uses {@code 2122262783}, the value WhatsApp Web injects into the
     * synthesized relay answer's {@code a=candidate:2 1 udp 2122262783 <ip> <port> typ host} line.
     */
    private static final long RELAY_HOST_CANDIDATE_PRIORITY = 2122262783L;

    /**
     * The RFC 8445 local preference of the local host candidate.
     *
     * @implNote This implementation uses the maximum local preference so the single host candidate is the
     * most preferred local candidate.
     */
    private static final int ICE_HOST_LOCAL_PREFERENCE = 65535;

    /**
     * The ICE component id of the RTP component.
     */
    private static final int ICE_RTP_COMPONENT_ID = 1;

    /**
     * The number of random bytes a generated local ICE ufrag is derived from.
     *
     * @implNote This implementation uses {@code 4} bytes, whose base64url encoding exceeds the RFC 8445
     * four character ufrag minimum.
     */
    private static final int ICE_UFRAG_RANDOM_BYTES = 4;

    /**
     * The number of random bytes a generated local ICE password is derived from.
     *
     * @implNote This implementation uses {@code 18} bytes, whose base64url encoding exceeds the RFC 8445
     * twenty two character password minimum.
     */
    private static final int ICE_PASSWORD_RANDOM_BYTES = 18;

    /**
     * The number of recent outbound audio packets retained for the redundancy schemes.
     */
    private static final int PACKET_CACHE_CAPACITY = 256;

    /**
     * The RTP payload type stamped on outbound audio packets and matched to recognize inbound audio, the
     * dynamic type the WhatsApp audio stream uses on the wire.
     *
     * @implNote This implementation uses {@code 120}, the payload type WhatsApp carries on both directions of
     * a 1:1 audio call (the peer's RTP and the relayed media both carry {@code PT 120}). The peer's decoder is
     * bound to it, so an outbound packet stamped with any other type is dropped as an unknown payload and never
     * reaches the peer's jitter buffer.
     */
    private static final int AUDIO_PAYLOAD_TYPE = 120;

    /**
     * The fixed twelve byte base RTP header length, with no CSRC list; the four byte
     * {@link #RTP_HEADER_EXTENSION_LENGTH} header extension follows it on outbound media packets.
     */
    private static final int RTP_HEADER_LENGTH = 12;

    /**
     * The four byte length of the {@code 0xDEBE} RTP header extension preamble WhatsApp stamps on every outbound
     * media packet: the two byte {@code 0xDEBE} profile word followed by a two byte extension word count.
     *
     * <p>This is only the preamble. The audio path stamps a zero word count (an empty extension, total four
     * bytes) while the video path follows the preamble with two or three element words (id3 + id6 + id9, see
     * {@link OutboundVideoRtpPacketizer#packetize(byte[], long, boolean, boolean, boolean)}); a packet's full extension
     * length is this preamble plus the word count times four.
     *
     * @implNote The audio extension is stamped empty because its per call extmap id is not recoverable (see
     * {@code sendAudioPacket}); the live capture shows the relay requires the extension bit set and the
     * {@code de be} profile present on every media packet to forward the stream.
     */
    private static final int RTP_HEADER_EXTENSION_LENGTH = 4;

    /**
     * The trailing room, in bytes, reserved after a cleartext RTP packet for the hop by hop SRTP
     * authentication tag the transport appends in place.
     *
     * <p>The relay transport protects an outbound packet in place, writing the SRTP tag immediately after
     * the cleartext bytes, so any buffer handed to {@link MediaTransport#sendMedia} must carry this much
     * spare room past its declared length. It matches the trailing allowance the audio and video
     * packetizers reserve and comfortably covers the {@link SrtpCryptoSuite#AES_CM_128_HMAC_SHA1_80} tag.
     */
    private static final int RTP_SRTP_TAG_ROOM = 64;

    /**
     * The RTP timestamp clock rate of the audio stream, equal to the sample rate.
     */
    private static final int AUDIO_RTP_CLOCK_RATE = AUDIO_SAMPLE_RATE_HZ;

    /**
     * The RTP payload type stamped on outbound video packets, a dynamic type distinct from the audio type
     * so the inbound demux routes by payload type.
     *
     * <p>The value is the {@code 97} the live data channel capture shows every WhatsApp video RTP packet
     * carrying (the audio type is {@code 120}); the demux treats any non audio payload type as video, so the
     * value only has to match what the peer expects for its decoder.
     */
    private static final int VIDEO_PAYLOAD_TYPE = 97;

    /**
     * The RTP timestamp clock rate of the video stream, ninety kilohertz, the standard video RTP clock the
     * jitter buffer's capture timestamps are denominated in.
     */
    private static final int VIDEO_RTP_CLOCK_RATE = 90_000;

    /**
     * The maximum size, in bytes, of one outbound media datagram the video packetizer fragments an access
     * unit down to, the {@code options.mtu_size} the call negotiates.
     *
     * <p>The outbound video packetizer fragments an H264 access unit into {@code FU-A} packets so no single packet
     * exceeds this transmission unit, and a small picture stays a single NAL packet. The budget for the
     * codec payload of one packet is this value minus the twelve byte RTP header and the hop by hop SRTP
     * tag room, so a fragment plus its headers fits one path datagram.
     *
     * @implNote This implementation uses {@code 1200}, the {@code options.mtu_size} value the
     * {@code <voip_settings>} document carries. It is not read back through
     * {@link VoipParams#getInteger(VoipParamKey)} because the value is nested under the JSON {@code options}
     * section while the modelled {@code mtu_size} key resolves at the document root, so the negotiated leaf
     * lands as an unmodelled value and the constant is the faithful budget.
     */
    private static final int VIDEO_MTU_BYTES = 1200;

    /**
     * The default outbound video geometry width, in pixels, until the negotiated capture capability is
     * threaded through.
     */
    private static final int VIDEO_WIDTH = 640;

    /**
     * The default outbound video geometry height, in pixels, until the negotiated capture capability is
     * threaded through.
     */
    private static final int VIDEO_HEIGHT = 480;

    /**
     * The default outbound video frame rate, in frames per second.
     */
    private static final int VIDEO_FRAME_RATE = 30;

    /**
     * The default target encoder bitrate, in bits per second, advertised by the driver manager camera
     * capture bridge.
     *
     * <p>Only the geometry of the bridge source sizes the encoder; the advertised bitrate is a positive
     * placeholder the rate controller adapts, so this carries the public {@code VideoOutput} default of one
     * megabit per second rather than a negotiated value.
     */
    private static final int VIDEO_DEFAULT_BITRATE_BPS = 1_000_000;

    /**
     * The placeholder device identifier passed to the driver manager's camera capture when the call sources
     * outbound video from a platform camera.
     *
     * <p>The engine's camera source factory opens the platform default camera regardless of the identifier,
     * but {@link VoipDriverManager#startVideoCapture} requires a non-{@code null} identifier; this names the
     * default device explicitly until a device selection control threads a chosen camera id through.
     */
    private static final String MANAGER_CAMERA_DEVICE_ID = "default";

    /**
     * The video jitter estimator's frame size rolling window depth.
     */
    private static final int VIDEO_JITTER_WINDOW = 30;

    /**
     * The render loop poll interval, in nanoseconds, of the video jitter buffer drain.
     */
    private static final long VIDEO_POLL_INTERVAL_NANOS = 5_000_000L;

    /**
     * Holds the call identifier, used in diagnostics and as the keying context.
     */
    private final String callId;

    /**
     * Holds the media transport this session ships outbound media through, as SCTP DATA on its data
     * channel, and feeds inbound socket datagrams into.
     */
    private final MediaTransport transport;

    /**
     * Holds the host UDP socket that backs the {@code RTCPeerConnection}-equivalent ICE/DTLS transport,
     * owning the inbound receive loop; the transport ships its ICE checks and DTLS records out through it.
     */
    private final DatagramChannel channel;

    /**
     * Holds the relay endpoint used as the bootstrap ICE/DTLS destination before a candidate pair is
     * nominated.
     */
    private final SocketAddress relayAddress;

    /**
     * Holds the audio codec backing the encode and decode seams, or {@code null} when the audio pipeline
     * could not be opened.
     *
     * <p>Either an {@link OpusAudioCodec} or an {@link MLowAudioCodec}, selected per call by the
     * {@code encode.use_mlow_codec_v1} voip parameter; both are permits of the sealed {@link AudioCodec}
     * interface, whose {@linkplain AudioCodec#modify(OpusCodecParams) modify} and
     * {@linkplain AudioCodec#close() close} are the only operations this session drives on it.
     */
    private final AudioCodec audioCodec;

    /**
     * Holds the Opus repacketizer combining a frames per packet group, or {@code null} when the audio
     * pipeline could not be opened or the call selected the {@link MLowAudioCodec}.
     *
     * <p>The repacketizer is Opus specific frames per packet packing; MLow self packs its sixty millisecond
     * packet inside the codec, so an MLow call leaves this {@code null} and the encoder sender passes each
     * MLow packet through unchanged rather than re packing it.
     */
    private final OpusRepacketizer repacketizer;

    /**
     * Holds the engine's driver manager, the one per engine registry this session routes its platform
     * audio capture and playback bring up through.
     *
     * <p>The manager owns the audio capture and playback drivers for the engine's lifetime; this session
     * starts and stops them through the manager rather than holding a driver directly, so a driver is
     * returned to {@link com.github.auties00.cobalt.calls.platform.audio.AudioDriverState#INITIALIZED} on this
     * call's teardown and reused by the next call rather than closed.
     */
    private final VoipDriverManager voipDriverManager;

    /**
     * Tracks whether this call drives its capture through a platform device on the {@link #voipDriverManager}
     * rather than an application capture source, so {@link #startAudioCapture()} and {@link #close()} drive
     * the manager only when the call relies on a platform microphone.
     */
    private final boolean captureUsesDevice;

    /**
     * Tracks whether this call drives its playback through a platform device on the {@link #voipDriverManager}
     * rather than an application playback sink, so {@link #startAudioPlayback()} and {@link #close()} drive
     * the manager only when the call relies on a platform speaker.
     */
    private final boolean playbackUsesDevice;

    /**
     * Holds the capture reader pump draining the capture ring into the encoder, or {@code null} when the
     * audio send pipeline is not up.
     */
    private final AudioReaderPump readerPump;

    /**
     * Holds the playback writer pump filling the playback ring from the decoder, or {@code null} when the
     * audio receive pipeline is not up.
     */
    private final AudioWriterPump writerPump;

    /**
     * Holds the bridge presenting captured device blocks as the reader pump's {@link AudioOutput} source
     * when the call relies on a platform capture device, or {@code null} when the application supplied its
     * own capture source.
     */
    private final CaptureSourceBridge captureSource;

    /**
     * Holds the render loop delivering decoded frames to the application playback sink when the call
     * supplied one, or {@code null} when the call renders to a platform playback device through the writer
     * pump instead.
     */
    private final AudioPlaybackLoop playbackRenderLoop;

    /**
     * Holds the WebRTC audio processor conditioning the live microphone capture with echo cancellation and
     * noise suppression before the encoder, closed on teardown, or {@code null} when the capture is not a
     * live acoustic source or the native WebRTC APM shim is not built.
     *
     * <p>Engaged only when the capture source reports {@link AudioOutput#isLiveCapture()} and the shim is
     * {@linkplain WebRtcAudioProcessor#nativeApmAvailable() present}; a clean line level source (a file, a
     * tone, silence, or application written frames) and a tree without the native library both leave this
     * {@code null} and encode the capture unconditioned. When present, the capture reader pump drives its
     * {@link WebRtcAudioProcessor#process(short[], short[], short[])} from its single virtual thread,
     * honouring the processor's single writer contract; the far end reference it cancels against is the last
     * block the playback path rendered.
     */
    private final WebRtcAudioProcessor audioProcessor;

    /**
     * Holds the inbound NetEq jitter buffer the receiver inserts received packets into and pulls render
     * decisions from, flushed on teardown, or {@code null} when the audio receive pipeline is not up.
     */
    private final LiveNetEq netEq;

    /**
     * Holds the audio decoder the NetEq jitter buffer and the receiver share, closed on teardown, or
     * {@code null} when the audio receive pipeline is not up.
     */
    private final AudioDecoder audioDecoder;

    /**
     * Holds the live reference to the call's video pipeline, holding {@code null} on an audio only call,
     * when the video codec could not be opened, or until a mid call audio to video upgrade builds one.
     *
     * <p>This is the single source of truth for the call's video pipeline, shared with the
     * {@link InboundMediaDemux} and the transport's inbound RTCP listener so the transport receive thread
     * reads the pipeline a mid call upgrade publishes rather than a snapshot taken at bring up. The encode
     * loop, the render loop, and the inbound PLI key frame path therefore all observe one reference. The
     * holder is published into by {@link #startLocalVideo()} under the {@link #videoSendStarted} guard and
     * read everywhere through {@link AtomicReference#get()}, whose volatile semantics safely hand the upgrade's
     * pipeline to the receive thread.
     */
    private final AtomicReference<VideoPipeline> videoPipeline;

    /**
     * Holds the call's application data controller driving the in call reaction, transcription, rekey, and
     * feedback side channel over the SCTP data channel, closed on teardown.
     *
     * <p>Built by {@link LiveMediaPlane#assemble} with no inbound observers attached and exposed through
     * {@link #appDataController()} so the lifecycle controller can attach the in call control units (which
     * are built later than the media plane) to its observer seams. It owns the {@link AppDataChannel} that
     * writes app data as SCTP DATA through the transport's data channel send seam; closing it cancels its
     * reaction timers.
     */
    private final AppDataController appDataController;

    /**
     * Holds the call's voip param manager: the parsed {@code <voip_settings>} bundles stored by settings
     * type, the selected active set, and the dynamic rate control updater the rate control tick reads
     * through {@link #activeParams()}.
     *
     * <p>The manager is populated and its active set selected by {@link LiveMediaPlane#assemble} before the
     * session is constructed, so it is always non-{@code null}; a bring up that received no settings bundles
     * holds an empty manager with no active set.
     */
    private final LiveVoipParamManager voipParamManager;

    /**
     * Holds the call's bandwidth estimation and rate control loop: the sender side estimator, the GoogCC
     * delay based estimator, and the audio and video rate controllers, driven per inbound RTCP feedback and
     * by a periodic fallback tick.
     *
     * <p>Built by {@link LiveMediaPlane#assemble} and registered as the transport's inbound RTCP feedback
     * listener; the {@link InboundMediaDemux} tees each inbound media packet's arrival timing into its
     * delay based estimator. {@link #start()} starts its periodic fallback tick and {@link #close()} stops
     * it. It is never {@code null}: a relay session always brings up the rate control loop.
     */
    private final RateControlLoop rateControlLoop;

    /**
     * Holds the call's SFU subscription publisher: the receive subscription cache, the hop by hop
     * RTCP feedback table, and the {@link StreamDescriptors} built from this client's SSRC layout.
     *
     * <p>Built by {@link LiveMediaPlane#assemble} from the call's {@link StreamLayout} and the membership
     * {@linkplain com.github.auties00.cobalt.calls.engine.participant.ParticipantProvider provider}; the
     * session owns it and {@link #close()} releases it, cancelling its resend timer and clearing its tables.
     * The {@code 0x0003} subscription resend reaches the wire through the transport's keepalive thread
     * subscription hook ({@code LiveRelayTransport.onSubscriptionResend} {@literal ->}
     * {@code sendSubscriptionEnvelope}), driven by the same resender this publisher was built with; this
     * publisher is retained for its receive subscription cache and hop by hop RTCP feedback table, owned and
     * torn down with the call. It is never {@code null}: a relay session always builds it.
     */
    private final LiveSubscriptionPublisher subscriptionPublisher;

    /**
     * Holds the seam that starts the driver manager's camera capture for a call that sources outbound video
     * from a platform camera, or {@code null} when the call sends no video or supplied its own video source.
     *
     * <p>Bound after construction by {@link LiveMediaPlane#assemble} (the manager and the encode bridge are
     * built there) and invoked once by {@link #start()} after the video pipeline's encode loop is up, so the
     * camera begins capturing into a bridge the encoder is already draining. It is the device start
     * counterpart for video of {@link #startAudioCapture()} and {@link #startAudioPlayback()}.
     */
    private Runnable videoCaptureStarter;

    /**
     * Holds the seam that builds and publishes the video pipeline on a mid call audio to video upgrade, or
     * {@code null} on a call brought up with video (whose pipeline already exists) or a session that cannot
     * raise video.
     *
     * <p>Bound after construction by {@link LiveMediaPlane#assemble} only for a call brought up audio only,
     * because building the pipeline mid call needs the relay {@link MediaTransport}, the call's application
     * streams, the driver manager, and the rate control loop, all of which the assembler holds. It is run at
     * most once by {@link #startLocalVideo()} under the {@link #videoSendStarted} guard: it opens the
     * negotiated video codec, builds the inbound jitter buffer, binds the outbound video RTP sink onto the
     * live transport, brings the rate control loop's video controller online, and publishes the pipeline into
     * {@link #videoPipeline} so the receive thread routes inbound video and arms the encoder on an inbound
     * picture loss indication. It returns the built pipeline, or {@code null} when the codec could not be
     * opened (the call then stays audio only).
     */
    private java.util.function.Supplier<VideoPipeline> videoUpgrade;

    /**
     * Holds the datagram receive thread feeding inbound socket packets to the transport, or {@code null}
     * before start.
     */
    private Thread receiveThread;

    /**
     * Tracks whether the session is running, so the pump threads exit on close.
     */
    private final AtomicBoolean running;

    /**
     * Tracks whether the outbound video send path (the encode loop and the platform camera capture, when
     * device backed) has been started, so {@link #start()} and {@link #startLocalVideo()} drive it at most
     * once and a repeated in call camera turn on does not double start the encoder or the camera.
     *
     * <p>On a call brought up audio only it additionally gates the whole mid call upgrade: the thread that
     * wins the compare and set is the one that builds the video pipeline through {@link #videoUpgrade},
     * publishes it into {@link #videoPipeline}, and starts both loops, so a concurrent or repeated turn on
     * neither builds a second pipeline nor double publishes.
     */
    private final AtomicBoolean videoSendStarted;

    /**
     * Tracks whether {@link #close()} has run, so a second close is a no op.
     */
    private final AtomicBoolean closed;

    /**
     * Constructs a media session over its already built pipeline components.
     *
     * <p>This constructor only stores the wired components; {@link LiveMediaPlane#bringUp} builds them and
     * calls {@link #start()} to launch the pump threads and the device drivers. The audio component
     * references may be {@code null} when a device or codec could not be opened, in which case the
     * corresponding pipeline half stays idle while the transport and the other half still run.
     *
     * @param callId         the call identifier
     * @param transport      the media transport
     * @param channel        the host UDP socket backing the ICE/DTLS transport
     * @param relayAddress   the relay endpoint used as the bootstrap ICE/DTLS destination
     * @param audioCodec     the audio codec (Opus or MLow), or {@code null} when the audio pipeline is not up
     * @param repacketizer   the Opus repacketizer, or {@code null} on an MLow call or when the audio
     *                       pipeline is not up
     * @param voipDriverManager the engine's driver manager the platform capture and playback bring up routes
     *                          through; never {@code null}
     * @param captureUsesDevice whether the call drives a platform capture device through the manager rather
     *                          than an application capture source
     * @param playbackUsesDevice whether the call drives a platform playback device through the manager rather
     *                          than an application playback sink
     * @param readerPump        the capture reader pump, or {@code null} when the send pipeline is not up
     * @param writerPump        the playback writer pump driving a platform device, or {@code null} when the
     *                          application sink renders through the render loop or the receive pipeline is
     *                          not up
     * @param captureSource     the capture device to-{@link AudioOutput} bridge, or {@code null} when the
     *                          application supplied its own capture source
     * @param playbackRenderLoop the render loop delivering decoded frames to the application playback sink,
     *                          or {@code null} when a platform playback device is used
     * @param audioProcessor    the WebRTC audio processor conditioning the live microphone capture, or
     *                          {@code null} when the capture is not a live acoustic source or the native
     *                          WebRTC APM shim is not built
     * @param netEq             the inbound NetEq jitter buffer, or {@code null} when the receive pipeline is
     *                          not up
     * @param audioDecoder      the audio decoder the NetEq and receiver share, or {@code null}
     * @param videoPipeline     the shared live video pipeline holder, the same {@link AtomicReference} the
     *                          assembler handed the demux and the inbound RTCP listener, holding the built
     *                          pipeline or {@code null} on an audio only call
     * @param voipParamManager  the call's voip param manager holding the stored bundles and the selected
     *                          active set; never {@code null}
     * @param appDataController the call's application data controller driving the reaction, transcription,
     *                          rekey, and feedback side channel; never {@code null}
     * @param rateControlLoop   the call's bandwidth estimation and rate control loop, driven per inbound
     *                          RTCP feedback and by a periodic fallback tick; never {@code null}
     * @param subscriptionPublisher the call's SFU subscription publisher owning the receive subscription
     *                          cache, the RTCP feedback table, and this client's stream descriptors; never
     *                          {@code null}
     */
    private LiveMediaSession(String callId, MediaTransport transport,
                             DatagramChannel channel, SocketAddress relayAddress,
                             AudioCodec audioCodec, OpusRepacketizer repacketizer,
                             VoipDriverManager voipDriverManager, boolean captureUsesDevice,
                             boolean playbackUsesDevice,
                             AudioReaderPump readerPump, AudioWriterPump writerPump,
                             CaptureSourceBridge captureSource, AudioPlaybackLoop playbackRenderLoop,
                             WebRtcAudioProcessor audioProcessor,
                             LiveNetEq netEq, AudioDecoder audioDecoder,
                             AtomicReference<VideoPipeline> videoPipeline, LiveVoipParamManager voipParamManager,
                             AppDataController appDataController, RateControlLoop rateControlLoop,
                             LiveSubscriptionPublisher subscriptionPublisher) {
        this.callId = callId;
        this.transport = transport;
        this.channel = channel;
        this.relayAddress = relayAddress;
        this.audioCodec = audioCodec;
        this.repacketizer = repacketizer;
        this.voipDriverManager = voipDriverManager;
        this.captureUsesDevice = captureUsesDevice;
        this.playbackUsesDevice = playbackUsesDevice;
        this.readerPump = readerPump;
        this.writerPump = writerPump;
        this.captureSource = captureSource;
        this.playbackRenderLoop = playbackRenderLoop;
        this.audioProcessor = audioProcessor;
        this.netEq = netEq;
        this.audioDecoder = audioDecoder;
        this.videoPipeline = videoPipeline;
        this.voipParamManager = voipParamManager;
        this.appDataController = appDataController;
        this.rateControlLoop = rateControlLoop;
        this.subscriptionPublisher = subscriptionPublisher;
        this.running = new AtomicBoolean();
        this.videoSendStarted = new AtomicBoolean();
        this.closed = new AtomicBoolean();
    }

    /**
     * Returns a snapshot copy of the call's currently active voip param set.
     *
     * <p>The active set is the bundle the bring up selected for the call's media mode, with any later
     * participant count and dynamic rate control overrides applied. The later rate control tick reads this
     * accessor each round to source its bandwidth bounds and congestion masks from the negotiated
     * parameters, falling back to compiled in defaults when the set is absent. A call that received no
     * {@code <voip_settings>} bundles, or whose default bundle was missing, has no active set and yields
     * {@link Optional#empty()}.
     *
     * @return a copy of the active voip param set, or {@link Optional#empty()} when none is selected
     */
    Optional<VoipParams> activeParams() {
        return voipParamManager.activeParams();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation always carries an app data controller because the relay transport this
     * session brings up always opens the relay app data RTP stream, so the returned {@link Optional} is
     * never empty for a live relay session.
     */
    @Override
    public Optional<AppDataController> appDataController() {
        return Optional.of(appDataController);
    }

    /**
     * Launches the session's transport, pump threads, and device drivers.
     *
     * <p>Starts the media transport (which begins its ICE and DTLS bring up) and the inbound socket receive
     * loop, then the capture and playback pumps and their device drivers and, when present, the video
     * pipeline; when the call sources outbound video from a platform camera, the camera capture is started
     * last, after the encode loop is up, so the camera feeds a bridge the encoder is already draining. A
     * device start failure is logged and isolated so the rest of the plane still runs; a transport start
     * failure propagates so the bring up fails as a whole.
     *
     * <p>The rate control loop's periodic fallback tick is also started here; the per feedback tick is
     * already armed before start through the transport's inbound RTCP listener the bring up registered.
     *
     * @throws WhatsAppCallException if the transport cannot be started
     */
    private void start() {
        running.set(true);
        transport.start();

        receiveThread = Thread.ofVirtual().name("calls-media-ingress-" + callId).start(this::receiveLoop);

        startAudioPlayback();
        startAudioCapture();
        var pipeline = videoPipeline.get();
        if (pipeline != null) {
            pipeline.start();
            startVideoSend();
        }
        rateControlLoop.start();
    }

    /**
     * Starts the outbound video send path once: the encode loop and, when the call sources video from a
     * platform camera, the camera capture.
     *
     * <p>Shared by {@link #start()} (the eager start for a call brought up with video, so its picture flows
     * from connection) and {@link #startLocalVideo()} (the in call camera turn on). The
     * {@link #videoSendStarted} guard makes it idempotent so a call already sending video, or a repeated
     * turn on, neither re launches the encode loop nor double starts the camera. The inbound video render loop
     * is started separately in {@link VideoPipeline#start()} so a video call always renders the peer's picture
     * regardless of whether the local side is sending. A {@code null} {@link #videoPipeline} (an audio only
     * call) has no encode loop to start, so this is a no op for it.
     */
    private void startVideoSend() {
        var pipeline = videoPipeline.get();
        if (pipeline == null) {
            return;
        }
        if (!videoSendStarted.compareAndSet(false, true)) {
            return;
        }
        beginOutboundVideo(pipeline);
    }

    /**
     * Starts the outbound encode loop and, when device backed, the platform camera capture for an already
     * built and published pipeline.
     *
     * <p>Shared by the eager {@link #startVideoSend()} and the in call upgrade in {@link #startLocalVideo()};
     * both hold the {@link #videoSendStarted} one shot guard before calling it, so this performs no further
     * gating. The camera capture is started after the encode loop so the camera feeds a bridge the encoder is
     * already draining, matching the bring up order.
     *
     * @param pipeline the built, published video pipeline whose encode loop is started; never {@code null}
     */
    private void beginOutboundVideo(VideoPipeline pipeline) {
        pipeline.startEncode();
        if (videoCaptureStarter != null) {
            videoCaptureStarter.run();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation reproduces the media plane half of the audio to video upgrade: a call
     * placed or accepted with video already has its pipeline and camera started at bring up, so the
     * {@link #videoSendStarted} guard makes an in call camera turn on a no op for it; a call brought up
     * audio only builds its video pipeline here, the first time the camera turns on. The build opens the
     * negotiated {@link VideoCodec}, builds the inbound {@link VideoJitterBuffer}, binds the outbound video
     * RTP sink onto the live {@link MediaTransport}, brings the rate control loop's video controller online in
     * the same transaction, then publishes the pipeline into {@link #videoPipeline} so the transport receive
     * thread routes inbound video and arms the encoder on an inbound picture loss indication, and finally
     * starts the inbound render loop and the outbound encode loop. The {@code video_state} signaling announce
     * is the other half and is driven by {@link LifecycleController#startLocalVideo(String)} under the call
     * lock, not from this media plane seam.
     *
     * <p>Three native sub behaviours are deliberately not reproduced and are tracked as honest gaps below:
     * the audio stream is not destroyed and recreated, the new video stream draws a fresh local SSRC rather
     * than the participant setup allocated one, and no SFU stream subscription update is sent.
     *
     * @implSpec The publication is volatile (the {@link #videoPipeline} holder is an {@link AtomicReference}),
     * and the build is gated one shot by {@link #videoSendStarted}, so a concurrent transport receive thread
     * either sees no pipeline (and routes the packet as audio, the audio only behaviour) or sees the fully
     * built pipeline; it never observes a half constructed one. A repeated turn on, or a turn on on a call
     * already sending video, is a no op.
     */
    @Override
    public void startLocalVideo() {
        if (videoPipeline.get() != null) {
            startVideoSend();
            return;
        }
        if (videoUpgrade == null) {
            return;
        }
        // The one shot guard gates the whole upgrade: only the winner builds, publishes, and starts the
        // pipeline. A loser (a concurrent or repeated turn on) returns without touching the holder.
        if (!videoSendStarted.compareAndSet(false, true)) {
            return;
        }
        // The seam opens the codec, builds the jitter buffer, binds the transport video sink, and brings the
        // rate control video controller online; it returns null when the codec could not be opened, in which
        // case the call stays audio only (the guard is already spent, so the failed upgrade is terminal,
        // matching a video from start call whose codec failed to open at bring up).
        var pipeline = videoUpgrade.get();
        if (pipeline == null) {
            return;
        }
        // Publish before starting either loop so the receive thread sees the pipeline as soon as it can route
        // a packet to it. The render loop renders the peer's picture; the encode loop and camera capture raise
        // the local track.
        videoPipeline.set(pipeline);
        pipeline.start();
        beginOutboundVideo(pipeline);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation forwards an outbound key frame request to the video pipeline's
     * {@link VideoPipeline#requestKeyFrame()} passthrough, which arms the encoder so its next encode produces
     * an intra frame; the request is one shot and consumed by the encode loop's next pass. A session that
     * brought up no video pipeline (an audio only call) has no encoder to arm, so this is a no op for it.
     */
    @Override
    public void requestKeyFrame() {
        var pipeline = videoPipeline.get();
        if (pipeline != null) {
            pipeline.requestKeyFrame();
        }
    }

    /**
     * Binds the seam {@link #startVideoSend()} runs to start the driver manager's camera capture.
     *
     * <p>The manager and the encode bridge are built by {@link LiveMediaPlane#assemble} after this session,
     * so the camera capture start cannot be passed to the constructor; the plane binds it here before
     * {@link #start()}, the same post construction wiring the video pipeline's frame sink uses. It is bound
     * only when the call sources outbound video from a platform camera (a video call that supplied no
     * application video source whose codec opened). It is run once, when {@link #startVideoSend()} first starts
     * the outbound video send path (eagerly from {@link #start()} for a video call, or from
     * {@link #startLocalVideo()} for an in call camera turn on).
     *
     * @param videoCaptureStarter the seam that starts the manager's camera capture; never {@code null}
     */
    private void bindVideoCaptureStarter(Runnable videoCaptureStarter) {
        this.videoCaptureStarter = videoCaptureStarter;
    }

    /**
     * Binds the seam {@link #startLocalVideo()} runs to build the video pipeline on a mid call upgrade.
     *
     * <p>The seam needs the live transport, the application streams, and the rate control loop, all built by
     * {@link LiveMediaPlane#assemble} around this session, so it cannot be passed to the constructor; the
     * plane binds it here before {@link #start()}, the same post construction wiring the camera capture
     * starter uses. It is bound only on a call brought up audio only (a call already carrying video has its
     * pipeline and needs none). It is run at most once, by {@link #startLocalVideo()} under the
     * {@link #videoSendStarted} guard, the first time the camera turns on.
     *
     * @param videoUpgrade the seam that builds and returns the upgrade video pipeline, or returns
     *                     {@code null} when the codec could not be opened; never {@code null}
     */
    private void bindVideoUpgrade(java.util.function.Supplier<VideoPipeline> videoUpgrade) {
        this.videoUpgrade = videoUpgrade;
    }

    /**
     * Starts the audio receive pipeline: either the application render loop or a platform playback device
     * fed by the writer pump.
     *
     * <p>When the call supplied an application playback sink the render loop is started and the platform
     * device is not opened; otherwise the platform playback device and its demand driven writer pump are
     * started. A start failure is logged and isolated so the send path and the transport still run; the call
     * then carries outbound audio without local playback.
     */
    private void startAudioPlayback() {
        if (playbackRenderLoop != null) {
            playbackRenderLoop.start();
            return;
        }
        if (!playbackUsesDevice || writerPump == null) {
            return;
        }
        try {
            // The native WebAudio playback ring is float32 at 16 kHz mono in 320 sample (20 ms) chunks: the
            // audio worklet views the shared buffer as a float array and the sample rate and chunk default to
            // 16000 and 320. The rate and chunk are passed faithfully here (AUDIO_SAMPLE_RATE_HZ,
            // AUDIO_FRAME_SAMPLES). The bits per sample value is ignored by the ring (the ring is always
            // float32), so it is not a startPlayback argument.
            // TODO: render the playback device boundary as float32 to match the native ring. Cobalt's audio
            //  pipeline (AudioPlaybackRing, AudioWriterPump, the decoder pull) is int16 (short[]) end to end,
            //  and startPlayback carries no element type argument, so the int16 to float32 conversion belongs
            //  to the not yet implemented FFM playback driver rather than this call site; until that driver
            //  lands the element type mismatch is confined to the device boundary, not the relay media path.
            voipDriverManager.startPlayback(null, AUDIO_SAMPLE_RATE_HZ, AUDIO_FRAME_SAMPLES, AUDIO_CHANNELS);
            writerPump.start();
        } catch (RuntimeException exception) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING,
                        "calls playback start failed for call {0}: {1}", callId, exception.getMessage());
            }
        }
    }

    /**
     * Starts the audio send pipeline: the reader pump draining the capture source, and the platform capture
     * device behind it when one was opened.
     *
     * <p>When the call supplied an application capture source the reader pump drains it directly with no
     * platform device to initialize; otherwise the platform capture device is started and the reader pump
     * drains its bridge. A start failure is logged and isolated so the receive path and the transport still
     * run; the call then carries inbound audio without local capture.
     */
    private void startAudioCapture() {
        if (readerPump == null) {
            return;
        }
        if (!captureUsesDevice) {
            readerPump.start();
            return;
        }
        try {
            // The native WebAudio capture ring is float32 at 16 kHz mono in 320 sample (20 ms) chunks: the
            // audio worklet views the shared buffer as a float array and the sample rate and chunk default to
            // 16000 and 320 (a browser that will not honor a 16 kHz context resamples 48 kHz to 16 kHz in the
            // worklet before writing the ring, so the ring is 16 kHz float32 on every browser). The rate and
            // chunk are passed faithfully here (AUDIO_SAMPLE_RATE_HZ, AUDIO_FRAME_SAMPLES); the bits per sample
            // value is ignored by the ring and is not a startAudioCapture argument.
            // TODO: present the capture device boundary as float32 to match the native ring. Cobalt's audio
            //  pipeline (AudioCaptureRing, AudioReaderPump, the encoder accept) is int16 (short[]) end to end,
            //  and startAudioCapture carries no element type argument, so the float32 to int16 conversion
            //  belongs to the not yet implemented FFM capture driver rather than this call site; until that
            //  driver lands the element type mismatch is confined to the device boundary, not the relay media
            //  path.
            voipDriverManager.startAudioCapture(AudioDeviceType.MICROPHONE, null, AUDIO_SAMPLE_RATE_HZ,
                    AUDIO_FRAME_SAMPLES, AUDIO_CHANNELS);
            readerPump.start();
        } catch (RuntimeException exception) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING,
                        "calls capture start failed for call {0}: {1}", callId, exception.getMessage());
            }
        }
    }

    /**
     * Reads inbound datagrams from the host socket and hands each, with its source address, to the
     * transport until the session stops.
     *
     * <p>Each datagram the channel delivers is copied to a fresh array and passed to
     * {@link MediaTransport#onInboundDatagram(byte[], SocketAddress)} together with the source address the
     * channel reports, which the transport's ICE/DTLS demultiplex needs to answer a binding request. A
     * closed channel ends the loop; a transient read error is logged and the loop continues.
     */
    private void receiveLoop() {
        var buffer = ByteBuffer.allocate(MAX_DATAGRAM_BYTES);
        while (running.get()) {
            buffer.clear();
            try {
                var source = channel.receive(buffer);
                if (source == null) {
                    continue;
                }
                buffer.flip();
                // The transport takes ownership of each datagram array and may retain it past this call: a
                // DTLS classified datagram is offered by RelayDataChannel.feedDtlsRecord into a cross thread
                // queue the DTLS pump drains, so each datagram is handed off in its own array.
                var datagram = new byte[buffer.remaining()];
                buffer.get(datagram);
                transport.onInboundDatagram(datagram, source);
            } catch (java.nio.channels.ClosedChannelException exception) {
                break;
            } catch (IOException exception) {
                if (running.get() && Log.DEBUG) {
                    LOGGER.log(Level.DEBUG,
                            "calls media receive error for call {0}: {1}", callId, exception.getMessage());
                }
            }
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        running.set(false);
        stopQuietly(rateControlLoop::stop);
        stopQuietly(readerPump == null ? null : readerPump::stop);
        stopQuietly(!captureUsesDevice ? null
                : () -> voipDriverManager.stopAudioCapture(AudioDeviceType.MICROPHONE));
        stopQuietly(captureSource == null ? null : captureSource::shutdown);
        // Release the capture APM (native arena) after the reader pump that drives its process() is stopped.
        stopQuietly(audioProcessor == null ? null : audioProcessor::close);
        stopQuietly(playbackRenderLoop == null ? null : playbackRenderLoop::stop);
        stopQuietly(!playbackUsesDevice ? null : () -> {
            if (writerPump != null) {
                writerPump.stop();
            }
            voipDriverManager.stopPlayback();
        });
        var pipeline = videoPipeline.get();
        // Stop the manager camera whenever a pipeline exists rather than only when videoUsesDevice was set at
        // bring up: a mid call upgrade may have started a device camera on a call brought up audio only, and
        // stopVideoCapture is a documented no op when no manager camera ran (an app supplied VideoOutput), so
        // this safely covers both the bring up with video and the upgraded mid call cases.
        stopQuietly(pipeline == null ? null : voipDriverManager::stopVideoCapture);
        stopQuietly(pipeline == null ? null : pipeline::close);
        stopQuietly(appDataController::close);
        stopQuietly(subscriptionPublisher::close);
        interruptQuietly(receiveThread);
        stopQuietly(transport::close);
        stopQuietly(() -> {
            if (audioCodec != null) {
                audioCodec.close();
            }
        });
        stopQuietly(() -> {
            if (repacketizer != null) {
                repacketizer.close();
            }
        });
        stopQuietly(netEq == null ? null : netEq::flush);
        stopQuietly(audioDecoder == null ? null : audioDecoder::close);
        stopQuietly(() -> {
            try {
                channel.close();
            } catch (IOException exception) {
                throw new java.io.UncheckedIOException(exception);
            }
        });
    }

    /**
     * Runs an action, swallowing any {@link RuntimeException} it raises.
     *
     * <p>Used on the teardown path so one component's close failure does not prevent the rest of the
     * session from being released. A {@code null} action is a no op.
     *
     * @param action the teardown action to run, or {@code null}
     */
    private static void stopQuietly(Runnable action) {
        if (action == null) {
            return;
        }
        try {
            action.run();
        } catch (RuntimeException exception) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "calls media teardown step failed", exception);
        }
    }

    /**
     * Interrupts a thread if it is live, ignoring a {@code null} reference.
     *
     * @param thread the thread to interrupt, or {@code null}
     */
    private static void interruptQuietly(Thread thread) {
        if (thread != null) {
            thread.interrupt();
        }
    }

    /**
     * The production {@link MediaPlane}: brings up a {@link LiveMediaSession} from a call's relay
     * block and call key, wiring the codec, SFrame, and transport units into the two way media chain.
     *
     * <p>The factory is constructed once per engine and reused for every call. It parses the relay block
     * for the relay endpoint and the hop by hop key, derives the hop by hop SRTP context, opens the host
     * UDP socket that backs the ICE/DTLS transport, builds the audio and (when requested) video pipelines,
     * and starts them. A bring up that cannot satisfy a step releases whatever it partially built and
     * surfaces the failure as a non fatal {@link WhatsAppCallException.DataChannel}.
     *
     * @implNote This implementation reports a media plane connection to the lifecycle controller through
     * the {@code connectionSink} when the transport reports its first traffic. The connection sink is bound
     * after the controller is constructed (the controller and this factory are mutually dependent), so it is
     * held behind a mutable reference the assembler sets.
     */
    public static final class LiveMediaPlane implements MediaPlane {
        /**
         * The logger for {@link LiveMediaPlane}.
         */
        private static final System.Logger LOGGER = Log.get(LiveMediaPlane.class);

        /**
         * Holds the host the brought up sessions reach for the datagram fallback, the rendered video sink,
         * and randomness.
         */
        private final VoipHostApi host;

        /**
         * Holds the local account's own {@code <lid>:<device>@lid} device JID, the keying input for the
         * deterministic media SSRCs and the per participant SFrame base key, or {@code null} until the client
         * is logged in.
         *
         * <p>The self device JID is derived from this account sub-store as the LID device form (the account LID
         * user with the device number off the account JID), the form {@link CallSecureSsrcGenerator} and the
         * SFrame derivation are byte verified against; read live per bring up through {@link #selfDeviceJid()}
         * so a self JID that became known after the engine was built (the engine is built before the client
         * logs in) is picked up.
         */
        private final LinkedWhatsAppAccountStore accountStore;

        /**
         * The running wall clock deadline, in nanos, the next media send is held until; advanced by each
         * packet's own audio duration and resynced to the current time after a stall so the send cadence
         * tracks the audio clock smoothly without bursting to catch up.
         */
        private long sendPacingDeadlineNanos;

        /**
         * The RTP timestamp of the previously paced media send, used to measure the audio duration by which
         * each packet advances the deadline.
         */
        private long sendPacingLastTimestamp;

        /**
         * Whether the send pacing deadline has been seeded by the first media send.
         */
        private boolean sendPacingStarted;

        /**
         * The running wall clock deadline, in nanos, the next outbound video access unit is held until;
         * advanced by each frame's presentation timestamp span and resynced to the current time after a stall,
         * so the video send cadence tracks the source frame rate smoothly without bursting to catch up.
         */
        private long videoSendPacingDeadlineNanos;

        /**
         * The presentation timestamp, in microseconds, of the previously paced video send, used to measure the
         * span by which each access unit advances the deadline.
         */
        private long videoSendPacingLastPtsMicros;

        /**
         * Whether the video send pacing deadline has been seeded by the first video send.
         */
        private boolean videoSendPacingStarted;

        /**
         * Holds the engine's single driver manager, the one per engine registry every brought up session
         * routes its platform audio capture and playback and its no application source video capture
         * through.
         *
         * <p>The assembler builds and {@linkplain VoipDriverManager#initialize() initializes} the manager
         * once and shares it across every call this plane brings up, matching the engine's one driver
         * manager per client; a session opens no device directly.
         */
        private final VoipDriverManager voipDriverManager;

        /**
         * The server calling feature gate, read per bring up for the group call initial bandwidth estimate
         * seed decision.
         *
         * <p>{@link CallsFeatureGate#isInitBweForGroupCallEnabled()} is read per bring up rather than captured
         * at build time so the value reflects the warmed AB props cache by the time a call starts (the engine
         * is built at client construction, before the first AB props sync) and is not read on a possibly cold
         * cache. Threaded into each group call's {@link RateControlLoop} so the loop seeds its initial estimate
         * for a group call rather than ramping from the conservative cold start band; a one to one call ignores
         * it.
         */
        private final CallsFeatureGate featureGate;

        /**
         * Constructs a media plane over the given host and the engine's driver manager.
         *
         * @param host              the host for the datagram fallback, video render, and randomness
         * @param accountStore      the account sub-store the self device JID is derived from, read live per
         *                          bring up through {@link #selfDeviceJid()}
         * @param voipDriverManager the engine's initialized driver manager the brought up sessions route their
         *                          platform capture and playback through
         * @param featureGate       the server calling feature gate, read per bring up for the group call
         *                          initial bandwidth estimate seed decision
         * @throws NullPointerException if any argument is {@code null}
         */
        public LiveMediaPlane(VoipHostApi host, LinkedWhatsAppAccountStore accountStore,
                       VoipDriverManager voipDriverManager, CallsFeatureGate featureGate) {
            this.host = Objects.requireNonNull(host, "host cannot be null");
            this.accountStore = Objects.requireNonNull(accountStore, "accountStore cannot be null");
            this.voipDriverManager = Objects.requireNonNull(voipDriverManager, "voipDriverManager cannot be null");
            this.featureGate = Objects.requireNonNull(featureGate, "featureGate cannot be null");
        }

        /**
         * Returns the local account's own LID device JID, the {@code <lid>:<device>@lid} form the media plane
         * keys its per device SSRCs and per participant SFrame base key on, or {@code null} when the account
         * is not yet paired.
         *
         * <p>Composed live from the injected account sub-store: the account LID user and the {@code @lid}
         * domain, with the device number carried on the account phone number JID. Reading live per bring up
         * picks up a LID that became known after the engine was built. When the session is not yet LID paired
         * this falls back to the account phone number JID; the plane's own random layout fallback covers a
         * fully unseeded holder.
         *
         * @return the own LID device JID, or the account phone number JID when no LID is set, or {@code null}
         *         when the account is not yet paired
         */
        private Jid selfDeviceJid() {
            var lid = accountStore.lid().orElse(null);
            if (lid == null) {
                return accountStore.jid().orElse(null);
            }
            var device = accountStore.jid().map(Jid::device).orElse(0);
            return lid.withServer(JidServer.lid()).withDevice(device);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote This implementation parses the relay {@code <te2>} endpoint and the relay
         * {@code <hbh_key>}, opens the host {@link DatagramChannel} that backs the ICE/DTLS transport, derives
         * the hop by hop SRTP master through {@link CallE2eKeyDerivation#deriveHbhSrtpMaster} and, when the
         * relay advertises a positive {@code warp_mi_tag_len}, the WARP message integrity key through the
         * matching two step chain {@link CallE2eKeyDerivation#deriveWarpAuthKey}, builds the
         * {@link LiveRelayTransport} that carries media as SCTP DATA, opens
         * the Opus codec and the SFrame chain (on a group call), parses the {@code <voip_settings>} bundles into
         * a {@link LiveVoipParamManager} and selects the active set for the call's media mode, and starts
         * every pump. The local media SSRC layout is derived deterministically from the self device JID (read
         * live through {@link #selfDeviceJid()}, the same JID the SFrame key derivation uses) and {@code callId}
         * through {@link #buildStreamLayout(String, Jid, boolean)} rather than drawn at random; a bring up
         * before the self JID is known falls back to a random layout. A relay block missing an endpoint or a
         * hop by hop key, or a socket or codec that cannot be created, surfaces as a
         * {@link WhatsAppCallException.DataChannel}.
         */
        @Override
        public Session bringUp(String callId, Stanza relay, List<Stanza> voipSettings, byte[] callKey,
                               boolean isCaller, boolean video, int participantCount,
                               CallMembership membership, MediaStreams streams, Jid peerDeviceJid,
                               Optional<String> electedRelayName, MediaSessionListener listener) {
            Objects.requireNonNull(callId, "callId cannot be null");
            Objects.requireNonNull(relay, "relay cannot be null");
            Objects.requireNonNull(voipSettings, "voipSettings cannot be null");
            Objects.requireNonNull(callKey, "callKey cannot be null");
            Objects.requireNonNull(streams, "streams cannot be null");
            Objects.requireNonNull(listener, "listener cannot be null");
            var relayInfo = RelayInfo.of(relay)
                    .orElseThrow(() -> new WhatsAppCallException.DataChannel(
                            "relay block for call " + callId + " is not a <relay> element"));

            // Parse the negotiated <voip_settings> bundles and select the active set up front so the codec
            // selector can read the per call encode.use_mlow_codec_v1 flag before the codec is opened, and so the
            // relay connection selection can read the vp->enable_edgeray_dtls_active_mode DTLS role flag (a
            // voip param, not a <relay> element field); the same manager is threaded into assemble so it is
            // not rebuilt.
            var voipParamManager = buildVoipParamManager(voipSettings, video, participantCount, isCaller,
                    peerDeviceJid);
            var useMlowCodec = useMlowCodec(voipParamManager);

            var relayConnection = selectRelayConnection(relayInfo, edgerayDtlsActiveMode(voipParamManager),
                    electedRelayName)
                    .orElseThrow(() -> new WhatsAppCallException.DataChannel(
                            "relay block for call " + callId + " carries no usable endpoint"));
            var relayAddress = relayConnection.address();
            var hopByHopKey = parseHopByHopKey(relay)
                    .orElseThrow(() -> new WhatsAppCallException.DataChannel(
                            "relay block for call " + callId + " carries no hbh_key"));

            DatagramChannel channel = null;
            LiveHbhSrtpRelay hbhSrtp = null;
            AudioCodec audioCodec = null;
            OpusRepacketizer repacketizer = null;
            try {
                // The host UDP socket is the RTCPeerConnection equivalent ICE/DTLS transport socket; it is
                // left unconnected so the ICE checks and DTLS records can be sent to each candidate
                // destination rather than to a single fixed relay address.
                channel = DatagramChannel.open();
                hbhSrtp = LiveHbhSrtpRelay.fromHopByHopKey(hopByHopKey, SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_80);

                // Hop by hop WARP message integrity: the relay turns it on by advertising a positive
                // warp_mi_tag_len attribute on the <relay> block (the same attribute RelayInfo parses). The
                // tag is keyed by the warp auth key, a two step chained HKDF over the same relay hop by hop
                // key split the hbh SRTP master derives from (warp auth salt then warp auth key, output 32),
                // not a flat single step HKDF. The key is derived only when the relay enables MI, and an absent
                // or non positive length leaves MI off and the key underived.
                var warpMiTagLength = Math.max(0, relay.getAttributeAsInt("warp_mi_tag_len", 0));
                var warpAuthKey = warpMiTagLength > 0
                        ? CallE2eKeyDerivation.deriveWarpAuthKey(hopByHopKey)
                        : null;

                // Push the raw E2E call key into the membership so every participant's crypto block (SFrame
                // chain key plus SRTP master) is derived per its active device JID on offer accept. This is
                // the seam the crypto core exposes (CallMembership.installCallKey, keygen version 2); the
                // lifecycle controller re installs the rotated key here again on each enc rekey. A one to one
                // call carries no membership and a key that is not the full raw length cannot derive, so both
                // are skipped.
                if (membership != null && callKey.length == CallE2eKeyDerivation.RAW_E2E_KEY_LENGTH) {
                    membership.installCallKey(callKey, CallE2eKeyDerivation.SUPPORTED_KEYGEN_VER);
                }

                var sframeProvider = video ? groupSframeProvider(callKey) : null;
                // MLow self packs its 60 ms packet, so it needs no Opus repacketizer; the Opus path opens
                // both the codec and the repacketizer. The repacketizer stays null on the MLow path so no
                // unused libopus repacketizer state is allocated.
                if (useMlowCodec) {
                    audioCodec = new MLowAudioCodec();
                } else {
                    audioCodec = new OpusAudioCodec(defaultAudioParams());
                    repacketizer = new OpusRepacketizer();
                }

                // The local SSRC layout the encode path stamps and the subscription layer advertises is the
                // deterministic set derived for the self device, keyed by the call id and the device JID (the
                // same self JID the SFrame key derivation reads), never random.
                var streamLayout = buildStreamLayout(callId, selfDeviceJid(), video);

                // One to one media is end to end SRTP the relay forwards opaquely, keyed by the per participant
                // master the call key and device JID derive, not the relay hop by hop SRTP a group or SFU leg
                // uses. Build the self (outbound) and peer (inbound) contexts on a one to one call (no
                // membership) with a full raw key and a resolvable peer device; a group call leaves them null
                // and keeps the hop by hop path.
                E2eMediaSrtp e2eSend = null;
                E2eMediaSrtp e2eRecv = null;
                if (membership == null && callKey.length == CallE2eKeyDerivation.RAW_E2E_KEY_LENGTH) {
                    var localDeviceJid = selfDeviceJid();
                    // The E2E recv key is keyed by the peer's DEVICE JID (for example "...:2@lid"), which the
                    // signaling layer recorded; the relay block's <participant> list carries only the
                    // user level (bare) JID, so prefer the signaling device JID and fall back to the relay
                    // participant (matched by peer_pid) only when the device JID is not yet known.
                    var resolvedPeer = peerDeviceJid;
                    if (resolvedPeer == null) {
                        var peerPid = relayInfo.peerPidValue();
                        for (var participant : relayInfo.participants()) {
                            if (peerPid.isPresent()
                                    ? participant.pid() == peerPid.getAsInt()
                                    : !participant.jid().equals(localDeviceJid)) {
                                resolvedPeer = participant.jid();
                                break;
                            }
                        }
                    }
                    if (localDeviceJid != null && resolvedPeer != null) {
                        e2eSend = new E2eMediaSrtp(
                                CallE2eKeyDerivation.deriveSrtpMaster(callKey, localDeviceJid));
                        e2eRecv = new E2eMediaSrtp(
                                CallE2eKeyDerivation.deriveSrtpMaster(callKey, resolvedPeer));
                        if (Log.INFO) {
                            LOGGER.log(Level.INFO,
                                    "calls one-to-one E2E-SRTP engaged: self={0}(sendAudioSsrc=0x{2}) peer={1}(expectAudioSsrc=0x{3})",
                                    localDeviceJid, resolvedPeer,
                                    Integer.toHexString(CallSecureSsrcGenerator.audioMainSsrc(callId, localDeviceJid)),
                                    Integer.toHexString(CallSecureSsrcGenerator.audioMainSsrc(callId, resolvedPeer)));
                        }
                    } else {
                        if (Log.WARNING) {
                            LOGGER.log(Level.WARNING,
                                    "calls one-to-one E2E-SRTP NOT engaged (self={0} peer={1}); media stays hop-by-hop",
                                    localDeviceJid, resolvedPeer);
                        }
                    }
                }
                // The local device's relay assigned participant id (the <relay> block's self_pid attribute,
                // RelayInfo.selfPidValue), the same PID space the receive subscription pidResolver maps peer
                // JIDs into from the relay <participant> list. It is stamped into every self sender subscription
                // descriptor (the 0x4025 SSrcsToPidAssignments pid field) so the SFU maps each forwarded SSRC
                // back to this device. A connected relay leg always assigns it; an absent attribute (a malformed
                // block) falls back to participant 0.
                var selfPid = relayInfo.selfPidValue().orElse(0);
                return assemble(callId, relayConnection, channel, hbhSrtp, warpAuthKey, warpMiTagLength,
                        audioCodec, repacketizer, sframeProvider, isCaller, video, voipParamManager, participantCount,
                        streamLayout, membership, streams, useMlowCodec, e2eSend, e2eRecv, selfPid, listener);
            } catch (WhatsAppCallException exception) {
                releaseQuietly(channel, hbhSrtp, audioCodec, repacketizer);
                throw exception;
            } catch (IOException exception) {
                releaseQuietly(channel, hbhSrtp, audioCodec, repacketizer);
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "calls media bring-up socket open failed for call " + callId,
                            exception);
                }
                throw new WhatsAppCallException.DataChannel(
                        "could not open the transport datagram socket for call " + callId, exception);
            } catch (RuntimeException exception) {
                releaseQuietly(channel, hbhSrtp, audioCodec, repacketizer);
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "calls media bring-up failed for call " + callId, exception);
                }
                throw new WhatsAppCallException.DataChannel(
                        "could not bring up the media plane for call " + callId, exception);
            } catch (UnsatisfiedLinkError error) {
                // A host without the bundled native media libraries (libopus, libsrtp, OpenH264) cannot
                // bring the media plane up; surface it as the non fatal call exception the controller
                // isolates rather than letting the Error escape the call handling thread.
                releaseQuietly(channel, hbhSrtp, audioCodec, repacketizer);
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING,
                            "calls media bring-up native libraries unavailable for call " + callId, error);
                }
                throw new WhatsAppCallException.DataChannel(
                        "the native media libraries are unavailable for call " + callId, error);
            }
        }

        /**
         * Assembles and starts the session from its already opened transport and codec components and the
         * call's application media streams.
         *
         * <p>Builds the relay transport with the SFrame aware media sink, the capture and playback audio
         * pipelines, the optional video pipeline, the {@link LiveMediaSession}, and starts it. The transport's
         * first traffic event drives the media connected notification to the controller. The capture and
         * playback halves prefer the application streams: when {@code streams} carries an
         * {@linkplain MediaStreams#audioCapture() audio capture source}, the reader pump drains it
         * directly (a microphone bound source captures from its device behind the same interface), and when it
         * carries an {@linkplain MediaStreams#audioPlayback() audio playback sink}, a render loop
         * delivers each decoded frame to it (a speaker bound sink renders to its device); only a stream the
         * call did not supply falls back to a platform device started through the {@link #voipDriverManager}.
         * Video follows the same rule: a supplied {@linkplain MediaStreams#videoCapture() video capture
         * source} drives the encode loop directly, and a video call that supplied none has the
         * {@link #voipDriverManager} start a platform camera whose frames feed the encode loop through a
         * {@link ManagerVideoSourceBridge} (a camera the manager cannot start leaves the call receive only on
         * video).
         *
         * <p>It also brings up the app data plane: an {@link AppDataChannel} that writes each
         * serialized app data payload as SCTP DATA through the transport's data channel send seam, and an
         * {@link AppDataController} that demultiplexes inbound app data to its reaction, transcription, rekey,
         * and feedback seams. The controller is built with no inbound observers attached; the in call control
         * units the lifecycle controller builds later attach themselves to it through
         * {@link #appDataController()}. Routing inbound app data from the SCTP data channel into this
         * controller is a later capture phase that is not wired yet.
         *
         * @param callId         the call identifier
         * @param relayConnection the selected relay endpoint with its ICE credentials and DTLS role, the
         *                       bootstrap destination the ICE checks and DTLS records are sent to
         * @param channel        the host UDP socket backing the ICE/DTLS transport
         * @param hbhSrtp        the hop by hop SRTP context
         * @param warpAuthKey    the hop by hop WARP message integrity key derived for this relay, or
         *                       {@code null} when the relay advertised no positive {@code warp_mi_tag_len}
         * @param warpMiTagLength the relay advertised WARP MI tag length in bytes, or {@code 0} when disabled
         * @param audioCodec     the audio codec, an {@link OpusAudioCodec} or, when {@code useMlow} is set, an
         *                       {@link MLowAudioCodec}
         * @param repacketizer   the Opus repacketizer, or {@code null} when {@code useMlow} is set
         * @param sframeProvider   the group SFrame chain, or {@code null} on a one to one call
         * @param isCaller         whether the local side placed the call, gating the history based estimate
         *                         seed
         * @param video            whether the video pipeline is brought up
         * @param voipParamManager the call's voip param manager, already parsed from the {@code <voip_settings>}
         *                         bundles and retuned for the participant count, the source of the active set
         * @param participantCount the call's current membership size, used to seed the initial group call
         *                         bandwidth estimate
         * @param streamLayout     the deterministic local SSRC layout this client transmits on and the
         *                         subscription layer advertises
         * @param membership       the call's participant manager on a group call, read for inbound app data
         *                         attribution and threaded into the subscription publisher, or {@code null} on
         *                         a one to one call
         * @param streams          the application capture sources and playback sinks the pipelines drive
         * @param useMlow          whether the call selected the MLow codec, gating the matching
         *                         {@link MLowAudioDecoder} and the repacketizer bypass on the send path
         * @param e2eSend          the end to end SRTP context protecting outbound media on a one to one call,
         *                         or {@code null} for a group call whose media is hop by hop SRTP
         * @param e2eRecv          the end to end SRTP context unprotecting inbound media on a one to one call,
         *                         or {@code null} for a group call whose media is hop by hop SRTP
         * @param selfPid          the local device's relay assigned participant id (the {@code <relay>} block's
         *                         {@code self_pid}), stamped into every self sender subscription descriptor
         * @return the started media session
         */
        private Session assemble(String callId, RelayConnection relayConnection, DatagramChannel channel,
                                 LiveHbhSrtpRelay hbhSrtp, byte[] warpAuthKey,
                                 int warpMiTagLength, AudioCodec audioCodec,
                                 OpusRepacketizer repacketizer, SFrameKeyProvider sframeProvider, boolean isCaller,
                                 boolean video, LiveVoipParamManager voipParamManager, int participantCount,
                                 StreamLayout streamLayout, CallMembership membership,
                                 MediaStreams streams, boolean useMlow,
                                 E2eMediaSrtp e2eSend, E2eMediaSrtp e2eRecv, int selfPid,
                                 MediaSessionListener listener) {
            var relayAddress = relayConnection.address();
            // Route the decode side through the same codec the encode side selected: an MLow call decodes
            // through an MLowAudioDecoder, otherwise the default OpusAudioDecoder. LiveNetEq renders each frame
            // through the AudioDecoderReceiver.FrameDecoder codec seam, so the per call decoder (with its
            // postfilter on the MLow path) is wrapped once here and threaded into both the jitter buffer (for
            // the decode conceal time stretch render) and the receiver (which is a thin getAudio adapter).
            AudioDecoder audioDecoder = useMlow
                    ? new MLowAudioDecoder(AUDIO_SAMPLE_RATE_HZ, AUDIO_CHANNELS,
                    mlowLpcPostfilterEnabled(voipParamManager))
                    : new OpusAudioDecoder(AUDIO_SAMPLE_RATE_HZ, AUDIO_CHANNELS);
            var netEq = new LiveNetEq(NetEqConfig.defaults(), audioFrameDecoder(audioDecoder));
            // When the call sends video but the application supplied no VideoOutput, source the outbound
            // leg from a platform camera through the driver manager: the manager's camera capture driver
            // pushes frames into this bridge, which the encode loop drains as its VideoOutput. When the app
            // did supply a source it is used directly (no double source). A receive only video leg (no app
            // source and a camera the manager cannot start) leaves the bridge unfed and the encode loop idle.
            var managerVideoBridge = video && streams.videoCapture() == null
                    ? new ManagerVideoSourceBridge(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_FRAME_RATE,
                    VIDEO_DEFAULT_BITRATE_BPS, () -> notifyCaptureInterrupted(callId, listener))
                    : null;
            VideoOutput videoCaptureSource = streams.videoCapture() != null
                    ? streams.videoCapture()
                    : managerVideoBridge;
            var videoPipeline = video
                    ? buildVideoPipeline(callId, videoCaptureSource, streams.videoPlayback())
                    : null;
            // The single shared video pipeline holder: the demux, the inbound RTCP listener, and the session
            // all read it rather than a captured snapshot, so a mid call audio to video upgrade that publishes
            // the pipeline here is seen by the transport receive thread (inbound video routing and the
            // inbound PLI key frame path) and not lost to the null captured at an audio only bring up. It
            // holds the built pipeline now, or null until an upgrade builds one.
            var videoPipelineRef = new AtomicReference<>(videoPipeline);

            // Bandwidth estimation and rate control loop. The loop holds the WhatsApp sender side AIMD
            // estimator fused through the combiner and gated by the hold machine, the GoogCC delay based
            // estimator the inbound demux tees each packet's arrival timing into, and the
            // audio and (on a video call) video rate controllers that drive OpusAudioCodec.modify and the
            // video pipeline's encoder. Built before the demux so the demux can tee into its delay based
            // estimator, and registered as the transport's inbound RTCP feedback listener just after the
            // transport is built so each feedback packet drives one tick. A group call (participantCount > 0;
            // a one to one call tracks no roster and passes 0) seeds its initial estimate rather than ramping
            // from the conservative cold start band when ENABLE_INIT_BWE_FOR_GROUP_CALL is on, read from the
            // feature gate at bring up.
            var seedInitialGroupBwe = participantCount > 0 && featureGate.isInitBweForGroupCallEnabled();
            // The relay transport is built below; the rate loop and the app data send seam reach it through
            // this forward holder, set once it is constructed. The loop reads the live SCTP send buffer
            // occupancy each round through it: the real per stream buffered amount once the transport is up,
            // and 0 before it connects (the loop ticks only on inbound RTCP feedback, which arrives after the
            // transport connects, so the pre connect zero is never actually consumed).
            var transportHolder = new AtomicReference<LiveRelayTransport>();
            var rateControlLoop = new RateControlLoop(callId, isCaller, video, audioCodec,
                    defaultAudioParams(), videoPipeline, voipParamManager,
                    () -> {
                        var liveTransport = transportHolder.get();
                        return liveTransport != null ? liveTransport.sctpBufferedAmount() : 0L;
                    },
                    seedInitialGroupBwe);

            // App data plane over the one SCTP data channel: on WhatsApp app data rides the same data channel
            // as media, written as SCTP DATA, and the controller demultiplexes inbound app data to its
            // reaction, transcription, rekey, and feedback seams. The outbound sink writes through the
            // transport's data channel send seam (transport.sendAppData), reached through the same forward
            // holder set once the transport is built below.
            var appDataControllerHolder = new AtomicReference<AppDataController>();
            // Inbound app data attribution: the native app data path keys each inbound batch by the sending
            // participant device. A batch whose sender is not resolved falls back to the call JID.
            var appDataCallFallback = Jid.of(callId + "@call");
            var inboundAppDataSender = new AtomicReference<>(appDataCallFallback);
            var appDataChannel = new AppDataChannel(
                    bytes -> {
                        var liveTransport = transportHolder.get();
                        return liveTransport != null && liveTransport.sendAppData(bytes);
                    },
                    payloads -> {
                        var controller = appDataControllerHolder.get();
                        if (controller != null) {
                            controller.onReceive(inboundAppDataSender.get(), payloads);
                        }
                    });
            // TODO: route inbound app data from the SCTP data channel into appDataChannel.receivePayloads and
            //  resolve its sender into inboundAppDataSender. App data (rekey, reactions, transcription) rides
            //  the SCTP data channel or relay RTP app data, demuxed by leading byte; the inbound demux and per
            //  batch sender resolution are not yet read by any production path. (The 0x4000 attribute of the
            //  0x0003 envelope is NOT app data: it is a control WARP carrying sequence, downlink bandwidth,
            //  video encoding, and participant report, whose body is hop by hop SRTP sealed; see the
            //  SubscriptionEnvelope class javadoc.) The outbound app data send seam (transport.sendAppData) is
            //  wired.

            var inboundMedia = new InboundMediaDemux(netEq, videoPipelineRef, sframeProvider,
                    rateControlLoop);

            // The host UDP socket is the RTCPeerConnection equivalent ICE/DTLS transport socket: the
            // transport drives ICE and ships DTLS records over it; the relay endpoint is the bootstrap
            // destination until ICE nominates a pair.
            var socketEgress = (LiveRelayTransport.Egress) (payload, destination) -> socketSend(channel,
                    destination == null ? relayAddress : destination, payload);
            // The relay path is a locally synthesized RTCPeerConnection: there is no signaled remote
            // candidate, ufrag, pwd, or <certificate>. The IceAgent runs as the controlling agent against a
            // single relay host candidate (foundation 2, component 1, udp, priority 2122262783, typ host),
            // with remote ufrag = auth_token (or token) and remote pwd = the relay <key> raw bytes, and the
            // DTLS data channel pins the fixed relay certificate fingerprint rather than expecting a signaled
            // one.
            var iceAgent = new IceAgent(
                    generateIceUfrag(),
                    generateIcePassword(),
                    relayConnection.remoteUfrag(),
                    relayConnection.remotePassword(),
                    true);
            iceAgent.addLocalCandidate(localHostCandidate(channel));
            iceAgent.appendRemoteCandidates(List.of(relayHostCandidate(relayAddress)));
            var dataChannel = new RelayDataChannel(
                    socketEgress,
                    relayAddress,
                    relayConnection.activeMode(),
                    RelayDataChannel.RELAY_CERT_FINGERPRINT_SHA256);
            // The 0x0003 subscription envelope's outer {@code MESSAGE-INTEGRITY} is {@code HMAC-SHA1} keyed by the relay <key>
            // used verbatim (the same relay STUN app credential as the relay connectivity ping), and its
            // 0x0016 {@code XOR-MAPPED-ADDRESS} carries the selected relay endpoint's transport address; thread both
            // into the transport so its receive subscription resend can assemble and ship the envelope.
            var relayReflexiveAddress = relayAddress instanceof InetSocketAddress inet ? inet : null;
            // TODO: wire LiveCallHttpSignaler. At transport construction build a CallTransportController with a
            //  LiveCallHttpSignaler whose CallSignalingTransport routes {"start_session_request":{}} over the
            //  live client transport, correlate start_session_response by request id, then start the
            //  MediaTransport (the ICE/DTLS bring up needs the ICE/DTLS material the current signaling drops).
            var transport = new LiveRelayTransport(
                    hbhSrtp,
                    warpAuthKey,
                    warpMiTagLength,
                    relayConnection.remotePassword(),
                    relayConnection.relayToken(),
                    relayReflexiveAddress,
                    inboundMedia::onTransportMedia,
                    iceAgent,
                    socketEgress,
                    dataChannel,
                    e2eSend,
                    e2eRecv);
            transportHolder.set(transport);
            // The demux is built before the transport, so install the NACK send seam now: a confirmed inbound
            // audio or video gap ships a generic NACK back through the transport's standalone SRTCP send path.
            inboundMedia.nackEmitter(transport::sendNack);

            // Per RTCP rate control tick: the relay transport unprotects and parses each inbound RTCP compound
            // packet into an RtcpFeedback and delivers it here, where one feedback drives one rate control
            // round. A peer key frame request (PSFB PLI or FIR) additionally arms the local video encoder so
            // its next encode emits an intra picture. The pipeline is read from the shared holder rather than
            // captured, so after a mid call upgrade an inbound PLI arms the freshly built encoder; a call with
            // no video pipeline yet has none to arm. The feedback's round trip estimate also feeds the NACK
            // scheduler so a packet is requested only once roughly a round trip has elapsed since it was
            // noticed missing.
            transport.onInboundRtcp(feedback -> {
                rateControlLoop.onRtcpFeedback(feedback);
                if (feedback.hasRtt()) {
                    var rttMillis = feedback.rttNs() / 1_000_000L;
                    if (netEq != null) {
                        netEq.updateRttMillis(rttMillis);
                    }
                    inboundMedia.updateVideoRttMillis(rttMillis);
                }
                var pipeline = videoPipelineRef.get();
                if (feedback.keyFrameRequested() && pipeline != null) {
                    pipeline.requestKeyFrame();
                }
            });

            if (videoPipeline != null && videoCaptureSource != null) {
                var videoPacketizer = new OutboundVideoRtpPacketizer(streamLayout.videoStream0Ssrc(),
                        rateControlLoop::videoTargetBitrateBps, rateControlLoop::bandwidthEstimateBps);
                videoPipeline.bindFrameSink(encoded -> sendVideoPacket(transport, videoPacketizer, encoded));
            }

            // The SFU subscription publisher owns the receive subscription cache and the hop by hop RTCP
            // feedback table, and is threaded the membership provider so the receive subscription compute
            // (LiveSubscriptionPublisher.computeRxSubscription) can read the roster (firstConnectedPeer,
            // videoStreamSubscriberCount). On each resend tick the resender re ships this client's own send
            // stream descriptors as the fixed 0x4024 WA_SUBSCRIPTION attribute (buildSelfStreamDescriptors):
            // the audio plus both simulcast video layers, each a media/FEC/NACK triple of the deterministic
            // SSRCs, which is the form a WhatsApp relay call acks with 0x0103; the 0x4025 sender subscription
            // snapshot rides the media plane UDP path, not this channel. It is shipped inside the 0x0003 STUN
            // envelope as SCTP DATA on the data channel: the envelope's outer {@code MESSAGE-INTEGRITY} is {@code HMAC-SHA1}
            // keyed by the relay <key>, and the 0x0016 carries the selected relay endpoint's reflexive address.
            // The leading 0x4000 WARP attribute is an optional rate control report Cobalt does not emit: its
            // control body is sealed by the relay hop by hop SRTP layer (not the SFrame transform, which never
            // engages on the relayed SFU send path) with a seal that is not reproducible (see the
            // SubscriptionEnvelope class javadoc). The subscription resend is driven on the transport's
            // keepalive thread through LiveRelayTransport.onSubscriptionResend: a WhatsApp 1:1 relay call sends
            // this client's 0x0003 subscription on the data channel at connect and the relay acks it with
            // 0x0103, and the relay forwards a peer's media only after it has the subscription, so the
            // transport ships it as a connect time burst and then on content change. The publisher below is
            // retained for its receive subscription cache and hop by hop RTCP feedback table.
            var subResender = subscriptionResender(transportHolder, buildSelfStreamDescriptors(callId, selfDeviceJid()));
            transport.onSubscriptionResend(() -> subResender.accept(null));
            var subscriptionPublisher = new LiveSubscriptionPublisher(
                    new TimerHeap(),
                    System::nanoTime,
                    subResender,
                    LiveSubscriptionPublisher.DEFAULT_RESEND_INTERVAL,
                    membership == null ? null : membership.participantProvider());

            var packetCache = new StreamPacketCache(PACKET_CACHE_CAPACITY);
            var rtpPacketizer = new OutboundRtpPacketizer(streamLayout.audioSsrc(),
                    (useMlow ? MLOW_FRAMES_PER_PACKET : OPUS_FRAME_BLOCKS) * AUDIO_FRAME_SAMPLES);
            // TODO: wire the group call SFrame seal. On a group call, seal the combined payload with the
            //  SFrame chain before sending by wrapping the self device's sframeProvider (built by
            //  groupSframeProvider from the call key, the same key installed into the membership via
            //  installCallKey) in an SFrameSecureFrame and supplying frame -> secureFrame.seal(frame) here (the
            //  symmetric open lands in InboundMediaDemux.openSframe). The seal and open transform and the
            //  trailer codec already exist (com.github.auties00.cobalt.calls.media.sframe), but the exact
            //  SFrame media frame byte layout is not confirmed: the CTR IV layout within the 16 byte block, the
            //  counter mask XOR, and the ciphertext, tag, and trailer ordering are unverified, and SFrameCipher
            //  carries its own unresolved per key id counter mask salt value and chain key to cipher
            //  derivation. WhatsApp does not engage the per frame SFrame transform on the relayed SFU send path
            //  (the per participant keys are derived and rotated, but the media rides the relay hop by hop
            //  SRTP), and a 1:1 call uses no SFrame, so no SFrame media frame is produced to read the layout
            //  from. Leaving the payload for the hop by hop encrypted relay is therefore the faithful behaviour;
            //  wiring an unverified layout would make every group call frame Cobalt sends undecryptable by real
            //  peers, a worse outcome than the current pass through.
            AudioEncoderSender.SFrameTransform sframeSeal = null;
            // The frames per packet packer is Opus specific: the Opus repacketizer concatenates the buffered
            // Opus frames into one multi frame Opus packet. MLow encodes a self contained packet inside the
            // codec (its TOC names the frame configuration), so its packet must NOT be re packed by the Opus
            // repacketizer (which would reject or corrupt the non Opus bytes); the MLow packer passes the
            // single buffered packet through unchanged.
            //
            // Frame sizing: MLow ships WhatsApp's 60 ms ptime while Opus ships 20 ms. MLow cannot concatenate
            // separately encoded frames (a 60 ms MLow packet is three 20 ms internal frames under one TOC and
            // one range coded bitstream), so the whole 960 sample span is encoded in one call: the capture
            // pump stays on the 20 ms cadence the audio processing front end needs, and a Pcm60msAggregator
            // (wired below) joins MLOW_FRAMES_PER_PACKET captured blocks into one 960 sample block before the
            // sender's single MLow encode; Opus drives the sender directly per 20 ms block. The outbound RTP
            // timestamp advances by the packet's own sample span (OutboundRtpPacketizer samplesPerPacket: 960
            // for MLow, 320 for Opus) so the playout clock stays in step. Rendering a 60 ms MLow packet a peer
            // sends us (NetEq serving its 960 decoded samples across three 20 ms get periods) is the inbound
            // counterpart and is independent of this send path.
            // FRAMES_PER_PACKET is 1, so the Opus packer buffers a single frame per flush: a one frame Opus
            // packet is the encoded frame itself, so the repacketizer round trip (three FFI calls and two
            // native copies) reproduces the input bytes exactly. The encoder allocates a fresh byte[] per
            // encode, so returning payload() unchanged needs no defensive clone and stays wire identical.
            // The repacketizer.combine path stays intact for any FRAMES_PER_PACKET > 1 configuration.
            AudioEncoderSender.FramePacker framePacker = useMlow
                    ? frames -> frames.getFirst().payload()
                    : frames -> frames.size() == 1
                            ? frames.getFirst().payload()
                            : repacketizer.combine(frames.stream().map(EncodedAudioFrame::payload).toList());
            // options.enable_additional_dtx_frames_at_call_start_ms is a duration; converting it to frames at
            // the call's 20 ms Opus frame period gives how many discontinuous transmission frames the sender
            // transmits at call start before reverting to dropping them.
            var additionalDtxFramesAtCallStart = (int) (voipParamManager.activeParams()
                    .map(params -> params.getInteger(
                            VoipParamKey.OPTIONS_ENABLE_ADDITIONAL_DTX_FRAMES_AT_CALL_START_MS).orElse(0L))
                    .orElse(0L) / 20L);
            var sender = new AudioEncoderSender(
                    audioCodec::encode,
                    framePacker,
                    sframeSeal,
                    (payload, extendedSequence, level) -> sendAudioPacket(transport, rtpPacketizer, payload,
                            extendedSequence, level),
                    rtpPacketizer::noteSilenceFrame,
                    packetCache,
                    FRAMES_PER_PACKET,
                    additionalDtxFramesAtCallStart);

            var capture = openCaptureSource(streams);
            var captureRing = new AudioCaptureRing(RING_CAPACITY_SAMPLES);
            // On the MLow path a Pcm60msAggregator joins MLOW_FRAMES_PER_PACKET captured 20 ms blocks into one
            // 60 ms block before the sender's single MLow encode; Opus drives the sender directly. The pump and
            // the WebRTC APM front end stay on the 20 ms cadence either way.
            AudioReaderPump.AudioBlockSink encoderSink = useMlow
                    ? new Pcm60msAggregator(sender, MLOW_FRAMES_PER_PACKET)
                    : new Pcm60msAggregator(sender, OPUS_FRAME_BLOCKS);
            // Auto plug the WebRTC APM (AEC3 + noise suppression, no AGC) ahead of the encoder for a raw mic
            // capture. It engages only for a true live acoustic source (isLiveCapture: the microphone source or
            // the platform capture device bridge, never a file/tone/silence/bot source) and only once the
            // cobalt native webrtc apm shim is built (nativeApmAvailable is false until the native lib lands), so
            // both a non mic source and the not yet built native tree encode the capture unconditioned. When
            // engaged the capture APM reads the last block rendered to the speaker as the far end AEC reference
            // through the render tap the playback path publishes; process() is driven only from the reader
            // pump's single virtual thread, honouring the processor's single writer contract.
            var conditionCapture = capture.source() != null
                    && capture.source().isLiveCapture()
                    && WebRtcAudioProcessor.nativeApmAvailable();
            WebRtcAudioProcessor audioProcessor = null;
            RenderReferenceTap renderTap = null;
            AudioReaderPump.AudioBlockSink audioBlockSink = encoderSink;
            if (conditionCapture) {
                audioProcessor = new WebRtcAudioProcessor(WebRtcAudioProcessor.Config.aecAndNoiseSuppression());
                renderTap = new RenderReferenceTap();
                audioBlockSink = new ApmCaptureSink(audioProcessor, renderTap, encoderSink);
            }
            var readerPump = capture.source() == null
                    ? null
                    : new AudioReaderPump(capture.source(), captureRing, audioBlockSink, AUDIO_FRAME_SAMPLES,
                    CAPTURE_STARTUP_SEED_SAMPLES);

            var receiver = new com.github.auties00.cobalt.calls.media.audio.pipeline.AudioDecoderReceiver(netEq);
            var playback = openPlaybackSink(callId, receiver, streams, renderTap);

            var transportConnected = new AtomicBoolean();
            transport.onTransportEvent(event -> onTransportEvent(callId, event, transportConnected, listener));

            // No real inbound observers are attached here: the reaction and transcription observers are the
            // in call control units the lifecycle controller builds later and attaches through the
            // controller's attach seams (reached via OrchestratedCall.appDataController). The controller is
            // built with no op observers and retransmission disabled. It is built last, after every throwing
            // bring up step, so a failure part way through the bring up cannot leak its timer scheduler before
            // the session that owns its close takes ownership; once the session is built, session.close()
            // closes it (including on the start() failure path below). Reaction retransmission stays disabled,
            // and that is faithful rather than a placeholder: the native controller arms the retransmission
            // timer only when a per call flag is set, and that flag has no compiled default and no writer in
            // the shared voip core, so it is zero unless the server enables it, and the reaction voip params
            // are absent from the voip_settings union.
            var appDataController = new AppDataController(appDataChannel, false);
            appDataControllerHolder.set(appDataController);

            var videoUsesDevice = managerVideoBridge != null && videoPipeline != null;
            var session = new LiveMediaSession(callId, transport, channel, relayAddress,
                    audioCodec, repacketizer, voipDriverManager, capture.usesDevice(), playback.usesDevice(),
                    readerPump, playback.writerPump(),
                    capture.bridge(), playback.renderLoop(), audioProcessor, netEq, audioDecoder, videoPipelineRef,
                    voipParamManager, appDataController, rateControlLoop, subscriptionPublisher);
            if (videoUsesDevice) {
                session.bindVideoCaptureStarter(() -> startManagerVideoCapture(callId, managerVideoBridge));
            }
            // A call brought up audio only gets an upgrade seam so an in call camera turn on can build the
            // video pipeline mid call; a call already carrying video has its pipeline and needs none. The seam
            // captures the live transport, the application streams, and the rate control loop, all of which
            // only exist here, and is run once by the session under its one shot video send guard.
            if (videoPipeline == null) {
                session.bindVideoUpgrade(() -> upgradeToVideo(callId, transport, streams, rateControlLoop, session,
                        streamLayout.videoStream0Ssrc(), listener));
            }
            try {
                session.start();
            } catch (RuntimeException exception) {
                session.close();
                throw exception;
            }
            return session;
        }

        /**
         * Builds, wires, and brings online the video pipeline for a call upgraded from audio only to video
         * mid call, returning it for the session to publish and start.
         *
         * <p>This is the media plane construction half of an in call audio to video upgrade, run once by
         * {@link LiveMediaSession#startLocalVideo()} the first time the camera turns on a call brought up
         * audio only. It resolves the outbound video source the same way the bring up does: the application
         * {@linkplain MediaStreams#videoCapture() video source} when one was supplied, otherwise a fresh
         * {@link ManagerVideoSourceBridge} the driver manager's camera capture feeds. It opens the negotiated
         * video codec and builds the inbound jitter buffer through {@link #buildVideoPipeline}, binds the
         * outbound video RTP sink onto the live transport (a fresh {@link OutboundVideoRtpPacketizer}), binds
         * the session's camera capture starter when the source is the manager bridge, and brings the
         * rate control loop's video controller online through {@link RateControlLoop#enableVideo}. It
         * returns the built pipeline, or {@code null} when the codec could not be opened, in which case the
         * call stays audio only.
         *
         * @implNote This implementation builds the video stream, encoder, jitter buffer, and rate controller
         * lazily at upgrade rather than pre allocating them at the audio call's bring up, which is how the
         * native engine sequences it (the video objects are created at upgrade, not at call setup). Three
         * native sub behaviours are deliberately not reproduced, each a pre existing gap rather than new to the
         * upgrade path:
         * <ul>
         *   <li>The native upgrade destroys and recreates the audio stream in the same transaction; Cobalt
         *       leaves the running audio pipeline untouched, since the relay leg carries audio over the same
         *       hop by hop SRTP context before and after the upgrade and there is no audio renegotiation on the
         *       wire to mirror.</li>
         *   <li>The new video stream reuses the participant setup allocated first simulcast layer video SSRC
         *       (the native engine allocates the video SSRC set even for an audio call) the bring up computed
         *       into the call's {@link StreamLayout}, threaded in as {@code videoStream0Ssrc}, matching the
         *       native engine reusing the pre allocated video SSRC rather than minting a fresh one at
         *       upgrade.</li>
         *   <li>No SFU stream subscription update is sent (the native engine gates it to three or more
         *       participants); the relay 1:1 leg carries the video on the tokens already negotiated, and the
         *       on wire subscription publish is not yet read by any production path for every stream, not just
         *       this one (the {@code 0x0003} subscription envelope send seam exists on the transport but the
         *       publisher is not yet wired to it; see {@link LiveSubscriptionPublisher#publishRxSubscription}).</li>
         * </ul>
         *
         * @param callId          the call identifier
         * @param transport       the live relay transport the outbound video RTP sink is bound onto
         * @param streams         the call's application media streams, read for the video capture source
         * @param rateControlLoop the call's rate control loop whose video controller is brought online
         * @param session         the session whose camera capture starter is bound when the source is the
         *                        manager bridge
         * @param videoSsrc       the deterministic first simulcast layer video SSRC the new packetizer stamps,
         *                        from the call's {@link StreamLayout}
         * @param listener        the call's media session listener the local capture interrupted notification
         *                        is reported to
         * @return the built video pipeline, or {@code null} when the video codec could not be opened
         */
        private VideoPipeline upgradeToVideo(String callId, MediaTransport transport, MediaStreams streams,
                                             RateControlLoop rateControlLoop, LiveMediaSession session,
                                             int videoSsrc, MediaSessionListener listener) {
            var managerVideoBridge = streams.videoCapture() == null
                    ? new ManagerVideoSourceBridge(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_FRAME_RATE,
                    VIDEO_DEFAULT_BITRATE_BPS, () -> notifyCaptureInterrupted(callId, listener))
                    : null;
            var videoCaptureSource = streams.videoCapture() != null ? streams.videoCapture() : managerVideoBridge;
            var pipeline = buildVideoPipeline(callId, videoCaptureSource, streams.videoPlayback());
            if (pipeline == null) {
                return null;
            }
            if (videoCaptureSource != null) {
                var videoPacketizer = new OutboundVideoRtpPacketizer(videoSsrc,
                        rateControlLoop::videoTargetBitrateBps, rateControlLoop::bandwidthEstimateBps);
                pipeline.bindFrameSink(encoded -> sendVideoPacket(transport, videoPacketizer, encoded));
            }
            if (managerVideoBridge != null) {
                session.bindVideoCaptureStarter(() -> startManagerVideoCapture(callId, managerVideoBridge));
            }
            rateControlLoop.enableVideo(pipeline);
            return pipeline;
        }

        /**
         * Starts the driver manager's camera capture for a video call that supplied no application video
         * source, forwarding captured frames into the encode bridge.
         *
         * <p>Drives the manager's camera capture driver through its
         * {@link com.github.auties00.cobalt.calls.platform.video.VideoCaptureDriver} state machine so each
         * captured frame is forwarded to {@code bridge}, which the video pipeline's encode loop drains. The
         * capture geometry is the call's default outbound video geometry, matching the geometry the bridge
         * advertises and the encoder is sized to. A host without a usable camera fails the start; the failure
         * is logged and isolated so the call still receives and renders the peer's video and carries audio,
         * it simply sends no video, the same receive only outcome as a video call whose codec could not be
         * opened.
         *
         * @param callId the call identifier
         * @param bridge the bridge the camera driver forwards captured frames into and the encode loop drains
         */
        private void startManagerVideoCapture(String callId, ManagerVideoSourceBridge bridge) {
            try {
                var capability = new VideoCaptureCapability(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_FRAME_RATE);
                voipDriverManager.startVideoCapture(MANAGER_CAMERA_DEVICE_ID, bridge, capability);
            } catch (RuntimeException exception) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING,
                            "calls camera capture unavailable for call {0}; sending no video: {1}",
                            callId, exception.getMessage());
                }
            }
        }

        /**
         * Forwards a platform camera capture revocation for a call to its media session listener.
         *
         * <p>Runs on the capture driver's pump thread when the driver enters
         * {@link com.github.auties00.cobalt.calls.platform.video.VideoCaptureDriver.State#INTERRUPTED}, wired
         * as each manager camera bridge's interrupted callback. Notifies the controller through the call's
         * {@link MediaSessionListener#onCaptureInterrupted(String)}, which drives the call's local video to
         * paused and broadcasts it to the peer.
         *
         * @param callId   the identifier of the call whose platform camera capture was revoked
         * @param listener the call's media session listener
         */
        private void notifyCaptureInterrupted(String callId, MediaSessionListener listener) {
            listener.onCaptureInterrupted(callId);
        }

        /**
         * Builds the call's voip param manager from the negotiated {@code <voip_settings>} bundles and
         * selects the active set for the call's media mode.
         *
         * <p>Each {@code <voip_settings>} stanza is parsed through
         * {@link VoipSettings#of(Stanza, VoipParamJsonDeserializer)} and its non empty parameter set is stored
         * under its {@code (device JID, settings type)} key: the offer acknowledgement delivers one
         * {@code jid}-tagged bundle per callee device, while the delivered offer carries a single un tagged
         * bundle (the callee's own config, stored under a {@code null} device key). The active set is then
         * selected for the device whose codec this side encodes against: the caller selects the answering
         * {@code peerDeviceJid}'s bundle (the offer acknowledgement carries a different
         * {@value #USE_MLOW_CODEC_PARAM} per callee device), and the callee selects its own un tagged bundle.
         * Within the chosen device, selection picks the call's media mode ({@link VoipSettingsType#VIDEO} for a
         * video call, otherwise {@link VoipSettingsType#AUDIO}), falling back to the mandatory
         * {@link VoipSettingsType#NONE} default when the mode specific bundle is absent. The lifecycle order is
         * store then select then the participant count override, matching the engine's
         * {@code EMPTY -> PARSED -> STORED -> ACTIVE -> OVERRIDDEN_BY_COUNT} sequence; the per round dynamic
         * rate control pass runs later, off the rate control tick.
         *
         * @implNote This implementation maps the wire {@code type} attribute onto {@link VoipSettingsType}
         * through {@link #voipSettingsType(Stanza)}: {@code audio} and {@code video} select the matching
         * overlay and every other value (the captured traffic carries {@code default}, or omits the
         * attribute entirely on the single bundle the offer acknowledgement delivers) selects
         * {@link VoipSettingsType#NONE}, the mandatory baseline. The count override is invoked with an empty
         * override list because the count dependent values are not yet modelled; the call is kept to preserve
         * the store, select, count override, dynamic tick order so the override slot is wired when the values
         * land.
         *
         * @param voipSettings     the {@code <voip_settings>} bundle nodes, in wire order
         * @param video            whether the call carries video, selecting the video overlay
         * @param participantCount the call's current membership size, used for the count override
         * @param isCaller         whether the local side placed the call, selecting the answering peer
         *                         device's bundle rather than the local side's own bundle
         * @param peerDeviceJid    the answering peer device whose bundle the caller selects, or {@code null};
         *                         a callee selects its own un tagged bundle and ignores this
         * @return the populated voip param manager with its active set selected
         */
        private LiveVoipParamManager buildVoipParamManager(List<Stanza> voipSettings, boolean video,
                                                           int participantCount, boolean isCaller,
                                                           Jid peerDeviceJid) {
            var manager = new LiveVoipParamManager();
            var deserializer = new VoipParamJsonDeserializer();
            for (var node : voipSettings) {
                var settings = VoipSettings.of(node, deserializer);
                var deviceJid = node.getAttributeAsJid(VOIP_SETTINGS_JID_ATTRIBUTE).orElse(null);
                settings.nonEmptyParams()
                        .ifPresent(params -> manager.store(deviceJid, voipSettingsType(node), params));
            }
            // The caller encodes against the answering peer device, so it selects that device's bundle (the
            // offer acknowledgement delivers a different use_mlow_codec_v1 per callee device); the callee
            // encodes its own delivered config, the single un tagged bundle stored under the null device key.
            var selectionDevice = isCaller ? peerDeviceJid : null;
            manager.selectActive(selectionDevice, video ? VoipSettingsType.VIDEO : VoipSettingsType.AUDIO);
            // TODO: pass the real participant count override values once the count dependent parameter set
            //  keyed on the call size is modelled; the override runs empty for now to keep the store, select,
            //  count override, dynamic tick lifecycle order intact.
            manager.overrideForParticipantCount(participantCount, List.of());
            return manager;
        }

        /**
         * Returns whether the call selected the MLow audio codec over the Opus codec.
         *
         * <p>Reads the {@value #USE_MLOW_CODEC_PARAM} flag from the active voip param set, the {@code encode}
         * section of the active device's server pushed {@code <voip_settings>} bundle, and treats it as a
         * boolean: an explicit {@code false} (or {@code "0"}) selects Opus and any other state selects MLow.
         * When the flag is absent, including a call that negotiated no active set, the call defaults to MLow.
         *
         * @implNote This implementation reads the flag from the {@code <voip_settings>} the server injects per
         * device into the delivered offer (the callee's own bundle) and the offer acknowledgement (one bundle
         * per callee device); {@link #buildVoipParamManager(List, boolean, int, boolean, Jid)} selects the
         * active device's bundle, so the flag read is the one that applies to the device this side encodes
         * against. The absent case defaults to MLow, matching WhatsApp: it decodes inbound audio as MLow and
         * treats an absent or unset {@code use_mlow_codec_v1} as MLow, switching to Opus only on an explicit
         * {@code false}. This gates the inbound decoder, which must render whatever the peer encodes; the peer
         * bundle carries {@code encode.use_mlow_codec_v1} on some devices and omits it on others, so defaulting
         * an omitted flag to Opus would decode an MLow peer's stream through {@link OpusAudioDecoder} and yield
         * noise. The flag is read by its {@code <voip_settings>} wire path {@value #USE_MLOW_CODEC_PARAM}
         * through {@link VoipParamKey#ofWirePath(String)}.
         * <p>The session codec is selected once at bring up from the active peer device's bundle and is not
         * hot swapped mid call. This is faithful rather than a shortcut: a one to one call fixes the answering
         * device at accept (a later accept from another of the peer's devices ends the call instead of
         * re selecting), and a group call forwards a single SFrame stream the server keeps codec homogeneous,
         * so the selected peer's flag does not change under the local encoder. The engine's per stream
         * recreate on change path is therefore intentionally not reproduced: Cobalt runs one session codec, not
         * the engine's per stream codecs.
         *
         * @param voipParamManager the call's voip param manager whose active set carries the flag
         * @return {@code true} when the call routes its audio through {@link MLowAudioCodec} and
         * {@link MLowAudioDecoder}, {@code false} when it stays on {@link OpusAudioCodec} and
         * {@link OpusAudioDecoder}
         */
        private static boolean useMlowCodec(LiveVoipParamManager voipParamManager) {
            var key = VoipParamKey.ofWirePath(USE_MLOW_CODEC_PARAM).orElse(null);
            var active = voipParamManager.activeParams();
            var flag = key == null ? Optional.<Boolean>empty() : active.flatMap(params -> params.getBoolean(key));
            var useMlow = flag.orElse(true);
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG,
                        "calls codec select: keyResolved={0}, activeParams={1}, use_mlow_codec_v1={2} -> useMlow={3}",
                        key != null, active.isPresent(), flag.map(String::valueOf).orElse("ABSENT"), useMlow);
            }
            return useMlow;
        }

        /**
         * Resolves whether the MLow decode postfilter chain runs its gated LPC postfilter for the call.
         *
         * <p>Reads the per call {@value #MLOW_ENABLE_LPC_POSTFILTER_PARAM} integer tunable from the manager's
         * active set; a non zero value enables the LPC postfilter, and an absent, zero, or non integer value
         * (including a call that negotiated no active set) leaves it disabled, the native and live client
         * default. The harmonic and high pass postfilters always run regardless of this flag.
         *
         * @param voipParamManager the call's voip param manager whose active set carries the flag
         * @return {@code true} when the {@link MLowAudioDecoder} runs the LPC postfilter, {@code false}
         * otherwise
         */
        private static boolean mlowLpcPostfilterEnabled(LiveVoipParamManager voipParamManager) {
            var key = VoipParamKey.ofWirePath(MLOW_ENABLE_LPC_POSTFILTER_PARAM).orElse(null);
            var params = voipParamManager.activeParams().orElse(null);
            if (key == null || params == null) {
                return false;
            }
            return params.getInteger(key).orElse(0L) != 0L;
        }

        /**
         * Maps a {@code <voip_settings>} stanza's wire {@code type} attribute onto the settings bucket it
         * names.
         *
         * <p>A {@code type} of {@code audio} selects {@link VoipSettingsType#AUDIO} and {@code video}
         * selects {@link VoipSettingsType#VIDEO}; every other value, including the wire {@code default} and
         * an absent attribute, selects {@link VoipSettingsType#NONE}, the mandatory baseline bundle.
         *
         * @param stanza the {@code <voip_settings>} stanza whose bucket is resolved
         * @return the settings type the stanza belongs under
         */
        private VoipSettingsType voipSettingsType(Stanza stanza) {
            var type = stanza.getAttributeAsString(VOIP_SETTINGS_TYPE_ATTRIBUTE, VOIP_SETTINGS_TYPE_DEFAULT);
            return switch (type) {
                case VOIP_SETTINGS_TYPE_AUDIO -> VoipSettingsType.AUDIO;
                case VOIP_SETTINGS_TYPE_VIDEO -> VoipSettingsType.VIDEO;
                default -> VoipSettingsType.NONE;
            };
        }

        /**
         * Resolves the audio capture half: the application source when one was supplied, otherwise a platform
         * capture device routed through the {@link #voipDriverManager} behind a {@link CaptureSourceBridge}.
         *
         * <p>An application capture source is drained directly by the reader pump, since a device backed
         * source (a microphone) captures from its own device behind the {@link AudioOutput} interface, so the
         * driver manager is not engaged. When the call supplied no capture source a {@link CaptureSourceBridge}
         * is installed as the manager's captured audio sink and the half is marked as device driven, so
         * {@link LiveMediaSession#startAudioCapture()} starts the manager's microphone capture; the manager
         * owns the capture driver for the engine's lifetime, so no per call driver is opened here. A platform
         * device that turns out to be unavailable surfaces only when capture is started, where the session
         * isolates it.
         *
         * @param streams the call's application media streams
         * @return the resolved capture half: the reader pump source, whether it is device driven through the
         *         manager, and the device bridge when device driven
         */
        private AudioCaptureHalf openCaptureSource(MediaStreams streams) {
            var appCapture = streams.audioCapture();
            if (appCapture != null) {
                return new AudioCaptureHalf(appCapture, false, null);
            }
            var bridge = new CaptureSourceBridge();
            voipDriverManager.onCapturedAudio((samples, deviceType) -> bridge.offer(samples));
            return new AudioCaptureHalf(bridge, true, bridge);
        }

        /**
         * Resolves the audio playback half: a render loop delivering decoded frames to the application sink
         * when one was supplied, otherwise a platform playback device routed through the
         * {@link #voipDriverManager} and fed by the demand driven writer pump.
         *
         * <p>An application playback sink receives each decoded frame from a render loop that pulls the
         * receiver at the jitter buffer get period, since a device backed sink (a speaker) renders to its own
         * device behind the {@link AudioInput} interface, so the driver manager is not engaged. When the call
         * supplied no playback sink an {@link AudioPlaybackRing} is installed as the manager's rendered audio
         * source and fed by an {@link AudioWriterPump}, and the half is marked as device driven so
         * {@link LiveMediaSession#startAudioPlayback()} starts the manager's playback; the manager owns the
         * playback driver for the engine's lifetime, so no per call driver is opened here. A platform device
         * that turns out to be unavailable surfaces only when playback is started, where the session isolates
         * it.
         *
         * <p>When {@code renderTap} is non-{@code null} the live capture APM is engaged, so each decoded 20 ms
         * block this half renders is published into the tap as the far end reference the capture echo canceller
         * cancels against: the render loop publishes it after every full pull on its playback thread, and the
         * writer pump path publishes it from the block it pulls before the platform device consumes it. When
         * {@code renderTap} is {@code null} no reference is tracked and the playback path is unchanged.
         *
         * @param callId    the call identifier
         * @param receiver  the decode and conceal receiver the playback half pulls rendered frames from
         * @param streams   the call's application media streams
         * @param renderTap the far end reference tap the rendered block is published into for the capture APM,
         *                  or {@code null} when live capture conditioning is not engaged
         * @return the resolved playback half: the render loop or, when device driven, the writer pump and ring
         */
        private AudioPlaybackHalf openPlaybackSink(String callId,
                                                   com.github.auties00.cobalt.calls.media.audio.pipeline.AudioDecoderReceiver receiver,
                                                   MediaStreams streams,
                                                   RenderReferenceTap renderTap) {
            var appPlayback = streams.audioPlayback();
            if (appPlayback != null) {
                var renderLoop = new AudioPlaybackLoop(callId, receiver, appPlayback, renderTap);
                return new AudioPlaybackHalf(renderLoop, false, null, null);
            }
            var playbackRing = new AudioPlaybackRing(RING_CAPACITY_SAMPLES);
            AudioWriterPump.AudioBlockSource playbackSource = renderTap == null
                    ? receiver
                    : (out, length) -> {
                        var produced = receiver.pull(out, length);
                        renderTap.publish(out, produced);
                        return produced;
                    };
            var writerPump = new AudioWriterPump(playbackRing, playbackSource, AUDIO_FRAME_SAMPLES);
            voipDriverManager.requestAudioDataSource(
                    (out, frameCount) -> drainPlaybackRing(playbackRing, out, frameCount));
            return new AudioPlaybackHalf(null, true, writerPump, playbackRing);
        }

        /**
         * Reacts to one relay transport event, notifying the controller on the first traffic bearing
         * event.
         *
         * <p>The relay path is usable from relay creation, so any of relay create success or the
         * downlink/uplink traffic start events marks the media plane connected; the notification fires
         * once. A relay bind failure is logged; the controller observes the failed call through its own
         * timers.
         *
         * <p>The connection notification is dispatched on a fresh virtual thread rather than inline,
         * because the relay transport emits its first event synchronously inside the bring up call (which
         * the controller drives while still applying the call's answer transition under its per call lock);
         * deferring the notification lets the controller finish the answer transition to an answerable state
         * before {@link LifecycleController#onMediaConnected(String)} runs and advances the call to
         * {@link CallLifecycleState#CALL_ACTIVE}, matching the engine reporting the bidirectional media state
         * asynchronously rather than from the bring up stack.
         *
         * @param callId    the call identifier
         * @param event     the transport event
         * @param connected the once only latch guarding the media connected notification
         * @param listener  the call's media session listener the media connected notification is reported to
         */
        private void onTransportEvent(String callId, TransportEvent event, AtomicBoolean connected,
                                      MediaSessionListener listener) {
            switch (event) {
                case RELAY_CREATE_SUCCESS, RX_TRAFFIC_STARTED, TX_TRAFFIC_START -> {
                    if (connected.compareAndSet(false, true)) {
                        Thread.ofVirtual().name("calls-media-connected-" + callId)
                                .start(() -> listener.onConnected(callId));
                    }
                }
                case RELAY_BINDS_FAILED -> {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "calls relay binds failed for call {0}", callId);
                    }
                }
                case RX_TRAFFIC_STOPPED, TX_TRAFFIC_STOPPED, RX_APP_DATA -> {
                }
            }
        }

        /**
         * Ships one outbound media payload as an RTP packet through the transport.
         *
         * <p>Builds the RTP packet (header plus the SFrame or cleartext payload) into a buffer with
         * trailing room for the hop by hop SRTP tag, then hands it to {@link MediaTransport#sendMedia}.
         *
         * @param transport        the relay transport
         * @param packetizer       the outbound RTP packetizer holding the SSRC, sequence, and timestamp
         * @param payload          the media payload to send
         * @param extendedSequence the packet's extended sequence number
         * @param level            the audio level extension the sender measured (computed for level
         *                         metering; the wire extension is not stamped because its negotiated extmap
         *                         id is unknown, see the TODO)
         */
        private void sendAudioPacket(MediaTransport transport, OutboundRtpPacketizer packetizer, byte[] payload,
                                     long extendedSequence, AudioLevelRtpExtension level) {
            // TODO: stamp the audio level RTP header extension on the packet. The value octet and framing are
            //  known (one byte = (level & 0x7f) | (voiceActive << 7), with the RFC 8285 one byte 0xBEDE or two
            //  byte 0x100x profiles and the element header (id << 4) | (len - 1)), and AudioLevelRtpExtension
            //  already serializes both forms. What is missing is the per call extmap id: the engine registers
            //  it at runtime and it is not on the signaling wire. Recovering the id needs a live media datagram
            //  or SDP extmap capture; until then the level is metered but not stamped, which affects only mixer
            //  or SFU level metering, not interop with the cleartext payload.
            try {
                var packet = packetizer.packetize(payload, extendedSequence);
                var rtpTimestamp = DataUtils.getInt(packet, 4, ByteOrder.BIG_ENDIAN) & 0xFFFFFFFFL;
                paceSend(rtpTimestamp);
                // The wire length excludes the trailing SRTP tag room the packetizer over allocates and
                // includes the packet's populated header extension.
                transport.sendMedia(packet, packet.length - 64);
            } catch (RuntimeException exception) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "calls outbound audio send failed", exception);
            }
        }

        /**
         * Paces the outbound media send to wall clock real time, using the packet's RTP timestamp to measure
         * how much audio each packet carries so the wire cadence matches what a microphone source would
         * deliver.
         *
         * <p>Holds each packet until a running deadline ({@link #sendPacingDeadlineNanos}) that advances by the
         * packet's own audio duration, derived from how far the RTP timestamp moved at the
         * {@code AUDIO_RTP_CLOCK_RATE} clock. The relay then receives one packet per packet duration of
         * wall clock instead of as fast as the file decoder, capture ring, and encoder can produce them, which
         * would otherwise burst a startup or post stall backlog onto the wire and over run the peer's jitter
         * buffer. The deadline is an absolute schedule advanced one packet span at a time: ordinary scheduling
         * jitter within a single packet is absorbed by holding that schedule (a late send is followed by a
         * shorter gap that returns to the grid), so the average cadence neither drifts slow and starves the
         * peer nor bunches and over runs it. Only a true stall, where the deadline has fallen more than one
         * full packet behind real time, resyncs to the current time. This is the final cadence gate at the wire.
         *
         * @param rtpTimestamp the unsigned 32 bit RTP timestamp of the packet about to be sent
         */
        private void paceSend(long rtpTimestamp) {
            var nowNanos = System.nanoTime();
            if (!sendPacingStarted) {
                sendPacingStarted = true;
                sendPacingDeadlineNanos = nowNanos;
                sendPacingLastTimestamp = rtpTimestamp;
                return;
            }
            var packetAudioNanos = (rtpTimestamp - sendPacingLastTimestamp) * 1_000_000_000L / AUDIO_RTP_CLOCK_RATE;
            sendPacingLastTimestamp = rtpTimestamp;
            sendPacingDeadlineNanos += packetAudioNanos;
            if (sendPacingDeadlineNanos < nowNanos - packetAudioNanos) {
                sendPacingDeadlineNanos = nowNanos;
            }
            for (var remaining = sendPacingDeadlineNanos - System.nanoTime(); remaining > 0;
                 remaining = sendPacingDeadlineNanos - System.nanoTime()) {
                LockSupport.parkNanos(remaining);
            }
        }

        /**
         * Paces the outbound video send to wall clock real time, using the access unit's presentation
         * timestamp so the wire cadence matches the source frame rate rather than the decode and encode speed.
         *
         * <p>Holds each access unit until a running deadline ({@link #videoSendPacingDeadlineNanos}) that
         * advances by the frame's presentation timestamp span since the previous send. A capture whose frames
         * arrive faster than real time, such as a media file or generated source that decodes and encodes as
         * fast as it can, would otherwise burst the whole clip onto the wire and over run the peer's jitter
         * buffer; a device camera is already paced by its capture hardware, so its spans already track
         * wall clock and this gate adds no delay. The deadline is an absolute schedule advanced one frame span
         * at a time, so ordinary scheduling jitter is absorbed by holding the schedule; only a true stall,
         * where the deadline has fallen more than one full span behind real time, resyncs to the current time.
         * This is the video counterpart of {@link #paceSend(long)}, the final cadence gate at the wire.
         *
         * @param framePtsMicros the presentation timestamp of the access unit about to be sent, in
         *                       microseconds
         */
        private void paceVideoSend(long framePtsMicros) {
            var nowNanos = System.nanoTime();
            if (!videoSendPacingStarted) {
                videoSendPacingStarted = true;
                videoSendPacingDeadlineNanos = nowNanos;
                videoSendPacingLastPtsMicros = framePtsMicros;
                return;
            }
            var frameSpanNanos = (framePtsMicros - videoSendPacingLastPtsMicros) * 1_000L;
            videoSendPacingLastPtsMicros = framePtsMicros;
            videoSendPacingDeadlineNanos += frameSpanNanos;
            if (videoSendPacingDeadlineNanos < nowNanos - frameSpanNanos) {
                videoSendPacingDeadlineNanos = nowNanos;
            }
            for (var remaining = videoSendPacingDeadlineNanos - System.nanoTime(); remaining > 0;
                 remaining = videoSendPacingDeadlineNanos - System.nanoTime()) {
                LockSupport.parkNanos(remaining);
            }
        }

        /**
         * Ships one encoded video access unit as one or more video RTP packets through the transport,
         * fragmenting it to the path transmission unit.
         *
         * <p>Paces the send to wall clock through {@link #paceVideoSend(long)} before packetizing, so a source
         * that decodes and encodes faster than real time does not burst the whole stream onto the wire.
         *
         * <p>Splits the access unit into RTP payloads through
         * {@link H264RtpPacketization#packetizeAccessUnit(byte[], int)} (a single NAL payload per NAL that
         * fits {@link #VIDEO_MTU_BYTES}, an {@code FU-A} fragment run for a NAL too large), packetizes each through
         * {@link OutboundVideoRtpPacketizer#packetize(byte[], long, boolean, boolean, boolean)} sharing the access unit's
         * ninety kilohertz timestamp, and ships each through {@link MediaTransport#sendMedia} with the marker
         * bit set only on the last packet of the picture. An empty access unit (a dropped frame) sends
         * nothing.
         *
         * @implNote This implementation performs the H264 RTP fragmentation through
         * {@link H264RtpPacketization}: a NAL larger than the per packet budget is split into {@code FU-A} units and
         * the RTP marker is set on the final packet of the access unit, the symmetric counterpart of the
         * inbound reassembly in {@link InboundH264Depacketizer}. The relayed video is not additionally sealed
         * per frame with the end to end SFrame transform: WhatsApp does not run the video SFrame transform on
         * the SFU relayed video send path (the media rides the relay hop by hop SRTP), so the fragments are
         * protected only by hop by hop SRTP, the same as a one to one video call.
         *
         * @param transport   the relay transport
         * @param packetizer  the outbound video RTP packetizer holding the SSRC, sequence, and timestamp
         * @param frame       the encoded access unit to send
         */
        private void sendVideoPacket(MediaTransport transport, OutboundVideoRtpPacketizer packetizer,
                                     EncodedVideoFrame frame) {
            try {
                paceVideoSend(frame.ptsMicros());
                var payloads = H264RtpPacketization.packetizeAccessUnit(frame.payload(), VIDEO_MTU_BYTES);
                for (var index = 0; index < payloads.size(); index++) {
                    var payload = payloads.get(index);
                    var marker = index == payloads.size() - 1;
                    var packet = packetizer.packetize(payload, frame.ptsMicros(), marker, index == 0, frame.keyFrame());
                    // The wire length excludes the trailing SRTP tag room the packetizer over allocates and
                    // includes the packet's header extension.
                    transport.sendMedia(packet, packet.length - 64);
                }
            } catch (RuntimeException exception) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "calls outbound video send failed", exception);
            }
        }

        /**
         * Writes one outbound ICE or DTLS datagram to a destination over the host UDP socket.
         *
         * <p>This is the {@link LiveRelayTransport.Egress} the transport ships its STUN connectivity checks
         * and DTLS records through; the host socket is the {@code RTCPeerConnection}-equivalent ICE/DTLS
         * transport socket, not a raw media socket. A send failure is best effort: a dropped datagram is a
         * routine loss the connectivity check retransmission or the DTLS retransmission recovers.
         *
         * @param channel     the host UDP socket
         * @param destination the transport address to send to
         * @param payload     the datagram bytes
         * @return the number of bytes accepted, or zero on failure
         */
        private static int socketSend(DatagramChannel channel, SocketAddress destination, byte[] payload) {
            try {
                return channel.send(ByteBuffer.wrap(payload), destination);
            } catch (IOException exception) {
                return 0;
            }
        }

        /**
         * Drains rendered samples from the playback ring for the playback driver and raises a demand pulse.
         *
         * <p>This is the {@link com.github.auties00.cobalt.calls.platform.audio.AudioPlaybackDriver.RenderedAudioSource}
         * the playback driver pulls each time its device needs samples; it copies up to {@code frameCount}
         * buffered samples out of the ring (a short or zero count when the ring is nearly drained, which the
         * driver pads with silence) and signals demand so the writer pump tops the ring back up in time.
         *
         * @param playbackRing the playback ring the writer pump fills
         * @param out          the buffer to render samples into
         * @param frameCount   the maximum number of samples to render
         * @return the number of samples copied, in {@code [0, frameCount]}
         */
        private static int drainPlaybackRing(AudioPlaybackRing playbackRing, short[] out, int frameCount) {
            var produced = playbackRing.read(out, 0, frameCount);
            playbackRing.signalDemand();
            return produced;
        }

        /**
         * Builds the video pipeline for a video call, or returns {@code null} when the codec cannot be
         * opened.
         *
         * <p>Opens the negotiated baseline {@link VideoCodec} through the registry and wires a
         * {@link VideoJitterBuffer} inbound decode and render loop that delivers each decoded picture to
         * the call's {@linkplain MediaStreams#videoPlayback() application video sink} when one was
         * supplied, falling back to {@link VoipHostApi#renderVideoFrame} otherwise; the pipeline also drives
         * the outbound encode leg from {@code videoCaptureSource} when one is present. That source is the
         * application {@link VideoOutput} when the call supplied one, or the
         * {@link ManagerVideoSourceBridge} the {@link #voipDriverManager}'s camera capture feeds when it did
         * not, or {@code null} on a receive only video leg. The encoder geometry is taken from the source's
         * advertised {@link VideoOutput#width()} by {@link VideoOutput#height()} at {@link VideoOutput#fps()}
         * when one is present, so the codec accepts the source's frames without a geometry mismatch rejection,
         * and the negotiated default geometry otherwise. A codec the build cannot open (the native binding is
         * absent) is logged and the call proceeds audio only.
         *
         * @param callId             the call identifier
         * @param videoCaptureSource the resolved outbound video source the encode loop drains, or
         *                           {@code null} when the call sends no video
         * @param videoPlayback      the application video playback sink, or {@code null} to render through the
         *                           host
         * @return the built video pipeline, or {@code null} when the codec could not be opened
         */
        private VideoPipeline buildVideoPipeline(String callId, VideoOutput videoCaptureSource,
                                                 VideoInput videoPlayback) {
            try {
                var registry = VideoCodecRegistry.INSTANCE;
                var width = videoCaptureSource != null ? videoCaptureSource.width() : VIDEO_WIDTH;
                var height = videoCaptureSource != null ? videoCaptureSource.height() : VIDEO_HEIGHT;
                var frameRate = videoCaptureSource != null ? videoCaptureSource.fps() : VIDEO_FRAME_RATE;
                var params = VideoCodecParams.forResolution(VideoDecoderCapability.H264, width, height, frameRate);
                var codec = registry.open(params);
                var estimator = new VideoJitterEstimator(VIDEO_JITTER_WINDOW);
                var timing = new VideoTimingController(estimator);
                var jitterBuffer = new VideoJitterBuffer(estimator, timing,
                        com.github.auties00.cobalt.calls.media.video.jitter.AvSyncFeedbackSink.noop());
                return new VideoPipeline(callId, host, codec, params, jitterBuffer, videoCaptureSource,
                        videoPlayback);
            } catch (RuntimeException | UnsatisfiedLinkError error) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING,
                            "calls video codec unavailable for call {0}; proceeding audio-only: {1}",
                            callId, error.getMessage());
                }
                return null;
            }
        }

        /**
         * Builds the group SFrame key provider for a group call, installing the per participant base key
         * derived from the call key and the local device JID.
         *
         * <p>A one to one call relies on shared key hop by hop SRTP and supplies no SFrame provider; this
         * is called only on a group call. The base key derivation mixes the local device JID, so each
         * participant device seals under a distinct key. A {@code null} return (the local JID is not yet
         * known, or the call key is not the full raw length) leaves the call without an SFrame chain; the
         * provider can be keyed only once both inputs are present.
         *
         * @implNote This implementation installs the per participant base key through
         * {@link CallE2eKeyDerivation#deriveSframeBaseKey}, whose schedule is verified byte for byte against a
         * live WhatsApp group call: for a captured raw key and device JID the derived base key matches
         * exactly, and the same raw key with a different device JID yields a different base key. The base key
         * is installed under key id {@code 0}, matching the native key provider.
         *
         * @param callKey the thirty two byte raw end to end call key
         * @return the SFrame key provider with the local base key installed, or {@code null} when the local
         *         JID is not yet known or the call key is not the full raw length
         */
        private SFrameKeyProvider groupSframeProvider(byte[] callKey) {
            var self = selfDeviceJid();
            if (self == null || callKey.length != CallE2eKeyDerivation.RAW_E2E_KEY_LENGTH) {
                return null;
            }
            var baseKey = CallE2eKeyDerivation.deriveSframeBaseKey(callKey, self);
            var provider = new SFrameKeyProvider();
            provider.setChainKey(baseKey, 0L);
            return provider;
        }

        /**
         * Builds the local SSRC layout this client transmits on and the subscription layer advertises.
         *
         * <p>Derives the self device's deterministic media SSRCs from the call id and the device JID through
         * {@link CallSecureSsrcGenerator}: the audio main SSRC (media type {@code 0}) the audio packetizer
         * stamps, and, on a video call, the two simulcast video stream primary SSRCs (media type {@code 2})
         * the video packetizer stamps, plus the application data SSRC (media type {@code 6}) the relay
         * app data stream stamps. An audio only call leaves the video SSRCs {@link StreamLayout#ABSENT_SSRC}
         * so no video descriptor is advertised, matching the native builder that appends a video descriptor
         * only when video is active.
         *
         * @implNote This implementation reproduces the self device SSRC set the native engine allocates and
         * advertises: the audio triple primary (code {@code 0}), the two simulcast video triple primaries
         * (code {@code 2}, stream ids {@code 0} and {@code 1}), and the app data SSRC (code {@code 6}). The
         * live transcription SSRC and the hop by hop FEC transmit and receive SSRCs (media types {@code 7} and
         * {@code 8}) are left {@link StreamLayout#ABSENT_SSRC} here: the native engine appends each only when
         * its feature flag is set, and those flag defaults are unknown, so advertising the streams
         * unconditionally would over declare what this client carries. The uplink prefetch flag the app data
         * and video descriptors carry is runtime derived and left {@code false}, its observed default. The SSRC
         * values themselves are byte verified against a live call in {@link CallSecureSsrcGenerator} (for the
         * LID device JID form); the keying JID this method is handed is the own {@code <lid>:<device>@lid}
         * device JID {@link #selfDeviceJid()} returns, the same JID the SFrame key derivation reads, so the
         * SSRCs and the SFrame keys are both derived from the verified device JID form and match the values a
         * peer pre registers. A {@code null} self JID (the account not yet paired) yields a random audio and
         * app data layout so the call still runs; this is a degenerate edge, not the faithful path, since a
         * random SSRC will not match the value the peer pre registers.
         *
         * @param callId        the call id keying the secure SSRC generation
         * @param selfDeviceJid the local device's call JID, or {@code null} when the account is not yet paired
         * @param video         whether the call sends video, so the video simulcast SSRCs are allocated
         * @return the local SSRC layout
         */
        private static StreamLayout buildStreamLayout(String callId, Jid selfDeviceJid, boolean video) {
            if (selfDeviceJid == null) {
                // Degenerate edge: without the self device JID the deterministic SSRCs cannot be derived, so a
                // random audio and app data layout keeps the call running. Not faithful (a random SSRC will not
                // match what the peer pre registers); production always seeds the self JID at assembly.
                var random = new java.security.SecureRandom();
                return new StreamLayout(random.nextInt(), StreamLayout.ABSENT_SSRC, StreamLayout.ABSENT_SSRC,
                        random.nextInt(), StreamLayout.ABSENT_SSRC, StreamLayout.ABSENT_SSRC,
                        StreamLayout.ABSENT_SSRC, false);
            }
            // The keying JID is the self device's "<lid>:<device>@lid" device JID, the form the native SSRC
            // generation and the SFrame derivation key on and the form CallSecureSsrcGenerator is verified
            // against: LiveMediaPlane.selfDeviceJid() returns the own LID device JID (the account LID user with
            // the device number off the account JID), not the account phone number JID. The same accessor feeds
            // groupSframeProvider, so the stamped SSRCs and the SFrame base key are both derived from the
            // verified device JID form and match the values a peer pre registers.
            var device = selfDeviceJid;
            var audioSsrc = CallSecureSsrcGenerator.audioMainSsrc(callId, device);
            var appDataSsrc = CallSecureSsrcGenerator.appDataSsrc(callId, device);
            var videoStream0Ssrc = video
                    ? CallSecureSsrcGenerator.videoTriple(callId, device, 0).primary()
                    : StreamLayout.ABSENT_SSRC;
            var videoStream1Ssrc = video
                    ? CallSecureSsrcGenerator.videoTriple(callId, device, 1).primary()
                    : StreamLayout.ABSENT_SSRC;
            return new StreamLayout(audioSsrc, videoStream0Ssrc, videoStream1Ssrc, appDataSsrc,
                    StreamLayout.ABSENT_SSRC, StreamLayout.ABSENT_SSRC, StreamLayout.ABSENT_SSRC, false);
        }

        /**
         * Returns the callback fired on each transport keepalive tick that ships this client's {@code 0x0003}
         * subscription envelope over the data channel.
         *
         * <p>The relay forwards a peer's media to this client only after it has this client's subscription and
         * stops forwarding within a few seconds once it goes stale, so the transport resends the envelope on its
         * keepalive cadence ({@code rx_subscription_timer}) through
         * {@link LiveRelayTransport#onSubscriptionResend(Runnable)}; it is shipped on every relay call
         * (one to one audio included) and is never gated on the receive subscription feature predicate. The
         * {@code 0x4024} {@link StunAttributeType#WA_SUBSCRIPTION} attribute carries the fixed
         * {@link StreamDescriptors} declaring this client's own send streams (the audio plus both simulcast video
         * layers, each a media/FEC/NACK triple of distinct SSRCs), built once at assembly by
         * {@link #buildSelfStreamDescriptors(String, Jid)} since the deterministic SSRCs do not change for the
         * call's lifetime; each tick re ships that same descriptor set through
         * {@link LiveRelayTransport#sendSubscriptionEnvelope(StreamDescriptors)}. The descriptors are this
         * client's own streams only: each endpoint declares its own streams in its own envelope, matching the
         * live capture, so the call roster is not read here. It is a no op when the transport is not yet built;
         * the transport itself drops the send when it holds no relay key or reflexive address or the data channel
         * is not open. The {@link SubscriptionStunAttribute} the publisher hands this callback is ignored: this
         * revision ships the {@code 0x4024} descriptor envelope, so the descriptor set is the authoritative
         * content and the publisher's tick is used only as the resend cadence.
         *
         * @param transportHolder the holder of the relay transport the envelope is shipped through, set once the
         *                        transport is built
         * @param selfDescriptors this client's own send stream descriptors carried as the {@code 0x4024}
         *                        attribute; never {@code null}
         * @return the resend callback that emits the {@code 0x0003} envelope
         */
        private static Consumer<SubscriptionStunAttribute> subscriptionResender(
                AtomicReference<LiveRelayTransport> transportHolder,
                StreamDescriptors selfDescriptors) {
            return _ -> {
                var transport = transportHolder.get();
                if (transport == null) {
                    return;
                }
                transport.sendSubscriptionEnvelope(selfDescriptors);
            };
        }

        /**
         * Builds the fused {@code 0x4024} {@link StreamSubscriptions} matrix from the local send layout and the
         * connected peers' receive SSRCs.
         *
         * <p>The matrix is a flat list of {@link StreamSubscriptions.Entry} entries, one per logical stream the
         * client sends or wishes to receive, in the captured order: the participant less self block first, then
         * one block per connected peer in the order {@code peers} supplies. Each block holds the audio entry
         * (no stream index, the stream zero default the protobuf omits) followed by the two simulcast video
         * entries (stream index {@code 1} and {@code 2}) when the call carries video and the stream's SSRC is
         * present. The self block carries no participant field (it is the local sender, the
         * {@code 0xf26a1143}-style own WARP SSRC the capture shows first); a peer block carries the peer's
         * one based positional index ({@code 1} for {@code peers.get(0)}, {@code 2} for {@code peers.get(1)},
         * and so on) as its participant field. Every SSRC is carried as an unsigned {@code long} in the
         * {@code 0..0xFFFFFFFF} range so a high bit set SSRC encodes as a five byte unsigned varint rather than
         * the ten byte sign extended form a negative {@code int} would produce.
         *
         * <p>An entry is emitted only when its SSRC is present: the self audio entry is always present (the
         * layout always carries an audio SSRC), the self video entries are present only when {@code video} is
         * set and the layout carries the corresponding simulcast SSRC, and a peer's video entries are present
         * only when {@code video} is set and the peer derived that video stream's SSRC. A peer always
         * contributes its audio entry, since {@link CallMembership#connectedPeerStreamSubscriptions()} only
         * returns peers whose audio SSRC is derived.
         *
         * @implNote This implementation reproduces the fused matrix WhatsApp publishes: the self block is the
         * local {@link StreamLayout}, and each connected peer block is that peer's deterministic per stream
         * SSRCs (audio main, video stream {@code 0} primary, video stream {@code 1} primary) tagged with the
         * peer's client local positional index. The audio entry omits the stream index and the two video
         * entries carry {@code stream=1} and {@code stream=2}, matching the wire form. The participant index is
         * the positional loop index (self omitted, peers {@code 1..N}) rather than the relay PID that the
         * {@code 0x4021} receive subscription uses.
         * @param streamLayout the local send SSRC layout the self block is built from; never {@code null}
         * @param peers        the connected peers' per stream receive SSRCs in subscription order; never
         *                     {@code null}
         * @param video        whether the call carries video, gating the video stream entries
         * @return the fused stream subscription matrix
         * @throws NullPointerException if {@code streamLayout} or {@code peers} is {@code null}
         */
        static StreamSubscriptions buildStreamSubscriptions(StreamLayout streamLayout,
                                                            List<CallMembership.PeerStreamSsrcs> peers,
                                                            boolean video) {
            Objects.requireNonNull(streamLayout, "streamLayout cannot be null");
            Objects.requireNonNull(peers, "peers cannot be null");
            // TODO: this builds only the connect time 0x4024 subscription, which is shipped on every relay
            //  call (1:1 audio and video included) and is never gated, since the relay forwards media only once
            //  it has it. The separate periodic default stream (video) subscription carrying the leading
            //  0x4000 WARP report is not emitted yet: the 0x4000 control body is sealed by the relay hop by hop
            //  SRTP layer and is not reproducible (see the SubscriptionEnvelope class javadoc). When the 0x4000
            //  emission is added it MUST be gated on WhatsApp's default stream predicate, NOT on this 0x4024:
            //  video_state == 1 AND connected non self peers >= 3 AND the enable_rx_subscription master voip
            //  param AND connected participants including self >= min_num_participants_to_enable_rx_sub.
            var entries = new ArrayList<StreamSubscriptions.Entry>();
            appendStreamSubscriptionBlock(entries, null,
                    streamLayout.audioSsrc(),
                    video ? streamLayout.videoStream0Ssrc() : StreamLayout.ABSENT_SSRC,
                    video ? streamLayout.videoStream1Ssrc() : StreamLayout.ABSENT_SSRC);
            var participantIndex = 1;
            for (var peer : peers) {
                appendStreamSubscriptionBlock(entries, participantIndex,
                        peer.audioSsrc(),
                        video ? peer.videoStream0Ssrc() : StreamLayout.ABSENT_SSRC,
                        video ? peer.videoStream1Ssrc() : StreamLayout.ABSENT_SSRC);
                participantIndex++;
            }
            return new StreamSubscriptionsBuilder()
                    .entries(entries)
                    .build();
        }

        /**
         * Appends one participant's audio and video stream subscription entries to the matrix.
         *
         * <p>Emits the audio entry (no stream index) when {@code audioSsrc} is present, then the first and
         * second simulcast video entries (stream index {@code 1} and {@code 2}) when their SSRCs are present.
         * Each present SSRC is carried as an unsigned {@code long}. An SSRC equal to
         * {@link StreamLayout#ABSENT_SSRC} contributes no entry, so a participant sending only audio yields a
         * single audio entry.
         *
         * @param into        the accumulator the entries are appended to
         * @param participant the participant index to tag the entries with, or {@code null} for the self block
         * @param audioSsrc   the participant's audio main SSRC, or {@link StreamLayout#ABSENT_SSRC} to omit it
         * @param videoSsrc0  the participant's first simulcast video SSRC, or {@link StreamLayout#ABSENT_SSRC}
         * @param videoSsrc1  the participant's second simulcast video SSRC, or {@link StreamLayout#ABSENT_SSRC}
         */
        private static void appendStreamSubscriptionBlock(List<StreamSubscriptions.Entry> into,
                                                          Integer participant,
                                                          int audioSsrc,
                                                          int videoSsrc0,
                                                          int videoSsrc1) {
            appendStreamSubscriptionEntry(into, participant, null, audioSsrc);
            appendStreamSubscriptionEntry(into, participant, 1, videoSsrc0);
            appendStreamSubscriptionEntry(into, participant, 2, videoSsrc1);
        }

        /**
         * Appends one stream subscription entry to the matrix when its SSRC is present.
         *
         * <p>Builds a {@link StreamSubscriptions.Entry} binding the participant and stream indices to the SSRC
         * carried as an unsigned {@code long}, and appends it. When {@code ssrc} is
         * {@link StreamLayout#ABSENT_SSRC} nothing is appended.
         *
         * @param into        the accumulator the entry is appended to
         * @param participant the participant index, or {@code null} for a self entry
         * @param stream      the one based stream index, or {@code null} for the audio stream
         * @param ssrc        the stream SSRC, or {@link StreamLayout#ABSENT_SSRC} to skip
         */
        private static void appendStreamSubscriptionEntry(List<StreamSubscriptions.Entry> into,
                                                          Integer participant,
                                                          Integer stream,
                                                          int ssrc) {
            if (ssrc == StreamLayout.ABSENT_SSRC) {
                return;
            }
            into.add(new StreamSubscriptionsEntryBuilder()
                    .participant(participant)
                    .stream(stream)
                    .ssrc(Integer.toUnsignedLong(ssrc))
                    .build());
        }

        /**
         * Builds the {@code 0x4024} {@link StreamDescriptors} declaring this client's own send streams.
         *
         * <p>Emits a media, FEC, and NACK descriptor for the audio stream and for each of the two simulcast
         * video streams, the nine descriptor self stream set the live WhatsApp Web capture ships on every relay
         * call (audio only included). Each layer's three descriptors carry the three distinct SSRCs
         * {@link CallSecureSsrcGenerator} derives for that layer (the audio {@code 0}/{@code 1}/{@code 4} code
         * family and the video {@code 2}/{@code 3}/{@code 5} family), not one SSRC repeated, and the relay reads
         * them to forward this client's media. The descriptors are this client's own streams only: a peer is not
         * represented, since each endpoint declares its own streams in its own envelope. When
         * {@code selfDeviceJid} is {@code null} (the degenerate no identity bring up) the descriptor list is
         * empty.
         *
         * @param callId        the call id string from the offer the deterministic SSRCs are derived under
         * @param selfDeviceJid this client's own device JID the SSRCs are derived from, or {@code null}
         * @return the local send stream descriptors carried as the {@code 0x4024} attribute
         */
        static StreamDescriptors buildSelfStreamDescriptors(String callId, Jid selfDeviceJid) {
            var descriptors = new ArrayList<StreamDescriptor>();
            if (selfDeviceJid != null) {
                var device = selfDeviceJid;
                appendSelfMediaTriple(descriptors, StreamDescriptor.StreamLayer.AUDIO,
                        CallSecureSsrcGenerator.audioTriple(callId, device));
                appendSelfMediaTriple(descriptors, StreamDescriptor.StreamLayer.VIDEO_STREAM0,
                        CallSecureSsrcGenerator.videoTriple(callId, device, 0));
                appendSelfMediaTriple(descriptors, StreamDescriptor.StreamLayer.VIDEO_STREAM1,
                        CallSecureSsrcGenerator.videoTriple(callId, device, 1));
            }
            return new StreamDescriptorsBuilder()
                    .streamDescriptors(descriptors)
                    .build();
        }

        /**
         * Appends the media, FEC, and NACK descriptors for one send layer from its SSRC triple.
         *
         * <p>The layer contributes three descriptors, in media, FEC, NACK order, carrying the triple's distinct
         * {@code primary}, {@code fec}, and {@code oobNack} SSRCs respectively. Each descriptor binds the layer
         * through its {@link StreamDescriptor.StreamLayer} field and the companion role through its
         * {@link StreamDescriptor.PayloadType} field.
         *
         * @param into   the accumulator the descriptors are appended to
         * @param layer  the logical send layer the triple describes
         * @param triple the layer's {primary, FEC, out of band NACK} SSRC triple
         */
        private static void appendSelfMediaTriple(List<StreamDescriptor> into,
                                                  StreamDescriptor.StreamLayer layer,
                                                  CallSecureSsrcGenerator.SsrcTriple triple) {
            into.add(new StreamDescriptorBuilder()
                    .streamLayer(layer)
                    .payloadType(StreamDescriptor.PayloadType.MEDIA)
                    .ssrc(triple.primary())
                    .build());
            into.add(new StreamDescriptorBuilder()
                    .streamLayer(layer)
                    .payloadType(StreamDescriptor.PayloadType.FEC)
                    .ssrc(triple.fec())
                    .build());
            into.add(new StreamDescriptorBuilder()
                    .streamLayer(layer)
                    .payloadType(StreamDescriptor.PayloadType.NACK)
                    .ssrc(triple.oobNack())
                    .build());
        }

        /**
         * Builds the {@code 0x4025} {@link SenderSubscriptions} publishing this client's own send stream
         * SSRC to PID assignments.
         *
         * <p>Emits exactly four {@link SenderSubscriptionExt} sources, in the load bearing wire order the live
         * caller capture pins: the first simulcast video stream, the second simulcast video stream, the audio
         * stream, then the app data stream. Each source carries the ordered SSRCs the SFU forwards the stream on
         * and, optionally, a {@link SenderSubscriptionExt.PidTemporalLayer} descriptor binding the
         * {@code selfPid} relay participant id to the SVC temporal layer:
         * <ul>
         * <li>video stream {@code 0}: the {@code [primary, FEC, NACK]} triple
         *     ({@link CallSecureSsrcGenerator#videoTriple(String, Jid, int)} stream {@code 0}); a
         *     {@code (selfPid, ENHANCEMENT)} descriptor only when {@code sendingVideo}, otherwise no descriptor;
         * <li>video stream {@code 1}: the {@code [primary, FEC, NACK]} triple (stream {@code 1}); never a
         *     descriptor;
         * <li>audio: the {@code [primary, FEC, NACK]} triple
         *     ({@link CallSecureSsrcGenerator#audioTriple(String, Jid)}); a {@code (selfPid, BASE)}
         *     descriptor;
         * <li>app data: the single {@link CallSecureSsrcGenerator#appDataSsrc(String, Jid)} SSRC; a
         *     {@code (selfPid, BASE)} descriptor.
         * </ul>
         *
         * <p>Every SSRC is carried as an unsigned {@code long} in the {@code 0..0xFFFFFFFF} range so a
         * high bit set SSRC encodes as a five byte unsigned varint rather than the ten byte sign extended form a
         * negative {@code int} would produce. The assignments are this client's own streams only; each endpoint
         * declares its own streams in its own envelope. When {@code selfDeviceJid} is {@code null} (the
         * degenerate no identity bring up) the subscription list is empty.
         *
         * @implNote This implementation reproduces the four source {@code SenderSubscriptions} WhatsApp
         * publishes as the {@code 0x4025} attribute. A {@code BASE} temporal layer is modelled as an absent
         * {@code layer_id} ({@code null}) rather than the {@code BASE} enum constant: {@code BASE} is the
         * proto3 enum default, which the wire form drops, so an audio or app data descriptor emits only
         * {@code 08 <pid>} while the video stream {@code 0} descriptor emits {@code 08 <pid> 10 01} for
         * {@code ENHANCEMENT}; passing the {@code BASE} constant would instead emit a spurious {@code 10 00}.
         * The exact length is per call, since the deterministic SSRCs and the relay self PID are session
         * specific varint widths.
         *
         * @param callId        the call id string from the offer the deterministic SSRCs are derived under
         * @param selfDeviceJid this client's own device JID the SSRCs are derived from, or {@code null}
         * @param selfPid       the local device's relay assigned participant id stamped into each descriptor
         * @param sendingVideo  whether this client sends video, gating the video stream 0 descriptor
         * @return the local sender subscriptions carried as the {@code 0x4025} attribute
         */
        static SenderSubscriptions buildSelfSenderSubscriptions(String callId, Jid selfDeviceJid, int selfPid,
                                                                boolean sendingVideo) {
            var subscriptions = new ArrayList<SenderSubscriptionExt>();
            if (selfDeviceJid != null) {
                var device = selfDeviceJid;
                var video0 = CallSecureSsrcGenerator.videoTriple(callId, device, 0);
                subscriptions.add(senderSubscriptionSource(
                        unsignedSsrcs(video0.primary(), video0.fec(), video0.oobNack()),
                        sendingVideo ? Integer.valueOf(selfPid) : null,
                        sendingVideo ? SenderSubscriptionExt.TemporalLayer.ENHANCEMENT : null));
                var video1 = CallSecureSsrcGenerator.videoTriple(callId, device, 1);
                subscriptions.add(senderSubscriptionSource(
                        unsignedSsrcs(video1.primary(), video1.fec(), video1.oobNack()),
                        null, null));
                var audio = CallSecureSsrcGenerator.audioTriple(callId, device);
                subscriptions.add(senderSubscriptionSource(
                        unsignedSsrcs(audio.primary(), audio.fec(), audio.oobNack()),
                        selfPid, null));
                subscriptions.add(senderSubscriptionSource(
                        unsignedSsrcs(CallSecureSsrcGenerator.appDataSsrc(callId, device)),
                        selfPid, null));
            }
            return new SenderSubscriptionsBuilder()
                    .subscriptions(subscriptions)
                    .build();
        }

        /**
         * Builds one {@link SenderSubscriptionExt} source from its ordered SSRCs and an optional PID descriptor.
         *
         * <p>The source's {@link SenderSubscriptionExt.SSrcsToPidAssignments} carries the {@code ssrcs} list
         * verbatim. A {@code null} {@code pid} omits the {@code pids} entry entirely (the source declares SSRCs
         * with no PID descriptor); a non-{@code null} {@code pid} contributes a single
         * {@link SenderSubscriptionExt.PidTemporalLayer} binding the PID to {@code layer}. A {@code null}
         * {@code layer} models the {@code BASE} proto3 default, which the wire form drops, so the descriptor
         * emits only {@code 08 <pid>}; a non-{@code null} {@code layer} emits the temporal layer field.
         *
         * @param ssrcs the ordered unsigned SSRCs the source declares
         * @param pid   the relay participant id to bind, or {@code null} to emit no PID descriptor
         * @param layer the temporal layer the PID occupies, or {@code null} for the dropped {@code BASE} default
         * @return the assembled sender subscription source
         */
        private static SenderSubscriptionExt senderSubscriptionSource(List<Long> ssrcs, Integer pid,
                                                                      SenderSubscriptionExt.TemporalLayer layer) {
            var pids = pid == null
                    ? List.<SenderSubscriptionExt.PidTemporalLayer>of()
                    : List.of(new SenderSubscriptionExtPidTemporalLayerBuilder()
                            .pid(pid)
                            .layerId(layer)
                            .build());
            var assignments = new SenderSubscriptionExtSSrcsToPidAssignmentsBuilder()
                    .ssrcs(ssrcs)
                    .pids(pids)
                    .build();
            return new SenderSubscriptionExtBuilder()
                    .ssrcLayers(assignments)
                    .build();
        }

        /**
         * Converts a sequence of 32 bit SSRCs to the unsigned {@code long} range the {@code 0x4025} packed
         * SSRC field encodes.
         *
         * <p>Each {@code int} SSRC is widened through {@link Integer#toUnsignedLong(int)} so a high bit set SSRC
         * becomes a value in {@code 0..0xFFFFFFFF} that encodes as a minimal unsigned varint rather than a
         * sign extended ten byte one.
         *
         * @param ssrcs the 32 bit SSRCs in declaration order
         * @return the SSRCs as unsigned {@code long} values, in the same order
         */
        private static List<Long> unsignedSsrcs(int... ssrcs) {
            var list = new ArrayList<Long>(ssrcs.length);
            for (var ssrc : ssrcs) {
                list.add(Integer.toUnsignedLong(ssrc));
            }
            return list;
        }

        /**
         * Returns the default Opus codec parameters for the call audio format.
         *
         * <p>Delegates to {@link OpusCodecParams#forSampleRate(int, int, OpusApplication)}, which seeds the
         * bitrate triplet and the maximum bandwidth ceiling from the per sample rate {@link OpusDefaultAttr}
         * table and applies the WhatsApp voice defaults (variable bitrate on, in band FEC on, discontinuous
         * transmission on, voice signal, single frame per packet, twenty millisecond frame), matching the
         * sixteen kilohertz mono format the public call audio uses.
         *
         * @return the default Opus codec parameters
         */
        private static OpusCodecParams defaultAudioParams() {
            return OpusCodecParams.forSampleRate(AUDIO_SAMPLE_RATE_HZ, AUDIO_CHANNELS, OpusApplication.VOIP);
        }

        /**
         * Builds the {@link LiveNetEq} decode and conceal codec seam over the call's decoder.
         *
         * <p>The {@link LiveNetEq} jitter buffer decodes or conceals one rendered frame per playback pull
         * through this seam, then time stretches and serves it; the seam runs on the single playback pump
         * thread, so the decoder's single writer contract holds and the encode path (a separate
         * {@link OpusAudioCodec} on the capture pump thread) never races it.
         *
         * <p>A normal decode pairs the decoded samples with the packet's own voice activity verdict read
         * through {@link AudioDecoder#packetHasVoiceActivity(byte[])}, which the jitter buffer records and the
         * receiver reads back for audio level metering and mixing; a forward error correction reconstruction
         * reports inactive because it carries no packet of its own.
         *
         * @param decoder the call's audio decoder the jitter buffer renders through
         * @return the jitter buffer's decode and conceal codec seam
         */
        private static com.github.auties00.cobalt.calls.media.audio.pipeline.AudioDecoderReceiver.FrameDecoder
        audioFrameDecoder(AudioDecoder decoder) {
            return new com.github.auties00.cobalt.calls.media.audio.pipeline.AudioDecoderReceiver.FrameDecoder() {
                @Override
                public com.github.auties00.cobalt.calls.media.audio.pipeline.AudioDecoderReceiver.DecodedFrame
                decode(byte[] payload, int frameSamples, boolean fec) {
                    var decoded = decoder.decodeWithVoiceActivity(payload, frameSamples, fec);
                    return new com.github.auties00.cobalt.calls.media.audio.pipeline.AudioDecoderReceiver.DecodedFrame(
                            decoded.pcm(), decoded.voiceActive());
                }

                @Override
                public com.github.auties00.cobalt.calls.media.audio.pipeline.AudioDecoderReceiver.FrameDecoder.DecodedInto
                decodeInto(byte[] payload, int frameSamples, boolean fec, short[] destination) {
                    var into = decoder.decodeWithVoiceActivityInto(payload, frameSamples, fec, destination);
                    return new com.github.auties00.cobalt.calls.media.audio.pipeline.AudioDecoderReceiver.FrameDecoder.DecodedInto(
                            into.length(), into.voiceActive());
                }

                @Override
                public short[] conceal(int frameSamples) {
                    return decoder.conceal(frameSamples);
                }
            };
        }

        /**
         * Selects the relay endpoint the call's transport brings up against, with its ICE credentials and
         * DTLS role.
         *
         * <p>Only UDP endpoints ({@code protocol == 0}) are usable. When {@code electedRelayName} is present and
         * the block carries an endpoint with that {@linkplain RelayEndpoint#relayName() name}, the selection is
         * first restricted to the endpoints of that relay, so this side binds the relay the peer aware
         * {@code <relaylatency>} election chose and both ends converge on a shared relay rather than each
         * picking its locally fastest one. An absent election, or one naming a relay this block does not carry,
         * leaves every endpoint eligible. Among the eligible endpoints the lowest round trip time one wins,
         * using each endpoint's {@code c2r_rtt} when present and its {@code xrtt_ms} estimate otherwise. The
         * selected endpoint's transport address becomes the bootstrap destination, its referenced
         * {@code <auth_token>} (or {@code <token>}) becomes the remote ICE ufrag, and the relay block's raw
         * {@code <key>} becomes the remote ICE password; an empty result means the block carried no usable
         * endpoint, key, or credential.
         *
         * @implNote This implementation synthesizes the answer restricted to the relay endpoint: the remote
         * ufrag is {@code auth_token ?? token}, the remote pwd is the relay {@code <key>}, and the single host
         * candidate is the elected endpoint's address. The address decode runs through the shared
         * {@link RelayEndpoint} parser. The DTLS active mode is carried as {@link RelayConnection#activeMode()}
         * from the caller supplied flag rather than read off the relay block:
         * {@code enable_edgeray_dtls_active_mode} is a voip param (a {@link VoipParamKey} catalogue entry)
         * delivered through the {@code <voip_settings>} channel, not a {@code <relay>} element attribute or
         * child, so the {@link RelayInfo} parse cannot surface it;
         * {@link #edgerayDtlsActiveMode(LiveVoipParamManager)} reads it from the active voip param set and the
         * bring up threads it in here. A relay block whose negotiated voip params do not set the flag selects
         * {@code false} (the relay is the DTLS server, the only observed mode).
         *
         * @param relayInfo        the parsed relay block
         * @param activeMode       whether the negotiated voip params enabled DTLS active mode (the relay is then
         *                         the DTLS client and this side is the DTLS server)
         * @param electedRelayName the name of the relay the peer aware election chose, restricting the
         *                         selection to that relay's endpoints when the block carries one; an empty value
         *                         or an unmatched name leaves every endpoint eligible
         * @return the selected relay connection, or empty when none can be formed
         */
        private static Optional<RelayConnection> selectRelayConnection(RelayInfo relayInfo, boolean activeMode,
                                                                       Optional<String> electedRelayName) {
            var key = relayInfo.key();
            if (key == null) {
                return Optional.empty();
            }
            // Restrict to the elected relay only when the block actually carries it, so a stale or unmatched
            // election name never empties the candidate set and drops the call to no usable endpoint.
            var elected = electedRelayName
                    .filter(name -> relayInfo.endpoints().stream().anyMatch(e -> name.equals(e.relayName())))
                    .orElse(null);
            RelayEndpoint best = null;
            InetSocketAddress bestAddress = null;
            var bestRtt = Integer.MAX_VALUE;
            for (var endpoint : relayInfo.endpoints()) {
                if (endpoint.protocol() != 0) {
                    continue;
                }
                if (elected != null && !elected.equals(endpoint.relayName())) {
                    continue;
                }
                var socketAddress = endpoint.toSocketAddress();
                if (socketAddress.isEmpty()) {
                    continue;
                }
                var rtt = endpoint.c2rRttValue().orElse(endpoint.xrttMs());
                if (best == null || rtt < bestRtt) {
                    best = endpoint;
                    bestAddress = socketAddress.get();
                    bestRtt = rtt;
                }
            }
            if (best == null) {
                return Optional.empty();
            }
            var address = bestAddress;
            var remoteUfrag = relayCredential(relayInfo, best);
            if (remoteUfrag == null) {
                return Optional.empty();
            }
            return Optional.of(new RelayConnection(address, remoteUfrag, key,
                    relayTokenBytes(relayInfo, best), activeMode));
        }

        /**
         * Resolves whether the relay leg runs DTLS in active mode, flipping the relay to the DTLS client role.
         *
         * <p>Reads the {@value #ENABLE_EDGERAY_DTLS_ACTIVE_MODE_PARAM} integer tunable from the manager's
         * active set; a non zero value enables active mode (the relay is the DTLS client and this side is the
         * DTLS server), and an absent, zero, or non integer value (including a call that negotiated no active
         * set) leaves the relay as the DTLS server, the common and only observed mode.
         *
         * <p>The flag is a voip param delivered through the {@code <voip_settings>} channel, not a field of the
         * {@code <relay>} block: WhatsApp Web reads {@code enableEdgerayDtlsActiveMode} off the relay object it
         * surfaces, but in the binary wire stanza the underlying {@code enable_edgeray_dtls_active_mode}
         * arrives as a voip param rather than a relay attribute or child, so it is read here from the param
         * manager rather than from {@link RelayInfo}.
         *
         * @param voipParamManager the call's voip param manager whose active set carries the flag
         * @return {@code true} when the relay leg runs DTLS active mode, {@code false} otherwise
         */
        private static boolean edgerayDtlsActiveMode(LiveVoipParamManager voipParamManager) {
            var key = VoipParamKey.ofWirePath(ENABLE_EDGERAY_DTLS_ACTIVE_MODE_PARAM).orElse(null);
            var params = voipParamManager.activeParams().orElse(null);
            if (key == null || params == null) {
                return false;
            }
            return params.getInteger(key).orElse(0L) != 0L;
        }

        /**
         * Resolves the relay credential bytes an endpoint references into the remote ICE ufrag string.
         *
         * <p>The endpoint's {@code auth_token_id} selects an {@code <auth_token>} credential when present,
         * otherwise its {@code token_id} selects a {@code <token>}; the selected bytes are the remote ufrag.
         * The raw credential bytes are base64 encoded into the ufrag string: WhatsApp splices
         * {@code authToken ?? token} verbatim into the synthesized answer's {@code a=ice-ufrag} (and the
         * browser then carries that string verbatim as the STUN {@code USERNAME} remote ufrag the relay
         * validates), and surfaces those binary credentials as their base64 text. The relay block delivers the
         * {@code <auth_token>} and
         * {@code <token>} as raw bytes, so they are encoded here, whereas the {@code <key>} that keys the
         * STUN {@code MESSAGE-INTEGRITY} (the ICE password) already arrives as a base64 string and is used
         * undecoded (see {@link RelayInfo#key()}).
         *
         * @implNote This implementation uses standard base64 with padding ({@link Base64#getEncoder()}),
         * matching the encoding the relay block's {@code <key>} is delivered in and the form the relay answer
         * carries: the remote ufrag is the base64 of the raw auth token bytes.
         *
         * @param relayInfo the parsed relay block
         * @param endpoint  the selected endpoint
         * @return the base64 remote ufrag string, or {@code null} when no referenced credential is present
         */
        private static String relayCredential(RelayInfo relayInfo, RelayEndpoint endpoint) {
            var authTokenId = endpoint.authTokenId();
            if (authTokenId != RelayEndpoint.UNSET_ID) {
                for (var authToken : relayInfo.authTokens()) {
                    if (authToken.id() == authTokenId) {
                        return Base64.getEncoder().encodeToString(authToken.bytes());
                    }
                }
            }
            var tokenId = endpoint.tokenId();
            for (var token : relayInfo.tokens()) {
                if (tokenId == RelayEndpoint.UNSET_ID || token.id() == tokenId) {
                    return Base64.getEncoder().encodeToString(token.bytes());
                }
            }
            return null;
        }

        /**
         * Resolves the relay {@code <token>} bytes an endpoint references, for the leading {@code 0x4000}
         * {@code RELAY-TOKEN} attribute of the {@code 0x0003} subscription envelope.
         *
         * <p>The endpoint's {@code token_id} selects a {@code <token>} child; its bytes are carried verbatim,
         * because the relay binds the credential they reference before verifying the subscription's message
         * integrity. Unlike the ICE ufrag ({@link #relayCredential(RelayInfo, RelayEndpoint)}, which prefers
         * an {@code <auth_token>} and base64 encodes it), the {@code RELAY-TOKEN} is the raw {@code <token>} bytes.
         *
         * @param relayInfo the parsed relay block
         * @param endpoint  the selected endpoint
         * @return the raw relay token bytes, or {@code null} when no referenced token is present
         */
        private static byte[] relayTokenBytes(RelayInfo relayInfo, RelayEndpoint endpoint) {
            var tokenId = endpoint.tokenId();
            for (var token : relayInfo.tokens()) {
                if (tokenId == RelayEndpoint.UNSET_ID || token.id() == tokenId) {
                    return token.bytes();
                }
            }
            return null;
        }

        /**
         * Builds the single relay host candidate the controlling ICE agent checks against.
         *
         * <p>The relay path synthesizes one remote host candidate at the relay address with the fixed
         * priority {@code 2122262783} the synthesized answer carries.
         *
         * @param relayAddress the relay transport address
         * @return the relay host candidate
         */
        private static IceCandidate relayHostCandidate(SocketAddress relayAddress) {
            return new IceCandidate((InetSocketAddress) relayAddress, IceCandidate.Type.HOST,
                    IceCandidate.Protocol.UDP, RELAY_HOST_CANDIDATE_PRIORITY);
        }

        /**
         * Builds the local host candidate for the bound host UDP socket.
         *
         * <p>The candidate's address is the socket's local address; its priority is the RFC 8445 host
         * priority for component one.
         *
         * @param channel the bound host UDP socket
         * @return the local host candidate
         * @throws WhatsAppCallException.DataChannel if the socket's local address cannot be read
         */
        private static IceCandidate localHostCandidate(DatagramChannel channel) {
            try {
                var local = channel.getLocalAddress();
                var address = local instanceof InetSocketAddress inet
                        ? inet
                        : new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
                return IceCandidate.of(address, IceCandidate.Type.HOST, IceCandidate.Protocol.UDP,
                        ICE_HOST_LOCAL_PREFERENCE, ICE_RTP_COMPONENT_ID);
            } catch (IOException exception) {
                throw new WhatsAppCallException.DataChannel("could not read the local socket address", exception);
            }
        }

        /**
         * Generates a fresh local ICE username fragment.
         *
         * @return a base64url ICE ufrag with at least the RFC 8445 minimum length
         */
        private static String generateIceUfrag() {
            return base64Url(VoipCryptoNative.randomBytes(ICE_UFRAG_RANDOM_BYTES));
        }

        /**
         * Generates a fresh local ICE password.
         *
         * @return a base64url ICE password with at least the RFC 8445 minimum length, as raw ASCII bytes
         */
        private static byte[] generateIcePassword() {
            return base64Url(VoipCryptoNative.randomBytes(ICE_PASSWORD_RANDOM_BYTES))
                    .getBytes(StandardCharsets.US_ASCII);
        }

        /**
         * Encodes bytes as an unpadded base64url string.
         *
         * @param bytes the bytes to encode
         * @return the unpadded base64url encoding
         */
        private static String base64Url(byte[] bytes) {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        }

        /**
         * Carries a selected relay endpoint's bootstrap address and synthesized ICE/DTLS parameters.
         *
         * @param address        the relay transport address the ICE checks and DTLS records are sent to
         * @param remoteUfrag    the remote ICE ufrag, the relay {@code auth_token} or {@code token}
         * @param remotePassword the remote ICE password, the relay {@code <key>} raw bytes
         * @param relayToken     the relay {@code <token>} bytes carried as the {@code 0x4000} {@code RELAY-TOKEN}
         *                       attribute of the {@code 0x0003} subscription envelope, or {@code null} when the
         *                       endpoint references no token
         * @param activeMode     whether the relay enabled DTLS active mode (the relay is then the DTLS
         *                       client)
         */
        private record RelayConnection(SocketAddress address, String remoteUfrag, byte[] remotePassword,
                                       byte[] relayToken, boolean activeMode) {
        }

        /**
         * Parses the relay block's {@code <hbh_key>} into the thirty byte hop by hop key.
         *
         * <p>The {@code <hbh_key>} element carries forty base64 ASCII characters that decode to the
         * thirty byte hop by hop key. A relay block carrying no {@code <hbh_key>}, or one that does not
         * decode to thirty bytes, yields an empty result.
         *
         * @param relay the relay block subtree
         * @return the thirty byte hop by hop key, or empty when none can be parsed
         */
        private static Optional<byte[]> parseHopByHopKey(Stanza relay) {
            return relay.getChild("hbh_key")
                    .flatMap(Stanza::toContentString)
                    .flatMap(LiveMediaPlane::decodeHopByHopKey);
        }

        /**
         * Base64 decodes a {@code <hbh_key>} value to the thirty byte hop by hop key.
         *
         * @param encoded the base64 ASCII {@code <hbh_key>} value
         * @return the thirty byte key, or empty when it does not decode to exactly thirty bytes
         */
        private static Optional<byte[]> decodeHopByHopKey(String encoded) {
            try {
                var decoded = Base64.getDecoder().decode(encoded.trim());
                if (decoded.length != CallE2eKeyDerivation.HBH_KEY_LENGTH) {
                    return Optional.empty();
                }
                return Optional.of(decoded);
            } catch (IllegalArgumentException exception) {
                return Optional.empty();
            }
        }

        /**
         * Releases partially built bring up components after a failure, swallowing any close error.
         *
         * @param channel      the host UDP socket, or {@code null}
         * @param hbhSrtp      the hop by hop SRTP context, or {@code null}
         * @param audioCodec   the audio codec (Opus or MLow), or {@code null}
         * @param repacketizer the Opus repacketizer, or {@code null}
         */
        private static void releaseQuietly(DatagramChannel channel,
                                           LiveHbhSrtpRelay hbhSrtp, AudioCodec audioCodec,
                                           OpusRepacketizer repacketizer) {
            stopQuietly(repacketizer == null ? null : repacketizer::close);
            stopQuietly(audioCodec == null ? null : audioCodec::close);
            stopQuietly(hbhSrtp == null ? null : hbhSrtp::close);
            stopQuietly(channel == null ? null : () -> {
                try {
                    channel.close();
                } catch (IOException exception) {
                    throw new java.io.UncheckedIOException(exception);
                }
            });
        }
    }

    /**
     * The resolved audio capture half: the reader pump source plus whether it is driven by a platform
     * device through the engine's driver manager.
     *
     * <p>When the call supplied an application capture source the {@link #source()} is that source,
     * {@link #usesDevice()} is {@code false}, and the {@link #bridge()} is {@code null}; when it relies on a
     * platform device the source is the device {@link #bridge()}, {@link #usesDevice()} is {@code true}, and
     * the manager's microphone capture is started for it.
     *
     * @param source     the {@link AudioOutput} the reader pump drains, or {@code null} when the capture half
     *                   is idle
     * @param usesDevice whether the half is driven by a platform capture device through the driver manager
     *                   rather than an application source
     * @param bridge     the device to-{@link AudioOutput} bridge, or {@code null} when the application source
     *                   is used
     */
    private record AudioCaptureHalf(AudioOutput source, boolean usesDevice, CaptureSourceBridge bridge) {
    }

    /**
     * The resolved audio playback half: either an application render loop or a platform device, routed
     * through the engine's driver manager, fed by the demand driven writer pump.
     *
     * <p>When the call supplied an application playback sink the {@link #renderLoop()} delivers decoded
     * frames to it, {@link #usesDevice()} is {@code false}, and the platform fields are {@code null}; when it
     * relies on a platform device {@link #usesDevice()} is {@code true}, the {@link #writerPump()} and
     * {@link #ring()} are its pump and ring, and the manager's playback is started for it.
     *
     * @param renderLoop the application playback render loop, or {@code null} when a platform device is used
     * @param usesDevice whether the half is driven by a platform playback device through the driver manager
     *                   rather than an application sink
     * @param writerPump the demand driven writer pump feeding the platform device, or {@code null} when the
     *                   application sink is used
     * @param ring       the playback ring the writer pump fills and the device drains, or {@code null} when
     *                   the application sink is used
     */
    private record AudioPlaybackHalf(AudioPlaybackLoop renderLoop, boolean usesDevice,
                                     AudioWriterPump writerPump, AudioPlaybackRing ring) {
    }

    /**
     * A virtual thread render loop that pulls decoded frames from the audio receiver at the jitter buffer
     * get period and delivers each to an application {@link AudioInput}
     * playback sink.
     *
     * <p>This is the application stream playback counterpart to the platform writer pump: instead of pacing a
     * device line through an {@link AudioPlaybackRing}, it renders one frame per get period and offers it to
     * the application sink, blocking on a steady twenty millisecond cadence the receiver's decode satisfies.
     * A speaker bound sink renders the offered frame to its device, while a programmatic sink (a bot, a
     * call to call bridge) buffers it for the application to read; either way the loop is the single producer
     * of the sink. The presentation timestamp stamped on each frame advances one get period per pull from a
     * zero origin, which a consumer that reorders or schedules frames can rely on as monotonically
     * non decreasing within the call. The loop runs on its own virtual thread, blocks freely, and exits on
     * {@link #stop()}.
     *
     * @implNote This implementation renders on a fixed twenty millisecond cadence rather than on a host
     * demand pulse, because an application sink (unlike a platform AudioWorklet) raises no demand signal; the
     * receiver's {@link com.github.auties00.cobalt.calls.media.audio.pipeline.AudioDecoderReceiver#pull(short[], int)}
     * is the same per frame decide then render the platform writer pump drives, so the only difference from
     * the platform path is the pacing source and the delivery target.
     */
    private static final class AudioPlaybackLoop {
        /**
         * The render cadence, in nanoseconds, equal to the receiver's jitter buffer get period.
         */
        private static final long RENDER_PERIOD_NANOS =
                com.github.auties00.cobalt.calls.media.audio.pipeline.AudioDecoderReceiver.GET_PERIOD_MILLIS * 1_000_000L;

        /**
         * The number of microseconds in one rendered frame's presentation timestamp advance.
         */
        private static final long FRAME_PTS_MICROS =
                com.github.auties00.cobalt.calls.media.audio.pipeline.AudioDecoderReceiver.GET_PERIOD_MILLIS * 1_000L;

        /**
         * The depth of the reusable sample buffer ring the loop hands borrowed frames out of, sized to
         * the buffered sink's frame capacity plus one.
         *
         * @implNote This implementation uses eleven, one more than the ten frames a sink may hold in flight.
         * A buffer is reused only after the ring has cycled through the other ten hand outs, by which point
         * the frame that borrowed it has necessarily been handed to and consumed by the sink through
         * {@link com.github.auties00.cobalt.calls.stream.AudioInput#offerAudio(com.github.auties00.cobalt.calls.stream.AudioFrame)},
         * so refilling a ring buffer never aliases a frame still reachable through
         * {@link com.github.auties00.cobalt.calls.stream.AudioInput#readAudio()}.
         */
        private static final int RING_DEPTH = 11;

        /**
         * The call identifier, used in the render thread name and diagnostics.
         */
        private final String callId;

        /**
         * The decode and conceal receiver the loop pulls one rendered frame from per cadence tick.
         */
        private final com.github.auties00.cobalt.calls.media.audio.pipeline.AudioDecoderReceiver receiver;

        /**
         * The application playback sink each rendered frame is delivered to.
         */
        private final AudioInput sink;

        /**
         * The far end reference tap each fully rendered block is published into for the capture echo
         * canceller, or {@code null} when the live capture APM is not engaged.
         */
        private final RenderReferenceTap renderTap;

        /**
         * Tracks whether the loop is running, so the render thread exits on {@link #stop()}.
         */
        private final AtomicBoolean running;

        /**
         * The render thread pulling and delivering frames, or {@code null} before {@link #start()}.
         */
        private Thread thread;

        /**
         * Constructs a playback render loop over the receiver and the application sink.
         *
         * @param callId    the call identifier
         * @param receiver  the decode and conceal receiver to pull rendered frames from
         * @param sink      the application playback sink to deliver frames to
         * @param renderTap the far end reference tap each rendered block is published into for the capture
         *                  APM, or {@code null} when live capture conditioning is not engaged
         */
        private AudioPlaybackLoop(String callId,
                                  com.github.auties00.cobalt.calls.media.audio.pipeline.AudioDecoderReceiver receiver,
                                  AudioInput sink,
                                  RenderReferenceTap renderTap) {
            this.callId = callId;
            this.receiver = receiver;
            this.sink = sink;
            this.renderTap = renderTap;
            this.running = new AtomicBoolean();
        }

        /**
         * Starts the render loop on a fresh virtual thread.
         *
         * <p>Calling this more than once, or after {@link #stop()}, has no effect: only the first start binds
         * the thread.
         */
        private void start() {
            if (running.compareAndSet(false, true)) {
                thread = Thread.ofVirtual().name("calls-audio-render-" + callId).start(this::loop);
            }
        }

        /**
         * Stops the render loop and unblocks its thread.
         *
         * <p>Clears the running flag and interrupts the loop thread so a pending render cadence park returns
         * promptly and the loop exits. Idempotent.
         */
        private void stop() {
            if (running.compareAndSet(true, false)) {
                var current = thread;
                if (current != null) {
                    current.interrupt();
                    java.util.concurrent.locks.LockSupport.unpark(current);
                }
            }
        }

        /**
         * Runs the render loop until the loop is stopped.
         *
         * <p>Each tick pulls one frame from the receiver directly into the next buffer of a reusable
         * {@link #RING_DEPTH}-deep ring, stamps the advancing presentation timestamp, and offers the
         * borrowed buffer to the application sink, then parks for the render period. A full size pull, the
         * only outcome the steady jitter buffer cadence produces, hands the ring buffer out as is and
         * advances the ring, so steady state playback allocates no per frame sample array; a short pull is
         * copied out to the produced length instead, leaving the ring buffer to be refilled in place. A
         * pull yielding no samples still parks so the loop neither spins nor starves the sink. An interrupt
         * ends the loop.
         *
         * @implNote This implementation is the single writer of the ring: the loop runs on one virtual
         * thread bound in {@link #start()}, so the round robin index is touched by no other thread and the
         * ring depth alone guards against aliasing a buffer still queued in the sink.
         */
        private void loop() {
            var ring = new short[RING_DEPTH][com.github.auties00.cobalt.calls.media.audio.pipeline.AudioDecoderReceiver.FRAME_SAMPLES];
            var ringIndex = 0;
            var ptsMicros = 0L;
            while (running.get()) {
                try {
                    var buffer = ring[ringIndex];
                    var produced = receiver.pull(buffer, buffer.length);
                    if (produced > 0) {
                        if (renderTap != null) {
                            renderTap.publish(buffer, produced);
                        }
                        short[] samples;
                        if (produced == buffer.length) {
                            samples = buffer;
                            ringIndex = (ringIndex + 1) % RING_DEPTH;
                        } else {
                            samples = Arrays.copyOf(buffer, produced);
                        }
                        sink.offerAudio(new AudioFrame(samples, ptsMicros));
                        ptsMicros += FRAME_PTS_MICROS;
                    }
                    java.util.concurrent.locks.LockSupport.parkNanos(RENDER_PERIOD_NANOS);
                    if (Thread.interrupted()) {
                        break;
                    }
                } catch (RuntimeException exception) {
                    if (Log.DEBUG) {
                        LOGGER.log(Level.DEBUG,
                                "calls audio render error for call {0}: {1}", callId, exception.getMessage());
                    }
                }
            }
        }
    }

    /**
     * A single slot publisher of the most recently rendered 20 ms playback block, read by the live capture
     * echo canceller as its far end reference.
     *
     * <p>The playback path (the render loop on its playback thread, or the writer pump source on the pump
     * thread) publishes each fully rendered block here; the capture {@link WebRtcAudioProcessor}, running on
     * the reader pump thread, reads the latest block as the far end reference the echo canceller cancels
     * against. The two threads meet only through one {@code volatile} reference, so the read sees a complete,
     * immutable snapshot with no lock; a slightly stale reference is acceptable because the render to capture
     * algorithmic delay is carried by the APM's stream delay setting, not by exact block alignment. Until the
     * first block is published the reference is silence, so the canceller has a valid zero reference from the
     * first captured block.
     */
    private static final class RenderReferenceTap {
        /**
         * The shared zero filled block returned as the reference until the first real block is published.
         */
        private static final short[] SILENCE = new short[AUDIO_FRAME_SAMPLES];

        /**
         * The most recently published 20 ms render block, exactly {@code AUDIO_FRAME_SAMPLES} samples, or
         * {@code null} before the first block is rendered.
         *
         * <p>Written by the playback thread and read by the capture thread through this one {@code volatile}
         * field; each published value is a fresh immutable array, so a reader never observes a torn block.
         */
        private volatile short[] latest;

        /**
         * Publishes one rendered block as the far end reference, ignoring a short (concealment) block.
         *
         * <p>Copies the first {@code length} samples into a fresh array and stores it, but only when the block
         * carries a full {@code AUDIO_FRAME_SAMPLES}-sample frame; a shorter pull leaves the previous
         * reference in place, since the echo canceller requires a full length reference and one stale full
         * block is a better reference than a zero padded partial one.
         *
         * @param block  the rendered samples; only the first {@code length} are read
         * @param length the number of valid samples at the start of {@code block}
         */
        private void publish(short[] block, int length) {
            if (length >= AUDIO_FRAME_SAMPLES) {
                latest = Arrays.copyOf(block, AUDIO_FRAME_SAMPLES);
            }
        }

        /**
         * Returns the most recently published render block, or silence when none has been rendered yet.
         *
         * @return the latest {@code AUDIO_FRAME_SAMPLES}-sample far end reference block, never {@code null}
         */
        private short[] reference() {
            var current = latest;
            return current != null ? current : SILENCE;
        }
    }

    /**
     * The live capture conditioning stage inserted ahead of the encoder: runs each captured 20 ms block
     * through the {@link WebRtcAudioProcessor} before forwarding it to the encoder sink.
     *
     * <p>This sits between the {@link AudioReaderPump} and the encoder bound {@link Pcm60msAggregator} on the
     * reader pump's single virtual thread, so it is the sole caller of
     * {@link WebRtcAudioProcessor#process(short[], short[], short[])} and honours the processor's
     * single writer contract. Each 20 ms block is echo cancelled against the last block the playback path
     * rendered (read from the {@link RenderReferenceTap}) and noise suppressed, then the cleaned block is
     * handed to the downstream encoder sink; the wire payload the encoder produces keeps its size and cadence.
     * A block that is not exactly one {@link WebRtcAudioProcessor#BLOCK_SAMPLES} frame is forwarded
     * unconditioned, which the wired {@link AudioReaderPump} never produces (it always drains full 20 ms
     * blocks) but which keeps the stage total.
     *
     * @implNote This implementation conditions into a dedicated scratch block rather than in place, then
     * forwards that block; the downstream {@link Pcm60msAggregator} copies out of it before the next drain,
     * so one reusable buffer suffices. It conditions the CAPTURED audio only: it never touches the playback
     * output, and a non live capture source and a tree without the native shim both bypass this sink entirely.
     */
    private static final class ApmCaptureSink implements AudioReaderPump.AudioBlockSink {
        /**
         * The WebRTC audio processor conditioning each captured block.
         */
        private final WebRtcAudioProcessor processor;

        /**
         * The far end reference tap the last rendered block is read from as the echo canceller reference.
         */
        private final RenderReferenceTap renderTap;

        /**
         * The downstream encoder sink each conditioned block is forwarded to.
         */
        private final AudioReaderPump.AudioBlockSink downstream;

        /**
         * The reusable block the processor conditions into before it is forwarded downstream.
         */
        private final short[] conditioned;

        /**
         * Constructs a conditioning sink over the processor, the reference tap, and the downstream sink.
         *
         * @param processor  the WebRTC audio processor conditioning each captured block; never {@code null}
         * @param renderTap  the far end reference tap the echo canceller reference is read from; never
         *                   {@code null}
         * @param downstream the encoder sink each conditioned block is forwarded to; never {@code null}
         */
        private ApmCaptureSink(WebRtcAudioProcessor processor, RenderReferenceTap renderTap,
                               AudioReaderPump.AudioBlockSink downstream) {
            this.processor = Objects.requireNonNull(processor, "processor cannot be null");
            this.renderTap = Objects.requireNonNull(renderTap, "renderTap cannot be null");
            this.downstream = Objects.requireNonNull(downstream, "downstream cannot be null");
            this.conditioned = new short[WebRtcAudioProcessor.BLOCK_SAMPLES];
        }

        /**
         * {@inheritDoc}
         *
         * @implNote This implementation conditions a full {@link WebRtcAudioProcessor#BLOCK_SAMPLES} block
         * against the latest render reference and forwards the cleaned block; a block of any other length is
         * forwarded unchanged.
         */
        @Override
        public void accept(short[] block, int length) {
            if (length == WebRtcAudioProcessor.BLOCK_SAMPLES) {
                processor.process(block, renderTap.reference(), conditioned);
                downstream.accept(conditioned, WebRtcAudioProcessor.BLOCK_SAMPLES);
            } else {
                downstream.accept(block, length);
            }
        }
    }

    /**
     * The RTP packetizer for the outbound audio stream: stamps each payload with a twelve byte RTP header
     * carrying the deterministic audio SSRC, the running sequence number, and the running media timestamp.
     *
     * <p>The packetizer owns the per stream RTP state the transport does not: the synchronization source,
     * the sixteen bit sequence number, and the media clock timestamp that advances one frame per transmitted
     * packet and, through {@link #noteSilenceFrame()}, one frame per silence frame discontinuous transmission
     * suppresses, so the clock tracks real time across a gap and the resuming packet carries the RTP marker.
     * It writes the header followed by the payload into a buffer with trailing room for the hop by hop
     * SRTP authentication tag the transport appends.
     */
    private static final class OutboundRtpPacketizer {
        /**
         * Holds the synchronization source of the outbound stream, the deterministic audio main SSRC the
         * peer pre registers a receive context for.
         */
        private final int ssrc;

        /**
         * Holds the next sixteen bit RTP sequence number to stamp.
         */
        private int sequence;

        /**
         * Holds the next RTP media clock timestamp to stamp, advanced by {@link #samplesPerPacket} per packet.
         */
        private long timestamp;

        /**
         * Holds the media clock sample span of one outbound packet, the amount the timestamp advances per
         * send: {@code AUDIO_FRAME_SAMPLES} (320, a 20 ms Opus packet) or
         * {@code MLOW_FRAMES_PER_PACKET * AUDIO_FRAME_SAMPLES} (960, a 60 ms MLow packet), so the peer's
         * playout clock advances by the audio each packet actually carries.
         */
        private final int samplesPerPacket;

        /**
         * Whether the RTP marker bit has been stamped on any packet yet, set once the first packet of the
         * stream has carried it.
         */
        private boolean markerStamped;

        /**
         * Whether the next packet resumes a talkspurt after a discontinuous transmission silence gap and so
         * must carry the RTP marker bit, set by {@link #noteSilenceFrame()} and cleared once a packet stamps
         * it.
         */
        private boolean talkspurtResuming;

        /**
         * Constructs a packetizer stamping the given audio SSRC with the RTP sequence seeded at one (the value
         * WhatsApp starts at, not zero) and the timestamp zeroed.
         *
         * <p>The SSRC is the self device's deterministic audio main SSRC
         * ({@link CallSecureSsrcGenerator#audioMainSsrc(String, Jid)}, media type {@code 0}), the
         * value the peer pre registers a receive context for, never random.
         *
         * @param ssrc             the outbound audio stream SSRC
         * @param samplesPerPacket the media clock sample span of one packet, the per packet timestamp advance
         */
        private OutboundRtpPacketizer(int ssrc, int samplesPerPacket) {
            this.ssrc = ssrc;
            this.samplesPerPacket = samplesPerPacket;
            this.sequence = 1;
            this.timestamp = 0;
        }

        /**
         * Packetizes one media payload into an RTP packet buffer with trailing SRTP tag room.
         *
         * <p>Builds the twelve byte RTP header (version two, no padding, the extension bit set, the dynamic
         * audio payload type, the running sequence and timestamp, and the stream SSRC), appends the eight byte
         * {@code 0xDEBE} header extension carrying a single one byte header element, copies the payload after
         * it, and leaves sixty four bytes of trailing room for the transport's hop by hop SRTP tag. The
         * sequence advances by one and the timestamp by {@link #samplesPerPacket}, the packet's own media clock
         * span.
         *
         * @param payload          the media payload to wrap
         * @param extendedSequence the sender's extended sequence (used only for diagnostics; the wire
         *                         sequence is the packetizer's own running counter)
         * @return the RTP packet buffer, sized for the header, the extension, the payload, and the SRTP tag
         * @implNote This implementation sets the extension bit ({@code 0x90}) and writes the eight byte
         * {@code de be 00 01 30 01 00 00} header extension WhatsApp stamps on every audio packet: the profile
         * word {@code 0xDEBE} stored {@code DE BE} (byte swapped to match WhatsApp's little endian store), an
         * extension word count of one, then a single RFC 8285 one byte header element ({@code 0x30}, id three,
         * one data byte) with value {@code 0x01} padded to the word boundary. WhatsApp's relay requires the
         * extension bit set and the {@code de be} profile present on every media packet to forward the stream;
         * every audio packet carries this same id three element with the constant value {@code 0x01}. The end
         * to end SRTP transform encrypts the payload after this header, locating its end from the extension
         * bit, so the extension rides in the clear ahead of the ciphertext.
         */
        private byte[] packetize(byte[] payload, long extendedSequence) {
            var extensionLength = RTP_HEADER_EXTENSION_LENGTH + 4;
            var packet = new byte[RTP_HEADER_LENGTH + extensionLength + payload.length + 64];
            packet[0] = (byte) 0x90;
            // The RTP marker bit flags the first packet of a talkspurt; the peer's jitter buffer keys playout
            // start off it. It rides the first packet of the stream and, when discontinuous transmission drops
            // silence frames, the first packet that resumes after each gap (noteSilenceFrame arms the flag).
            var marker = !markerStamped || talkspurtResuming;
            packet[1] = (byte) ((marker ? 0x80 : 0x00) | (AUDIO_PAYLOAD_TYPE & 0x7F));
            markerStamped = true;
            talkspurtResuming = false;
            packet[2] = (byte) ((sequence >>> 8) & 0xFF);
            packet[3] = (byte) (sequence & 0xFF);
            DataUtils.putInt(packet, 4, (int) timestamp, ByteOrder.BIG_ENDIAN);
            DataUtils.putInt(packet, 8, ssrc, ByteOrder.BIG_ENDIAN);
            packet[12] = (byte) 0xDE;
            packet[13] = (byte) 0xBE;
            packet[14] = 0;
            packet[15] = 1;
            packet[16] = (byte) 0x30;
            packet[17] = (byte) 0x01;
            packet[18] = 0;
            packet[19] = 0;
            System.arraycopy(payload, 0, packet, RTP_HEADER_LENGTH + extensionLength, payload.length);
            sequence = (sequence + 1) & 0xFFFF;
            // Advance the media clock by this packet's own sample span. A silence frame the sender suppressed
            // advances the same clock through noteSilenceFrame instead, so across a discontinuous transmission
            // gap the next transmitted packet's timestamp reflects the elapsed silence (16 kHz clock, the rate
            // the offer advertises, confirmed by a live measurement of WhatsApp's outbound stream).
            timestamp += samplesPerPacket;
            return packet;
        }

        /**
         * Accounts one suppressed discontinuous transmission silence frame in the media clock and arms the
         * talkspurt marker for the next transmitted packet.
         *
         * <p>The sender drops a silence frame instead of transmitting it, but the frame still spans one
         * packet's worth of media time. Advancing the timestamp by {@link #samplesPerPacket} here keeps the
         * media clock aligned with real time across the gap, so the next transmitted packet's timestamp
         * reflects the elapsed silence and the peer conceals exactly that much; arming the marker makes that
         * packet start a new talkspurt.
         *
         * @implNote This implementation advances the same media clock {@link #packetize} stamps, so a stream
         * that suppresses no frames behaves identically to a continuous one: the timestamp of the packet for
         * captured frame {@code n} is {@code n} frames of audio from the start whether the intervening frames
         * were sent or suppressed. It assumes one captured frame per packet ({@code FRAMES_PER_PACKET == 1},
         * the configured value), so a suppressed frame is exactly one {@link #samplesPerPacket} span.
         */
        private void noteSilenceFrame() {
            timestamp += samplesPerPacket;
            talkspurtResuming = true;
        }
    }

    /**
     * The MLow send path block aggregator: joins {@code MLOW_FRAMES_PER_PACKET} captured 20 ms blocks into one
     * 60 ms block so MLow ships WhatsApp's 60 ms ptime while the capture pump keeps the 20 ms cadence the
     * audio processing front end needs.
     *
     * <p>Unlike Opus, MLow cannot concatenate separately encoded frames: a 60 ms MLow packet is three 20 ms
     * internal frames sharing one TOC byte and one range coded bitstream, so the whole 960 sample span must
     * be encoded in a single call. This aggregator sits between the {@link AudioReaderPump} and the
     * {@link AudioEncoderSender}: it copies each captured block into a 960 sample buffer and, each time the
     * buffer fills, hands the full 60 ms block to the wrapped sink in one
     * {@link AudioReaderPump.AudioBlockSink#accept(short[], int)} call, which the sender encodes and ships as
     * one packet through its ordinary single frame path. The audio processing front end has already run on the
     * 20 ms blocks inside the pump, so accumulating here does not disturb it, and the sender keeps its natural
     * one block one packet contract with no empty frame signalling. A partial buffer outstanding at call
     * teardown (fewer than {@code MLOW_FRAMES_PER_PACKET} blocks, the last under 60 ms of audio) is dropped,
     * since the pump stops without draining it.
     *
     * @implNote This implementation copies through its own buffer rather than retaining the pump's reused
     * block, whose contents are valid only for the duration of one
     * {@link AudioReaderPump.AudioBlockSink#accept(short[], int)} call. It drains any input longer than the
     * buffer's remaining room across successive 60 ms emits, so it does not assume
     * {@code AUDIO_FRAME_SAMPLES}-aligned blocks even though the wired pump delivers them.
     */
    private static final class Pcm60msAggregator implements AudioReaderPump.AudioBlockSink {
        /**
         * The sink each assembled 60 ms block is handed to, the audio encoder sender.
         */
        private final AudioReaderPump.AudioBlockSink sink;

        /**
         * The 960 sample (60 ms) buffer captured blocks are copied into before being handed to the sink.
         */
        private final short[] buffer;

        /**
         * The number of valid samples currently held in {@link #buffer}.
         */
        private int filled;

        /**
         * Constructs an aggregator that assembles {@code blockCount} captured 20 ms blocks into one block
         * before handing it to the given sink (three for MLow's 60 ms packet, two for Opus's 40 ms frame).
         *
         * @param sink       the sink each full assembled block is handed to; never {@code null}
         * @param blockCount the number of {@code AUDIO_FRAME_SAMPLES}-sample 20 ms blocks aggregated into one
         *                   emitted block
         */
        private Pcm60msAggregator(AudioReaderPump.AudioBlockSink sink, int blockCount) {
            this.sink = Objects.requireNonNull(sink, "sink cannot be null");
            this.buffer = new short[blockCount * AUDIO_FRAME_SAMPLES];
            this.filled = 0;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote This implementation copies the block into the 60 ms buffer and, whenever the buffer fills,
         * hands it to the wrapped sink and resets; a block carrying more than the remaining room is drained
         * across successive emits.
         */
        @Override
        public void accept(short[] block, int length) {
            var offset = 0;
            while (offset < length) {
                var take = Math.min(length - offset, buffer.length - filled);
                System.arraycopy(block, offset, buffer, filled, take);
                filled += take;
                offset += take;
                if (filled == buffer.length) {
                    sink.accept(buffer, buffer.length);
                    filled = 0;
                }
            }
        }
    }

    /**
     * The H264 RTP payload codec: splits an encoded access unit into the RTP payloads that carry it and
     * reassembles received RTP payloads back into an access unit, in RFC 6184 single NAL, {@code STAP-A}, and {@code FU-A}
     * forms.
     *
     * <p>The outbound side ({@link #packetizeAccessUnit(byte[], int)}) parses the Annex B access unit the
     * H264 encoder emits into its NAL units, then emits one RTP payload per NAL that fits the per packet
     * budget (a single NAL packet) and a sequence of fragmentation unit payloads for a NAL too large to fit
     * ({@code FU-A}), so no payload exceeds the path transmission unit. The inbound side
     * ({@link InboundH264Depacketizer}) is a per stream reassembler that recovers each NAL from a single NAL,
     * {@code STAP-A} aggregation, or {@code FU-A} fragment run payload and concatenates the NAL units of one timestamp into
     * an Annex B access unit the decoder accepts, completing the access unit on the RTP marker bit.
     *
     * @implNote This implementation follows the H264 RTP payload format WhatsApp carries. The depacketizer's
     * byte operations are the contract this codec mirrors: a payload's first byte low five bits select the
     * form ({@code 0x1c} = type 28 {@code FU-A}, {@code 0x18} = type 24 {@code STAP-A}, anything else a single NAL); an {@code FU-A}
     * payload carries a one byte indicator then a one byte FU header whose top bit is the start bit, second
     * bit the end bit, and low five bits the original NAL type, and the reassembled NAL header is
     * {@code (indicator & 0xe0) | (fuHeader & 0x1f)}; a {@code STAP-A} payload carries the one byte aggregation header
     * then, per aggregated NAL, a two byte big endian length prefix and that many NAL bytes. The packetizer
     * fragments at the negotiated {@link #VIDEO_MTU_BYTES} so an {@code FU-A} fragment plus its RTP and SRTP overhead
     * fits one datagram, and the caller sets the RTP marker bit only on the last packet of an access unit. The
     * decoder geometry is the negotiated default until the inbound resolution is threaded through. {@code STAP-A} is
     * emitted as well as parsed: {@link #flushAggregate} aggregates a run of consecutive budget fitting NAL
     * units into one payload (the {@code maxNRI | 24} header then a two byte length prefix per NAL), so a key
     * frame's small parameter set NALs ride one {@code STAP-A} ahead of the slice's {@code FU-A} run; the aggregation
     * threshold is the payload budget, and a lone budget fitting NAL stays a single NAL packet.
     */
    private static final class H264RtpPacketization {
        /**
         * The RFC 6184 NAL unit type mask, the low five bits of a NAL header or RTP payload header byte.
         */
        private static final int NAL_TYPE_MASK = 0x1F;

        /**
         * The RFC 6184 NAL reference idc and forbidden zero mask, the high three bits of a NAL header byte
         * carried forward from an {@code FU-A} indicator into the reassembled NAL header.
         */
        private static final int NAL_NRI_MASK = 0xE0;

        /**
         * The RFC 6184 {@code STAP-A} aggregation packet NAL type, twenty four.
         */
        private static final int NAL_TYPE_STAP_A = 24;

        /**
         * The RFC 6184 fragmentation unit A NAL type, twenty eight.
         */
        private static final int NAL_TYPE_FU_A = 28;

        /**
         * The FU header start bit mask, the high bit marking the first fragment of a fragmented NAL.
         */
        private static final int FU_START_BIT = 0x80;

        /**
         * The FU header end bit mask, the second highest bit marking the last fragment of a fragmented NAL.
         */
        private static final int FU_END_BIT = 0x40;

        /**
         * The number of bytes the codec payload budget reserves per packet beyond the RTP header: the
         * hop by hop SRTP authentication tag room the transport appends.
         */
        private static final int PACKET_OVERHEAD_BYTES = RTP_SRTP_TAG_ROOM;

        /**
         * Hidden constructor; this is a stateless helper exposing only static members and the inbound
         * reassembler.
         */
        private H264RtpPacketization() {
            throw new AssertionError("No instances");
        }

        /**
         * A NAL unit as an offset and length into the access unit byte stream, deferring the copy of the
         * NAL bytes until a single NAL payload must escape or the aggregate or fragment buffer is written.
         *
         * @param offset the start offset of the NAL, including its one byte header, within the access unit
         * @param length the NAL byte length, including its one byte header
         */
        private record NalSlice(int offset, int length) {
        }

        /**
         * Splits one Annex B H264 access unit into the RTP payloads that carry it.
         *
         * <p>The access unit is parsed into its NAL units on the Annex B start codes the encoder emits. A run
         * of consecutive NAL units that fit the per packet codec budget together is aggregated into one {@code STAP-A}
         * payload (a run of a single such NAL stays a single NAL payload); a NAL too large to fit is split into
         * fragmentation unit A payloads whose first carries the start bit, whose last carries the end bit, and
         * whose reassembled NAL type is the original NAL type. So a key frame's small parameter set NALs ride
         * one {@code STAP-A} ahead of the slice's {@code FU-A} run. An empty or start code only access unit yields no payloads.
         *
         * @param accessUnit the Annex B access unit bytes (NAL units delimited by {@code 00 00 01} or
         *                   {@code 00 00 00 01} start codes)
         * @param mtuBytes   the maximum datagram size; the per packet codec payload budget is this minus the
         *                   RTP header and the SRTP tag room
         * @return the RTP payloads carrying the access unit, in send order, never {@code null}
         */
        private static List<byte[]> packetizeAccessUnit(byte[] accessUnit, int mtuBytes) {
            var payloadBudget = mtuBytes - RTP_HEADER_LENGTH - PACKET_OVERHEAD_BYTES;
            if (payloadBudget < 2) {
                payloadBudget = 2;
            }
            var payloads = new ArrayList<byte[]>();
            var pending = new ArrayList<NalSlice>();
            var pendingBytes = 1;
            for (var nal : splitAnnexBNalUnits(accessUnit)) {
                if (nal.length() == 0) {
                    continue;
                }
                if (nal.length() > payloadBudget) {
                    pendingBytes = flushAggregate(accessUnit, pending, payloads);
                    fragmentNal(accessUnit, nal, payloadBudget, payloads);
                    continue;
                }
                if (pendingBytes + 2 + nal.length() > payloadBudget) {
                    pendingBytes = flushAggregate(accessUnit, pending, payloads);
                }
                pending.add(nal);
                pendingBytes += 2 + nal.length();
            }
            flushAggregate(accessUnit, pending, payloads);
            return payloads;
        }

        /**
         * Flushes the pending run of budget fitting NAL units into one RTP payload and clears it.
         *
         * <p>A run of two or more NAL units becomes one {@code STAP-A} aggregation payload (the {@code maxNRI | 24}
         * aggregation header, then per NAL a two byte big endian length prefix and the NAL bytes); a run of a
         * single NAL unit becomes one single NAL payload; an empty run emits nothing. The pending list is
         * cleared and the reset aggregate byte count, the one {@code STAP-A} header byte, is returned.
         *
         * @param stream  the access unit byte stream the pending slices index into
         * @param pending the run of budget fitting NAL unit slices awaiting aggregation, cleared on return
         * @param out     the payload list the aggregated or single NAL payload is appended to
         * @return the reset pending aggregate byte count, one for the {@code STAP-A} header byte
         */
        private static int flushAggregate(byte[] stream, List<NalSlice> pending, List<byte[]> out) {
            if (pending.size() == 1) {
                var nal = pending.get(0);
                out.add(Arrays.copyOfRange(stream, nal.offset(), nal.offset() + nal.length()));
            } else if (pending.size() >= 2) {
                var total = 1;
                var maxNri = 0;
                for (var nal : pending) {
                    total += 2 + nal.length();
                    maxNri = Math.max(maxNri, stream[nal.offset()] & NAL_NRI_MASK);
                }
                var stapA = new byte[total];
                stapA[0] = (byte) (maxNri | NAL_TYPE_STAP_A);
                var offset = 1;
                for (var nal : pending) {
                    stapA[offset++] = (byte) ((nal.length() >>> 8) & 0xFF);
                    stapA[offset++] = (byte) (nal.length() & 0xFF);
                    System.arraycopy(stream, nal.offset(), stapA, offset, nal.length());
                    offset += nal.length();
                }
                out.add(stapA);
            }
            pending.clear();
            return 1;
        }

        /**
         * Splits an {@code FU-A} NAL into fragmentation unit A payloads sized to the per packet budget.
         *
         * <p>The NAL's one byte header is removed and its body is split into chunks each at most
         * {@code payloadBudget - 2} bytes (the {@code FU-A} payload carries a one byte indicator and a one byte FU
         * header before the fragment body). The indicator carries the original NAL's reference idc and
         * forbidden zero bits with the {@code FU-A} type; the FU header carries the start bit on the first fragment,
         * the end bit on the last, and the original NAL type in its low five bits.
         *
         * @param stream        the access unit byte stream the NAL slice indexes into
         * @param nal           the NAL unit slice including its one byte header; longer than {@code payloadBudget}
         * @param payloadBudget the per packet codec payload budget
         * @param out           the payload list each fragment is appended to
         */
        private static void fragmentNal(byte[] stream, NalSlice nal, int payloadBudget, List<byte[]> out) {
            var header = stream[nal.offset()] & 0xFF;
            var indicator = (byte) ((header & NAL_NRI_MASK) | NAL_TYPE_FU_A);
            var nalType = header & NAL_TYPE_MASK;
            var fragmentBudget = Math.max(1, payloadBudget - 2);
            var bodyOffset = 1;
            var bodyLength = nal.length() - 1;
            while (bodyLength > 0) {
                var chunk = Math.min(fragmentBudget, bodyLength);
                var first = bodyOffset == 1;
                var last = bodyLength - chunk == 0;
                var fuHeader = nalType;
                if (first) {
                    fuHeader |= FU_START_BIT;
                }
                if (last) {
                    fuHeader |= FU_END_BIT;
                }
                var payload = new byte[2 + chunk];
                payload[0] = indicator;
                payload[1] = (byte) fuHeader;
                System.arraycopy(stream, nal.offset() + bodyOffset, payload, 2, chunk);
                out.add(payload);
                bodyOffset += chunk;
                bodyLength -= chunk;
            }
        }

        /**
         * Splits an Annex B byte stream into its NAL units, dropping the start codes.
         *
         * <p>A NAL unit boundary is the three byte {@code 00 00 01} start code prefix, which a four byte
         * {@code 00 00 00 01} start code carries as its last three bytes; the bytes after one prefix up to the
         * next prefix (or the end) are one NAL unit, with the extra leading zero of a four byte code trimmed off
         * the preceding NAL's tail. A stream with no start code is treated as a single NAL unit so a
         * length prefix free or already bare access unit is not dropped.
         *
         * @param stream the Annex B (or bare) access unit bytes
         * @return the NAL units as offset and length slices into {@code stream}, in order, each including its
         *         one byte NAL header but no start code
         */
        private static List<NalSlice> splitAnnexBNalUnits(byte[] stream) {
            var nals = new ArrayList<NalSlice>();
            var firstTriple = findStartCode(stream, 0);
            if (firstTriple < 0) {
                if (stream.length > 0) {
                    nals.add(new NalSlice(0, stream.length));
                }
                return nals;
            }
            var nalStart = firstTriple + 3;
            while (nalStart <= stream.length) {
                var nextTriple = findStartCode(stream, nalStart);
                if (nextTriple < 0) {
                    if (nalStart < stream.length) {
                        nals.add(new NalSlice(nalStart, stream.length - nalStart));
                    }
                    break;
                }
                // The NAL ends at the next start code prefix; a four byte start code (the triple preceded by an
                // extra zero) leaves that leading zero out of the NAL's tail.
                var nalEnd = nextTriple > nalStart && stream[nextTriple - 1] == 0 ? nextTriple - 1 : nextTriple;
                if (nalEnd > nalStart) {
                    nals.add(new NalSlice(nalStart, nalEnd - nalStart));
                }
                nalStart = nextTriple + 3;
            }
            return nals;
        }

        /**
         * Returns the index of the next {@code 00 00 01} start code prefix at or after the given offset, or
         * {@code -1} when none remains.
         *
         * <p>The returned index is the prefix's first {@code 0x00} byte; the NAL unit it delimits begins three
         * bytes later, and a four byte {@code 00 00 00 01} start code is the same prefix with one more leading
         * zero, so the prefix scan locates both forms.
         *
         * @param stream the byte stream to scan
         * @param from   the offset to start scanning from
         * @return the index of the {@code 00 00 01} prefix's first {@code 0x00} byte, or {@code -1}
         */
        private static int findStartCode(byte[] stream, int from) {
            for (var i = Math.max(0, from); i + 2 < stream.length; i++) {
                if (stream[i] == 0 && stream[i + 1] == 0 && stream[i + 2] == 1) {
                    return i;
                }
            }
            return -1;
        }

        /**
         * Wraps a reassembled list of NAL units into an Annex B access unit.
         *
         * <p>Each NAL unit is prefixed with a four byte {@code 00 00 00 01} start code, the form the H264
         * decoder accepts, and the prefixed NAL units are concatenated in order.
         *
         * @param nalUnits the NAL units of one access unit, in order
         * @return the Annex B access unit bytes
         */
        private static byte[] toAnnexB(List<byte[]> nalUnits) {
            var size = 0;
            for (var nal : nalUnits) {
                size += 4 + nal.length;
            }
            var out = new byte[size];
            var offset = 0;
            for (var nal : nalUnits) {
                out[offset + 2] = 0;
                out[offset + 3] = 1;
                System.arraycopy(nal, 0, out, offset + 4, nal.length);
                offset += 4 + nal.length;
            }
            return out;
        }
    }

    /**
     * The per stream inbound H264 RTP reassembler: recovers each NAL unit from a single NAL, {@code STAP-A}, or {@code FU-A}
     * RTP payload and concatenates the NAL units of one access unit, completing it on the RTP marker bit.
     *
     * <p>The demux hands this each cleartext video RTP payload (after the RTP header is stripped and the
     * SFrame layer, when present, is opened) together with the packet's RTP timestamp and marker bit. A
     * single NAL payload contributes one NAL, a {@code STAP-A} payload contributes the aggregated NALs it carries, and
     * an {@code FU-A} run contributes one NAL reassembled across its start to end fragments. The reassembler buffers
     * the NAL units of the current access unit until the marker bit (the last packet of a picture) or a
     * timestamp change flushes them as one Annex B access unit; a fragment lost mid FU A drops the partial NAL.
     *
     * @implNote This implementation reproduces the inbound side WhatsApp runs: the form switch on the
     * payload header's low five bits, the {@code FU-A} fragment accumulation that prepends the reconstructed NAL
     * header {@code (indicator & 0xe0) | (fuHeader & 0x1f)} on the start fragment, and the {@code STAP-A} split on the
     * two byte big endian length prefixes. The native depacketizer additionally rewrites a duplicate H264 SPS
     * NAL inside a {@code STAP-A} packet and validates SPS and PPS NAL ordering; those parameter set rewrites are not
     * reproduced here because they are an OpenH264 decoder specific normalisation the Cobalt decoder does not
     * require, so the aggregated NALs are passed through in wire order.
     */
    private static final class InboundH264Depacketizer {
        /**
         * The NAL units accumulated for the access unit currently being reassembled, in arrival order.
         */
        private final List<byte[]> currentAccessUnit = new ArrayList<>();

        /**
         * The fragments of the {@code FU-A} NAL currently being reassembled, or empty when no {@code FU-A} is in progress.
         */
        private final List<byte[]> currentFragments = new ArrayList<>();

        /**
         * The reconstructed NAL header byte of the {@code FU-A} NAL in progress, valid only while
         * {@link #currentFragments} is non empty.
         */
        private int currentFragmentNalHeader;

        /**
         * The RTP timestamp of the access unit currently being reassembled, or {@code -1} when none is in
         * progress.
         */
        private long currentTimestamp = -1;

        /**
         * Whether any NAL appended to the access unit in progress carries an IDR slice or a parameter set,
         * accumulated as NALs are reassembled so the completed picture's key frame verdict needs no rescan.
         */
        private boolean currentKeyFrame;

        /**
         * The key frame verdict of the most recently completed access unit, read back by
         * {@link #wasKeyFrame()}.
         */
        private boolean lastKeyFrame;

        /**
         * Accepts one cleartext video RTP payload, returning a completed Annex B access unit when the packet
         * closes one.
         *
         * <p>A timestamp different from the access unit in progress flushes any buffered NAL units first (a
         * missing marker is tolerated this way), then the payload's form contributes its NAL units to the
         * current access unit, and the marker bit completes and returns it. A payload that contributes nothing
         * (an empty payload, an incomplete {@code STAP-A}, or an {@code FU-A} fragment that does not complete a NAL) returns
         * {@code null}.
         *
         * @param packet    the inbound cleartext RTP packet holding the payload as the slice
         *                  {@code [offset, offset + length)}
         * @param offset    the start offset of the cleartext RTP payload within {@code packet} (the bytes
         *                  past the twelve byte RTP header and any header extension)
         * @param length    the length, in bytes, of the cleartext RTP payload within {@code packet}
         * @param timestamp the packet's RTP timestamp, the access unit grouping key
         * @param marker    whether the RTP marker bit is set, marking the last packet of the access unit
         * @return the completed Annex B access unit, or {@code null} when the packet does not close one
         */
        private byte[] accept(byte[] packet, int offset, int length, long timestamp, boolean marker) {
            if (currentTimestamp != -1 && timestamp != currentTimestamp) {
                currentFragments.clear();
                currentAccessUnit.clear();
                currentKeyFrame = false;
            }
            currentTimestamp = timestamp;
            if (length == 0) {
                return marker ? completeAccessUnit() : null;
            }
            var type = packet[offset] & H264RtpPacketization.NAL_TYPE_MASK;
            if (type == H264RtpPacketization.NAL_TYPE_FU_A) {
                acceptFragment(packet, offset, length);
            } else if (type == H264RtpPacketization.NAL_TYPE_STAP_A) {
                acceptStapA(packet, offset, length);
            } else {
                addNal(Arrays.copyOfRange(packet, offset, offset + length));
            }
            return marker ? completeAccessUnit() : null;
        }

        /**
         * Accepts one fragmentation unit A payload, completing a NAL when the end fragment arrives.
         *
         * <p>The start fragment opens the reassembly and records the reconstructed NAL header from the {@code FU-A}
         * indicator's reference bits and the FU header's NAL type; each fragment's body is appended; the end
         * fragment closes the NAL and adds it to the current access unit. A fragment arriving without a start
         * fragment in progress is dropped (the start was lost), and a start fragment arriving mid reassembly
         * discards the previous partial NAL.
         *
         * @param packet the inbound cleartext RTP packet holding the {@code FU-A} payload (indicator byte, FU header
         *               byte, then the fragment body) as the slice {@code [offset, offset + length)}
         * @param offset the start offset of the {@code FU-A} payload within {@code packet}
         * @param length the length, in bytes, of the {@code FU-A} payload within {@code packet}
         */
        private void acceptFragment(byte[] packet, int offset, int length) {
            if (length < 2) {
                return;
            }
            var indicator = packet[offset] & 0xFF;
            var fuHeader = packet[offset + 1] & 0xFF;
            var start = (fuHeader & H264RtpPacketization.FU_START_BIT) != 0;
            var end = (fuHeader & H264RtpPacketization.FU_END_BIT) != 0;
            if (start) {
                currentFragments.clear();
                currentFragmentNalHeader = (indicator & H264RtpPacketization.NAL_NRI_MASK)
                        | (fuHeader & H264RtpPacketization.NAL_TYPE_MASK);
            } else if (currentFragments.isEmpty()) {
                return;
            }
            currentFragments.add(Arrays.copyOfRange(packet, offset + 2, offset + length));
            if (end && !currentFragments.isEmpty()) {
                var size = 1;
                for (var fragment : currentFragments) {
                    size += fragment.length;
                }
                var nal = new byte[size];
                nal[0] = (byte) currentFragmentNalHeader;
                var nalCursor = 1;
                for (var fragment : currentFragments) {
                    System.arraycopy(fragment, 0, nal, nalCursor, fragment.length);
                    nalCursor += fragment.length;
                }
                currentFragments.clear();
                addNal(nal);
            }
        }

        /**
         * Appends one reassembled NAL to the access unit in progress, updating the running key frame verdict.
         *
         * <p>The NAL's type field (its first byte masked with {@link H264RtpPacketization#NAL_TYPE_MASK}) is
         * tested for an instantaneous decoder refresh slice ({@code 5}), a sequence parameter set ({@code 7}),
         * or a picture parameter set ({@code 8}); any of these marks the picture as a key frame, mirroring the
         * bit identical type test the encode side records on {@link EncodedVideoFrame#keyFrame()}.
         *
         * @param nal the reassembled NAL bytes to append
         */
        private void addNal(byte[] nal) {
            currentAccessUnit.add(nal);
            if (nal.length != 0) {
                var type = nal[0] & H264RtpPacketization.NAL_TYPE_MASK;
                if (type == 5 || type == 7 || type == 8) {
                    currentKeyFrame = true;
                }
            }
        }

        /**
         * Accepts one {@code STAP-A} aggregation payload, adding each aggregated NAL to the current access unit.
         *
         * <p>The one byte aggregation header is skipped, then each aggregated NAL is read as a two byte
         * big endian length prefix followed by that many NAL bytes; a truncated prefix or length ends the
         * parse.
         *
         * @param packet the inbound cleartext RTP packet holding the {@code STAP-A} payload (aggregation header, then
         *               per NAL length prefixed units) as the slice {@code [base, base + length)}
         * @param base   the start offset of the {@code STAP-A} payload within {@code packet}
         * @param length the length, in bytes, of the {@code STAP-A} payload within {@code packet}
         */
        private void acceptStapA(byte[] packet, int base, int length) {
            var offset = 1;
            while (offset + 2 <= length) {
                var nalLength = ((packet[base + offset] & 0xFF) << 8) | (packet[base + offset + 1] & 0xFF);
                offset += 2;
                if (nalLength == 0 || offset + nalLength > length) {
                    return;
                }
                addNal(Arrays.copyOfRange(packet, base + offset, base + offset + nalLength));
                offset += nalLength;
            }
        }

        /**
         * Flushes the buffered NAL units into an Annex B access unit and resets for the next picture.
         *
         * <p>Returns {@code null} when no NAL unit was buffered (for example a marker on a packet whose NALs
         * were all dropped), so the caller inserts nothing.
         *
         * @return the completed Annex B access unit, or {@code null} when nothing was buffered
         */
        private byte[] completeAccessUnit() {
            if (currentAccessUnit.isEmpty()) {
                currentFragments.clear();
                currentTimestamp = -1;
                currentKeyFrame = false;
                lastKeyFrame = false;
                return null;
            }
            var accessUnit = H264RtpPacketization.toAnnexB(currentAccessUnit);
            currentAccessUnit.clear();
            currentFragments.clear();
            currentTimestamp = -1;
            lastKeyFrame = currentKeyFrame;
            currentKeyFrame = false;
            return accessUnit;
        }

        /**
         * Returns whether the access unit last returned by {@link #accept(byte[], int, int, long, boolean)} carried an
         * IDR slice or a parameter set, the running verdict accumulated during reassembly.
         *
         * @return {@code true} when the most recently completed access unit is a key frame
         */
        private boolean wasKeyFrame() {
            return lastKeyFrame;
        }
    }

    /**
     * The RTP packetizer for the outbound video stream: stamps each encoded access unit with a twelve byte
     * RTP header carrying the deterministic video SSRC, the running sequence number, and the ninety kilohertz
     * capture timestamp.
     *
     * <p>The packetizer owns the per stream video RTP state the transport does not: the synchronization
     * source, the sixteen bit sequence number, and the ninety kilohertz media clock timestamp derived from
     * each access unit's presentation timestamp. The caller splits each access unit into RTP payloads through
     * {@link H264RtpPacketization#packetizeAccessUnit(byte[], int)} and packetizes each payload here, sharing
     * one ninety kilohertz timestamp across the picture's packets and setting the marker bit only on the last;
     * each call writes the header followed by the payload bytes into a buffer with trailing room for the
     * hop by hop SRTP authentication tag the transport appends.
     */
    private static final class OutboundVideoRtpPacketizer {
        /**
         * Holds the synchronization source of the outbound video stream, the deterministic primary SSRC of
         * the first video simulcast layer the peer pre registers a receive context for.
         */
        private final int ssrc;

        /**
         * Supplies the live video target bitrate, in bits per second, stamped as the id13 keyframe descriptor's
         * bitrate field on every keyframe after the first.
         */
        private final IntSupplier targetBitrate;

        /**
         * Supplies the live combined sender bandwidth estimate, in bits per second, stamped as the id13 keyframe
         * descriptor's estimate field on every keyframe after the first.
         */
        private final IntSupplier bandwidthEstimate;

        /**
         * Holds the next sixteen bit RTP sequence number to stamp.
         */
        private int sequence;

        /**
         * Holds the number of id13 keyframe descriptors stamped so far, distinguishing the call's first keyframe
         * (which carries a zero bitrate and a zero state byte) from later ones.
         */
        private int keyframeDescriptorsSent;

        /**
         * Holds the sixteen bit picture number stamped in the id3 frame marking header extension element on the
         * first packet of each access unit.
         *
         * <p>WhatsApp widens the id3 element to three bytes on a frame's first packet, the trailing two bytes
         * carrying a picture counter that advances by one per access unit; this field holds the next value.
         */
        private int frameNumber;

        /**
         * Holds the rolling sixteen bit transport send tag stamped in the id9 header extension element on every
         * packet.
         *
         * <p>WhatsApp stamps a per packet sixteen bit value in the id9 element; this counter advances by one per
         * packet to supply it.
         */
        private int extensionSendTag;

        /**
         * Constructs a video packetizer stamping the given video SSRC with a zeroed sequence.
         *
         * <p>The SSRC is the self device's deterministic first simulcast layer primary video SSRC
         * ({@link CallSecureSsrcGenerator#videoTriple(String, Jid, int)} stream {@code 0},
         * media type {@code 2}), the value the peer pre registers a receive context for, never random.
         *
         * @param ssrc              the outbound video stream SSRC
         * @param targetBitrate     supplies the live video target bitrate for the id13 keyframe descriptor
         * @param bandwidthEstimate supplies the live bandwidth estimate for the id13 keyframe descriptor
         */
        private OutboundVideoRtpPacketizer(int ssrc, IntSupplier targetBitrate, IntSupplier bandwidthEstimate) {
            this.ssrc = ssrc;
            this.targetBitrate = targetBitrate;
            this.bandwidthEstimate = bandwidthEstimate;
            this.sequence = 0;
            this.frameNumber = 0;
            this.extensionSendTag = 0;
            this.keyframeDescriptorsSent = 0;
        }

        /**
         * Packetizes one RTP payload of an access unit into a video RTP packet buffer with trailing SRTP tag
         * room, stamping the marker bit on the last packet of the picture.
         *
         * <p>Builds the twelve byte fixed header (version two, no padding, the extension bit set, the marker
         * bit set only when {@code marker} is set, the dynamic video payload type, the running sequence, the
         * ninety kilohertz timestamp derived from the access unit's microsecond presentation timestamp shared
         * across the picture's packets, and the stream SSRC), then the populated {@code 0xBEDE} one byte header
         * extension, then the payload, leaving sixty four bytes of trailing room for the transport's
         * hop by hop SRTP tag. The sequence advances by one and every packet of one access unit carries the
         * same timestamp so the depacketizer groups them.
         *
         * <p>The header extension carries elements in ascending id order. Every packet carries id3 (frame flags,
         * one byte, widened to three bytes carrying the sixteen bit picture number on a frame's first packet),
         * id6 (transmission offset), and id9 (transport send tag). A keyframe packet additionally carries id5 (a
         * per stream config tag), and a keyframe's first packet additionally carries id13 (the extended
         * descriptor holding the target bitrate). The extension is two words for an inter frame packet, three for
         * a frame start or a non first keyframe packet, and six for a keyframe's first packet.
         *
         * @implNote The extension is reconstructed from byte level captures of real WhatsApp 1:1 video calls:
         * every video packet carries the populated {@code 0xBEDE} extension, never the empty {@code de be 00 00}
         * word, and the relay's forwarding gate requires the extension bit and {@code de be} profile. The profile
         * word is written {@code DE BE} (the byte swapped {@code 0xBEDE}) to match WhatsApp's little endian store.
         * id3 flags are stamped {@code 0x00}, id6 zero, and id9 advances per packet. id5 is stamped
         * {@code 0x2a00}, a fixed value in the captured per stream {@code 0x2900}-{@code 0x2c00} range the
         * receiver reads opaquely. id13 is {@code [0x09][u24 target-bitrate][u32 bandwidth-estimate][state]}: the
         * call's first keyframe carries zeros and a {@code 0x00} state byte, every later keyframe the live video
         * target bitrate and combined bandwidth estimate (both read per keyframe from the rate control loop) and
         * a {@code 0x01} state byte. The {@code u24} bitrate matches the captured BWE driven ramp; the {@code u32}
         * estimate matches the captured available bandwidth field (a low motion sender's climbs well above its
         * send bitrate).
         *
         * @param payload    the RTP payload bytes to wrap (a single NAL, a {@code STAP-A} aggregate, or one {@code FU-A}
         *                   fragment of the access unit)
         * @param ptsMicros  the access unit's presentation timestamp in microseconds, mapped to the
         *                   ninety kilohertz RTP clock
         * @param marker     whether to set the RTP marker bit, marking the last packet of the access unit
         * @param frameStart whether this is the first packet of the access unit, which widens the id3 element to
         *                   carry the advancing picture number
         * @param keyframe   whether the access unit is an intra picture, which adds the id5 element to every
         *                   packet and the id13 descriptor to the first
         * @return the RTP packet buffer, sized for the header, the header extension, the payload, and the SRTP
         *         tag; the wire length is {@code packet.length} less the trailing sixty four byte SRTP room
         */
        private byte[] packetize(byte[] payload, long ptsMicros, boolean marker, boolean frameStart, boolean keyframe) {
            var timestamp = ptsMicros * VIDEO_RTP_CLOCK_RATE / 1_000_000L;
            var descriptor = keyframe && frameStart;
            var words = descriptor ? 6 : ((keyframe || frameStart) ? 3 : 2);
            var extensionLength = RTP_HEADER_EXTENSION_LENGTH + words * 4;
            if (frameStart) {
                frameNumber = (frameNumber + 1) & 0xFFFF;
            }
            var sendTag = extensionSendTag = (extensionSendTag + 1) & 0xFFFF;
            var packet = new byte[RTP_HEADER_LENGTH + extensionLength + payload.length + 64];
            packet[0] = (byte) 0x90;
            packet[1] = (byte) ((marker ? 0x80 : 0x00) | (VIDEO_PAYLOAD_TYPE & 0x7F));
            packet[2] = (byte) ((sequence >>> 8) & 0xFF);
            packet[3] = (byte) (sequence & 0xFF);
            DataUtils.putInt(packet, 4, (int) timestamp, ByteOrder.BIG_ENDIAN);
            DataUtils.putInt(packet, 8, ssrc, ByteOrder.BIG_ENDIAN);
            packet[12] = (byte) 0xDE;
            packet[13] = (byte) 0xBE;
            packet[14] = (byte) ((words >>> 8) & 0xFF);
            packet[15] = (byte) (words & 0xFF);
            var cursor = 16;
            if (frameStart) {
                packet[cursor++] = (byte) 0x32;
                packet[cursor++] = 0x00;
                packet[cursor++] = (byte) ((frameNumber >>> 8) & 0xFF);
                packet[cursor++] = (byte) (frameNumber & 0xFF);
            } else {
                packet[cursor++] = (byte) 0x30;
                packet[cursor++] = 0x00;
            }
            if (keyframe) {
                packet[cursor++] = (byte) 0x51;
                packet[cursor++] = (byte) 0x2A;
                packet[cursor++] = 0x00;
            }
            packet[cursor++] = (byte) 0x61;
            packet[cursor++] = 0x00;
            packet[cursor++] = 0x00;
            packet[cursor++] = (byte) 0x91;
            packet[cursor++] = (byte) ((sendTag >>> 8) & 0xFF);
            packet[cursor++] = (byte) (sendTag & 0xFF);
            if (descriptor) {
                var first = keyframeDescriptorsSent == 0;
                keyframeDescriptorsSent++;
                var rate = first ? 0 : targetBitrate.getAsInt();
                var estimate = first ? 0 : bandwidthEstimate.getAsInt();
                packet[cursor++] = (byte) 0xD8;
                packet[cursor++] = 0x09;
                packet[cursor++] = (byte) ((rate >>> 16) & 0xFF);
                packet[cursor++] = (byte) ((rate >>> 8) & 0xFF);
                packet[cursor++] = (byte) (rate & 0xFF);
                packet[cursor++] = (byte) ((estimate >>> 24) & 0xFF);
                packet[cursor++] = (byte) ((estimate >>> 16) & 0xFF);
                packet[cursor++] = (byte) ((estimate >>> 8) & 0xFF);
                packet[cursor++] = (byte) (estimate & 0xFF);
                packet[cursor] = (byte) (first ? 0x00 : 0x01);
            }
            System.arraycopy(payload, 0, packet, RTP_HEADER_LENGTH + extensionLength, payload.length);
            sequence = (sequence + 1) & 0xFFFF;
            return packet;
        }
    }

    /**
     * The inbound media RTP demultiplexer: routes each cleartext media packet by its RTP payload type to
     * the audio jitter buffer or the video jitter buffer.
     *
     * <p>The transport's media sink hands this the hop by hop decrypted RTP bytes (carried as SCTP DATA on
     * the data channel and decrypted by the transport); the payload type selects the route. An audio packet
     * has its header stripped, its body SFrame opened on a group call, and the recovered codec packet
     * inserted into NetEq for the writer pump to render. Any other payload type is treated as video and
     * inserted into the video jitter buffer when one is up. A malformed packet, or one whose SFrame opening
     * fails, is dropped. Application data does not ride RTP on the web transport: it is carried as its own
     * SCTP DATA messages and demultiplexed by the transport, so it does not reach this media demux.
     */
    private static final class InboundMediaDemux {
        /**
         * Holds the NetEq jitter buffer audio packets are inserted into.
         */
        private final LiveNetEq netEq;

        /**
         * Holds the live reference to the call's video pipeline inbound video packets are inserted into,
         * holding {@code null} until the call carries video.
         *
         * <p>This is the session's single video pipeline holder, shared rather than snapshotted, so the
         * receive thread reads the pipeline a mid call audio to video upgrade publishes rather than the
         * {@code null} captured when an audio only call was brought up. It is read on every inbound media
         * packet through {@link AtomicReference#get()}, whose volatile semantics safely publish the upgrade's
         * pipeline to this thread.
         */
        private final AtomicReference<VideoPipeline> videoPipeline;

        /**
         * Holds the group SFrame chain that opens an inbound payload, or {@code null} on a one to one
         * call.
         */
        private final SFrameKeyProvider sframeProvider;

        /**
         * Holds the rate control loop each inbound media packet's arrival timing is teed into for the GoogCC
         * delay based estimate.
         */
        private final RateControlLoop rateControlLoop;

        /**
         * Reassembles inbound video RTP payloads into Annex B access units across single NAL, {@code STAP-A}, and {@code FU-A}
         * forms before they are inserted into the video jitter buffer.
         *
         * <p>Owned by the demux as per stream reassembly state and driven only on the single transport receive
         * thread, so it needs no synchronization. A one to one or audio only call never feeds it; a video call
         * routes every video packet through it so a picture fragmented across RTP packets is reassembled rather
         * than truncated to its first packet.
         */
        private final InboundH264Depacketizer videoDepacketizer;

        /**
         * Holds the callback that ships a generic NACK for a remote audio stream's lost sequence numbers, or
         * {@code null} until the transport is built and installs it.
         *
         * <p>Installed by the assembly through {@link #nackEmitter(BiConsumer)} once the transport exists,
         * since the demux is built first; the receive thread reads it after each audio or video insert, so it
         * is {@code volatile}. The first argument is the remote stream SSRC, the second the due sequence
         * numbers.
         */
        private volatile BiConsumer<Integer, List<Integer>> nackEmitter;

        /**
         * Holds the per remote video SSRC loss detectors that decide which inbound video sequence numbers to
         * request a retransmission for.
         *
         * <p>Keyed by the remote video synchronization source, with one {@link VideoNackTracker} created
         * lazily on the first packet of each source. The map and every tracker in it are touched only on the
         * single transport receive thread that drives {@link #acceptVideo(VideoPipeline, byte[])}, so a plain
         * {@link HashMap} needs no synchronization.
         */
        private final Map<Integer, VideoNackTracker> videoNackTrackers = new HashMap<>();

        /**
         * Holds the latest path round trip time estimate in milliseconds the video NACK scheduler spaces its
         * re NACK requests against, or {@code 0} before the first inbound RTCP feedback.
         *
         * <p>Written by the RTCP feedback handler through {@link #updateVideoRttMillis(long)} and read on the
         * receive thread by {@link #acceptVideo(VideoPipeline, byte[])}, so it is {@code volatile}; the value
         * is a lock free hand off, the same round trip estimate the audio jitter buffer is fed.
         */
        private volatile long videoRttMillis;

        /**
         * Constructs a demultiplexer over the audio jitter buffer, the optional video pipeline, the optional
         * SFrame chain, and the rate control loop.
         *
         * @param netEq           the NetEq jitter buffer audio packets are inserted into
         * @param videoPipeline   the live video pipeline holder video packets are inserted into, holding
         *                        {@code null} until the call carries video
         * @param sframeProvider  the group SFrame chain, or {@code null} on a one to one call
         * @param rateControlLoop the rate control loop each media packet's arrival timing is teed into
         */
        private InboundMediaDemux(LiveNetEq netEq, AtomicReference<VideoPipeline> videoPipeline,
                                  SFrameKeyProvider sframeProvider,
                                  RateControlLoop rateControlLoop) {
            this.netEq = netEq;
            this.videoPipeline = videoPipeline;
            this.sframeProvider = sframeProvider;
            this.rateControlLoop = rateControlLoop;
            this.videoDepacketizer = new InboundH264Depacketizer();
        }

        /**
         * Installs the callback that ships a generic NACK for a remote audio or video stream's lost sequence
         * numbers.
         *
         * <p>The assembly calls this once the transport exists, since the demux is constructed first; the
         * receive thread reads it after each audio or video insert. Passing {@code null} disables NACK
         * emission.
         *
         * @param nackEmitter the NACK callback, taking the remote stream SSRC and the due sequence numbers,
         *                    or {@code null} to disable
         */
        private void nackEmitter(BiConsumer<Integer, List<Integer>> nackEmitter) {
            this.nackEmitter = nackEmitter;
        }

        /**
         * Updates the path round trip time estimate the video NACK scheduler spaces its re NACK requests
         * against.
         *
         * <p>Called from the inbound RTCP feedback handler whenever a feedback report carries a round trip
         * sample, the same estimate the audio jitter buffer is fed. A negative estimate is floored to zero.
         * The value is read on the receive thread by {@link #acceptVideo(VideoPipeline, byte[])}; the write
         * is a lock free volatile hand off.
         *
         * @param rttMillis the path round trip time estimate, in milliseconds
         */
        private void updateVideoRttMillis(long rttMillis) {
            this.videoRttMillis = Math.max(0, rttMillis);
        }

        /**
         * Accepts one inbound cleartext media RTP packet from the transport media sink and routes it by its
         * RTP payload type.
         *
         * <p>This is the {@link Consumer Consumer&lt;byte[]&gt;} the {@link LiveRelayTransport} media sink
         * invokes for every hop by hop decrypted media packet. A packet shorter than the twelve byte RTP
         * header is dropped; otherwise the payload type selects the route: the audio payload type goes to the
         * audio pipeline and any other goes to the video pipeline when one is up (falling back to the audio
         * pipeline before a video pipeline exists). Each media packet's send and arrival timing is teed into
         * the rate control loop's GoogCC delay based estimator before it is decoded.
         *
         * @param rtpPacket the inbound cleartext RTP packet bytes
         */
        private void onTransportMedia(byte[] rtpPacket) {
            if (rtpPacket.length < RTP_HEADER_LENGTH) {
                return;
            }
            var payloadType = rtpPacket[1] & 0x7F;
            // Snapshot the live video pipeline once for this packet so the null check and the insert see the
            // same reference even if a mid call upgrade publishes the pipeline between them.
            var pipeline = videoPipeline.get();
            if (payloadType == AUDIO_PAYLOAD_TYPE) {
                teeArrivalTiming(rtpPacket, AUDIO_RTP_CLOCK_RATE);
                acceptAudio(rtpPacket);
            } else if (pipeline == null) {
                teeArrivalTiming(rtpPacket, AUDIO_RTP_CLOCK_RATE);
                acceptAudio(rtpPacket);
            } else {
                teeArrivalTiming(rtpPacket, VIDEO_RTP_CLOCK_RATE);
                acceptVideo(pipeline, rtpPacket);
            }
        }

        /**
         * Tees one inbound media packet's send and arrival timing into the rate control loop's GoogCC
         * delay based estimator.
         *
         * <p>The packet's RTP timestamp (in {@code clockRateHz} units) is converted to a millisecond send
         * time, the local arrival time is taken from {@link System#nanoTime()} in milliseconds, and the media
         * payload size (the packet length past the RTP header and any header extension) is the byte count; the
         * estimator groups packets by send burst and fits its over use trendline from these arrival deltas. A
         * failed tee is swallowed so an estimator hiccup never disturbs the decode path.
         *
         * @implNote This implementation feeds the estimator by converting the wire RTP timestamp to
         * milliseconds with {@code rtpTimestamp * 1000 / clockRateHz}, the standard RTP to wall clock scaling,
         * rather than carrying the abstime header extension send time the engine prefers when present: that
         * extension's negotiated id is unknown (the same extmap id gap the audio level extension carries on
         * the send side), so the media RTP timestamp is the available send time proxy. The arrival clock is
         * {@code System.nanoTime()} reduced to milliseconds, the same monotonic source the rest of the media
         * plane stamps arrivals with.
         *
         * @param rtpPacket   the inbound cleartext media RTP packet bytes
         * @param clockRateHz the stream's RTP timestamp clock rate, in hertz
         */
        private void teeArrivalTiming(byte[] rtpPacket, int clockRateHz) {
            var rtpTimestamp = DataUtils.getInt(rtpPacket, 4, ByteOrder.BIG_ENDIAN) & 0xFFFFFFFFL;
            var sendTimeMs = rtpTimestamp * 1000L / clockRateHz;
            var arrivalMs = System.nanoTime() / 1_000_000L;
            var payloadBytes = rtpPacket.length - rtpPayloadOffset(rtpPacket);
            try {
                rateControlLoop.onPacketReceived(sendTimeMs, arrivalMs, payloadBytes);
            } catch (RuntimeException exception) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "calls inbound rate-control tee failed", exception);
            }
        }

        /**
         * Returns the byte offset of the RTP payload, past the fixed header, any CSRC list, and the
         * {@code 0xBEDE} one byte header extension when the extension bit is set.
         *
         * <p>Every relayed WhatsApp media packet sets the extension bit and carries a {@code de be} header
         * extension ahead of the codec payload (the same one the outbound packetizers stamp), so stripping a
         * fixed twelve bytes would feed the extension bytes into the audio or video depacketizer. This reads
         * the extension's two byte word count and skips the whole block, and also skips any CSRC list, so the
         * returned offset points at the codec payload. A packet with the extension bit clear, or one too short
         * to hold the declared extension, yields the bare header length.
         *
         * @param rtpPacket the inbound RTP packet bytes
         * @return the offset of the codec payload past the header and any header extension
         */
        private static int rtpPayloadOffset(byte[] rtpPacket) {
            var offset = RTP_HEADER_LENGTH + (rtpPacket[0] & 0x0F) * 4;
            if ((rtpPacket[0] & 0x10) != 0 && rtpPacket.length >= offset + 4) {
                var extensionWords = ((rtpPacket[offset + 2] & 0xFF) << 8) | (rtpPacket[offset + 3] & 0xFF);
                offset += 4 + extensionWords * 4;
            }
            return Math.min(offset, rtpPacket.length);
        }

        /**
         * Strips the RTP header, opens the SFrame payload on a group call, and inserts the audio codec
         * packet into NetEq.
         *
         * <p>On a group call the body is SFrame opened under the chain's current cipher; an opening failure
         * drops the packet. The recovered codec packet is inserted into NetEq keyed by the wire RTP sequence
         * and timestamp.
         *
         * @param rtpPacket the inbound cleartext audio RTP packet bytes
         */
        private void acceptAudio(byte[] rtpPacket) {
            var sequence = ((rtpPacket[2] & 0xFF) << 8) | (rtpPacket[3] & 0xFF);
            var timestamp = DataUtils.getInt(rtpPacket, 4, ByteOrder.BIG_ENDIAN) & 0xFFFFFFFFL;
            var ssrc = DataUtils.getInt(rtpPacket, 8, ByteOrder.BIG_ENDIAN);
            // TODO: eliminate this body copy by threading (rtpPacket, offset, length) into openSframe and
            //  NetEq.insert. Unlike the video depacketizer (which is in this file), NetEq.insert is in the dsp
            //  package and retains the payload in its jitter buffer as a standalone byte[], and openSframe
            //  returns either its input unchanged (1:1) or a freshly decrypted buffer; adding a slice overload
            //  to NetEq is outside this cluster's two file scope, so the exact length copy stays for now.
            var body = Arrays.copyOfRange(rtpPacket, rtpPayloadOffset(rtpPacket), rtpPacket.length);
            var payload = openSframe(body);
            if (payload == null) {
                return;
            }
            try {
                netEq.insert(sequence, timestamp, payload);
            } catch (RuntimeException exception) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "calls inbound audio insert failed", exception);
                return;
            }
            // After inserting, request retransmission of any audio sequence gap the NACK scheduler now judges
            // due (the same loss list the jitter buffer would otherwise only conceal); the scheduler's re NACK
            // gate paces repeats, so this fires only on a confirmed gap. The insert stamps arrival with the
            // wall clock, so the due time query uses the same clock.
            var emitter = nackEmitter;
            if (emitter != null) {
                var due = netEq.pendingNackList(System.currentTimeMillis());
                if (!due.isEmpty()) {
                    emitter.accept(ssrc, due);
                }
            }
        }

        /**
         * Strips the RTP header, reassembles the access unit across its RTP packets, opens the SFrame layer on
         * a group call, and inserts a completed picture into the video jitter buffer.
         *
         * <p>The packet's sequence and timestamp order the frame in the jitter buffer; the marker bit (the
         * high bit of the second header byte) flags the last packet of a picture. The cleartext payload is fed
         * to {@link #videoDepacketizer}, which recovers each NAL from its single NAL, {@code STAP-A}, or {@code FU-A} form and
         * concatenates the NAL units of one access unit into an Annex B picture, completed on the marker bit;
         * the completed picture is then SFrame opened (a pass through on the relayed path). A packet that does
         * not close an access unit (a non final fragment) inserts nothing; the completed picture is inserted as
         * one {@link EncodedVideoFrame}, flagged a key frame when it carries an instantaneous decoder refresh or
         * parameter set NAL.
         *
         * <p>Every packet's sequence number is fed to the remote source's {@link VideoNackTracker} before
         * reassembly, since gap detection runs per RTP packet rather than per completed picture; a confirmed
         * gap past the reorder window ships a generic NACK through the shared {@link #nackEmitter}, the same
         * seam the audio path uses. A completed key frame is a refresh point, so the tracker is cleared on one
         * and no retransmission is requested across the reset.
         *
         * @implNote This implementation feeds the reassembly through {@link InboundH264Depacketizer} so a
         * picture fragmented across RTP packets is reassembled rather than truncated. The decoder geometry
         * stays the negotiated default ({@link #VIDEO_WIDTH} by {@link #VIDEO_HEIGHT}) rather than the inbound
         * resolution: the per stream resolution is carried in the bitstream the decoder re parses, and the SPS
         * derived inbound resolution is not threaded onto the {@link EncodedVideoFrame} here. The key frame
         * flag is derived from the reassembled NAL types ({@code 5} IDR, {@code 7} SPS, {@code 8} PPS),
         * matching the classification {@link EncodedVideoFrame#keyFrame()} records on the encode side.
         *
         * @param pipeline  the live video pipeline the recovered access unit is inserted into; never
         *                  {@code null}, snapshotted by {@link #onTransportMedia(byte[])}
         * @param rtpPacket the inbound cleartext video RTP packet bytes
         */
        private void acceptVideo(VideoPipeline pipeline, byte[] rtpPacket) {
            var sequence = ((rtpPacket[2] & 0xFF) << 8) | (rtpPacket[3] & 0xFF);
            var marker = (rtpPacket[1] & 0x80) != 0;
            var timestamp = DataUtils.getInt(rtpPacket, 4, ByteOrder.BIG_ENDIAN) & 0xFFFFFFFFL;
            var ssrc = DataUtils.getInt(rtpPacket, 8, ByteOrder.BIG_ENDIAN);
            // Gap detection runs per RTP packet, so feed every packet's sequence to this source's tracker
            // before reassembly (a fragment that never completes a picture still advances the sequence and so
            // still reveals a gap). The tracker map is touched only here, on the single transport receive
            // thread.
            var nowMillis = System.currentTimeMillis();
            var tracker = videoNackTrackers.computeIfAbsent(ssrc, _ -> new VideoNackTracker());
            tracker.recordReceived(sequence);
            // Feed the payload to the depacketizer as an in place slice of rtpPacket rather than a fresh
            // copy: the depacketizer reads only [payloadOffset, rtpPacket.length) and copies out every byte
            // it retains (each NAL is materialized by copyOfRange/arraycopy inside accept before it returns),
            // so no copy of the payload itself is needed here.
            var payloadOffset = rtpPayloadOffset(rtpPacket);
            var payloadLength = rtpPacket.length - payloadOffset;
            // Reassemble the RTP packets into the Annex B access unit first, then run the (currently pass
            // through) SFrame open once per completed picture. openSframe returns its input unchanged on the
            // relayed path, where the SFrame per frame transform is not engaged, so this order has no
            // behavioural effect today; the exact layering of the SFrame frame relative to the H264 RTP NAL
            // packetization on a topology that does engage SFrame is part of the same unresolved SFrame wire
            // layout the open awaits (see openSframe), so the open is applied per picture as the least
            // surprising placeholder rather than per fragment.
            var keyFrame = false;
            var accessUnit = videoDepacketizer.accept(rtpPacket, payloadOffset, payloadLength, timestamp, marker);
            if (accessUnit != null) {
                accessUnit = openSframe(accessUnit);
                if (accessUnit != null) {
                    keyFrame = videoDepacketizer.wasKeyFrame();
                    var frame = new EncodedVideoFrame(accessUnit, VideoDecoderCapability.H264, keyFrame,
                            VIDEO_WIDTH, VIDEO_HEIGHT, timestamp);
                    pipeline.insert(frame, System.nanoTime() / 1_000_000L, timestamp, sequence);
                }
            }
            // A key frame is a decoder refresh point: clear the tracker so no retransmission is requested
            // across the reset, since the packets before it are no longer needed to decode.
            if (keyFrame) {
                tracker.reset();
                return;
            }
            // After tracking, request retransmission of any video sequence gap the scheduler now judges due
            // past the reorder window; the re NACK gate paces repeats, so this fires only on a confirmed gap.
            var emitter = nackEmitter;
            if (emitter != null) {
                var due = tracker.nackList(nowMillis, videoRttMillis);
                if (!due.isEmpty()) {
                    emitter.accept(ssrc, due);
                }
            }
        }

        /**
         * Opens a group call SFrame body, or returns the body unchanged on a one to one call.
         *
         * <p>A one to one call carries no SFrame layer (the relay hop by hop SRTP is the only transport
         * crypto), so the body is the codec payload directly. A group call opens the body under the chain's
         * cipher; an absent chain key or a tag that does not verify yields {@code null} so the packet is
         * dropped.
         *
         * @param body the RTP payload body
         * @return the recovered codec payload, or {@code null} when SFrame opening fails
         */
        private byte[] openSframe(byte[] body) {
            if (sframeProvider == null) {
                return body;
            }
            // TODO: open the end to end SFrame frame on a group call. The integration once the layout is
            //  recovered is to resolve the sender device JID from the inbound RTP SSRC, look up that peer's
            //  SFrameKeyProvider from CallMembership.sframeProvidersByDevice() (the per peer provider seam the
            //  crypto core exposes; the membership derives every participant's chain key from the installed
            //  call key), wrap it in an SFrameSecureFrame, and return secureFrame.open(body), which reads the
            //  trailer length from the final byte, decodes the LEB128 key id and counter, resolves the cipher,
            //  enforces the replay window, and verifies the tag before {@code AES-CTR} decrypting. The transform and
            //  trailer codec already exist (com.github.auties00.cobalt.calls.media.sframe.SFrameSecureFrame),
            //  but two pieces remain unresolved: the per key id 12 byte counter mask salt value and the chain
            //  key to cipher material derivation (ratchet versus expand and the HKDF info label), both native
            //  callbacks. WhatsApp does not invoke the SFrame transform on the relayed media path at all: in
            //  SFU group call mode the per frame SFrame transform is not engaged (the per participant keys are
            //  derived and rotated, but the media rides the relay hop by hop SRTP layer), and a 1:1 call uses
            //  no SFrame. So passing the body through is the faithful behaviour here; wiring an unverified open
            //  under a placeholder derivation would diverge from WhatsApp and would drop every real peer's
            //  frame as a tag mismatch. A non SFU or mesh topology that installs the frame handler, or a native
            //  layer capture, is required before the open can be wired.
            return body;
        }
    }

    /**
     * The bridge presenting a platform capture device's pushed sample blocks as the reader pump's
     * {@link AudioOutput} source.
     *
     * <p>A platform capture driver pushes fixed size {@code short[]} blocks to a sink as they are captured,
     * while the {@link AudioReaderPump} pulls {@link AudioFrame}s from an {@link AudioOutput}. This bridge
     * adapts the push to the pull through a small bounded queue: the driver {@linkplain #offer(short[])
     * offers} each captured block, and the reader pump {@linkplain #takeAudio() takes} it as a frame, blocking
     * until one is available. The queue prefers freshness, dropping the oldest block when full so capture
     * never blocks on a slow drain.
     *
     * @implNote This implementation is the device side analogue of the application {@link AudioOutput}
     * embedder source: the native driver manager routes captured blocks straight into the engine, which
     * Cobalt models as this device bridge feeding the reader pump, keeping the application {@link AudioOutput}
     * a separate embedder seam for a programmatic source.
     */
    private static final class CaptureSourceBridge implements AudioOutput {
        /**
         * Holds the bounded queue of captured blocks the driver offers and the reader pump takes.
         */
        private final BlockingQueue<short[]> queue;

        /**
         * Tracks whether the bridge has been shut down, so a pending take returns {@code null}.
         */
        private final AtomicBoolean shutdown;

        /**
         * Constructs a capture source bridge with a small freshness preserving queue.
         */
        private CaptureSourceBridge() {
            this.queue = new ArrayBlockingQueue<>(8);
            this.shutdown = new AtomicBoolean();
        }

        /**
         * Offers one captured block from the device driver, dropping the oldest on a full queue.
         *
         * <p>Copies the driver's reused block (the driver overwrites it after the call) and enqueues the
         * copy; when the queue is full the oldest block is discarded so capture never blocks on a slow
         * reader pump, keeping playout latency bounded.
         *
         * @param samples the captured block; copied before enqueueing
         */
        private void offer(short[] samples) {
            if (shutdown.get()) {
                return;
            }
            var copy = samples.clone();
            if (!queue.offer(copy)) {
                queue.poll();
                queue.offer(copy);
            }
        }

        @Override
        public void writeAudio(AudioFrame frame) {
            Objects.requireNonNull(frame, "frame cannot be null");
            offer(frame.pcm());
        }

        @Override
        public AudioFrame takeAudio() throws InterruptedException {
            if (shutdown.get()) {
                return null;
            }
            var block = queue.poll(AUDIO_FRAME_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (block == null) {
                return shutdown.get() ? null : new AudioFrame(new short[0], 0L);
            }
            return new AudioFrame(block, 0L);
        }

        @Override
        public void shutdown() {
            shutdown.set(true);
            queue.clear();
        }

        @Override
        public boolean isLiveCapture() {
            return true;
        }
    }

    /**
     * The bridge presenting a platform video capture driver's forwarded frames as the video pipeline's
     * {@link VideoOutput} capture source.
     *
     * <p>When a video call supplies no application {@link VideoOutput}, the engine sources outbound video
     * from a platform camera through the {@link VoipDriverManager}: the manager's
     * {@link com.github.auties00.cobalt.calls.platform.video.VideoCaptureDriver} pumps captured frames to a
     * {@link VideoSink}, while the {@link VideoPipeline} encode loop pulls frames from a {@link VideoOutput}.
     * This bridge is both: it is the {@link VideoSink} the driver forwards each captured frame to through
     * {@link #accept(VideoFrame)}, and the {@link VideoOutput} the encode loop drains through
     * {@link #takeVideo()}. It adapts the driver's push to the encoder's pull through a small freshness preferring
     * queue, dropping the oldest frame when full so capture never blocks on a slow encoder, the same policy
     * the audio {@link CaptureSourceBridge} uses. The advertised {@link #width()} by {@link #height()} at
     * {@link #fps()} sizes the encoder to the negotiated capture geometry; {@link #writeVideo(VideoFrame)} is the
     * unused application facing face, since this is a device backed source the driver fills.
     *
     * @implNote This implementation is the video analogue of {@link CaptureSourceBridge}: the native driver
     * manager routes the camera driver's frames straight into the encoder, which Cobalt models as this bridge
     * feeding the encode loop, keeping the application {@link VideoOutput} a separate embedder seam for a
     * programmatic source. The driver's frame size change drop already runs in the driver, so this bridge
     * passes each forwarded frame through unfiltered.
     */
    private static final class ManagerVideoSourceBridge implements VideoOutput, VideoSink {
        /**
         * The result code returned to the driver when a forwarded frame is queued, matching the engine
         * convention where the video sink returns {@code 0} on success.
         */
        private static final int SINK_OK = 0;

        /**
         * The interval, in milliseconds, {@link #takeVideo()} waits per poll so a {@link #shutdown()} or an
         * {@link #onInterrupted()} unblocks a pending take within one interval rather than blocking forever
         * on a source that stopped feeding the queue.
         */
        private static final long INTERRUPT_POLL_MILLIS = 200;

        /**
         * Holds the bounded queue of forwarded frames the driver offers and the encode loop takes.
         */
        private final BlockingQueue<VideoFrame> queue;

        /**
         * Tracks whether the bridge has been shut down, so a pending take returns {@code null} and further
         * forwarded frames are dropped.
         */
        private final AtomicBoolean shutdown;

        /**
         * The advertised capture width in pixels the encoder is sized to.
         */
        private final int width;

        /**
         * The advertised capture height in pixels the encoder is sized to.
         */
        private final int height;

        /**
         * The advertised capture frame rate the encoder is paced at.
         */
        private final int fps;

        /**
         * The advertised target encoder bitrate in bits per second.
         */
        private final int bitrateBps;

        /**
         * Runs once when the capture driver reports its device was revoked, invoked from
         * {@link #onInterrupted()} so the media plane can drive the local video to paused and notify the peer.
         */
        private final Runnable interruptedCallback;

        /**
         * Holds the silence companion serving this device video source's inherited audio face, since a camera
         * capture source carries no audio and the engine sources the call's audio from a separate capture; the
         * inherited {@link AudioOutput} methods delegate to it.
         */
        private final AudioOutput audio = AudioOutput.fromSilence();

        /**
         * Constructs a video source bridge advertising the given capture geometry with a small
         * freshness preferring queue.
         *
         * @param width               the advertised capture width in pixels
         * @param height              the advertised capture height in pixels
         * @param fps                 the advertised capture frame rate
         * @param bitrateBps          the advertised target encoder bitrate in bits per second
         * @param interruptedCallback runs once when the capture driver reports its device was revoked
         */
        private ManagerVideoSourceBridge(int width, int height, int fps, int bitrateBps,
                                         Runnable interruptedCallback) {
            this.queue = new ArrayBlockingQueue<>(8);
            this.shutdown = new AtomicBoolean();
            this.width = width;
            this.height = height;
            this.fps = fps;
            this.bitrateBps = bitrateBps;
            this.interruptedCallback = Objects.requireNonNull(interruptedCallback, "interruptedCallback cannot be null");
        }

        /**
         * {@inheritDoc}
         *
         * @implNote This implementation copies the frame's pixels into a fresh buffer before enqueueing it,
         * because the queue retains frames across several {@link VideoOutput#takeVideo()} calls while a
         * device backed source may lend and immediately refill one pixel buffer per the
         * {@link VideoOutput#takeVideo()} borrow contract; queuing the borrowed frame directly would alias every
         * queued frame onto the source's latest picture.
         */
        @Override
        public int accept(VideoFrame frame) {
            Objects.requireNonNull(frame, "frame cannot be null");
            if (shutdown.get()) {
                return SINK_OK;
            }
            var owned = new VideoFrame(frame.pixels().clone(), frame.format(),
                    frame.width(), frame.height(), frame.ptsMicros());
            if (!queue.offer(owned)) {
                queue.poll();
                queue.offer(owned);
            }
            return SINK_OK;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote This implementation ends the bridge as its own {@linkplain #shutdown() source teardown}
         * so the encode loop's {@link #takeVideo()} unblocks and returns {@code null}, stopping the outbound
         * video pipeline cleanly rather than hanging on a source that stopped feeding; it then runs the
         * {@link #interruptedCallback} so the media plane drives the local video to paused and notifies the
         * peer.
         */
        @Override
        public void onInterrupted() {
            shutdown();
            interruptedCallback.run();
        }

        @Override
        public void writeVideo(VideoFrame frame) {
            Objects.requireNonNull(frame, "frame cannot be null");
            accept(frame);
        }

        @Override
        public VideoFrame takeVideo() throws InterruptedException {
            while (!shutdown.get()) {
                var frame = queue.poll(INTERRUPT_POLL_MILLIS, TimeUnit.MILLISECONDS);
                if (frame != null) {
                    return frame;
                }
            }
            return null;
        }

        @Override
        public void writeAudio(AudioFrame frame) throws InterruptedException {
            audio.writeAudio(frame);
        }

        @Override
        public AudioFrame takeAudio() throws InterruptedException {
            return audio.takeAudio();
        }

        @Override
        public boolean isLiveCapture() {
            return audio.isLiveCapture();
        }

        @Override
        public void shutdown() {
            shutdown.set(true);
            queue.clear();
            audio.shutdown();
        }

        @Override
        public int width() {
            return width;
        }

        @Override
        public int height() {
            return height;
        }

        @Override
        public int fps() {
            return fps;
        }

        @Override
        public int bitrateBps() {
            return bitrateBps;
        }
    }

    /**
     * The video pipeline for a video call: a two way encode send and jitter buffered decode render path over
     * the negotiated video codec.
     *
     * <p>The pipeline holds the call's {@link VideoCodec} and its inbound {@link VideoJitterBuffer}. The
     * inbound half runs a render thread that polls the jitter buffer once per tick, decodes each released
     * access unit, and delivers the decoded picture to the call's
     * {@linkplain MediaStreams#videoPlayback() application video sink} when one was supplied, falling
     * back to {@link VoipHostApi#renderVideoFrame} otherwise. The outbound half, present only when the call
     * supplied a {@linkplain MediaStreams#videoCapture() video capture source}, runs an encode thread
     * that pulls each raw picture from the source, encodes it with the same codec, and ships the access unit
     * through the transport video RTP send seam. {@link #close()} stops both threads and closes the codec.
     *
     * @implNote This implementation wires the application {@link VideoOutput} source straight into the encode
     * thread, the same programmatic source path the audio {@link AudioOutput} uses; a camera- or
     * screen backed source captures from its own device behind the {@link VideoOutput} interface, so no
     * separate platform capture driver is opened here. The outbound RTP packetization treats each encoded
     * access unit as one single NAL packet, matching the symmetric single access unit assumption on the
     * inbound demux side until the fragmented depacketizer lands.
     */
    private static final class VideoPipeline implements AutoCloseable {
        /**
         * The depth of the reusable pixel buffer ring the render loop hands borrowed frames out of, sized
         * to the buffered sink's frame capacity plus one.
         *
         * @implNote This implementation uses five, one more than the four frames a sink may hold in flight.
         * A ring buffer is passed back to the decoder for refill only after the ring has cycled through the
         * other four hand outs, by which point the frame that borrowed it has necessarily been handed to and
         * consumed by the sink through
         * {@link com.github.auties00.cobalt.calls.stream.VideoInput#offerVideo(com.github.auties00.cobalt.calls.stream.VideoFrame)},
         * so refilling a ring buffer never aliases a frame still reachable through
         * {@link com.github.auties00.cobalt.calls.stream.VideoInput#readVideo()}.
         */
        private static final int PLAYBACK_RING_DEPTH = 5;

        /**
         * Holds the call identifier, used in the thread names and diagnostics.
         */
        private final String callId;

        /**
         * Holds the host the decoded pictures are rendered through when no application video sink was
         * supplied.
         */
        private final VoipHostApi host;

        /**
         * Holds the negotiated video codec the pipeline encodes and decodes through.
         */
        private final VideoCodec codec;

        /**
         * Holds the parameter set the codec was opened with, the baseline the rate control loop re targets
         * each round.
         *
         * <p>The rate controller mutates only the bitrate, frame rate, and quantizer window through
         * {@link #modifyCodec(VideoCodecParams)}; this carries the immutable geometry and codec selection the
         * controller must preserve so a re target never trips the codec's geometry change rejection.
         */
        private final VideoCodecParams codecParams;

        /**
         * Holds the inbound video jitter buffer the render loop polls for due frames.
         */
        private final VideoJitterBuffer jitterBuffer;

        /**
         * Holds the application video capture source the encode loop drains, or {@code null} when the call
         * sends no video.
         */
        private final VideoOutput videoCapture;

        /**
         * Holds the application video playback sink decoded pictures are delivered to, or {@code null} when
         * the call renders received video through the host.
         */
        private final VideoInput videoPlayback;

        /**
         * Holds the round robin ring of reusable pixel buffers the decoder packs each borrowed frame into,
         * or {@code null} when the call renders through the host and no sink borrows a buffer.
         *
         * <p>Confined to the single render thread that drives {@link #renderReleased}. Each slot starts
         * {@code null} and is seeded with the decoder's first same geometry output, then handed back to
         * the decoder as its pack target on the next cycle; a resolution change re seeds the slot with a
         * fresh buffer the decoder allocates in its stead.
         */
        private final byte[][] playbackRing;

        /**
         * Holds the next {@link #playbackRing} slot to pack and hand out, advanced only when a decoded
         * picture is offered to the sink.
         */
        private int playbackRingIndex;

        /**
         * Holds the color space converter that normalizes each captured picture to planar I420 at the
         * advertised encoder geometry before the encode hand off.
         *
         * <p>Stateless and thread confined to the encode loop: it repacks an
         * {@link com.github.auties00.cobalt.calls.stream.VideoPixelFormat#NV12 NV12} capture to I420 and
         * resamples any off geometry picture to {@link #codecParams} dimensions, so the codec always
         * receives an I420 frame that matches the geometry it was opened with.
         */
        private final YuvConverter yuvConverter;

        /**
         * Holds the send seam each encoded access unit is shipped through, bound after the transport is
         * built and before {@link #start()}, or {@code null} when the call sends no video.
         */
        private Consumer<EncodedVideoFrame> frameSink;

        /**
         * Tracks whether the pipeline is running, so both threads exit on close.
         */
        private final AtomicBoolean running;

        /**
         * Holds the render thread polling the jitter buffer, or {@code null} before start.
         */
        private Thread renderThread;

        /**
         * Holds the encode thread draining the capture source, or {@code null} when the call sends no video
         * or before start.
         */
        private Thread encodeThread;

        /**
         * Constructs a video pipeline over the codec, jitter buffer, and the call's application video
         * streams.
         *
         * @param callId        the call identifier
         * @param host          the host decoded pictures are rendered through when no video sink was supplied
         * @param codec         the negotiated video codec
         * @param codecParams   the parameter set the codec was opened with, the rate control re target baseline
         * @param jitterBuffer  the inbound video jitter buffer
         * @param videoCapture  the application video capture source, or {@code null} when the call sends no
         *                      video
         * @param videoPlayback the application video playback sink, or {@code null} to render through the host
         */
        private VideoPipeline(String callId, VoipHostApi host, VideoCodec codec, VideoCodecParams codecParams,
                              VideoJitterBuffer jitterBuffer, VideoOutput videoCapture, VideoInput videoPlayback) {
            this.callId = callId;
            this.host = host;
            this.codec = codec;
            this.codecParams = codecParams;
            this.jitterBuffer = jitterBuffer;
            this.videoCapture = videoCapture;
            this.videoPlayback = videoPlayback;
            this.playbackRing = videoPlayback != null ? new byte[PLAYBACK_RING_DEPTH][] : null;
            this.yuvConverter = YuvConverter.create();
            this.running = new AtomicBoolean();
        }

        /**
         * Binds the send seam the outbound encode loop ships encoded access units through.
         *
         * <p>The seam needs the transport, which is built after the pipeline (the inbound demux holds the
         * pipeline reference), so the media plane binds it once the transport exists and before
         * {@link #start()} launches the encode loop.
         *
         * @param frameSink the send seam shipping encoded access units over the transport
         */
        private void bindFrameSink(Consumer<EncodedVideoFrame> frameSink) {
            this.frameSink = frameSink;
        }

        /**
         * Starts the inbound render loop.
         *
         * <p>The inbound decode and render path always starts so a video call renders the peer's picture
         * regardless of whether the local side sends video; the outbound encode and send path is started
         * separately through {@link #startEncode()} so the media session can drive it from the in call camera
         * turn on rather than unconditionally at bring up.
         */
        private void start() {
            running.set(true);
            renderThread = Thread.ofVirtual().name("calls-video-render-" + callId).start(this::renderLoop);
        }

        /**
         * Starts the outbound encode loop once, when the call supplies a video capture source and a bound send
         * seam.
         *
         * <p>Driven by {@link LiveMediaSession#startVideoSend()}, which guards it behind the session's
         * one shot video send flag. A call with no application or device video capture source, or one whose
         * send seam was never bound, has no outbound encode loop and this is a no op; a second invocation after
         * the encode thread is already live is also a no op, so the session may call it idempotently.
         */
        private void startEncode() {
            if (videoCapture == null || frameSink == null || encodeThread != null) {
                return;
            }
            running.set(true);
            encodeThread = Thread.ofVirtual().name("calls-video-encode-" + callId).start(this::encodeLoop);
        }

        /**
         * Inserts one received compressed video frame into the jitter buffer.
         *
         * <p>Called from the transport receive path with a depacketized access unit; the jitter buffer
         * orders it by capture timestamp and releases it at its render time. A frame the buffer rejects as
         * a duplicate is dropped.
         *
         * @param frame               the received compressed frame
         * @param arrivalMs           the local arrival time in milliseconds
         * @param captureRtpTimestamp the frame's ninety kilohertz capture RTP timestamp
         * @param sequenceNumber      the frame's RTP sequence number
         */
        private void insert(EncodedVideoFrame frame, long arrivalMs, long captureRtpTimestamp, int sequenceNumber) {
            jitterBuffer.insert(frame, arrivalMs, captureRtpTimestamp, sequenceNumber);
        }

        /**
         * Returns the parameter set the codec was opened with, the rate control re target baseline.
         *
         * <p>The rate control loop reads this once to seed its running video parameters and then re targets
         * the mutable subset (bitrate, frame rate, quantizer window) through
         * {@link #modifyCodec(VideoCodecParams)}; the immutable geometry and codec selection it carries fix
         * what the controller must preserve across a re target.
         *
         * @return the codec's open parameter set
         */
        private VideoCodecParams codecParams() {
            return codecParams;
        }

        /**
         * Re applies the mutable rate control subset of the given parameters onto the live encoder.
         *
         * <p>This is the rate control loop's seam onto the video encoder: it forwards to
         * {@link VideoCodec#modify(VideoCodecParams)}, which re applies the bitrate, frame rate, and the
         * controls a live encoder accepts without a reopen, leaving the geometry and codec selection
         * unchanged. It runs on the rate control thread; the codec is single writer, but the render and
         * encode loops invoke {@link VideoCodec#decode(byte[], long)} and
         * {@link VideoCodec#encode(VideoFrame, boolean)} on their own threads.
         *
         * @implNote This implementation serializes encoder reconfiguration against the encode loop by
         * forwarding directly to the codec, accepting that the live OpenH264/libvpx reconfigure call the
         * codec issues is itself the single mutation point; the codec's own state guards are relied upon
         * rather than adding a second lock here, matching the engine driving rate control and encode from the
         * one media stream context.
         *
         * @param params the parameter set whose mutable fields the encoder adopts; never {@code null}
         */
        private void modifyCodec(VideoCodecParams params) {
            codec.modify(params);
        }

        /**
         * Arms the encoder so its next encode produces a key frame.
         *
         * <p>This is the rate control loop's recovery seam: after a loss the receiver could not conceal, or
         * when the rate controller resets the stream, it forwards to {@link VideoCodec#requestKeyFrame()} so
         * the next encoded picture is an intra frame the decoder can resynchronize on.
         *
         * @implNote This implementation forwards directly to the codec; the request is one shot and consumed
         * by the next encode, so a request raised from the rate control thread takes effect on the encode
         * loop's next pass without further coordination.
         */
        private void requestKeyFrame() {
            codec.requestKeyFrame();
        }

        /**
         * Normalizes one captured picture to planar I420 at the advertised encoder geometry for the encode
         * hand off.
         *
         * <p>A capture that already matches the codec geometry is repacked to
         * {@link com.github.auties00.cobalt.calls.stream.VideoPixelFormat#I420 I420} through
         * {@link YuvConverter#toI420(VideoFrame)}, which returns an already I420 frame unchanged and splits
         * an {@link com.github.auties00.cobalt.calls.stream.VideoPixelFormat#NV12 NV12} frame's interleaved
         * chroma into the separate planes the native encoders consume. A capture whose dimensions differ
         * from the codec geometry is resampled to the advertised size through
         * {@link YuvConverter#scale(VideoFrame, int, int)}, which itself yields I420, so the codec always
         * receives an I420 picture matching the geometry it was opened with and its geometry check never
         * trips.
         *
         * @implNote This implementation performs the libyuv normalization the native converter drives at
         * the encode hand off, centralizing the NV12 to I420 repack that would otherwise be duplicated in
         * every {@link VideoCodec}; rotation is not applied because a captured {@link VideoFrame} carries no
         * orientation metadata to drive it.
         *
         * @param frame the captured picture, in I420 or NV12 and at any geometry
         * @return the picture as planar I420 at the codec's advertised geometry
         */
        private VideoFrame normalizeForEncode(VideoFrame frame) {
            if (frame.width() != codecParams.width() || frame.height() != codecParams.height()) {
                return yuvConverter.scale(frame, codecParams.width(), codecParams.height());
            }
            return yuvConverter.toI420(frame);
        }

        /**
         * Drains the capture source, encodes each picture, and ships it until the pipeline stops.
         *
         * <p>Each turn pulls one raw picture from the application video source with a blocking
         * {@link VideoOutput#takeVideo()}, normalizes it to planar I420 at the advertised geometry, encodes it
         * through the codec, and ships a non empty access unit through the send seam; an empty access unit
         * (a rate controller frame drop) is sent as nothing. A {@code null} pull or a cleared running flag
         * ends the loop, and an interrupt during the pull is treated as a stop.
         */
        private void encodeLoop() {
            while (running.get()) {
                VideoFrame frame;
                try {
                    frame = videoCapture.takeVideo();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    break;
                }
                if (frame == null) {
                    break;
                }
                try {
                    var encoded = codec.encode(normalizeForEncode(frame), false);
                    if (!encoded.isEmpty()) {
                        frameSink.accept(encoded);
                    }
                } catch (RuntimeException exception) {
                    if (Log.DEBUG) {
                        LOGGER.log(Level.DEBUG, "calls video encode error for call {0}: {1}",
                                callId, exception.getMessage());
                    }
                }
            }
        }

        /**
         * Polls the jitter buffer and renders due frames until the pipeline stops.
         *
         * <p>Each turn polls the buffer for a frame due now; a released frame is decoded and, when the
         * decode yields a displayable picture, delivered to the application video sink or rendered through
         * the host. A decode that yields no picture (a decoder waiting for a key frame) is skipped. The loop
         * parks briefly between polls so it does not spin.
         */
        private void renderLoop() {
            while (running.get()) {
                try {
                    var now = System.nanoTime() / 1_000_000L;
                    var released = jitterBuffer.poll(now);
                    if (released == null) {
                        java.util.concurrent.locks.LockSupport.parkNanos(VIDEO_POLL_INTERVAL_NANOS);
                        continue;
                    }
                    renderReleased(released);
                } catch (RuntimeException exception) {
                    if (Log.DEBUG) {
                        LOGGER.log(Level.DEBUG, "calls video render error for call {0}: {1}",
                                callId, exception.getMessage());
                    }
                }
            }
        }

        /**
         * Decodes one released access unit and delivers the resulting picture to the application sink or the
         * host.
         *
         * <p>Decodes the released frame; a decode that produces no displayable picture is skipped. When the
         * call supplied an application video sink the decoded picture is packed into the next buffer of a
         * reusable {@link #playbackRing} and offered to the sink as a borrowed {@link VideoFrame}, so a
         * steady resolution stream delivers received video with no per frame pixel allocation; otherwise
         * the picture is decoded straight into an off heap plane segment through
         * {@link VideoCodec#decodeToNative(byte[], long, java.lang.foreign.SegmentAllocator)} and handed to
         * {@link VoipHostApi#renderVideoFrame} as a {@link VoipHostApi.RenderedVideoFrame}, whose plane
         * segment is valid only for the duration of the render call.
         *
         * @implNote This implementation owns the ring on the single render thread and advances the
         * round robin index only when a picture is actually offered, so a decode that yields nothing (a
         * decoder awaiting a key frame) leaves the current slot untouched for the next pass. It re seeds a
         * slot from the returned frame's buffer after every decode: on a same geometry frame the decoder
         * packs into the buffer the slot supplied and hands it straight back, while a resolution change
         * makes the decoder allocate a correctly sized buffer that the slot then adopts for later reuse.
         *
         * @param released the released frame and its render time
         */
        private void renderReleased(VideoJitterBuffer.ReleasedFrame released) {
            var encoded = released.frame();
            if (videoPlayback != null) {
                var slot = playbackRingIndex;
                var picture = codec.decode(encoded.payload(), encoded.ptsMicros(), playbackRing[slot]);
                if (picture == null) {
                    return;
                }
                playbackRing[slot] = picture.pixels();
                playbackRingIndex = (slot + 1) % PLAYBACK_RING_DEPTH;
                videoPlayback.offerVideo(picture);
                return;
            }
            try (var arena = Arena.ofConfined()) {
                var picture = codec.decodeToNative(encoded.payload(), encoded.ptsMicros(), arena);
                if (picture == null) {
                    return;
                }
                host.renderVideoFrame(new VoipHostApi.RenderedVideoFrame(
                        null,
                        picture.width(),
                        picture.height(),
                        picture.plane(),
                        MemorySegment.NULL,
                        0,
                        VoipHostApi.RenderedVideoFrame.Format.I420,
                        picture.ptsMicros() / 1_000_000.0,
                        false));
            }
        }

        @Override
        public void close() {
            running.set(false);
            if (renderThread != null) {
                renderThread.interrupt();
            }
            if (encodeThread != null) {
                encodeThread.interrupt();
            }
            try {
                codec.close();
            } catch (RuntimeException exception) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "calls video codec close failed", exception);
            }
        }
    }

    /**
     * The call's bandwidth estimation and rate control loop: it composes the WhatsApp sender side AIMD
     * estimator, the GoogCC delay based estimator, and the audio and (on a video call) video rate
     * controllers, and drives the encoders from them.
     *
     * <p>The loop runs on two cadences. The primary cadence is per inbound RTCP feedback: the
     * relay transport unprotects and parses each compound RTCP packet into an {@link RtcpFeedback} and
     * delivers it to {@link #onRtcpFeedback(RtcpFeedback)}, which advances the
     * {@link LiveSenderBandwidthEstimator} (WhatsApp AIMD fused through the combiner and gated by the hold
     * machine), splits the combined target between the audio and video streams,
     * and re targets the Opus encoder through {@link OpusAudioCodec#modify(OpusCodecParams)} and the video
     * encoder through {@link VideoPipeline#modifyCodec(VideoCodecParams)}. The secondary cadence is a
     * periodic fallback tick on its own virtual thread that applies the matched dynamic
     * voip param overrides under the manager's {@link com.github.auties00.cobalt.calls.config.param.DynVoipParamUpdater}
     * round guard and re evaluates feedback staleness, so a stalled RTCP stream does not leave the encoder
     * frozen on a target the link can no longer carry. Independently, the inbound media demux tees each
     * media packet's send and arrival timing into the {@link GccDelayBasedEstimator} through
     * {@link #onPacketReceived(long, long, int)} so the delay based over use trendline feeds the
     * combiner.
     *
     * <p>The per feedback tick and the per packet tee both run on the single transport receive thread (the
     * relay transport demultiplexes a datagram synchronously on that thread), while the periodic tick runs
     * on its own thread; all three are serialized behind one {@link ReentrantLock} because the estimator and
     * rate control components are single writer.
     *
     * @implNote This implementation uses a {@link NoopMlBweEngine}: {@code ML-BWE} is unimplemented, so the steering
     * fold in stays neutral and the sender estimator, combiner, and rate controllers behave as if no model
     * exists. The cadence is per RTCP feedback rather than a fixed wall clock, matching the native engine
     * running off each received report; the periodic thread carries only the dynamic rule pass and the
     * staleness re evaluation. The bandwidth bounds are read from the negotiated {@code min_bwe} and
     * {@code max_bwe} scalars through {@link LiveMediaSession#activeParams()}, falling back to the compiled
     * {@link RateControlLoop#MIN_BITRATE_BPS} and {@link RateControlLoop#MAX_BITRATE_BPS} when the
     * {@code voip_settings} document omits them; the seed, the minimum remote gate, and the congestion enable
     * mask remain compiled in stand ins (see the constants).
     */
    private static final class RateControlLoop {
        /**
         * The compiled fallback lower bound, in bits per second, applied when the negotiated parameter set
         * does not carry {@code p->min_bwe}.
         *
         * <p>The constructor reads {@code p->min_bwe} from {@link LiveMediaSession#activeParams()} and falls
         * back to this value only when that scalar is absent, mirroring the engine applying its compiled
         * default when the {@code voip_settings} document omits the override.
         *
         * @implNote The compiled default of {@code min_bwe} is {@code 100000} bits per second (100 kbps); the
         * {@code voip_settings} document does not negotiate it, so this default governs.
         */
        private static final long MIN_BITRATE_BPS = 100_000;

        /**
         * The compiled fallback upper bound, in bits per second, applied when the negotiated parameter set
         * does not carry {@code p->max_bwe}.
         *
         * <p>The constructor reads {@code p->max_bwe} from {@link LiveMediaSession#activeParams()} and falls
         * back to this value only when that scalar is absent.
         *
         * @implNote The GoogCC delay based upper bound. Unlike {@link #MIN_BITRATE_BPS}, the compiled default
         * of {@code max_bwe} is unknown, and the {@code voip_settings} does not negotiate it, so the upstream
         * WebRTC default stands in as the fallback.
         */
        private static final long MAX_BITRATE_BPS = 8_000_000;

        /**
         * The compiled in seed estimate, in bits per second, used when no history seed is available.
         *
         * @implNote The GoogCC delay based start bitrate default, used when
         * {@link BweHistory#seedEstimateKbps()} yields nothing (the per call history is not persisted across
         * calls, so a first call has no seed).
         */
        private static final long SEED_BITRATE_BPS = 300_000;

        /**
         * The per round {@code min_remote_bitrate_estimate}, in bits per second, supplied to the sender
         * AIMD each round to seed its latched increase factor floor.
         *
         * <p>The healthy link increase uses {@code 1.05} while the remote estimate is absent or below the
         * latched floor and {@code 1.10} at or above it. The latch itself (seed once while still {@code 0},
         * then hold) lives in {@link AudioSenderBandwidthEstimator}; this constant is the per round input that
         * seeds it.
         *
         * @implNote The faithful value is {@code 0}: the minimum remote bitrate estimate is not a field of a
         * single inbound RTCP packet but the minimum, across all connected participants, of each participant's
         * reported remote bandwidth estimate. This rate control loop is driven per single {@link RtcpFeedback}
         * and holds no participant roster nor any per peer remote estimate, so the roster minimum cannot be
         * computed here; supplying {@code 0} keeps the latch unseeded, which is exactly the pre roster state in
         * which the increase factor is {@code 1.05} whenever the remote estimate is absent.
         */
        // TODO: thread the per round minimum remote bitrate estimate (the minimum over all connected
        //  participants' remote bandwidth estimates) into this loop and pass it here in place of 0, so the
        //  sender AIMD seeds its latched floor from the live roster. This loop is constructed without a
        //  participant view and the per peer remote estimate is not tracked on the relay downlink path, so the
        //  roster minimum is not available at this layer today.
        private static final long MIN_REMOTE_BITRATE_BPS = 0;

        /**
         * The high loss ratio threshold the sender AIMD treats as congestion, from {@code sbwe_loss_high}.
         *
         * <p>The negotiated {@code sbwe_loss_high} is {@code 30} percent.
         */
        private static final double LOSS_HIGH_THRESHOLD = 0.30;

        /**
         * The default loss ratio threshold the congestion detector's normal tier trips on, from
         * {@code sbwe_loss_low}.
         *
         * <p>The negotiated {@code sbwe_loss_low} is {@code 10} percent.
         */
        private static final double LOSS_LOW_THRESHOLD = 0.10;

        /**
         * The high tier round trip coefficient the congestion detector trips its aggressive flag on.
         *
         * @implNote The {@code cc_rtt_heavily_congestion_multiplier} default is {@code 2.0}: the engine sets
         * the heavily congested flag when {@code current_rtt >= 2.0 * baseline_rtt}, the same
         * {@code rtt > coefficient * baseline} form {@link CongestionSignalDetector} applies for its aggressive
         * tier. The key is absent from the {@code voip_settings}, so the compiled default governs.
         */
        private static final double RTT_HIGH_COEFFICIENT = 2.0;

        /**
         * The default tier round trip coefficient the congestion detector trips its congested flag on.
         *
         * @implNote The {@code cc_rtt_approaching_congestion_multiplier} default is {@code 0.6}: the engine
         * sets the approaching flag when {@code current_rtt >= 0.6 * baseline_rtt}. The key is absent from the
         * {@code voip_settings}, so the compiled default governs.
         */
        private static final double RTT_DEFAULT_COEFFICIENT = 0.6;

        /**
         * The bitmask of congestion signals the detector evaluates each round.
         *
         * @implNote The full enable union: {@code 0x1} round trip, {@code 0x200} remote loss, {@code 0x400}
         * local loss, {@code 0x800} remote timing, {@code 0x4000} local timing, {@code 0x8000} staleness. The
         * {@link CongestionSignalDetector#evaluate} path carries code for the round trip, remote loss, local
         * loss, and staleness bits; the two timing bits are reserved (no inter arrival timing input is fused on
         * the per feedback path), and the staleness bit only trips when a non zero feedback age is supplied,
         * which the periodic tick does and the per feedback tick (age zero) does not.
         */
        private static final long CONGESTION_ENABLE_MASK = CongestionSignalDetector.ENABLE_RTT
                | CongestionSignalDetector.ENABLE_REMOTE_PLR
                | CongestionSignalDetector.ENABLE_LOCAL_PLR
                | 0x0800
                | 0x4000
                | CongestionSignalDetector.ENABLE_STALENESS;

        /**
         * The period, in nanoseconds, of the fallback tick.
         *
         * <p>One second; the fallback tick carries only the dynamic rule pass and the staleness
         * re evaluation, so it runs an order of magnitude slower than the per feedback cadence.
         */
        private static final long FALLBACK_TICK_NANOS = 1_000_000_000L;

        /**
         * The staleness window, in milliseconds, past which the absence of RTCP feedback triggers the
         * fallback tick to re run rate control on the last known state.
         *
         * @implNote The {@code no RTCP in window} congestion condition; set to the sender AIMD's no feedback
         * window so a stalled report stream is treated as a degrading link.
         */
        private static final long FEEDBACK_STALENESS_MS = 500;

        /**
         * The logger for {@link RateControlLoop}.
         */
        private static final System.Logger LOGGER = Log.get(RateControlLoop.class);

        /**
         * The call identifier, used in the tick thread name and diagnostics.
         */
        private final String callId;

        /**
         * Whether the call carries video, so the video rate controller is driven.
         *
         * <p>Set at construction for a call brought up with video, and flipped to {@code true} by
         * {@link #enableVideo(VideoPipeline)} when an audio only call raises a local video track mid call.
         * It is read and written only under {@link #lock}, the same lock {@link #runTick} holds, so the video
         * branch sees a consistent {@code (video, videoRc, videoPipeline, videoParams)} tuple.
         */
        private boolean video;

        /**
         * The audio codec the audio rate controller re targets each round, an {@link OpusAudioCodec} or an
         * {@link MLowAudioCodec}.
         *
         * <p>The loop re targets it through {@link AudioCodec#modify(OpusCodecParams)} each round; the
         * {@link MLowAudioCodec} reads the live target bitrate and forwards it to its internal rate
         * controller just as the {@link OpusAudioCodec} re targets libopus.
         */
        private final AudioCodec audioCodec;

        /**
         * The video pipeline whose encoder the video rate controller re targets, or {@code null} until the
         * call carries video.
         *
         * <p>Set at construction for a call brought up with video, and bound by
         * {@link #enableVideo(VideoPipeline)} on a mid call upgrade; read and written only under {@link #lock}.
         */
        private VideoPipeline videoPipeline;

        /**
         * The call's voip param manager, read for the active set each round and driven for the periodic
         * dynamic rule pass.
         */
        private final LiveVoipParamManager voipParamManager;

        /**
         * The WhatsApp sender side bandwidth estimator producing the combined, gated target.
         */
        private final LiveSenderBandwidthEstimator senderBwe;

        /**
         * The GoogCC delay based estimator the per packet tee feeds, supplying the combiner's link capacity
         * input.
         */
        private final GccDelayBasedEstimator gcc;

        /**
         * The audio rate controller turning the audio share of the target into Opus encoder settings.
         */
        private final AudioRateController audioRc;

        /**
         * The video rate controller turning the video share of the target into video encoder settings, or
         * {@code null} until the call carries video.
         *
         * <p>Built at construction on a call brought up with video, and built lazily by
         * {@link #enableVideo(VideoPipeline)} on a mid call upgrade; read and written only under {@link #lock}.
         */
        private VideoRateController videoRc;

        /**
         * The machine learning bandwidth estimation engine, always a {@link NoopMlBweEngine}.
         *
         * <p>{@code ML-BWE} inference is not provisioned in Cobalt, so the engine loads no model and
         * {@link MlBweEngine#infer(MlBweSignals)} returns {@link MlBweOutputs#DISABLED}; the pure
         * delay based and sender side estimator runs standalone.
         */
        private final MlBweEngine mlEngine;

        /**
         * The configured maximum target bitrate, in bits per second, the negotiated {@code p->max_bwe} or the
         * compiled {@link #MAX_BITRATE_BPS} fallback, fed into the machine learning signal set each round.
         */
        private final long maxBitrateBps;

        /**
         * Supplies the current SCTP send buffer occupancy, in bytes, the video rate controller reads each
         * round as its buffer congestion signal.
         *
         * <p>The relay data channel runs the pure Java {@code com.github.auties00.sctp} stack, whose real
         * per stream send buffer occupancy is exposed through
         * {@link LiveRelayTransport.DataChannel#bufferedAmount()} and surfaced by
         * {@link MediaTransport#sctpBufferedAmount()}. This supplier reads that live value on the relay path
         * and {@code 0} on a non relay or not yet connected path, and {@link #runTick} feeds it to
         * {@link VideoRateController#apply} so the {@link SctpBufferCongestionController}'s standing tail and
         * peak plus slope tests see the actual buffer rather than a hardcoded zero. The controller's
         * feedback recency gate, against which {@code nowMs} (the round's feedback time) is measured, governs
         * how the occupancy releases the clamp, so a stalled report stream cannot release it prematurely. It
         * is read only from the video branch of {@link #runTick}: an audio only or no video round never
         * consults it, and a non relay path keeps the zero occupancy that governs the video target exactly as
         * before.
         *
         * @implNote {@link LongSupplier#getAsLong()} is a cheap field read on the SCTP send stream, so
         * driving it each round adds no latency to the rate control tick.
         */
        private final LongSupplier sctpBufferedAmount;

        /**
         * Serializes the per feedback tick, the per packet tee, and the periodic tick across their threads.
         */
        private final ReentrantLock lock;

        /**
         * The running Opus parameters the audio rate controller re targets, seeded from the codec's open
         * parameters and replaced by each round's result.
         */
        private OpusCodecParams audioParams;

        /**
         * The running video parameters the video rate controller re targets, seeded from the video pipeline's
         * open parameters and replaced by each round's result, or {@code null} until the call carries video.
         *
         * <p>Seeded at construction on a call brought up with video, and seeded by
         * {@link #enableVideo(VideoPipeline)} from the upgrade pipeline's open parameters on a mid call
         * upgrade; read and written only under {@link #lock}.
         */
        private VideoCodecParams videoParams;

        /**
         * The monotonic timestamp, in milliseconds, of the most recent feedback, or {@code -1} before any
         * feedback has arrived.
         */
        private long lastFeedbackMs;

        /**
         * The packet loss ratio from the most recent feedback, carried for the staleness re run.
         */
        private double lastPlr;

        /**
         * The round trip time, in nanoseconds, from the most recent feedback, carried for the staleness
         * re run, or {@code 0} when none was observed.
         */
        private long lastRttNs;

        /**
         * The combined sender bandwidth estimate, in bits per second, from the most recent rate control round,
         * published for the video packetizer's id13 keyframe descriptor; zero before the first round.
         *
         * <p>Written under {@link #lock} on each round and read without it through
         * {@link #bandwidthEstimateBps()}; declared {@code volatile} so the send thread observes each update.
         */
        private volatile int lastBandwidthEstimateBps;

        /**
         * The video target bitrate, in bits per second, from the most recent rate control round, published for
         * the video packetizer's id13 keyframe descriptor; zero before the first round or on an audio only call.
         *
         * <p>Written under {@link #lock} on each round and read without it through
         * {@link #videoTargetBitrateBps()}; declared {@code volatile} so the send thread observes each update.
         */
        private volatile int lastVideoTargetBitrateBps;

        /**
         * Tracks whether the periodic tick is running, so its thread exits on {@link #stop()}.
         */
        private final AtomicBoolean running;

        /**
         * The periodic fallback tick thread, or {@code null} before {@link #start()}.
         */
        private Thread tickThread;

        /**
         * Constructs the rate control loop for one call, seeding the sender estimate from the call's
         * bandwidth history on the caller side.
         *
         * <p>The sender side estimator is composed from the WA audio AIMD, the combiner (in the captured
         * {@code sbwe_combine_policy=3} {@link CombineMode#MIN_FLOOR} mode), the hold machine, the
         * conservative init clamp, and the congestion detector, bounded by the negotiated {@code p->min_bwe}
         * and {@code p->max_bwe} (or the compiled {@link #MIN_BITRATE_BPS} / {@link #MAX_BITRATE_BPS} fallback
         * when the negotiation omits them) and seeded at the history estimate (caller side) or the compiled
         * {@link #SEED_BITRATE_BPS} (callee side or empty history). The GoogCC delay based estimator shares
         * the same bounds and seed. The audio rate controller is always built; the video rate controller is
         * built only on a video call.
         *
         * <p>The conservative init clamp is enabled by default so a fresh estimate ramps cautiously from the
         * cold start band. When {@code seedInitialGroupBwe} is set (a group call with
         * {@code ENABLE_INIT_BWE_FOR_GROUP_CALL} on) the clamp is disabled so the seeded initial estimate is
         * used directly rather than held in the cautious band, the group call initial BWE seed the flag
         * selects.
         *
         * @implNote This implementation threads {@code ENABLE_INIT_BWE_FOR_GROUP_CALL} into the
         * {@link ConservativeInitMode} enable flag for a group call: the conservative clamp reads an enable
         * byte, and seeding an initial group call estimate corresponds to leaving that clamp off so the seed is
         * taken as is rather than ramped. A one to one call ({@code seedInitialGroupBwe} {@code false}) keeps
         * the clamp enabled.
         *
         * @param callId             the call identifier
         * @param isCaller           whether the local side placed the call, gating the history seed
         * @param video              whether the call carries video
         * @param audioCodec         the audio codec the audio rate controller re targets
         * @param audioParams        the Opus parameters the codec was opened with, the audio re target
         *                           baseline
         * @param videoPipeline      the video pipeline whose encoder is re targeted, or {@code null} on an
         *                           audio only call
         * @param voipParamManager   the call's voip param manager
         * @param sctpBufferedAmount supplies the SCTP send buffer occupancy, in bytes, the video rate
         *                           controller reads each round as its buffer congestion signal: the live
         *                           per stream buffered amount on the relay path, {@code 0} on a non relay or
         *                           not yet connected path
         * @param seedInitialGroupBwe whether to seed the initial estimate for a group call rather than ramp
         *                           from the conservative cold start band, set when this is a group call and
         *                           {@code ENABLE_INIT_BWE_FOR_GROUP_CALL} is on
         */
        private RateControlLoop(String callId, boolean isCaller, boolean video, AudioCodec audioCodec,
                                      OpusCodecParams audioParams, VideoPipeline videoPipeline,
                                      LiveVoipParamManager voipParamManager, LongSupplier sctpBufferedAmount,
                                      boolean seedInitialGroupBwe) {
            this.callId = callId;
            this.video = video;
            this.audioCodec = audioCodec;
            this.audioParams = audioParams;
            this.videoPipeline = videoPipeline;
            this.voipParamManager = voipParamManager;
            this.sctpBufferedAmount = sctpBufferedAmount;
            this.videoParams = videoPipeline != null ? videoPipeline.codecParams() : null;
            this.mlEngine = selectMlBweEngine();
            this.lock = new ReentrantLock();
            this.running = new AtomicBoolean();
            this.lastFeedbackMs = -1;
            this.lastPlr = 0.0;
            this.lastRttNs = 0;

            var params = voipParamManager.activeParams();
            // The audio rate control loop reads the audio (rc) section. rc.maxbwe is the audio ceiling;
            // rc.minbwe is not read in this WhatsApp revision (only vid_rc.minbwe exists, the video floor,
            // which must not be substituted here), so the audio floor uses the compiled default.
            // TODO: wire the audio minimum to rc.minbwe once a WhatsApp revision that reads it is modelled.
            var minBwe = MIN_BITRATE_BPS;
            var maxBwe = VoipParamKey.ofWirePath(RC_MAXBWE_PARAM)
                    .map(key -> resolveScalar(params, key, MAX_BITRATE_BPS))
                    .orElse(MAX_BITRATE_BPS);
            this.maxBitrateBps = maxBwe;
            var seedBps = seedBitrateBps(isCaller);
            // TODO: wire PacketPairEstimator. Own a PacketPairEstimator (HD thresholds from VoipParamKey), feed
            //  onPacketPair(combinedBytes, dispersionMs, arrivalMs), and pass linkCapacityBps()/isHdCapable() into
            //  LiveSenderBandwidthEstimator.update (the linkCapacityBps param nothing currently produces).
            this.senderBwe = new LiveSenderBandwidthEstimator(
                    new AudioSenderBandwidthEstimator(minBwe, maxBwe, seedBps,
                            LOSS_HIGH_THRESHOLD),
                    new BitrateCombiner(CombineMode.MIN_FLOOR),
                    new BweHoldController(),
                    new ConservativeInitMode(!seedInitialGroupBwe, minBwe, SEED_BITRATE_BPS),
                    new CongestionSignalDetector(RTT_HIGH_COEFFICIENT, RTT_DEFAULT_COEFFICIENT,
                            LOSS_HIGH_THRESHOLD, LOSS_LOW_THRESHOLD),
                    CONGESTION_ENABLE_MASK,
                    minBwe, maxBwe, seedBps);
            this.gcc = new GccDelayBasedEstimator(minBwe, maxBwe, seedBps);
            this.audioRc = new AudioRateController(
                    new UnifiedAudioQualityControl(UnifiedAudioQualityControl.Config.defaults()));
            this.videoRc = video ? new VideoRateController(SctpBufferCongestionController.defaults()) : null;
        }

        /**
         * Returns the machine learning bandwidth estimation engine for the call.
         *
         * <p>This is always a {@link NoopMlBweEngine}: {@code ML-BWE} inference is not provisioned, so the engine
         * loads no model and {@link MlBweEngine#infer(MlBweSignals)} returns {@link MlBweOutputs#DISABLED},
         * leaving the pure delay based and sender side estimator to run standalone.
         *
         * @return the machine learning engine for the call, never {@code null}
         */
        private static MlBweEngine selectMlBweEngine() {
            // TODO: wire LiveMlBweEngine. This hardcodes new NoopMlBweEngine(); construct
            //  new LiveMlBweEngine(configs, resolver) once the ExecuTorch backend is bundled in cobalt native,
            //  voip_settings is decoded to ModelConfig, and a ModelPathResolver resolves downloaded .pte paths.
            return new NoopMlBweEngine();
        }

        /**
         * Resolves a negotiated integer voip param scalar, or the compiled fallback when it is absent.
         *
         * <p>Reads the key from the active negotiated set. An empty set (settings not yet selected) or a key
         * the negotiation omits yields the fallback, matching the engine taking its compiled default when the
         * {@code voip_settings} document does not override the scalar.
         *
         * @param params   the active negotiated parameter set, possibly empty before settings arrive
         * @param key      the parameter to read
         * @param fallback the compiled default applied when the parameter is not negotiated
         * @return the negotiated value when present, otherwise {@code fallback}
         */
        private static long resolveScalar(Optional<VoipParams> params, VoipParamKey key, long fallback) {
            if (params.isEmpty()) {
                return fallback;
            }
            var value = params.get().getInteger(key);
            return value.isPresent() ? value.getAsLong() : fallback;
        }

        /**
         * Brings the video rate controller online for a call upgraded from audio only to video mid call.
         *
         * <p>Builds the {@link VideoRateController} the loop lacked while the call was audio only, seeds the
         * running {@link #videoParams} from the freshly built pipeline's opened parameters, binds the pipeline
         * as the encoder re target target, and flips the {@link #video} gate so the next {@link #runTick} round
         * splits the combined target between audio and video and re targets the video encoder. It is invoked
         * under no external lock; it takes {@link #lock} itself so the new state is published consistently to a
         * concurrent feedback or periodic round. It is idempotent: a call already carrying video (the gate
         * already set) is left untouched, so a repeated upgrade neither rebuilds the controller nor disturbs
         * the running estimate.
         *
         * @implNote This implementation reproduces the video rate controller's creation at the audio to video
         * upgrade rather than at call setup: the native engine builds the video quality manager inside the same
         * upgrade transaction, not at the audio call's bring up; an audio only call therefore has no video rate
         * controller until the camera is turned on. The combined target split and the per encoder re target are
         * otherwise the same path a video from start call runs.
         *
         * @param pipeline the freshly built video pipeline whose encoder the controller re targets; never
         *                 {@code null}
         */
        private void enableVideo(VideoPipeline pipeline) {
            lock.lock();
            try {
                if (video) {
                    return;
                }
                this.videoRc = new VideoRateController(SctpBufferCongestionController.defaults());
                this.videoPipeline = pipeline;
                this.videoParams = pipeline.codecParams();
                this.video = true;
            } finally {
                lock.unlock();
            }
        }

        /**
         * Returns the seed bandwidth estimate, in bits per second, for this call side.
         *
         * <p>On the caller side the seed is the mean of the per call bandwidth history (the history seeds a
         * fresh estimate so a reconnect does not restart from the floor); on the callee side, or when the
         * history is empty, the compiled {@link #SEED_BITRATE_BPS} is used.
         *
         * @implNote The {@link BweHistory} is constructed empty here because the cross call history is not
         * persisted across calls in Cobalt (the native engine persists it as storage attributes, see
         * {@link BweHistory}), so {@link BweHistory#seedEstimateKbps()} yields zero on a first call and the
         * compiled seed is used; the history seam is wired so a future persisted history feeds straight in.
         *
         * @param isCaller whether the local side placed the call
         * @return the seed estimate in bits per second
         */
        private static long seedBitrateBps(boolean isCaller) {
            if (isCaller) {
                var historySeedKbps = new BweHistory().seedEstimateKbps();
                if (historySeedKbps > 0) {
                    return (long) historySeedKbps * 1000;
                }
            }
            return SEED_BITRATE_BPS;
        }

        /**
         * Starts the periodic fallback tick on its own virtual thread.
         *
         * <p>The per feedback tick and the per packet tee are already armed by the bring up (the transport's
         * inbound RTCP listener and the inbound demux tee); this starts only the slower fallback cadence.
         */
        private void start() {
            running.set(true);
            tickThread = Thread.ofVirtual().name("calls-rate-control-" + callId).start(this::fallbackLoop);
        }

        /**
         * Stops the periodic fallback tick.
         *
         * <p>Idempotent: clears the running flag and interrupts the tick thread so the loop exits at its next
         * park or immediately.
         */
        private void stop() {
            running.set(false);
            if (tickThread != null) {
                tickThread.interrupt();
            }
        }

        /**
         * Feeds one inbound media packet's send and arrival timing to the GoogCC delay based estimator.
         *
         * <p>The inbound demux tees every audio and video media packet here; the estimator groups packets by
         * send burst and fits its over use trendline from the arrival deltas, and its output is read as the
         * combiner's link capacity input on the next feedback tick.
         *
         * @param sendTimeMs    the packet's RTP derived transmit timestamp, in milliseconds
         * @param arrivalTimeMs the packet's local arrival timestamp, in milliseconds
         * @param payloadBytes  the packet's media payload size, in bytes
         */
        private void onPacketReceived(long sendTimeMs, long arrivalTimeMs, int payloadBytes) {
            lock.lock();
            try {
                gcc.onPacketReceived(sendTimeMs, arrivalTimeMs, payloadBytes);
            } finally {
                lock.unlock();
            }
        }

        /**
         * Runs one rate control round from an inbound RTCP feedback packet.
         *
         * <p>This is the primary cadence: the parsed {@link RtcpFeedback} supplies the loss
         * fraction, the round trip time, and the remote receiver estimate (each carrying a non positive
         * sentinel when the report omitted it, normalized here to a neutral value); the sender side estimator
         * is advanced, its combined gated target is split between the audio and video streams, and the
         * encoders are re targeted. The feedback's loss and round trip are retained so the staleness tick can
         * re run on the last known state.
         *
         * @param feedback the parsed RTCP feedback for this round; never {@code null}
         */
        private void onRtcpFeedback(RtcpFeedback feedback) {
            lock.lock();
            try {
                var plr = feedback.hasLoss() ? Math.clamp(feedback.fractionLost(), 0.0, 1.0) : 0.0;
                var rttNs = feedback.hasRtt() ? feedback.rttNs() : 0;
                var remoteBweBps = feedback.hasRemoteBwe() ? feedback.remoteBweBps() : 0;
                lastPlr = plr;
                lastRttNs = rttNs;
                lastFeedbackMs = System.nanoTime() / 1_000_000L;
                runTick(plr, rttNs, remoteBweBps, lastFeedbackMs);
            } catch (RuntimeException exception) {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG,
                            "calls rate-control feedback tick failed for call {0}: {1}", callId,
                            exception.getMessage());
                }
            } finally {
                lock.unlock();
            }
        }

        /**
         * Advances the estimator and re targets the encoders for one round.
         *
         * <p>Reads the GoogCC delay based estimate as the combiner's link capacity input, advances the
         * {@link LiveSenderBandwidthEstimator}, folds in the machine learning engine's steering when one is
         * enabled, splits the combined target between the audio and video streams, and applies the audio and
         * (on a video call) video rate controllers, re targeting the Opus encoder and the video encoder with
         * the results. The {@code nowMs} timestamp also serves as the most recent feedback time the video
         * rate controller's SCTP buffer recency gate reads.
         *
         * @implNote The machine learning fold in calls {@link BitrateCombiner#applyProbingIncrease(long, long)}
         * with the engine's outputs; the engine is a {@link NoopMlBweEngine} ({@code ML-BWE} is unimplemented), so its
         * {@link MlBweOutputs#DISABLED} outputs are neutral, the increase is zero, and the pure AIMD plus
         * GoogCC plus combiner path runs unchanged.
         *
         * @param plr          the packet loss ratio for this round, in {@code [0, 1]}
         * @param rttNs        the round trip time for this round, in nanoseconds, or {@code 0} when none
         * @param remoteBweBps the remote receiver estimate for this round, in bits per second, or {@code 0}
         * @param nowMs        the monotonic timestamp, in milliseconds, of this round, and the most recent
         *                     feedback time the video rate controller reads
         */
        private void runTick(double plr, long rttNs, long remoteBweBps, long nowMs) {
            var linkCapacityBps = gcc.currentTargetBps();
            var target = senderBwe.onFeedback(plr, rttNs, remoteBweBps, MIN_REMOTE_BITRATE_BPS,
                    linkCapacityBps, nowMs);
            target = applyMlSteering(target, plr, rttNs, remoteBweBps);
            lastBandwidthEstimateBps = (int) target;
            // TODO: wire BweConfigSender. When the downlink BWE estimate falls, call
            //  CallTransportController.onDownlinkBweDrop(index, minRemoteBweKbps) from this media plane BWE
            //  tracking point (drives BweConfigSender.build + transport.sendStandaloneWarp).

            var rttMs = rttNs > 0 ? rttNs / 1_000_000.0 : 0.0;
            var audioTarget = audioShare(target);
            var audioResult = audioRc.apply(audioTarget, plr, rttMs, remoteBweBps, audioParams);
            audioParams = audioResult.params();
            audioCodec.modify(audioParams);

            if (video && videoRc != null && videoPipeline != null && videoParams != null) {
                var videoResult = videoRc.apply(videoShare(target), plr, sctpBufferedAmount.getAsLong(),
                        nowMs, videoParams);
                videoParams = videoResult.params();
                videoPipeline.modifyCodec(videoParams);
                lastVideoTargetBitrateBps = videoParams.targetBitrate();
            }
        }

        /**
         * Returns the video target bitrate, in bits per second, from the most recent rate control round.
         *
         * <p>The video packetizer reads this each keyframe to stamp the id13 descriptor's bitrate field.
         *
         * @return the latest video target bitrate, or {@code 0} before the first round or on an audio only call
         */
        private int videoTargetBitrateBps() {
            return lastVideoTargetBitrateBps;
        }

        /**
         * Returns the combined sender bandwidth estimate, in bits per second, from the most recent round.
         *
         * <p>The video packetizer reads this each keyframe to stamp the id13 descriptor's estimate field.
         *
         * @return the latest bandwidth estimate, or {@code 0} before the first round
         */
        private int bandwidthEstimateBps() {
            return lastBandwidthEstimateBps;
        }

        /**
         * Folds the machine learning engine's steering into the combined target, or returns it unchanged
         * when the engine is disabled.
         *
         * <p>Builds the round's {@link MlBweSignals} from the measured packet loss, round trip time, remote and
         * sender estimates, and the configured maximum bitrate, runs one
         * {@link MlBweEngine#infer(MlBweSignals)} round, and folds the engine's high definition target delta
         * through the combiner's probing increase. With the engine disabled (the {@link NoopMlBweEngine}) its
         * {@link MlBweOutputs#DISABLED} outputs carry no high definition target, so the additive is zero and
         * the target is returned unchanged.
         *
         * @implNote This builds the signal set with {@link MlBweSignals#ofThreaded(double, long, long, long,
         * long)} from the values the rate loop already measures (the unthreaded measured slots are sentinel
         * marked) and routes the engine's high definition target delta through
         * {@link BitrateCombiner#applyProbingIncrease(long, long)} (the combiner's active probing and machine
         * learning increase). The congestion verdict ({@link MlBweOutputs#congestionDetected()}, the output the
         * native congestion control acts on) is not yet folded into the target: that path drives a
         * {@link MlCongestionPidController} whose proportional, integral, and derivative gains, integral bounds,
         * target level, and minimum factor are runtime voip params absent from the {@code voip_settings}, and
         * the ramp down minimum time between thresholds are compiled defaults not statically known (the same
         * class as {@link #MIN_BITRATE_BPS}). With the engine disabled the verdict is always {@code false}, so
         * the gap has no runtime effect today; see the {@code TODO} below.
         *
         * @param targetBps    the combined gated target before machine learning steering, in bits per second
         * @param plr          the packet loss ratio for this round, in {@code [0, 1]}
         * @param rttNs        the round trip time for this round, in nanoseconds, or {@code 0} when none
         * @param remoteBweBps the remote receiver estimate for this round, in bits per second, or {@code 0}
         * @return the target after steering, in bits per second
         */
        private long applyMlSteering(long targetBps, double plr, long rttNs, long remoteBweBps) {
            if (!mlEngine.isEnabled()) {
                return targetBps;
            }
            var signals = MlBweSignals.ofThreaded(plr, rttNs, remoteBweBps, targetBps, maxBitrateBps);
            var outputs = mlEngine.infer(signals);
            // TODO: fold the congestion verdict (outputs.congestionDetected(), congestionProbability()) into
            //  the target through MlCongestionPidController.computeFactor and the BWE update mask bits (0x800
            //  stop mid call probing, 0x1000 and 0x2000) with the minimum time between ramp downs guard. The
            //  PID gains (kp, ki, kd), integral bounds, target level, and minimum factor are runtime
            //  voip params absent from the voip_settings, and the ramp down thresholds are compiled defaults
            //  not statically known. The engine is always a NoopMlBweEngine (the native inference backend was
            //  removed), so the verdict is always false and this is inert.
            var additive = outputs.hdTargetBps() > targetBps ? outputs.hdTargetBps() - targetBps : 0;
            return senderBwe.combiner().applyProbingIncrease(targetBps, additive);
        }

        /**
         * Returns the audio stream's share of the combined target, in bits per second.
         *
         * <p>On an audio only call the audio stream takes the full target. On a video call the audio stream
         * is reserved up to its codec ceiling so voice keeps priority, and the video stream takes the
         * remainder; the audio rate controller clamps this to the Opus bitrate window regardless.
         *
         * @param targetBps the combined gated target, in bits per second
         * @return the audio stream's share, in bits per second
         */
        // TODO: reserve the dynamically selected audio bitrate for the audio stream (video takes the
        //  remainder, down to the minimum video stream reserve floor) instead of reserving the audio codec
        //  ceiling. The split is simple (audio keeps its reserve, video takes the rest) and the matcher is
        //  first match wins over the video dynamic rule table, but the audio reserve is dynamically selected:
        //  the target band rules (yielding 12500, 15000, 20000, or 25000 bps) are evaluable here, yet the
        //  higher priority congestion and latency rules (yielding 30000 or 40000) gate on EMA smoothed short
        //  and long term round trip times whose window sizes are compiled defaults absent from the negotiation
        //  and not statically known. Because the long term round trip rule is not congestion gated, even the
        //  common case cannot be cleanly separated, so the codec ceiling stand in remains.
        private long audioShare(long targetBps) {
            if (!video) {
                return targetBps;
            }
            return Math.min(targetBps, audioParams.maxBitrate());
        }

        /**
         * Returns the video stream's share of the combined target, in bits per second.
         *
         * <p>The remainder of the target after the audio share is reserved, never negative; the video rate
         * controller clamps it to the video codec window.
         *
         * @param targetBps the combined gated target, in bits per second
         * @return the video stream's share, in bits per second
         */
        private long videoShare(long targetBps) {
            return Math.max(0, targetBps - audioShare(targetBps));
        }

        /**
         * Runs the periodic fallback tick until the loop stops.
         *
         * <p>Each turn applies the matched dynamic voip param overrides under the manager's round guard and,
         * when the RTCP feedback stream has gone stale, re runs one rate control round on the last known loss
         * and round trip so the encoder is not frozen on a target the link can no longer carry. The loop
         * parks one {@link #FALLBACK_TICK_NANOS} period between turns and exits on interrupt.
         */
        private void fallbackLoop() {
            while (running.get()) {
                try {
                    fallbackTick();
                } catch (RuntimeException exception) {
                    if (Log.DEBUG) {
                        LOGGER.log(Level.DEBUG,
                                "calls rate-control fallback tick failed for call {0}: {1}", callId,
                                exception.getMessage());
                    }
                }
                java.util.concurrent.locks.LockSupport.parkNanos(FALLBACK_TICK_NANOS);
                if (Thread.interrupted()) {
                    break;
                }
            }
        }

        /**
         * Applies one periodic fallback tick: the dynamic rule pass and the staleness re evaluation.
         *
         * <p>The dynamic rule pass runs every tick under the manager's
         * {@link com.github.auties00.cobalt.calls.config.param.DynVoipParamUpdater} round guard; the staleness
         * re run fires only when feedback has been seen at least once and the last feedback is older than
         * {@link #FEEDBACK_STALENESS_MS}.
         *
         * @implNote The matched dynamic overrides are passed empty because the dynamic rule condition
         * catalogue that selects which rules apply is not yet modelled, so the guarded pass runs with no
         * matched rules; the call is kept so the round guard and the store, select, count override, dynamic
         * tick order are exercised and the pass is wired the moment the catalogue lands. The staleness re run
         * drives one round with the last feedback's loss and round trip and a dropped remote estimate; the
         * {@code no RTCP in window} multiplicative decrease is approximated this way because
         * {@link LiveSenderBandwidthEstimator#onFeedback} hardcodes the sender AIMD's no feedback elapsed
         * input, so the wrapper's staleness branch is not directly exercised.
         */
        // TODO: pass the matched dynamic overrides once the dynamic rule condition catalogue is modelled; the
        //  guarded pass runs empty for now to preserve the lifecycle order.
        // TODO: drive the exact no RTCP multiplicative decrease through the sender AIMD's no feedback elapsed
        //  input once a feedback age seam exists on LiveSenderBandwidthEstimator; the staleness re run
        //  approximates it on the last known state.
        private void fallbackTick() {
            lock.lock();
            try {
                voipParamManager.applyDynamicRules(List.of());
                if (lastFeedbackMs < 0) {
                    return;
                }
                var nowMs = System.nanoTime() / 1_000_000L;
                if (nowMs - lastFeedbackMs > FEEDBACK_STALENESS_MS) {
                    runTick(lastPlr, lastRttNs, 0, nowMs);
                }
            } finally {
                lock.unlock();
            }
        }
    }
}
