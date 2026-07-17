package com.github.auties00.cobalt.calls.signaling.incall;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.CallMessages;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents a {@code <mute_v2>} in call action: a self mute state change or a request to mute
 * another participant.
 *
 * <p>The {@code mute_v2} element carries EXACTLY ONE of two mutually exclusive intents, distinguished
 * by which attribute is present:
 * <ul>
 *   <li>a self mute state report, carried as {@code mute-state="1"} (self muted) or
 *       {@code mute-state="0"} (self unmuted);
 *   <li>a request that the recipient mute itself, carried as {@code request-state="1"} (the
 *       group admin peer mute request).
 * </ul>
 * It additionally carries an optional {@code broadcast} flag, set when the action is fanned out to
 * every participant rather than addressed to a single peer. This supersedes the legacy {@code <mute>}
 * element ({@link SignalingType#MUTE}); only {@code mute_v2} is emitted and parsed.
 *
 * <p>On the wire a peer mute request is
 * {@snippet lang="xml" :
 * <mute_v2 call-id="..." call-creator="..." request-state="1" broadcast="1"/>
 * }
 * and a self mute state report is
 * {@snippet lang="xml" :
 * <mute_v2 call-id="..." call-creator="..." mute-state="1" broadcast="1"/>
 * }
 * Exactly one of {@code request-state} or {@code mute-state} is present; {@code broadcast} is optional
 * and defaults to clear.
 *
 * @see SignalingType#MUTE_V2
 */
public final class MuteV2Stanza implements InCallActionStanza {
    /**
     * The wire element tag for a mute_v2 action.
     */
    public static final String ELEMENT = "mute_v2";

    /**
     * The wire attribute carrying a request that the recipient mute itself.
     */
    private static final String REQUEST_STATE_ATTRIBUTE = "request-state";

    /**
     * The wire attribute carrying the sender's own mute state.
     */
    private static final String MUTE_STATE_ATTRIBUTE = "mute-state";

    /**
     * The wire attribute flagging a fanned out action.
     */
    private static final String BROADCAST_ATTRIBUTE = "broadcast";

    /**
     * The wire literal for a set ({@code true}) voip boolean flag.
     */
    private static final String FLAG_TRUE = "1";

    /**
     * The wire literal for a clear ({@code false}) voip boolean flag.
     */
    private static final String FLAG_FALSE = "0";

    /**
     * The call identifier; never {@code null}.
     */
    private final String callId;

    /**
     * The call creator's device JID; never {@code null}.
     */
    private final Jid callCreator;

    /**
     * {@code true} when this is a request that the recipient mute itself ({@code request-state="1"});
     * {@code false} when this reports the sender's own mute state.
     */
    private final boolean peerRequest;

    /**
     * The self mute state ({@code true} muted, {@code false} unmuted) when {@code peerRequest} is
     * {@code false}; ignored when {@code peerRequest} is {@code true}.
     */
    private final boolean muted;

    /**
     * {@code true} when the action is fanned out to all participants.
     */
    private final boolean broadcast;

    /**
     * Constructs a mute_v2 action, validating the components.
     *
     * @param callId      the call identifier; never {@code null}
     * @param callCreator the call creator's device JID; never {@code null}
     * @param peerRequest {@code true} when this is a request that the recipient mute itself
     *                    ({@code request-state="1"}); {@code false} when this reports the sender's own
     *                    mute state
     * @param muted       the self mute state ({@code true} muted, {@code false} unmuted) when
     *                    {@code peerRequest} is {@code false}; ignored when {@code peerRequest} is
     *                    {@code true}
     * @param broadcast   {@code true} when the action is fanned out to all participants
     * @throws NullPointerException if {@code callId} or {@code callCreator} is {@code null}
     */
    public MuteV2Stanza(String callId, Jid callCreator, boolean peerRequest, boolean muted, boolean broadcast) {
        this.callId = Objects.requireNonNull(callId, "callId cannot be null");
        this.callCreator = Objects.requireNonNull(callCreator, "callCreator cannot be null");
        this.peerRequest = peerRequest;
        this.muted = muted;
        this.broadcast = broadcast;
    }

    /**
     * Returns a {@code mute_v2} reporting the sender's own mute state.
     *
     * @param callId      the call identifier
     * @param callCreator the call creator's device JID
     * @param muted       {@code true} when the sender is muted, {@code false} when unmuted
     * @param broadcast   {@code true} when the action is fanned out to all participants
     * @return the self mute state action
     * @throws NullPointerException if {@code callId} or {@code callCreator} is {@code null}
     */
    public static MuteV2Stanza ofSelfState(String callId, Jid callCreator, boolean muted, boolean broadcast) {
        return new MuteV2Stanza(callId, callCreator, false, muted, broadcast);
    }

    /**
     * Returns a {@code mute_v2} requesting that the recipient mute itself.
     *
     * @param callId      the call identifier
     * @param callCreator the call creator's device JID
     * @param broadcast   {@code true} when the request is fanned out to all participants
     * @return the peer mute request action
     * @throws NullPointerException if {@code callId} or {@code callCreator} is {@code null}
     */
    public static MuteV2Stanza ofPeerRequest(String callId, Jid callCreator, boolean broadcast) {
        return new MuteV2Stanza(callId, callCreator, true, false, broadcast);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for a mute_v2 action
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for a mute_v2 action
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * Returns whether this is a request that the recipient mute itself.
     *
     * @return {@code true} when this is a peer mute request ({@code request-state="1"}); {@code false}
     *         when this reports the sender's own mute state
     */
    public boolean peerRequest() {
        return peerRequest;
    }

    /**
     * Returns the sender's own mute state.
     *
     * @return the self mute state ({@code true} muted, {@code false} unmuted) when {@link #peerRequest()}
     *         is {@code false}; meaningless when {@link #peerRequest()} is {@code true}
     */
    public boolean muted() {
        return muted;
    }

    /**
     * Returns whether the action is fanned out to all participants.
     *
     * @return {@code true} when the action is broadcast to all participants
     */
    public boolean broadcast() {
        return broadcast;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SignalingType#MUTE_V2}
     */
    @Override
    public SignalingType type() {
        return SignalingType.MUTE_V2;
    }

    /**
     * Builds the {@code <mute_v2 call-id call-creator (request-state|mute-state) broadcast/>} action
     * stanza.
     *
     * <p>Exactly one of {@code request-state} or {@code mute-state} is written: {@code request-state}
     * (always {@code 1}) when {@link #peerRequest()} is {@code true}, otherwise {@code mute-state}
     * carrying {@code 1} or {@code 0} for {@link #muted()}. The {@code broadcast} attribute is omitted
     * unless {@link #broadcast()} is {@code true}.
     *
     * @return the mute_v2 action stanza
     */
    @Override
    public Stanza toStanza() {
        var builder = CallMessages.stampHeader(new StanzaBuilder().description(ELEMENT), callId, callCreator);
        if (peerRequest) {
            builder.attribute(REQUEST_STATE_ATTRIBUTE, FLAG_TRUE);
        } else {
            builder.attribute(MUTE_STATE_ATTRIBUTE, muted ? FLAG_TRUE : FLAG_FALSE);
        }
        return builder
                .attribute(BROADCAST_ATTRIBUTE, FLAG_TRUE, broadcast)
                .build();
    }

    /**
     * Decodes a {@code <mute_v2>} action stanza into a {@link MuteV2Stanza}.
     *
     * <p>The stanza is classified as a peer mute request when it carries {@code request-state};
     * otherwise it is read as a self mute state report whose {@link #muted()} reflects the
     * {@code mute-state} attribute. An absent {@code broadcast} decodes to {@code false}.
     *
     * @param stanza the {@code <mute_v2>} stanza
     * @return the decoded mute_v2 action
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent, or if neither {@code request-state} nor
     *                                {@code mute-state} is present
     */
    public static MuteV2Stanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var broadcast = FLAG_TRUE.equals(stanza.getAttributeAsString(BROADCAST_ATTRIBUTE, FLAG_FALSE));
        if (stanza.getAttributeAsString(REQUEST_STATE_ATTRIBUTE).isPresent()) {
            return new MuteV2Stanza(callId, callCreator, true, false, broadcast);
        }
        var muteState = stanza.getAttributeAsString(MUTE_STATE_ATTRIBUTE)
                .orElseThrow(() -> new NoSuchElementException(
                        "mute_v2 requires either request-state or mute-state"));
        return new MuteV2Stanza(callId, callCreator, false, FLAG_TRUE.equals(muteState), broadcast);
    }

    /**
     * Returns whether {@code obj} is a {@link MuteV2Stanza} with equal components.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a value equal mute_v2 action
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof MuteV2Stanza that
                && peerRequest == that.peerRequest
                && muted == that.muted
                && broadcast == that.broadcast
                && Objects.equals(callId, that.callId)
                && Objects.equals(callCreator, that.callCreator));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this mute_v2 action
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator, peerRequest, muted, broadcast);
    }

    /**
     * Returns a string representation of this mute_v2 action for debugging.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "MuteV2Stanza[callId=" + callId
                + ", callCreator=" + callCreator
                + ", peerRequest=" + peerRequest
                + ", muted=" + muted
                + ", broadcast=" + broadcast
                + ']';
    }
}
