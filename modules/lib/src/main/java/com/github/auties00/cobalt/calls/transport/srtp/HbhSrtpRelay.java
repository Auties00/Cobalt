package com.github.auties00.cobalt.calls.transport.srtp;

import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SrtpAfbStreams;
import com.github.auties00.cobalt.calls.transport.subscription.RtcpRxSubscriptionTable;

/**
 * Protects and unprotects the media and control packets on the single hop by hop leg between this
 * client and the relay or SFU.
 *
 * <p>The media is keyed by the relay block rather than by exported DTLS keying material: the relay block
 * hands every device one shared hop by hop credential, and both peers of a one to one call and every
 * participant of a group call derive the same hop by hop SRTP master from it. A relay context is therefore
 * symmetric around one master per direction: outbound RTP and RTCP are encrypted toward the relay, and
 * inbound RTP and RTCP from the relay are decrypted, all under keys derived from that one credential by
 * {@link SfuKeyDeriver} and {@code CallE2eKeyDerivation}. The protected RTP and RTCP then travel as SCTP
 * DATA over the call's one SCTP data channel wrapped in DTLS; the DTLS layer protects the SCTP transport,
 * while the media payload stays hop by hop SRTP rather than {@code DTLS-SRTP}. This is distinct from the
 * end to end SFrame layer that the relay never sees; the relay only needs the hop by hop context to route
 * the still opaque media.
 *
 * <p>A context is created once per relay election from the derived hop by hop SRTP master and the
 * chosen {@link SrtpCryptoSuite}; it is then used for the lifetime of that relay leg and
 * {@linkplain #close() closed} on teardown or failover. The protect operations grow the packet in
 * place with the SRTP authentication tag, so the caller passes a buffer with trailing room; the
 * unprotect operations shrink it back. Beyond packet crypto a context tracks two pieces of hop by hop
 * feedback state: the per stream SRTP authenticated feedback indices published in
 * {@link SrtpAfbStreams}, and the fixed capacity {@link RtcpRxSubscriptionTable} of RTCP feedback
 * subscriptions that tells the SFU which feedback to forward.
 *
 * <p>The production implementation is {@link LiveHbhSrtpRelay}, backed by the libsrtp binding.
 * Implementations are safe for use from the single transport thread that owns the relay leg; they are
 * not required to be safe for concurrent protect and unprotect from different threads.
 *
 * @implSpec An implementation MUST key its outbound and inbound contexts from the same hop by hop
 *           master and salt, because the relay forwards on one hop by hop context; it MUST grow an
 *           outbound packet by exactly the suite's authentication tag length and reject an inbound
 *           packet whose tag fails authentication; and it MUST release every native resource it holds
 *           on {@link #close()}, after which any further protect or unprotect call throws.
 */
public interface HbhSrtpRelay extends AutoCloseable {
    /**
     * Encrypts an RTP packet in place toward the relay, appending the SRTP authentication tag.
     *
     * <p>The packet occupies the first {@code length} bytes of {@code packet}; on return the protected
     * packet occupies the first bytes of the returned length, which is {@code length} grown by the
     * suite's authentication tag length. The buffer MUST have at least that many trailing bytes free.
     *
     * @param packet the buffer holding the cleartext RTP packet, with trailing room for the tag
     * @param length the length, in bytes, of the cleartext RTP packet
     * @return the length, in bytes, of the protected RTP packet
     * @throws NullPointerException       if {@code packet} is {@code null}
     * @throws IllegalArgumentException   if {@code length} is negative or exceeds {@code packet.length}
     * @throws IllegalStateException      if the context has been closed
     * @throws WhatsAppCallException.Srtp if the SRTP protect operation fails
     * @implSpec An implementation MUST NOT mutate {@code packet} beyond the returned length and MUST
     *           leave the first {@code length} bytes' header intact, since SRTP encrypts only the payload.
     */
    int protectRtp(byte[] packet, int length);

    /**
     * Decrypts an RTP packet in place that arrived from the relay, verifying and stripping the SRTP
     * authentication tag.
     *
     * <p>The protected packet occupies the first {@code length} bytes of {@code packet}; on return the
     * cleartext packet occupies the first bytes of the returned length, which is {@code length} shrunk
     * by the suite's authentication tag length.
     *
     * @param packet the buffer holding the protected RTP packet
     * @param length the length, in bytes, of the protected RTP packet
     * @return the length, in bytes, of the cleartext RTP packet
     * @throws NullPointerException       if {@code packet} is {@code null}
     * @throws IllegalArgumentException   if {@code length} is negative or exceeds {@code packet.length}
     * @throws IllegalStateException      if the context has been closed
     * @throws WhatsAppCallException.Srtp if authentication fails or the SRTP unprotect operation fails
     * @implSpec An implementation MUST reject a packet whose authentication tag does not verify and
     *           MUST reject a replayed packet, throwing rather than returning cleartext.
     */
    int unprotectRtp(byte[] packet, int length);

    /**
     * Encrypts an RTCP packet in place toward the relay, appending the SRTCP authentication tag and
     * index.
     *
     * @param packet the buffer holding the cleartext RTCP packet, with trailing room for the trailer
     * @param length the length, in bytes, of the cleartext RTCP packet
     * @return the length, in bytes, of the protected RTCP packet
     * @throws NullPointerException       if {@code packet} is {@code null}
     * @throws IllegalArgumentException   if {@code length} is negative or exceeds {@code packet.length}
     * @throws IllegalStateException      if the context has been closed
     * @throws WhatsAppCallException.Srtp if the SRTCP protect operation fails
     */
    int protectRtcp(byte[] packet, int length);

    /**
     * Decrypts an RTCP packet in place that arrived from the relay, verifying and stripping the SRTCP
     * authentication tag and index.
     *
     * @param packet the buffer holding the protected RTCP packet
     * @param length the length, in bytes, of the protected RTCP packet
     * @return the length, in bytes, of the cleartext RTCP packet
     * @throws NullPointerException       if {@code packet} is {@code null}
     * @throws IllegalArgumentException   if {@code length} is negative or exceeds {@code packet.length}
     * @throws IllegalStateException      if the context has been closed
     * @throws WhatsAppCallException.Srtp if authentication fails or the SRTCP unprotect operation fails
     */
    int unprotectRtcp(byte[] packet, int length);

    /**
     * Returns the RTCP feedback subscription table that tells the SFU which feedback to forward for
     * this relay leg.
     *
     * <p>The table is the live state a subscription publisher reads when it serializes the hop by hop
     * feedback subscriptions; mutating the returned table changes what the next subscription publishes.
     *
     * @return the relay leg's RTCP feedback subscription table; never {@code null}
     * @throws IllegalStateException if the context has been closed
     */
    RtcpRxSubscriptionTable rtcpFeedbackSubscriptions();

    /**
     * Returns the per stream SRTP authenticated feedback indices for this relay leg as the model
     * {@link SrtpAfbStreams} report.
     *
     * <p>The report is a point in time snapshot of the highest RTP and RTCP indices the context has
     * observed per tracked SSRC; it is the body of the SRTP authenticated feedback control message and
     * the value attached to a WARP {@code SRTP-AFB} attribute.
     *
     * @return the authenticated feedback report for every tracked stream; never {@code null}
     * @throws IllegalStateException if the context has been closed
     */
    SrtpAfbStreams srtpAfbStreams();

    /**
     * Releases the native SRTP contexts and feedback state this relay leg holds.
     *
     * <p>After this call any protect, unprotect, or accessor throws {@link IllegalStateException}.
     * Closing an already closed context has no effect.
     *
     * @implSpec An implementation MUST be idempotent and MUST free every native resource it allocated.
     */
    @Override
    void close();
}
