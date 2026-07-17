package com.github.auties00.cobalt.calls.signaling.session;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.CallMessages;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents an {@code <offer_notice>} signal: the server's notice, delivered after the fact, that a
 * call was offered to this device while it was offline.
 *
 * <p>Unlike an {@link OfferStanza}, an offer notice does not ring a live call and carries none of the
 * codec, capability, or key material an answerable offer needs. It is a lightweight informational
 * record the server delivers so a companion that missed the offer in real time can still surface the
 * call in its history. It is not a voip engine signaling action: it is acknowledged with the same
 * {@code <ack class="call" type="offer_notice">} the receiver emits for any signal that is not a
 * receipt, but it is handled outside the engine's call lifecycle, so it implements no
 * {@link CallMessage} contract and carries no {@link SignalingType}.
 *
 * <p>On the wire the notice is the {@code <offer_notice>} child of a {@code <call>} envelope:
 * {@snippet lang="xml" :
 * <call id="..." from="<caller>" t="<unix-seconds>">
 *   <offer_notice call-creator="<caller-device>" call-id="..." type="group" media="video"/>
 * </call>
 * }
 * The {@code t} timestamp lives on the {@code <call>} envelope rather than on the child, so the notice
 * is decoded from the envelope through {@link #of(Stanza)}.
 *
 * @param callCreator the caller's device JID, taken from the {@code call-creator} attribute; never
 *                    {@code null}
 * @param callId      the call identifier; never {@code null}
 * @param group       whether the notice describes a group call (the {@code type} attribute equals
 *                    {@code "group"})
 * @param video       whether the notice describes a video call (the {@code media} attribute equals
 *                    {@code "video"})
 * @param offerTime   the instant the call was originally offered, decoded from the envelope {@code t}
 *                    attribute; never {@code null}
 */
@WhatsAppWebModule(moduleName = "WAWebHandleVoipOfferNotice")
public record OfferNoticeStanza(Jid callCreator, String callId, boolean group, boolean video, Instant offerTime) {
    /**
     * The wire element tag for an offer notice signal.
     */
    public static final String ELEMENT = "offer_notice";

    /**
     * The wire attribute naming the call class on the {@code <offer_notice>} child; a value of
     * {@code "group"} marks a group call.
     */
    private static final String TYPE_ATTRIBUTE = "type";

    /**
     * The wire attribute naming the call media on the {@code <offer_notice>} child; a value of
     * {@code "video"} marks a video call.
     */
    private static final String MEDIA_ATTRIBUTE = "media";

    /**
     * The wire attribute naming the original offer time on the {@code <call>} envelope, in Unix seconds.
     */
    private static final String TIME_ATTRIBUTE = "t";

    /**
     * The {@code type} value marking a group call.
     */
    private static final String GROUP_TYPE = "group";

    /**
     * The {@code media} value marking a video call.
     */
    private static final String VIDEO_MEDIA = "video";

    /**
     * Validates the record components.
     *
     * @throws NullPointerException if {@code callCreator}, {@code callId}, or {@code offerTime} is
     *                              {@code null}
     */
    public OfferNoticeStanza {
        Objects.requireNonNull(callCreator, "callCreator cannot be null");
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(offerTime, "offerTime cannot be null");
    }

    /**
     * Decodes the {@code <offer_notice>} child of a {@code <call>} envelope into an
     * {@link OfferNoticeStanza}.
     *
     * <p>Reads {@code call-creator} and {@code call-id} from the child, the {@code type} and
     * {@code media} flags from the child, and the original offer time from the envelope's {@code t}
     * attribute. A stanza with no {@code <offer_notice>} child, or one missing the {@code call-creator}
     * or {@code call-id} attribute, decodes to {@link Optional#empty()} so the receiver can drop a
     * malformed notice after acknowledging it rather than failing the read loop. A missing or
     * unparseable {@code t} attribute decodes to the {@linkplain Instant#EPOCH Unix epoch}, which the
     * downstream staleness gate treats as a stale notice.
     *
     * @param envelope the inbound {@code <call>} envelope carrying the {@code <offer_notice>} child
     * @return the decoded offer notice, or {@link Optional#empty()} when the stanza is not a valid
     *         offer notice
     * @throws NullPointerException if {@code envelope} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleVoipOfferNotice", exports = "callOfferNoticeParser", adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<OfferNoticeStanza> of(Stanza envelope) {
        Objects.requireNonNull(envelope, "envelope cannot be null");
        var notice = envelope.getChild(ELEMENT).orElse(null);
        if (notice == null) {
            return Optional.empty();
        }
        var callCreator = notice.getAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE).orElse(null);
        var callId = notice.getAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE).orElse(null);
        if (callCreator == null || callId == null) {
            return Optional.empty();
        }
        var group = GROUP_TYPE.equals(notice.getAttributeAsString(TYPE_ATTRIBUTE).orElse(null));
        var video = VIDEO_MEDIA.equals(notice.getAttributeAsString(MEDIA_ATTRIBUTE).orElse(null));
        var offerTime = Instant.ofEpochSecond(envelope.getAttributeAsLong(TIME_ATTRIBUTE, 0L));
        return Optional.of(new OfferNoticeStanza(callCreator, callId, group, video, offerTime));
    }
}
