package com.github.auties00.cobalt.calls.signaling.receive;

import com.github.auties00.cobalt.calls.signaling.CallStanza;
import com.github.auties00.cobalt.calls.signaling.SignalingType;
import com.github.auties00.cobalt.calls.signaling.incall.RaiseHandStanza;
import com.github.auties00.cobalt.calls.signaling.session.RingingStanza;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.Optional;

/**
 * Classifies an inbound {@code <call>} child element into its signaling type and a routing verdict.
 *
 * <p>This performs the classification half of inbound call dispatch: given the single child element of a
 * {@code <call>} stanza and the envelope context it arrived in, it reproduces the header validation and
 * the routing decision made for the message before any handler runs. It holds no per call or engine
 * state; it reads the stanza and returns a {@link Verdict} the receiver acts on, so one instance can
 * classify every inbound call regardless of which calls are active.
 *
 * <p>Classification proceeds in three stages. First the universal header is validated: the payload must
 * carry a {@code call-id} that is not empty and a {@code call-creator}, or the message is rejected.
 * Second the child element tag is resolved to a {@link SignalingType} through
 * {@link SignalingType#ofWireTag(String)}; a tag the taxonomy does not name is still routable when
 * {@link CallStanza#isKnownTag(String)} reports a decoder for it, which covers the few inbound actions
 * that name a {@code <call>} child yet carry no taxonomy ordinal ({@link RingingStanza},
 * {@link RaiseHandStanza}). Only a tag that is neither a taxonomy type nor a known decoder tag is
 * dropped. Third the receiving context decides the verdict: a stanza that is not LID addressed is dropped
 * before it reaches a call, and an offer or an in call action that arrives before its call object exists
 * is buffered for replay rather than processed immediately.
 *
 * <p>The routing verdict is deliberately coarse: it tells the receiver whether to process the message
 * now, drop it as malformed or unroutable, or buffer it for later replay. The finer decisions (which
 * state transition to drive, whether to ring, whether a terminate was already seen) belong to the
 * lifecycle layer the receiver forwards a processed message to; this router only gates and classifies.
 *
 * @implNote This implementation carries the {@code call-id} as a {@link String} rather than a fixed size
 * byte buffer, so an all zero call-id buffer maps to a call-id whose characters are all {@code '0'} (the
 * wire form of an all zero hex id); that form is rejected here alongside the absent and empty cases.
 * A per type fixed header length has no analogue in the typed {@link Stanza} model and is not checked;
 * the per type length is carried for reference by {@link SignalingType#fixedHeaderLength()}.
 */
public final class CallSignalingRouter {
    /**
     * The logger for {@link CallSignalingRouter}.
     */
    private static final System.Logger LOGGER = Log.get(CallSignalingRouter.class);

    /**
     * Classifies how the receiver must route an inbound {@code <call>} child element.
     *
     * <p>The verdict is the coarse decision made before any handler for the message runs: process the
     * message against its call now, drop it as malformed or unroutable, or buffer it for replay once the
     * call object exists. It carries no transition detail; the lifecycle layer the receiver forwards a
     * {@link #PROCESS} message to decides the state change.
     */
    public enum Disposition {
        /**
         * Marks a well formed, routable message the receiver forwards to the engine for handling now.
         */
        PROCESS,

        /**
         * Marks a message the receiver drops without forwarding or buffering.
         *
         * <p>Applies to a payload that fails header validation (an absent, empty, or all zero
         * {@code call-id}, or a missing {@code call-creator}), a payload whose child tag names neither an
         * action bearing {@link SignalingType} nor a known {@link CallStanza} decoder, and a payload that
         * arrives in a context the engine refuses (a stanza that is not LID addressed).
         */
        DROP,

        /**
         * Marks a message the receiver buffers for replay because its call object does not yet exist.
         *
         * <p>Applies to an offer and to an in call action that races ahead of the call it belongs to; the
         * receiver stores it through {@link CallMessageBuffer#buffer(String, Stanza)} and replays it once
         * the call is created.
         */
        BUFFER
    }

    /**
     * Holds the result of classifying an inbound {@code <call>} child element.
     *
     * <p>A {@link Disposition#PROCESS} or {@link Disposition#BUFFER} verdict carries the {@code call-id}
     * read from the payload and the resolved {@link SignalingType}, which is empty for a tag that is
     * decodable by {@link CallStanza} yet carries no taxonomy ordinal ({@link RingingStanza},
     * {@link RaiseHandStanza}); a {@link Disposition#DROP} verdict may carry an empty type when the drop
     * reason is a missing header or an unknown tag, and an empty call-id when the payload carried none.
     * Callers branch on {@link #disposition()} first and read {@link #type()} and {@link #callId()} only
     * for verdicts other than {@link Disposition#DROP}.
     *
     * <p>The {@link #type()} and {@link #callId()} components are wrapped in {@link Optional} so a verdict
     * can express the absence of a resolved type (a dropped message, or one with no taxonomy ordinal) or a
     * parsed call-id without a sentinel; the canonical accessors return the wrapped values directly.
     *
     * @param disposition the routing decision; never {@code null}
     * @param type        the resolved signaling type, empty when the payload failed header validation,
     *                    named no known type, or names a decodable tag with no taxonomy ordinal; never
     *                    {@code null}
     * @param callId      the call identifier read from the payload, empty when absent; never
     *                    {@code null}
     * @param callCreator the {@code call-creator} device JID read from the payload, empty when absent
     *                    (which occurs only on a header validation {@link Disposition#DROP}); never
     *                    {@code null}
     */
    public record Verdict(Disposition disposition, Optional<SignalingType> type, Optional<String> callId,
                          Optional<Jid> callCreator) {
        /**
         * Canonicalizes the record components.
         *
         * @throws NullPointerException if {@code disposition}, {@code type}, {@code callId}, or
         *                              {@code callCreator} is {@code null}
         */
        public Verdict {
            Objects.requireNonNull(disposition, "disposition cannot be null");
            Objects.requireNonNull(type, "type cannot be null");
            Objects.requireNonNull(callId, "callId cannot be null");
            Objects.requireNonNull(callCreator, "callCreator cannot be null");
        }
    }

    /**
     * Classifies an inbound {@code <call>} child element into its signaling type and routing verdict.
     *
     * <p>Validates the universal header, resolves the child element tag to a {@link SignalingType},
     * applies the LID only context gate, and decides whether the message is processed now or buffered for
     * its call that does not yet exist. A tag the taxonomy does not name is still routable when
     * {@link CallStanza#isKnownTag(String)} reports a decoder for it; such a tag carries an empty
     * {@link Verdict#type()} but is otherwise routed like any other action. The decision flow is:
     * <ul>
     *   <li>a payload with no {@code call-creator}, or whose {@code call-id} is absent, empty, or all zero
     *       (every character {@code '0'}), yields {@link Disposition#DROP} with an empty type;</li>
     *   <li>a child tag that resolves to no {@link SignalingType} and names no known
     *       {@link CallStanza} decoder yields {@link Disposition#DROP} with an empty type;</li>
     *   <li>a stanza that is not LID addressed yields {@link Disposition#DROP} with the resolved type,
     *       which is empty for a decodable tag that carries no ordinal;</li>
     *   <li>a message whose call object does not yet exist yields {@link Disposition#BUFFER} with the
     *       resolved type, which is empty for a decodable tag that carries no ordinal;</li>
     *   <li>otherwise the message yields {@link Disposition#PROCESS} with the resolved type, which is
     *       empty for a decodable tag that carries no ordinal.</li>
     * </ul>
     *
     * @param description  the wire tag (element description) of the {@code <call>} child element
     * @param callId       the payload's {@code call-id} attribute, or {@code null} when absent
     * @param callCreator  the payload's {@code call-creator} device JID, or {@code null} when absent
     * @param senderLid    the {@code sender_lid} attribute from the {@code <call>} envelope, or
     *                     {@code null} when the stanza is not LID addressed
     * @param callExists   whether a call object already exists for the payload's call identifier
     * @return the classification verdict; never {@code null}
     * @throws NullPointerException if {@code description} is {@code null}
     */
    public Verdict classify(String description, String callId, Jid callCreator, Jid senderLid, boolean callExists) {
        Objects.requireNonNull(description, "description cannot be null");

        var callCreatorOpt = Optional.ofNullable(callCreator);
        if (callId == null || callId.isEmpty() || isAllZeroCallId(callId) || callCreator == null) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "dropping call payload {0}: missing or invalid call-id/call-creator",
                        description);
            }
            return new Verdict(Disposition.DROP, Optional.empty(), Optional.ofNullable(callId), callCreatorOpt);
        }

        var type = SignalingType.ofWireTag(description);
        if (type.isEmpty() && !CallStanza.isKnownTag(description)) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "dropping call payload {0} for call {1}: unknown signaling tag",
                        description, callId);
            }
            return new Verdict(Disposition.DROP, Optional.empty(), Optional.of(callId), callCreatorOpt);
        }

        if (!isLidAddressed(senderLid, callCreator)) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "dropping call payload {0} for call {1}: not lid addressed",
                        description, callId);
            }
            return new Verdict(Disposition.DROP, type, Optional.of(callId), callCreatorOpt);
        }

        if (!callExists) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "buffering call payload {0} for call {1}: call not yet created",
                        description, callId);
            }
            return new Verdict(Disposition.BUFFER, type, Optional.of(callId), callCreatorOpt);
        }

        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "processing call payload {0} for call {1}", description, callId);
        }
        return new Verdict(Disposition.PROCESS, type, Optional.of(callId), callCreatorOpt);
    }

    /**
     * Returns whether a call-id that is not empty is the all zero call-id treated as blank.
     *
     * <p>An all zero call-id buffer has a wire form whose every character is {@code '0'}. A call-id that
     * is all {@code '0'} characters is therefore the typed {@link String} form of an all zero buffer and
     * is rejected the same way as an absent or empty call-id. The argument is assumed already not empty;
     * an empty string is handled by the caller's emptiness check.
     *
     * @param callId the call-id, assumed not empty, read from the payload
     * @return {@code true} when every character of {@code callId} is {@code '0'}
     */
    private boolean isAllZeroCallId(String callId) {
        for (var index = 0; index < callId.length(); index++) {
            if (callId.charAt(index) != '0') {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether an inbound call stanza is LID addressed.
     *
     * <p>A call stanza is dropped before dispatch unless it is LID addressed: the modern call plane
     * addresses every device by LID, so a stanza carrying neither a {@code sender_lid} on the envelope nor
     * a LID server {@code call-creator} predates the LID migration and is not routed. A stanza with a
     * {@code sender_lid} is LID addressed by definition; otherwise the call creator's server is inspected.
     *
     * @param senderLid   the {@code sender_lid} attribute from the {@code <call>} envelope, or
     *                    {@code null} when absent
     * @param callCreator the {@code call-creator} device JID from the payload
     * @return {@code true} when the stanza is LID addressed, {@code false} otherwise
     */
    private boolean isLidAddressed(Jid senderLid, Jid callCreator) {
        return senderLid != null || callCreator.hasLidServer();
    }
}
