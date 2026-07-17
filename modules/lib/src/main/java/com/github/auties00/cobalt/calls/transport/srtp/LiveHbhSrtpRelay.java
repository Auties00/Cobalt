package com.github.auties00.cobalt.calls.transport.srtp;

import com.github.auties00.cobalt.calls.crypto.CallE2eKeyDerivation;
import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SrtpAfbStreams;
import com.github.auties00.srtp.SrtpErrorStatus;
import com.github.auties00.srtp.SrtpPacket;
import com.github.auties00.srtp.SrtpTransformer;

import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.Objects;
import com.github.auties00.cobalt.calls.transport.subscription.RtcpRxSubscriptionTable;

/**
 * Implements {@link HbhSrtpRelay} over the pure Java {@code com.github.auties00:srtp} library, creating
 * separate outbound and inbound SRTP transformers for RTP media and for RTCP control, keyed from the derived
 * hop by hop media master and protecting and unprotecting packets in place.
 *
 * <p>The relay leg is symmetric: this client keys an outbound transformer that encrypts everything it sends to
 * the relay and an inbound transformer that decrypts everything it receives, each from a
 * {@value SrtpCryptoSuite#SUITE_MASTER_LENGTH} byte master (a 16 byte key followed by a 14 byte salt) derived
 * from the relay {@code <hbh_key>}. Both RTP media and RTCP control are keyed from the
 * {@link CallE2eKeyDerivation.HopByHopGroup#MEDIA} master; unique per source SSRCs keep the two keystreams
 * apart. Four transformers are created in total: an outbound and an inbound transformer for RTP media, and an
 * outbound and an inbound transformer for RTCP control. Protect and unprotect are kept on separate transformer
 * instances so each direction owns an independent rollover counter and replay window; a single key set covers
 * every stream multiplexed on the leg because each transformer creates a per source context on first use.
 *
 * <p>Each transform runs in place on the caller's packet array through a {@link SrtpPacket} view: protection
 * grows the packet by the authentication tag (the caller must size the array with that headroom) and
 * unprotection shrinks it. A per packet failure such as a bad tag or a replay is raised as a
 * {@link WhatsAppCallException.Srtp} so the caller drops the packet. An instance is used from the single
 * transport thread that owns the relay leg.
 */
public final class LiveHbhSrtpRelay implements HbhSrtpRelay {
    /**
     * The logger for {@link LiveHbhSrtpRelay}.
     */
    private static final System.Logger LOGGER = Log.get(LiveHbhSrtpRelay.class);

    /**
     * Length, in bytes, of the {@code AES-128} master key at the front of each
     * {@value SrtpCryptoSuite#SUITE_MASTER_LENGTH} byte hop by hop master.
     */
    private static final int MASTER_KEY_LENGTH = 16;

    /**
     * Holds the outbound transformer that encrypts RTP media packets toward the relay, keyed from the
     * {@link CallE2eKeyDerivation.HopByHopGroup#MEDIA} master.
     */
    private final SrtpTransformer rtpOutbound;

    /**
     * Holds the inbound transformer that decrypts RTP media packets from the relay, keyed from the
     * {@link CallE2eKeyDerivation.HopByHopGroup#MEDIA} master.
     */
    private final SrtpTransformer rtpInbound;

    /**
     * Holds the outbound transformer that encrypts RTCP control packets toward the relay, keyed from the
     * {@link CallE2eKeyDerivation.HopByHopGroup#MEDIA} master.
     */
    private final SrtpTransformer rtcpOutbound;

    /**
     * Holds the inbound transformer that decrypts RTCP control packets from the relay, keyed from the
     * {@link CallE2eKeyDerivation.HopByHopGroup#MEDIA} master.
     */
    private final SrtpTransformer rtcpInbound;

    /**
     * Holds the RTCP feedback subscription table for this relay leg.
     */
    private final RtcpRxSubscriptionTable rtcpFeedbackSubscriptions;

    /**
     * Holds the per stream SRTP authenticated feedback index tracker for this relay leg.
     */
    private final SrtpAfbStreamTracker afbStreamTracker;

    /**
     * Tracks whether this relay leg has been closed.
     */
    private boolean closed;

    /**
     * Creates a hop by hop relay context for the elected relay from its decoded {@code <hbh_key>} and a
     * crypto suite.
     *
     * <p>This is the convenience entry point for relay election: it derives the non directional hop by hop
     * media SRTP master from the relay {@code <hbh_key>} through
     * {@link CallE2eKeyDerivation#deriveHbhSrtpMaster(byte[], CallE2eKeyDerivation.HopByHopGroup)} and then
     * builds the transformers. The relay leg keys both RTP and RTCP from that one media master, separated by
     * SSRC. The {@code <hbh_key>} is the 30 byte value the relay block carried, base64 decoded by the signaling
     * layer, shared identically across every participant of a group call.
     *
     * @param hopByHopKey the {@value CallE2eKeyDerivation#HBH_KEY_LENGTH} byte decoded relay {@code <hbh_key>}
     * @param suite       the SRTP crypto suite to apply
     * @return a relay context keyed for the leg
     * @throws NullPointerException     if {@code hopByHopKey} or {@code suite} is {@code null}
     * @throws IllegalArgumentException if {@code hopByHopKey} is not exactly the decoded hop by hop key length
     */
    public static LiveHbhSrtpRelay fromHopByHopKey(byte[] hopByHopKey, SrtpCryptoSuite suite) {
        var mediaMaster = CallE2eKeyDerivation.deriveHbhSrtpMaster(hopByHopKey, CallE2eKeyDerivation.HopByHopGroup.MEDIA);
        return new LiveHbhSrtpRelay(mediaMaster, suite);
    }

    /**
     * Creates a hop by hop relay context from the derived hop by hop media SRTP master and a crypto suite.
     *
     * <p>The master is split into its 16 byte key and 14 byte salt. Four transformers are created over it: an
     * outbound and an inbound transformer for RTP media and an outbound and an inbound transformer for RTCP
     * control. The relay media context keys both RTP and RTCP from this one media master; unique per source
     * SSRCs separate the keystreams.
     *
     * @param mediaMaster the {@value SrtpCryptoSuite#SUITE_MASTER_LENGTH} byte hop by hop media SRTP master
     *                    keying RTP and RTCP, from {@link CallE2eKeyDerivation.HopByHopGroup#MEDIA}
     * @param suite       the SRTP crypto suite to apply
     * @throws NullPointerException     if {@code mediaMaster} or {@code suite} is {@code null}
     * @throws IllegalArgumentException if {@code mediaMaster} is not exactly
     *                                  {@value SrtpCryptoSuite#SUITE_MASTER_LENGTH} bytes long
     */
    public LiveHbhSrtpRelay(byte[] mediaMaster, SrtpCryptoSuite suite) {
        Objects.requireNonNull(mediaMaster, "mediaMaster cannot be null");
        Objects.requireNonNull(suite, "suite cannot be null");
        if (mediaMaster.length != SrtpCryptoSuite.SUITE_MASTER_LENGTH) {
            throw new IllegalArgumentException(
                    "mediaMaster must be " + SrtpCryptoSuite.SUITE_MASTER_LENGTH + " bytes, got " + mediaMaster.length);
        }
        this.rtcpFeedbackSubscriptions = new RtcpRxSubscriptionTable();
        this.afbStreamTracker = new SrtpAfbStreamTracker();
        var libSuite = toLibrarySuite(suite);
        var mediaKey = Arrays.copyOfRange(mediaMaster, 0, MASTER_KEY_LENGTH);
        var mediaSalt = Arrays.copyOfRange(mediaMaster, MASTER_KEY_LENGTH, SrtpCryptoSuite.SUITE_MASTER_LENGTH);
        SrtpTransformer rtpOut = null;
        SrtpTransformer rtpIn = null;
        SrtpTransformer rtcpOut = null;
        try {
            rtpOut = new SrtpTransformer(mediaKey, mediaSalt, libSuite);
            this.rtpOutbound = rtpOut;
            rtpIn = new SrtpTransformer(mediaKey, mediaSalt, libSuite);
            this.rtpInbound = rtpIn;
            rtcpOut = new SrtpTransformer(mediaKey, mediaSalt, libSuite);
            this.rtcpOutbound = rtcpOut;
            this.rtcpInbound = new SrtpTransformer(mediaKey, mediaSalt, libSuite);
        } catch (RuntimeException e) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "hbh srtp relay leg creation failed, suite=" + suite, e);
            }
            closeQuietly(rtpOut);
            closeQuietly(rtpIn);
            closeQuietly(rtcpOut);
            throw e;
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "hbh srtp relay leg created, suite={0}", suite);
        }
    }

    @Override
    public int protectRtp(byte[] packet, int length) {
        var view = wrap(packet, length);
        ensureOk(rtpOutbound.protectRtp(view));
        return view.length();
    }

    @Override
    public int unprotectRtp(byte[] packet, int length) {
        var view = wrap(packet, length);
        ensureOk(rtpInbound.unprotectRtp(view));
        return view.length();
    }

    @Override
    public int protectRtcp(byte[] packet, int length) {
        var view = wrap(packet, length);
        ensureOk(rtcpOutbound.protectRtcp(view));
        return view.length();
    }

    @Override
    public int unprotectRtcp(byte[] packet, int length) {
        var view = wrap(packet, length);
        ensureOk(rtcpInbound.unprotectRtcp(view));
        return view.length();
    }

    @Override
    public RtcpRxSubscriptionTable rtcpFeedbackSubscriptions() {
        requireOpen();
        return rtcpFeedbackSubscriptions;
    }

    @Override
    public SrtpAfbStreams srtpAfbStreams() {
        requireOpen();
        return afbStreamTracker.toReport();
    }

    /**
     * Returns the per stream SRTP authenticated feedback index tracker for this relay leg.
     *
     * <p>This exposes the mutable tracker so the transport advances the RTP and SRTCP watermarks as it
     * protects and unprotects packets; {@link #srtpAfbStreams()} renders its snapshot.
     *
     * @return the relay leg's authenticated feedback tracker; never {@code null}
     * @throws IllegalStateException if the context has been closed
     */
    public SrtpAfbStreamTracker afbStreamTracker() {
        requireOpen();
        return afbStreamTracker;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "hbh srtp relay leg closed");
        }
        closeQuietly(rtpOutbound);
        closeQuietly(rtpInbound);
        closeQuietly(rtcpOutbound);
        closeQuietly(rtcpInbound);
    }

    /**
     * Validates a heap packet and wraps its first {@code length} bytes in an in place {@link SrtpPacket} view.
     *
     * <p>Protection needs the backing array to carry the authentication tag as spare bytes after the packet;
     * the {@link SrtpPacket} enforces that headroom when protection runs.
     *
     * @param packet the heap buffer holding the packet, with trailing room on protect
     * @param length the length, in bytes, of the input packet
     * @return a mutable view over {@code packet[0..length)}
     * @throws NullPointerException     if {@code packet} is {@code null}
     * @throws IllegalArgumentException if {@code length} is negative or exceeds {@code packet.length}
     * @throws IllegalStateException    if the context has been closed
     */
    private SrtpPacket wrap(byte[] packet, int length) {
        Objects.requireNonNull(packet, "packet cannot be null");
        requireOpen();
        if (length < 0 || length > packet.length) {
            throw new IllegalArgumentException("length out of bounds: " + length + " for buffer " + packet.length);
        }
        return new SrtpPacket(packet, 0, length);
    }

    /**
     * Throws when an SRTP transform did not succeed.
     *
     * @param status the transform outcome
     * @throws WhatsAppCallException.Srtp if {@code status} is not {@link SrtpErrorStatus#OK}
     */
    private static void ensureOk(SrtpErrorStatus status) {
        if (status != SrtpErrorStatus.OK) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "srtp transform failed, status={0}", status);
            }
            throw new WhatsAppCallException.Srtp("SRTP transform failed with status " + status);
        }
    }

    /**
     * Verifies this relay leg has not been closed.
     *
     * @throws IllegalStateException if the context has been closed
     */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("HbhSrtpRelay has been closed");
        }
    }

    /**
     * Closes a transformer if it was created, swallowing a {@code null}.
     *
     * @param transformer the transformer to close, or {@code null}
     */
    private static void closeQuietly(SrtpTransformer transformer) {
        if (transformer != null) {
            transformer.close();
        }
    }

    /**
     * Maps the relay's {@link SrtpCryptoSuite} onto the SRTP library's suite.
     *
     * @param suite the relay crypto suite
     * @return the matching {@link com.github.auties00.srtp.SrtpCryptoSuite}
     */
    private static com.github.auties00.srtp.SrtpCryptoSuite toLibrarySuite(SrtpCryptoSuite suite) {
        return switch (suite) {
            case AES_CM_128_HMAC_SHA1_80 -> com.github.auties00.srtp.SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_80;
            case AES_CM_128_HMAC_SHA1_32 -> com.github.auties00.srtp.SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_32;
        };
    }
}
