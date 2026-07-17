package com.github.auties00.cobalt.calls.engine.participant;

import com.github.auties00.cobalt.calls.platform.VoipCryptoNative;
import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Generates the deterministic media SSRCs every endpoint of a call agrees on for a participant device.
 *
 * <p>WhatsApp never negotiates RTP SSRCs on the wire. Each endpoint independently computes the SSRC set
 * for every participant device from values both sides already share, so a sender stamps the exact SSRC
 * its peer has registered a receive context for in advance; a stream arriving on any other SSRC is
 * dropped as an unknown source and never decoded. The set per device covers the audio and the two
 * simulcast video streams (each with a primary, an FEC, and an out of band NACK companion), the screen
 * share stream, the hop by hop FEC streams, the AppData stream, and the IMU data stream.
 *
 * <p>Each SSRC is the little endian interpretation of a four byte HKDF SHA256 expansion keyed by the
 * shared {@code call-id}, salted by the four byte media type code, and contexted by the device
 * identifier:
 *
 * {@snippet :
 *   okm  = HKDF-SHA256(ikm  = callId,                 // the call-id ASCII string from the offer
 *                      salt = u32le(mediaType),       // audio 0/1/4, video 2/3/5, appdata 6, hbh-fec 7/8, imu 10
 *                      info = "<lid>:<device>@lid"    // plus "_<streamId>" when streamId > 0
 *                              + ("_" + streamId),
 *                      L    = 4)
 *   ssrc = okm[0] | okm[1]<<8 | okm[2]<<16 | okm[3]<<24
 * }
 *
 * <p>The {@code call-id} is the shared identifier carried on the wire in the offer, reproducible from
 * the call signaling alone; it is not the secret call key. The device identifier is the same
 * {@code <lid>:<device>@lid} string that keys the device's media SRTP. Every method is stateless and
 * thread safe.
 *
 * @implNote This implementation salts the HKDF with the little endian four byte media type code and
 *           reads the four byte output little endian. A media type code of zero salts with four zero
 *           bytes, which HMAC SHA256 zero pads identically to the RFC 5869 default with no salt. The
 *           HKDF is computed through {@link VoipCryptoNative}.
 */
public final class CallSecureSsrcGenerator {
    /**
     * The logger for {@link CallSecureSsrcGenerator}.
     */
    private static final System.Logger LOGGER = Log.get(CallSecureSsrcGenerator.class);

    /**
     * Holds the media type code of a receive (incoming) primary stream.
     */
    public static final int MEDIA_TYPE_RX = 0;

    /**
     * Holds the media type code of a receive (incoming) FEC stream.
     */
    public static final int MEDIA_TYPE_RX_FEC = 1;

    /**
     * Holds the media type code of a receive (incoming) out of band NACK stream.
     */
    public static final int MEDIA_TYPE_RX_OOB_NACK = 4;

    /**
     * Holds the media type code of a transmit (outgoing) primary stream.
     */
    public static final int MEDIA_TYPE_TX = 2;

    /**
     * Holds the media type code of a transmit (outgoing) FEC stream.
     */
    public static final int MEDIA_TYPE_TX_FEC = 3;

    /**
     * Holds the media type code of a transmit (outgoing) out of band NACK stream.
     */
    public static final int MEDIA_TYPE_TX_OOB_NACK = 5;

    /**
     * Holds the media type code of the AppData stream.
     */
    public static final int MEDIA_TYPE_APP_DATA = 6;

    /**
     * Holds the media type code of the transmit hop by hop FEC stream.
     */
    public static final int MEDIA_TYPE_HBH_FEC_TX = 7;

    /**
     * Holds the media type code of the receive hop by hop FEC stream.
     */
    public static final int MEDIA_TYPE_HBH_FEC_RX = 8;

    /**
     * Holds the media type code of the IMU data stream.
     */
    public static final int MEDIA_TYPE_IMU_DATA = 10;

    /**
     * Holds the number of simulcast video streams per device.
     */
    public static final int VIDEO_STREAM_COUNT = 2;

    /**
     * Holds the stream id of the audio stream and the first video stream, which is omitted from the
     * identifier (no {@code "_<id>"} suffix).
     */
    private static final int FIRST_STREAM_ID = 0;

    /**
     * Holds the byte length of one HKDF SHA256 SSRC expansion.
     */
    private static final int SSRC_BYTES = 4;

    /**
     * Prevents instantiation of this stateless generator holder.
     */
    private CallSecureSsrcGenerator() {
        throw new AssertionError("CallSecureSsrcGenerator is not instantiable");
    }

    /**
     * Generates one SSRC for a participant device, media type code, and stream id.
     *
     * <p>The HKDF input keying material is the {@code call-id}, the salt is the little endian four byte
     * media type code, and the info is the device JID for stream id {@code 0} or the device JID followed
     * by {@code "_<streamId>"} for any higher stream id. The four byte output is read little endian.
     *
     * @param callId     the {@code call-id} string from the offer (the shared identifier carried on the wire)
     * @param deviceJid  the participant device the stream belongs to
     * @param mediaType  the media type code (for example {@link #MEDIA_TYPE_RX} or {@link #MEDIA_TYPE_TX})
     * @param streamId   the per kind stream index, omitted from the identifier when {@code 0}
     * @return the 32 bit SSRC
     * @throws NullPointerException       if {@code callId} or {@code deviceJid} is {@code null}
     * @throws WhatsAppCallException.Srtp if the HKDF computation fails
     */
    public static int ssrc(String callId, Jid deviceJid, int mediaType, int streamId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(deviceJid, "deviceJid cannot be null");
        // The identifier is always "<user>:<device>@<server>" with the device present even for the
        // primary device 0, whereas Jid.toString() omits ":0" for the primary; build it explicitly so a
        // device 0 participant derives the same SSRC the peer registers in advance. Call participant JIDs
        // are agentless LID device JIDs, so no agent component appears.
        var base = deviceJid.user() + ":" + deviceJid.device() + "@" + deviceJid.server();
        var identifier = streamId > 0 ? base + "_" + streamId : base;
        var salt = new byte[]{
                (byte) mediaType, (byte) (mediaType >>> 8),
                (byte) (mediaType >>> 16), (byte) (mediaType >>> 24)};
        var okm = VoipCryptoNative.hkdfSha256(
                callId.getBytes(StandardCharsets.US_ASCII),
                salt,
                identifier.getBytes(StandardCharsets.US_ASCII),
                SSRC_BYTES);
        var ssrc = (okm[0] & 0xff)
                | (okm[1] & 0xff) << 8
                | (okm[2] & 0xff) << 16
                | (okm[3] & 0xff) << 24;
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "derived ssrc for device {0} mediaType={1} streamId={2}",
                    deviceJid, mediaType, streamId);
        }
        return ssrc;
    }

    /**
     * Generates the {primary, FEC, out of band NACK} SSRC triple of a participant device's audio stream.
     *
     * <p>The three SSRCs are derived with media type codes {@link #MEDIA_TYPE_RX},
     * {@link #MEDIA_TYPE_RX_FEC}, and {@link #MEDIA_TYPE_RX_OOB_NACK}. A device's audio stream uses this
     * {@code 0}/{@code 1}/{@code 4} code family, while a device's video streams use the
     * {@code 2}/{@code 3}/{@code 5} family of {@link #videoTriple(String, Jid, int)}; a 1:1
     * audio call only ever sends the audio primary.
     *
     * @param callId    the {@code call-id} string from the offer
     * @param deviceJid the participant device whose audio triple is generated
     * @return the audio SSRC triple
     * @throws NullPointerException       if {@code callId} or {@code deviceJid} is {@code null}
     * @throws WhatsAppCallException.Srtp if an HKDF computation fails
     */
    public static SsrcTriple audioTriple(String callId, Jid deviceJid) {
        return new SsrcTriple(
                ssrc(callId, deviceJid, MEDIA_TYPE_RX, FIRST_STREAM_ID),
                ssrc(callId, deviceJid, MEDIA_TYPE_RX_FEC, FIRST_STREAM_ID),
                ssrc(callId, deviceJid, MEDIA_TYPE_RX_OOB_NACK, FIRST_STREAM_ID));
    }

    /**
     * Generates the {primary, FEC, out of band NACK} SSRC triple of one of a participant device's two
     * simulcast video streams.
     *
     * <p>The three SSRCs are derived with media type codes {@link #MEDIA_TYPE_TX},
     * {@link #MEDIA_TYPE_TX_FEC}, and {@link #MEDIA_TYPE_TX_OOB_NACK} for the given video stream id. The
     * second simulcast stream (stream id {@code 1}) carries a {@code "_1"} identifier suffix, so its
     * SSRCs differ from the first stream's even though both use the same code family.
     *
     * @param callId        the {@code call-id} string from the offer
     * @param deviceJid     the participant device whose video triple is generated
     * @param videoStreamId the simulcast video stream index, in {@code [0, VIDEO_STREAM_COUNT)}
     * @return the video SSRC triple
     * @throws NullPointerException       if {@code callId} or {@code deviceJid} is {@code null}
     * @throws WhatsAppCallException.Srtp if an HKDF computation fails
     */
    public static SsrcTriple videoTriple(String callId, Jid deviceJid, int videoStreamId) {
        return new SsrcTriple(
                ssrc(callId, deviceJid, MEDIA_TYPE_TX, videoStreamId),
                ssrc(callId, deviceJid, MEDIA_TYPE_TX_FEC, videoStreamId),
                ssrc(callId, deviceJid, MEDIA_TYPE_TX_OOB_NACK, videoStreamId));
    }

    /**
     * Generates the audio main stream SSRC a participant device transmits on.
     *
     * <p>This is the SSRC the peer registers a receive context for, so the local sender stamps it onto
     * its outbound audio RTP and declares it in the relay stream descriptor publish. A 1:1 audio call
     * only ever sends this stream. It is the primary of {@link #audioTriple(String, Jid)}.
     *
     * @param callId    the {@code call-id} string from the offer
     * @param deviceJid the transmitting participant device
     * @return the 32 bit audio main SSRC
     * @throws NullPointerException       if {@code callId} or {@code deviceJid} is {@code null}
     * @throws WhatsAppCallException.Srtp if the HKDF computation fails
     */
    public static int audioMainSsrc(String callId, Jid deviceJid) {
        return ssrc(callId, deviceJid, MEDIA_TYPE_RX, FIRST_STREAM_ID);
    }

    /**
     * Generates the AppData stream SSRC for a participant device.
     *
     * @param callId    the {@code call-id} string from the offer
     * @param deviceJid the participant device whose AppData SSRC is generated
     * @return the 32 bit AppData SSRC
     * @throws NullPointerException       if {@code callId} or {@code deviceJid} is {@code null}
     * @throws WhatsAppCallException.Srtp if the HKDF computation fails
     */
    public static int appDataSsrc(String callId, Jid deviceJid) {
        return ssrc(callId, deviceJid, MEDIA_TYPE_APP_DATA, FIRST_STREAM_ID);
    }

    /**
     * Generates the IMU data stream SSRC for a participant device.
     *
     * @param callId    the {@code call-id} string from the offer
     * @param deviceJid the participant device whose IMU data SSRC is generated
     * @return the 32 bit IMU data SSRC
     * @throws NullPointerException       if {@code callId} or {@code deviceJid} is {@code null}
     * @throws WhatsAppCallException.Srtp if the HKDF computation fails
     */
    public static int imuDataSsrc(String callId, Jid deviceJid) {
        return ssrc(callId, deviceJid, MEDIA_TYPE_IMU_DATA, FIRST_STREAM_ID);
    }

    /**
     * Holds the {primary, FEC, out of band NACK} SSRC triple of one logical stream.
     *
     * <p>Every logical media stream is forwarded as up to three RTP streams that share one identifier:
     * the primary payload, its FEC repair stream, and its out of band NACK retransmission stream. The
     * three SSRCs are derived from the same device identifier and stream id but with distinct media type
     * codes.
     *
     * @param primary the primary stream SSRC
     * @param fec     the FEC repair stream SSRC
     * @param oobNack the out of band NACK retransmission stream SSRC
     */
    public record SsrcTriple(int primary, int fec, int oobNack) {
    }
}
