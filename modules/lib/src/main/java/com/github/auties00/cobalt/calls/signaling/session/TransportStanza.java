package com.github.auties00.cobalt.calls.signaling.session;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.CallMessages;
import com.github.auties00.cobalt.calls.signaling.SignalingType;
import com.github.auties00.cobalt.calls.signaling.relay.RelayInfo;

/**
 * Represents a {@code <transport>} signaling message, the transport plane exchange of a call.
 *
 * <p>The transport message is the carrier for transport bring up and maintenance. Its
 * {@code transport-message-type} attribute selects which transport payload it conveys, modeled by
 * {@link CallTransportSubType}: a candidate, a candidate list, the negotiated transport protocol, a
 * relay latency probe, the peer network health status, or the web peer to peer ICE/DTLS material.
 * Two payload shapes are carried inline by this message rather than by a separate child message: the
 * relay and network health path (subtype {@code 11}) adds a {@code health_status} to the
 * {@code <net>} element, and the ICE/DTLS path (subtype {@code 13}) adds the {@code ice-ufrag} and
 * {@code ice-pwd} attributes and a {@code <certificate>} child. The message may also carry a
 * {@link RelayInfo relay} block and a list of peer to peer candidates, each with a {@code priority}.
 *
 * <p>The message stamps the common {@code call-id} and {@code call-creator} attributes like every
 * call action. The {@code <net>} element reports the network medium and protocol the device selected;
 * the {@code p2p-cand-round} attribute counts the candidate gathering round; the {@code has-bot}
 * attribute marks a call that includes a bot.
 *
 * <p>On the wire the message is
 * {@snippet lang="xml" :
 * <transport call-id="..." call-creator="..." transport-message-type="13" p2p-cand-round="1"
 *            ice-ufrag="..." ice-pwd="...">
 *   <net medium="2" protocol="0"/>
 *   <certificate algorithm="sha-256" fingerprint="..."/>
 *   <relay>...</relay>
 *   <candidate priority="1"/>
 * </transport>
 * }
 * with the ICE/DTLS attributes and {@code <certificate>} present only for subtype {@code 13} and the
 * {@code health_status} on {@code <net>} present only for subtype {@code 11}.
 *
 * @see CallTransportSubType
 * @see RelayInfo
 */
public final class TransportStanza implements CallMessage {
    /**
     * The wire element tag for a transport message.
     */
    public static final String ELEMENT = "transport";

    /**
     * The sentinel value standing in for an absent {@code p2p-cand-round} attribute.
     */
    private static final int UNSET = -1;

    /**
     * The wire attribute marking a call that includes a bot.
     */
    private static final String HAS_BOT_ATTRIBUTE = "has-bot";

    /**
     * The wire attribute naming the transport sub message type.
     */
    private static final String TRANSPORT_MESSAGE_TYPE_ATTRIBUTE = "transport-message-type";

    /**
     * The wire attribute naming the peer to peer candidate gathering round.
     */
    private static final String P2P_CAND_ROUND_ATTRIBUTE = "p2p-cand-round";

    /**
     * The wire attribute naming the ICE username fragment.
     */
    private static final String ICE_UFRAG_ATTRIBUTE = "ice-ufrag";

    /**
     * The wire attribute naming the ICE password.
     */
    private static final String ICE_PWD_ATTRIBUTE = "ice-pwd";

    /**
     * The wire literal a boolean attribute carries when set; booleans on the call plane serialize as
     * {@code '1'}/{@code '0'} rather than {@code true}/{@code false}.
     */
    private static final String FLAG_TRUE = "1";

    /**
     * The call identifier this transport message's {@code call-id} header carries.
     */
    private final String callId;

    /**
     * The call creator device JID this transport message's {@code call-creator} header carries.
     */
    private final Jid callCreator;

    /**
     * Whether the {@code has-bot} attribute marks a call that includes a bot.
     */
    private final boolean hasBot;

    /**
     * The {@code transport-message-type} subtype, or {@code null} when the message carries the
     * implicit remote candidate subtype with no explicit attribute.
     */
    private final CallTransportSubType transportSubType;

    /**
     * The {@code p2p-cand-round} attribute, or {@code -1} when absent.
     */
    private final int p2pCandRound;

    /**
     * The {@code <net>} network descriptor, or {@code null} when absent.
     */
    private final Net net;

    /**
     * The {@code ice-ufrag} attribute, present for subtype {@code 13}, or {@code null} otherwise.
     */
    private final String iceUfrag;

    /**
     * The {@code ice-pwd} attribute, present for subtype {@code 13}, or {@code null} otherwise.
     */
    private final String icePwd;

    /**
     * The {@code <certificate>} descriptor, present for subtype {@code 13}, or {@code null} otherwise.
     */
    private final Certificate certificate;

    /**
     * The {@code <relay>} block, or {@code null} when absent.
     */
    private final RelayInfo relay;

    /**
     * The peer to peer {@code <candidate>} list, in wire order; never {@code null}.
     */
    private final List<Candidate> candidates;

    /**
     * Constructs a transport message, copying the candidate list immutably.
     *
     * @param callId           the {@code call-id} attribute; never {@code null}
     * @param callCreator      the {@code call-creator} device JID; never {@code null}
     * @param hasBot           whether the {@code has-bot} attribute marks a call that includes a bot
     * @param transportSubType the {@code transport-message-type} subtype, or {@code null} when the
     *                         message carries the implicit remote candidate subtype with no explicit
     *                         attribute
     * @param p2pCandRound     the {@code p2p-cand-round} attribute, or {@code -1} when absent
     * @param net              the {@code <net>} network descriptor, or {@code null} when absent
     * @param iceUfrag         the {@code ice-ufrag} attribute, present for subtype {@code 13}, or
     *                         {@code null} otherwise
     * @param icePwd           the {@code ice-pwd} attribute, present for subtype {@code 13}, or
     *                         {@code null} otherwise
     * @param certificate      the {@code <certificate>} descriptor, present for subtype {@code 13}, or
     *                         {@code null} otherwise
     * @param relay            the {@code <relay>} block, or {@code null} when absent
     * @param candidates       the peer to peer {@code <candidate>} list, in wire order; never
     *                         {@code null}
     * @throws NullPointerException if {@code callId}, {@code callCreator}, or {@code candidates} is
     *                              {@code null}
     */
    public TransportStanza(String callId,
                           Jid callCreator,
                           boolean hasBot,
                           CallTransportSubType transportSubType,
                           int p2pCandRound,
                           Net net,
                           String iceUfrag,
                           String icePwd,
                           Certificate certificate,
                           RelayInfo relay,
                           List<Candidate> candidates) {
        this.callId = Objects.requireNonNull(callId, "callId cannot be null");
        this.callCreator = Objects.requireNonNull(callCreator, "callCreator cannot be null");
        this.hasBot = hasBot;
        this.transportSubType = transportSubType;
        this.p2pCandRound = p2pCandRound;
        this.net = net;
        this.iceUfrag = iceUfrag;
        this.icePwd = icePwd;
        this.certificate = certificate;
        this.relay = relay;
        this.candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates cannot be null"));
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for a transport message
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for a transport message
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * Returns whether the {@code has-bot} attribute marks a call that includes a bot.
     *
     * @return {@code true} when this transport message marks a call that includes a bot
     */
    public boolean hasBot() {
        return hasBot;
    }

    /**
     * Returns the {@code transport-message-type} subtype, or {@code null} for the implicit
     * remote candidate subtype.
     *
     * @return the transport subtype, or {@code null} when absent
     */
    public CallTransportSubType transportSubType() {
        return transportSubType;
    }

    /**
     * Returns the {@code p2p-cand-round} attribute, or {@code -1} when absent.
     *
     * @return the candidate gathering round, or {@code -1} when absent
     */
    public int p2pCandRound() {
        return p2pCandRound;
    }

    /**
     * Returns the {@code <net>} network descriptor, or {@code null} when absent.
     *
     * @return the network descriptor, or {@code null} when absent
     */
    public Net net() {
        return net;
    }

    /**
     * Returns the {@code ice-ufrag} attribute, or {@code null} when absent.
     *
     * @return the ICE username fragment, or {@code null} when absent
     */
    public String iceUfrag() {
        return iceUfrag;
    }

    /**
     * Returns the {@code ice-pwd} attribute, or {@code null} when absent.
     *
     * @return the ICE password, or {@code null} when absent
     */
    public String icePwd() {
        return icePwd;
    }

    /**
     * Returns the {@code <certificate>} descriptor, or {@code null} when absent.
     *
     * @return the certificate descriptor, or {@code null} when absent
     */
    public Certificate certificate() {
        return certificate;
    }

    /**
     * Returns the {@code <relay>} block, or {@code null} when absent.
     *
     * @return the relay block, or {@code null} when absent
     */
    public RelayInfo relay() {
        return relay;
    }

    /**
     * Returns the peer to peer {@code <candidate>} list, in wire order.
     *
     * @return the immutable candidate list; never {@code null}
     */
    public List<Candidate> candidates() {
        return candidates;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SignalingType#TRANSPORT}
     */
    @Override
    public SignalingType type() {
        return SignalingType.TRANSPORT;
    }

    /**
     * Returns the transport sub message type this message conveys, if it carries an explicit one.
     *
     * @return an {@link Optional} holding the {@link CallTransportSubType}, or empty for the implicit
     *         remote candidate subtype
     */
    public Optional<CallTransportSubType> transportSubTypeValue() {
        return Optional.ofNullable(transportSubType);
    }

    /**
     * Returns the peer to peer candidate gathering round, if present.
     *
     * @return an {@link OptionalInt} holding the {@code p2p-cand-round}, or empty when absent
     */
    public OptionalInt p2pCandRoundValue() {
        return p2pCandRound == UNSET ? OptionalInt.empty() : OptionalInt.of(p2pCandRound);
    }

    /**
     * Returns the {@code <net>} network descriptor, if present.
     *
     * @return an {@link Optional} holding the network descriptor, or empty when absent
     */
    public Optional<Net> netValue() {
        return Optional.ofNullable(net);
    }

    /**
     * Returns the ICE username fragment, if present.
     *
     * @return an {@link Optional} holding the {@code ice-ufrag}, or empty when absent
     */
    public Optional<String> iceUfragValue() {
        return Optional.ofNullable(iceUfrag);
    }

    /**
     * Returns the ICE password, if present.
     *
     * @return an {@link Optional} holding the {@code ice-pwd}, or empty when absent
     */
    public Optional<String> icePwdValue() {
        return Optional.ofNullable(icePwd);
    }

    /**
     * Returns the {@code <certificate>} descriptor, if present.
     *
     * @return an {@link Optional} holding the certificate descriptor, or empty when absent
     */
    public Optional<Certificate> certificateValue() {
        return Optional.ofNullable(certificate);
    }

    /**
     * Returns the {@code <relay>} block, if present.
     *
     * @return an {@link Optional} holding the relay block, or empty when absent
     */
    public Optional<RelayInfo> relayValue() {
        return Optional.ofNullable(relay);
    }

    /**
     * Builds the {@code <transport>} action stanza for this message.
     *
     * <p>The stanza stamps {@code call-id} and {@code call-creator} as every action does. The subtype
     * attribute is omitted for the implicit remote candidate subtype; {@code has-bot} is written only
     * when set; {@code p2p-cand-round} is omitted when absent. The ICE attributes, certificate, relay
     * block, network descriptor, and candidate children are emitted only when present.
     *
     * @return the transport action stanza
     */
    @Override
    public Stanza toStanza() {
        var children = new ArrayList<Stanza>();
        if (net != null) {
            children.add(net.toNode());
        }
        if (certificate != null) {
            children.add(certificate.toNode());
        }
        if (relay != null) {
            children.add(relay.toNode());
        }
        for (var candidate : candidates) {
            children.add(candidate.toNode());
        }
        var builder = CallMessages.stampHeader(new StanzaBuilder().description(ELEMENT), callId, callCreator)
                .attribute(HAS_BOT_ATTRIBUTE, FLAG_TRUE, hasBot)
                .attribute(TRANSPORT_MESSAGE_TYPE_ATTRIBUTE, transportSubType == null ? null : transportSubType.wireValue())
                .attribute(P2P_CAND_ROUND_ATTRIBUTE, p2pCandRound, p2pCandRound != UNSET)
                .attribute(ICE_UFRAG_ATTRIBUTE, iceUfrag)
                .attribute(ICE_PWD_ATTRIBUTE, icePwd);
        if (!children.isEmpty()) {
            builder.content(children);
        }
        return builder.build();
    }

    /**
     * Decodes a {@code <transport>} action stanza into a {@link TransportStanza}.
     *
     * <p>The subtype is resolved through {@link CallTransportSubType#ofWireValue(int)}; an absent or
     * unrecognized {@code transport-message-type} leaves the {@link #transportSubType()} null, modeling
     * the implicit remote candidate subtype. The optional {@code <net>}, {@code <certificate>},
     * {@code <relay>}, and {@code <candidate>} children are decoded when present.
     *
     * @param stanza the {@code <transport>} stanza
     * @return the decoded message
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent
     */
    public static TransportStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var hasBot = FLAG_TRUE.equals(stanza.getAttributeAsString(HAS_BOT_ATTRIBUTE, null));
        var transportMessageType = stanza.getAttributeAsInt(TRANSPORT_MESSAGE_TYPE_ATTRIBUTE);
        var transportSubType = transportMessageType.isPresent()
                ? CallTransportSubType.ofWireValue(transportMessageType.getAsInt()).orElse(null)
                : null;
        var p2pCandRound = stanza.getAttributeAsInt(P2P_CAND_ROUND_ATTRIBUTE, UNSET);
        var iceUfrag = stanza.getAttributeAsString(ICE_UFRAG_ATTRIBUTE, null);
        var icePwd = stanza.getAttributeAsString(ICE_PWD_ATTRIBUTE, null);
        var net = stanza.getChild(Net.ELEMENT).flatMap(Net::of).orElse(null);
        var certificate = stanza.getChild(Certificate.ELEMENT).flatMap(Certificate::of).orElse(null);
        var relay = stanza.getChild(RelayInfo.ELEMENT).flatMap(RelayInfo::of).orElse(null);
        var candidates = new ArrayList<Candidate>();
        for (var child : stanza.getChildren(Candidate.ELEMENT)) {
            Candidate.of(child).ifPresent(candidates::add);
        }
        return new TransportStanza(callId, callCreator, hasBot, transportSubType, p2pCandRound,
                net, iceUfrag, icePwd, certificate, relay, candidates);
    }

    /**
     * Returns whether {@code obj} is a {@link TransportStanza} with equal components.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a value equal transport message
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof TransportStanza that
                && callId.equals(that.callId)
                && callCreator.equals(that.callCreator)
                && hasBot == that.hasBot
                && Objects.equals(transportSubType, that.transportSubType)
                && p2pCandRound == that.p2pCandRound
                && Objects.equals(net, that.net)
                && Objects.equals(iceUfrag, that.iceUfrag)
                && Objects.equals(icePwd, that.icePwd)
                && Objects.equals(certificate, that.certificate)
                && Objects.equals(relay, that.relay)
                && candidates.equals(that.candidates));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this transport message
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator, hasBot, transportSubType, p2pCandRound, net,
                iceUfrag, icePwd, certificate, relay, candidates);
    }

    /**
     * Returns a debug oriented string for this transport message.
     *
     * @return the debug string
     */
    @Override
    public String toString() {
        return "TransportStanza[callId=" + callId
                + ", callCreator=" + callCreator
                + ", hasBot=" + hasBot
                + ", transportSubType=" + transportSubType
                + ", p2pCandRound=" + p2pCandRound
                + ", net=" + net
                + ", iceUfrag=" + iceUfrag
                + ", icePwd=" + icePwd
                + ", certificate=" + certificate
                + ", relay=" + relay
                + ", candidates=" + candidates + ']';
    }

    /**
     * Represents the {@code <net>} network descriptor of a transport message.
     *
     * <p>The network descriptor reports the network medium and transport protocol the device is using.
     * It carries a {@code health_status} only for the relay and network health subtype
     * ({@link CallTransportSubType#PEER_HEALTH}).
     *
     * @param medium       the {@code medium} attribute, the network medium code, or {@code -1} when
     *                     absent
     * @param protocol     the {@code protocol} attribute, the transport protocol code, or {@code -1}
     *                     when absent
     * @param healthStatus the {@code health_status} attribute, present for the peer health subtype, or
     *                     {@code -1} when absent
     */
    public record Net(int medium, int protocol, int healthStatus) {
        /**
         * The wire element tag for a network descriptor.
         */
        public static final String ELEMENT = "net";

        /**
         * The wire attribute naming the network medium.
         */
        private static final String MEDIUM_ATTRIBUTE = "medium";

        /**
         * The wire attribute naming the transport protocol.
         */
        private static final String PROTOCOL_ATTRIBUTE = "protocol";

        /**
         * The wire attribute naming the network health status.
         */
        private static final String HEALTH_STATUS_ATTRIBUTE = "health_status";

        /**
         * Returns the network medium code, if present.
         *
         * @return an {@link OptionalInt} holding the {@code medium}, or empty when absent
         */
        public OptionalInt mediumValue() {
            return medium < 0 ? OptionalInt.empty() : OptionalInt.of(medium);
        }

        /**
         * Returns the transport protocol code, if present.
         *
         * @return an {@link OptionalInt} holding the {@code protocol}, or empty when absent
         */
        public OptionalInt protocolValue() {
            return protocol < 0 ? OptionalInt.empty() : OptionalInt.of(protocol);
        }

        /**
         * Returns the network health status, if present.
         *
         * @return an {@link OptionalInt} holding the {@code health_status}, or empty when absent
         */
        public OptionalInt healthStatusValue() {
            return healthStatus < 0 ? OptionalInt.empty() : OptionalInt.of(healthStatus);
        }

        /**
         * Builds the {@code <net medium=... protocol=... health_status=.../>} stanza for this descriptor.
         *
         * <p>Absent attributes are omitted rather than written as a sentinel.
         *
         * @return the network descriptor stanza
         */
        public Stanza toNode() {
            return new StanzaBuilder()
                    .description(ELEMENT)
                    .attribute(MEDIUM_ATTRIBUTE, medium, medium >= 0)
                    .attribute(PROTOCOL_ATTRIBUTE, protocol, protocol >= 0)
                    .attribute(HEALTH_STATUS_ATTRIBUTE, healthStatus, healthStatus >= 0)
                    .build();
        }

        /**
         * Decodes a {@code <net>} stanza into a {@link Net}.
         *
         * @param stanza the {@code <net>} stanza
         * @return the decoded network descriptor, or an empty result when the stanza is not a
         *         {@code <net>} element
         */
        public static Optional<Net> of(Stanza stanza) {
            if (stanza == null || !stanza.hasDescription(ELEMENT)) {
                return Optional.empty();
            }
            var medium = stanza.getAttributeAsInt(MEDIUM_ATTRIBUTE, -1);
            var protocol = stanza.getAttributeAsInt(PROTOCOL_ATTRIBUTE, -1);
            var healthStatus = stanza.getAttributeAsInt(HEALTH_STATUS_ATTRIBUTE, -1);
            return Optional.of(new Net(medium, protocol, healthStatus));
        }
    }

    /**
     * Represents the {@code <certificate>} descriptor of an ICE/DTLS transport message.
     *
     * <p>The certificate descriptor carries the DTLS fingerprint a web peer to peer transport offers,
     * present only for the ICE/DTLS subtype ({@link CallTransportSubType#ICE_DTLS}). The
     * {@code algorithm} names the fingerprint hash and the {@code fingerprint} is its textual value.
     *
     * @param algorithm   the {@code algorithm} attribute, the fingerprint hash name, or {@code null}
     *                    when absent
     * @param fingerprint the {@code fingerprint} attribute, the textual fingerprint, or {@code null}
     *                    when absent
     */
    public record Certificate(String algorithm, String fingerprint) {
        /**
         * The wire element tag for a certificate descriptor.
         */
        public static final String ELEMENT = "certificate";

        /**
         * The wire attribute naming the fingerprint hash algorithm.
         */
        private static final String ALGORITHM_ATTRIBUTE = "algorithm";

        /**
         * The wire attribute naming the fingerprint value.
         */
        private static final String FINGERPRINT_ATTRIBUTE = "fingerprint";

        /**
         * Returns the fingerprint hash algorithm name, if present.
         *
         * @return an {@link Optional} holding the {@code algorithm}, or empty when absent
         */
        public Optional<String> algorithmValue() {
            return Optional.ofNullable(algorithm);
        }

        /**
         * Returns the textual fingerprint, if present.
         *
         * @return an {@link Optional} holding the {@code fingerprint}, or empty when absent
         */
        public Optional<String> fingerprintValue() {
            return Optional.ofNullable(fingerprint);
        }

        /**
         * Builds the {@code <certificate algorithm=... fingerprint=.../>} stanza for this descriptor.
         *
         * @return the certificate descriptor stanza
         */
        public Stanza toNode() {
            return new StanzaBuilder()
                    .description(ELEMENT)
                    .attribute(ALGORITHM_ATTRIBUTE, algorithm)
                    .attribute(FINGERPRINT_ATTRIBUTE, fingerprint)
                    .build();
        }

        /**
         * Decodes a {@code <certificate>} stanza into a {@link Certificate}.
         *
         * @param stanza the {@code <certificate>} stanza
         * @return the decoded certificate descriptor, or an empty result when the stanza is not a
         *         {@code <certificate>} element
         */
        public static Optional<Certificate> of(Stanza stanza) {
            if (stanza == null || !stanza.hasDescription(ELEMENT)) {
                return Optional.empty();
            }
            var algorithm = stanza.getAttributeAsString(ALGORITHM_ATTRIBUTE, null);
            var fingerprint = stanza.getAttributeAsString(FINGERPRINT_ATTRIBUTE, null);
            return Optional.of(new Certificate(algorithm, fingerprint));
        }
    }

    /**
     * Represents one {@code <candidate>} of a transport message's peer to peer candidate list.
     *
     * <p>Each candidate is one local transport address the peer may try, ordered by its
     * {@code priority}. The candidate's attributes beyond {@code priority} are retained as a raw
     * attribute view so an unrecognized candidate shape round trips without loss.
     *
     * @param priority the {@code priority} attribute, the candidate priority, or {@code -1} when absent
     * @param stanza     the underlying candidate stanza, preserving every attribute and child; never
     *                 {@code null}
     */
    public record Candidate(int priority, Stanza stanza) {
        /**
         * The wire element tag for a peer to peer candidate.
         */
        public static final String ELEMENT = "candidate";

        /**
         * The wire attribute naming the candidate priority.
         */
        private static final String PRIORITY_ATTRIBUTE = "priority";

        /**
         * Canonicalizes the record components.
         *
         * @throws NullPointerException if {@code stanza} is {@code null}
         */
        public Candidate {
            Objects.requireNonNull(stanza, "stanza cannot be null");
        }

        /**
         * Returns the candidate priority, if present.
         *
         * @return an {@link OptionalInt} holding the {@code priority}, or empty when absent
         */
        public OptionalInt priorityValue() {
            return priority < 0 ? OptionalInt.empty() : OptionalInt.of(priority);
        }

        /**
         * Returns the underlying candidate stanza.
         *
         * @return the candidate stanza preserving every attribute and child; never {@code null}
         */
        public Stanza toNode() {
            return stanza;
        }

        /**
         * Decodes a {@code <candidate>} stanza into a {@link Candidate}.
         *
         * <p>The stanza is retained verbatim so a re encode preserves every attribute. A stanza that is not
         * a {@code <candidate>} element yields an empty result.
         *
         * @param stanza the {@code <candidate>} stanza
         * @return the decoded candidate, or an empty result when the stanza is not a {@code <candidate>}
         *         element
         */
        public static Optional<Candidate> of(Stanza stanza) {
            if (stanza == null || !stanza.hasDescription(ELEMENT)) {
                return Optional.empty();
            }
            var priority = stanza.getAttributeAsInt(PRIORITY_ATTRIBUTE, -1);
            return Optional.of(new Candidate(priority, stanza));
        }
    }
}
