package com.github.auties00.cobalt.calls.signaling.incall;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.CallMessages;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents a {@code <peer_state>} action sent during an active call: a report of a single peer's
 * membership state.
 *
 * <p>A peer state action communicates the membership and connection state tracked for one
 * participant. It carries the universal call header and nests one {@code <user>} child that pins the
 * target peer by its {@link #peerJid() JID} and decorates it with a {@link #state() numeric state
 * code} drawn from a twelve entry table. State code {@code 10} is the {@code cancel_offer} marker
 * used when cancelling an offer for specific participants.
 *
 * <p>On the wire the state code is not written numerically: it is projected through the twelve entry
 * table to a lower cased string, and the {@code <peer_state>} element nests a {@code <user>} child
 * carrying that string and the peer JID. The table is
 * {@code 0=invalid, 1=connected, 2=outgoing, 3=receipt, 4=rejected, 5=terminated, 6=timedout,
 * 7=creating, 8=invisible, 9=visible, 10=cancel_offer, 11=invited}; a code at or above {@code 12}
 * serializes as the {@code UNKNOWN PARTICIPANT STATE} sentinel used for an out of range value.
 *
 * <p>On the wire the element is
 * {@snippet lang="xml" :
 * <peer_state call-id="..." call-creator="..." t="1781494319173">
 *     <user state="cancel_offer" jid="258252122116273@lid"/>
 * </peer_state>
 * }
 * where {@code t} is the send time wall clock and {@code state} is the stringified state code.
 *
 * @implNote This implementation omits the {@code t} send time wall clock on encode, since it is an
 * engine internal value rather than a caller supplied one, and ignores an inbound {@code t} on
 * decode.
 * @see SignalingType#PEER_STATE
 */
public final class PeerStateStanza implements InCallActionStanza {
    /**
     * The wire element tag for a peer state action.
     */
    public static final String ELEMENT = "peer_state";

    /**
     * The wire element tag for the nested participant entry that carries the peer JID and state string.
     */
    private static final String USER_ELEMENT = "user";

    /**
     * The wire attribute on the {@code <user>} child naming the peer whose state is reported.
     */
    private static final String JID_ATTRIBUTE = "jid";

    /**
     * The wire attribute on the {@code <user>} child naming the stringified state code.
     */
    private static final String STATE_ATTRIBUTE = "state";

    /**
     * The string written for a state code that lies outside the {@link #STATE_NAMES} table.
     */
    private static final String UNKNOWN_STATE = "UNKNOWN PARTICIPANT STATE";

    /**
     * Maps each state code to its lower cased wire string, indexed by code.
     *
     * <p>This is the twelve entry table: the code is the array index and the value is the string
     * written into the {@code <user state>} attribute.
     */
    private static final String[] STATE_NAMES = {
            "invalid",
            "connected",
            "outgoing",
            "receipt",
            "rejected",
            "terminated",
            "timedout",
            "creating",
            "invisible",
            "visible",
            "cancel_offer",
            "invited"
    };

    /**
     * Maps each lower cased wire string back to its state code for inbound decoding.
     *
     * <p>This is the inverse of {@link #STATE_NAMES}: the state string written into a {@code <user state>}
     * attribute resolves back to the numeric code it represents.
     */
    private static final Map<String, Integer> CODE_BY_NAME = buildCodeByName();

    /**
     * The call identifier this peer state action's {@code call-id} header carries.
     */
    private final String callId;

    /**
     * The call creator device JID this peer state action's {@code call-creator} header carries.
     */
    private final Jid callCreator;

    /**
     * The JID of the peer whose state is reported, carried in the {@code <user>} child.
     */
    private final Jid peerJid;

    /**
     * The numeric state code ({@code 0..0xb}; an out of range value serializes as the unknown state
     * sentinel).
     */
    private final int state;

    /**
     * Builds the inverse of {@link #STATE_NAMES}, mapping each wire string to its state code.
     *
     * @return an unmodifiable map from state string to numeric code
     */
    private static Map<String, Integer> buildCodeByName() {
        var codeByName = new HashMap<String, Integer>(STATE_NAMES.length * 2);
        for (var code = 0; code < STATE_NAMES.length; code++) {
            codeByName.put(STATE_NAMES[code], code);
        }
        return Map.copyOf(codeByName);
    }

    /**
     * Constructs a peer state action.
     *
     * @param callId      the call identifier; never {@code null}
     * @param callCreator the call creator's device JID; never {@code null}
     * @param peerJid     the JID of the peer whose state is reported, carried in the {@code <user>} child;
     *                    never {@code null}
     * @param state       the numeric state code ({@code 0..0xb}; an out of range value serializes as the
     *                    unknown state sentinel)
     * @throws NullPointerException if {@code callId}, {@code callCreator}, or {@code peerJid} is
     *                              {@code null}
     */
    public PeerStateStanza(String callId, Jid callCreator, Jid peerJid, int state) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(callCreator, "callCreator cannot be null");
        Objects.requireNonNull(peerJid, "peerJid cannot be null");
        this.callId = callId;
        this.callCreator = callCreator;
        this.peerJid = peerJid;
        this.state = state;
    }

    /**
     * Returns the lower cased wire string the given state code projects to.
     *
     * <p>A code within {@code 0..0xb} resolves through the twelve entry {@link #STATE_NAMES} table; a code
     * at or above {@code 12} resolves to the {@link #UNKNOWN_STATE} sentinel written for an out of range
     * index.
     *
     * @param code the numeric state code
     * @return the wire string for the code
     */
    private static String stateName(int code) {
        return code >= 0 && code < STATE_NAMES.length ? STATE_NAMES[code] : UNKNOWN_STATE;
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for a peer state action
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for a peer state action
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * Returns the JID of the peer whose state is reported, carried in the {@code <user>} child.
     *
     * @return the target peer JID
     */
    public Jid peerJid() {
        return peerJid;
    }

    /**
     * Returns the numeric state code.
     *
     * @return the state code ({@code 0..0xb}; an out of range value serializes as the unknown state
     *         sentinel)
     */
    public int state() {
        return state;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SignalingType#PEER_STATE}
     */
    @Override
    public SignalingType type() {
        return SignalingType.PEER_STATE;
    }

    /**
     * Builds the {@code <peer_state call-id call-creator>} action stanza with its nested {@code <user>}
     * child.
     *
     * <p>The common header is stamped first, then a single {@code <user state jid/>} child carries the
     * stringified state code and the target peer JID. The engine internal {@code t} send time wall clock
     * is not written; the receiver treats it as informational.
     *
     * @return the peer state action stanza
     */
    @Override
    public Stanza toStanza() {
        var user = new StanzaBuilder()
                .description(USER_ELEMENT)
                .attribute(STATE_ATTRIBUTE, stateName(state))
                .attribute(JID_ATTRIBUTE, peerJid)
                .build();
        return CallMessages.stampHeader(new StanzaBuilder().description(ELEMENT), callId, callCreator)
                .content(user)
                .build();
    }

    /**
     * Decodes a {@code <peer_state>} action stanza into a {@link PeerStateStanza}.
     *
     * <p>The nested {@code <user>} child supplies the peer JID and the {@code state} string, which is
     * projected back through the twelve entry table to its numeric code; an unrecognized or absent state
     * string decodes to code {@code 0} ({@code invalid}). The {@code t} timestamp attribute, when present,
     * is ignored.
     *
     * @param stanza the {@code <peer_state>} stanza
     * @return the decoded peer state action
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute is
     *                                absent, or if the nested {@code <user>} child or its {@code jid}
     *                                attribute is absent
     */
    public static PeerStateStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var user = stanza.getChild(USER_ELEMENT)
                .orElseThrow(() -> new NoSuchElementException("peer_state is missing its user child"));
        var peerJid = user.getRequiredAttributeAsJid(JID_ATTRIBUTE);
        var state = user.getAttributeAsString(STATE_ATTRIBUTE)
                .map(name -> CODE_BY_NAME.getOrDefault(name, 0))
                .orElse(0);
        return new PeerStateStanza(callId, callCreator, peerJid, state);
    }

    /**
     * Returns whether {@code obj} is a {@link PeerStateStanza} with the same call header, peer, and state.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a value equal peer state action
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof PeerStateStanza that
                && callId.equals(that.callId)
                && callCreator.equals(that.callCreator)
                && peerJid.equals(that.peerJid)
                && state == that.state);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this peer state action
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator, peerJid, state);
    }

    /**
     * Returns a debug oriented string for this peer state action.
     *
     * @return the debug string
     */
    @Override
    public String toString() {
        return "PeerStateStanza[callId=" + callId + ", callCreator=" + callCreator
                + ", peerJid=" + peerJid + ", state=" + state + ']';
    }
}
