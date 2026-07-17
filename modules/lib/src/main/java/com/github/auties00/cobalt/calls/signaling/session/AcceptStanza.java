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
 * Represents an {@code <accept>} signal: the callee answers the call.
 *
 * <p>An accept is the callee's answer to an offer. For a relay call it carries, as its first child, the
 * server allocated {@code <relay>} block (the caller's relay credentials the server delivered in the
 * offer ack), which the server consumes to complete relay allocation before forwarding the accept; an
 * accept that omits it is torn down with {@code setup_failed}. It also carries the chosen network medium,
 * the offered audio codecs, and a video codec for a video call. The data channel certificate fingerprint
 * HMAC, end to end public key {@code <enc>} blobs, the {@code <media>} descriptor, and the embedded
 * {@code <transport>} block belong to the conditional Web P2P and DTLS data channel path and are absent
 * from a plain relay accept; the relay block is modeled by {@link RelayInfo}, while the key blobs and
 * transport block are held as raw subtrees because their grammar is owned by the participant crypto and
 * transport layers. The accept receives an ack or nack like the offer; that ack is parsed by the ack
 * layer, not by this class.
 *
 * <p>On the wire the element is {@snippet lang="xml" :
 * <accept call-id="..." call-creator="...">
 *   <relay self_pid="..." peer_pid="..." uuid="...">CREDENTIALS</relay>
 *   <capability ver="1">MASK</capability>
 *   <audio enc="opus" rate="16000"/>
 *   <net medium="2"/>
 *   <enc>KEY_BLOB</enc>
 *   <media enc="N" rate="16000"/>
 *   <encopt keygen="2"/>
 *   <transport .../>
 * </accept>
 * }
 *
 * @see SignalingType#ACCEPT
 */
public final class AcceptStanza implements CallMessage {
    /**
     * The wire element tag for an accept signal.
     */
    public static final String ELEMENT = "accept";

    /**
     * The wire element tag for the chosen network medium block.
     */
    private static final String NET_ELEMENT = "net";

    /**
     * The wire attribute naming the network medium classification on the {@code <net>} block.
     */
    private static final String MEDIUM_ATTRIBUTE = "medium";

    /**
     * The wire element tag for an audio format advertisement.
     */
    private static final String AUDIO_ELEMENT = CallCodecDescriptor.AUDIO_ELEMENT;

    /**
     * The wire element tag for a video format advertisement.
     */
    private static final String VIDEO_ELEMENT = CallCodecDescriptor.VIDEO_ELEMENT;

    /**
     * The wire element tag for a callee key material blob.
     */
    private static final String ENC_ELEMENT = "enc";

    /**
     * The wire element tag for the embedded transport block.
     */
    private static final String TRANSPORT_ELEMENT = "transport";

    /**
     * The wire element tag for the server allocated relay block.
     */
    private static final String RELAY_ELEMENT = "relay";

    /**
     * The call identifier; never {@code null}.
     */
    private final String callId;

    /**
     * The call creator's device JID; never {@code null}.
     */
    private final Jid callCreator;

    /**
     * The chosen network medium classification, or {@code -1} when absent.
     */
    private final int netMedium;

    /**
     * The callee's capability advertisements; never {@code null}, possibly empty.
     */
    private final List<CallCapability> capabilities;

    /**
     * The offered audio codec descriptors; never {@code null}, possibly empty.
     */
    private final List<CallCodecDescriptor> audioCodecs;

    /**
     * The offered video codec descriptors; never {@code null}, possibly empty.
     */
    private final List<CallCodecDescriptor> videoCodecs;

    /**
     * The raw {@code <enc>} key blob subtrees; never {@code null}, possibly empty.
     */
    private final List<Stanza> encKeys;

    /**
     * The negotiated media descriptor, or {@code null} when absent.
     */
    private final CallMediaDescriptor media;

    /**
     * The encryption options, or {@code null} when absent.
     */
    private final CallEncOptions encOptions;

    /**
     * The embedded {@code <transport>} subtree, or {@code null} when absent.
     */
    private final Stanza transport;

    /**
     * The parsed server allocated {@link RelayInfo relay block} (the caller's relay credentials) echoed as
     * the accept's first child for a relay call, or {@code null} when absent.
     */
    private final RelayInfo relay;

    /**
     * Constructs an accept signal, copying the capability, codec, and key lists.
     *
     * @param callId       the call identifier; never {@code null}
     * @param callCreator  the call creator's device JID; never {@code null}
     * @param netMedium    the chosen network medium classification, or {@code -1} when absent
     * @param capabilities the callee's capability advertisements; never {@code null}, possibly empty
     * @param audioCodecs  the offered audio codec descriptors; never {@code null}, possibly empty
     * @param videoCodecs  the offered video codec descriptors; never {@code null}, possibly empty
     * @param encKeys      the raw {@code <enc>} key blob subtrees; never {@code null}, possibly empty
     * @param media        the negotiated media descriptor, or {@code null} when absent
     * @param encOptions   the encryption options, or {@code null} when absent
     * @param transport    the embedded {@code <transport>} subtree, or {@code null} when absent
     * @param relay        the parsed server allocated relay block, or {@code null} when absent
     * @throws NullPointerException if {@code callId}, {@code callCreator}, {@code capabilities},
     *                              {@code audioCodecs}, {@code videoCodecs}, or {@code encKeys} is
     *                              {@code null}, or if any of those lists contains a {@code null} element
     */
    public AcceptStanza(String callId, Jid callCreator, int netMedium, List<CallCapability> capabilities,
                        List<CallCodecDescriptor> audioCodecs, List<CallCodecDescriptor> videoCodecs,
                        List<Stanza> encKeys, CallMediaDescriptor media, CallEncOptions encOptions,
                        Stanza transport, RelayInfo relay) {
        this.callId = Objects.requireNonNull(callId, "callId cannot be null");
        this.callCreator = Objects.requireNonNull(callCreator, "callCreator cannot be null");
        Objects.requireNonNull(capabilities, "capabilities cannot be null");
        Objects.requireNonNull(audioCodecs, "audioCodecs cannot be null");
        Objects.requireNonNull(videoCodecs, "videoCodecs cannot be null");
        Objects.requireNonNull(encKeys, "encKeys cannot be null");
        this.netMedium = netMedium;
        this.capabilities = List.copyOf(capabilities);
        this.audioCodecs = List.copyOf(audioCodecs);
        this.videoCodecs = List.copyOf(videoCodecs);
        this.encKeys = List.copyOf(encKeys);
        this.media = media;
        this.encOptions = encOptions;
        this.transport = transport;
        this.relay = relay;
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for an accept
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for an accept
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * Returns the callee's capability advertisements.
     *
     * @return the capability advertisements; never {@code null}, possibly empty
     */
    public List<CallCapability> capabilities() {
        return capabilities;
    }

    /**
     * Returns the offered audio codec descriptors.
     *
     * @return the audio codec descriptors; never {@code null}, possibly empty
     */
    public List<CallCodecDescriptor> audioCodecs() {
        return audioCodecs;
    }

    /**
     * Returns the offered video codec descriptors.
     *
     * @return the video codec descriptors; never {@code null}, possibly empty
     */
    public List<CallCodecDescriptor> videoCodecs() {
        return videoCodecs;
    }

    /**
     * Returns the raw {@code <enc>} key blob subtrees.
     *
     * @return the key blob subtrees; never {@code null}, possibly empty
     */
    public List<Stanza> encKeys() {
        return encKeys;
    }

    /**
     * Returns the chosen network medium classification, if present.
     *
     * @return an {@link OptionalInt} holding the network medium, or empty when absent
     */
    public OptionalInt netMediumValue() {
        return netMedium < 0 ? OptionalInt.empty() : OptionalInt.of(netMedium);
    }

    /**
     * Returns the negotiated media descriptor, if present.
     *
     * @return an {@link Optional} holding the media descriptor, or empty when absent
     */
    public Optional<CallMediaDescriptor> mediaDescriptor() {
        return Optional.ofNullable(media);
    }

    /**
     * Returns the encryption options, if present.
     *
     * @return an {@link Optional} holding the encryption options, or empty when absent
     */
    public Optional<CallEncOptions> encOptionsValue() {
        return Optional.ofNullable(encOptions);
    }

    /**
     * Returns the embedded {@code <transport>} subtree, if present.
     *
     * @return an {@link Optional} holding the transport stanza, or empty when absent
     */
    public Optional<Stanza> transportNode() {
        return Optional.ofNullable(transport);
    }

    /**
     * Returns the parsed server allocated {@code <relay>} block, if present.
     *
     * @return an {@link Optional} holding the relay info, or empty when absent
     */
    public Optional<RelayInfo> relayInfo() {
        return Optional.ofNullable(relay);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SignalingType#ACCEPT}
     */
    @Override
    public SignalingType type() {
        return SignalingType.ACCEPT;
    }

    /**
     * Builds the {@code <accept>} action stanza with its relay, capability, audio, video, key, media,
     * encryption, and transport children.
     *
     * <p>Children are emitted in the order relay, capabilities, audio formats, video formats, network medium,
     * key blobs, media, encryption options, transport; the relay block leads when present, each audio format
     * is a flat {@code <audio>} element and each video format a flat {@code <video>} element, the network
     * medium a {@code <net medium>} element emitted only when present, and absent optional children are
     * omitted.
     *
     * @return the accept action stanza
     */
    @Override
    public Stanza toStanza() {
        var children = new ArrayList<Stanza>();
        if (relay != null) {
            children.add(relay.toNode());
        }
        for (var capability : capabilities) {
            children.add(capability.toStanza());
        }
        for (var codec : audioCodecs) {
            children.add(codec.toStanza());
        }
        for (var codec : videoCodecs) {
            children.add(codec.toStanza());
        }
        if (netMedium >= 0) {
            children.add(new StanzaBuilder()
                    .description(NET_ELEMENT)
                    .attribute(MEDIUM_ATTRIBUTE, netMedium)
                    .build());
        }
        children.addAll(encKeys);
        if (media != null) {
            children.add(media.toStanza());
        }
        if (encOptions != null) {
            children.add(encOptions.toStanza());
        }
        if (transport != null) {
            children.add(transport);
        }
        var builder = CallMessages.stampHeader(new StanzaBuilder().description(ELEMENT), callId, callCreator);
        if (!children.isEmpty()) {
            builder.content(children);
        }
        return builder.build();
    }

    /**
     * Decodes an {@code <accept>} action stanza into an {@link AcceptStanza}.
     *
     * <p>The {@code <relay>} block, capability children, the flat {@code <audio>} and {@code <video>} format
     * children, the {@code <net medium>} child, the {@code <enc>} key blobs, the {@code <media>} descriptor,
     * the {@code <encopt>} options, and the embedded {@code <transport>} subtree are each decoded when
     * present; the relay block is parsed into a {@link RelayInfo}, and the key blobs and transport subtree
     * are retained verbatim.
     *
     * @param stanza the {@code <accept>} stanza
     * @return the decoded accept signal
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent
     */
    public static AcceptStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var netMedium = stanza.getChild(NET_ELEMENT)
                .map(net -> net.getAttributeAsInt(MEDIUM_ATTRIBUTE, -1))
                .orElse(-1);
        var capabilities = stanza.streamChildren(CallCapability.ELEMENT)
                .flatMap(child -> CallCapability.of(child).stream())
                .toList();
        var audioCodecs = stanza.streamChildren(AUDIO_ELEMENT)
                .flatMap(audio -> CallCodecDescriptor.of(audio).stream())
                .toList();
        var videoCodecs = stanza.streamChildren(VIDEO_ELEMENT)
                .flatMap(video -> CallCodecDescriptor.of(video).stream())
                .toList();
        var encKeys = stanza.streamChildren(ENC_ELEMENT)
                .toList();
        var media = stanza.getChild(CallMediaDescriptor.ELEMENT).flatMap(CallMediaDescriptor::of).orElse(null);
        var encOptions = stanza.getChild(CallEncOptions.ELEMENT).flatMap(CallEncOptions::of).orElse(null);
        var transport = stanza.getChild(TRANSPORT_ELEMENT).orElse(null);
        var relay = stanza.getChild(RELAY_ELEMENT).flatMap(RelayInfo::of).orElse(null);
        return new AcceptStanza(callId, callCreator, netMedium, capabilities, audioCodecs, videoCodecs, encKeys,
                media, encOptions, transport, relay);
    }

    /**
     * Returns whether {@code obj} is an {@link AcceptStanza} with equal fields.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a value equal accept
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof AcceptStanza that
                && netMedium == that.netMedium
                && callId.equals(that.callId)
                && callCreator.equals(that.callCreator)
                && capabilities.equals(that.capabilities)
                && audioCodecs.equals(that.audioCodecs)
                && videoCodecs.equals(that.videoCodecs)
                && encKeys.equals(that.encKeys)
                && Objects.equals(media, that.media)
                && Objects.equals(encOptions, that.encOptions)
                && Objects.equals(transport, that.transport)
                && Objects.equals(relay, that.relay));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this accept
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator, netMedium, capabilities, audioCodecs, videoCodecs, encKeys,
                media, encOptions, transport, relay);
    }

    /**
     * Returns a debug oriented string for this accept.
     *
     * @return the debug string
     */
    @Override
    public String toString() {
        return "AcceptStanza[callId=" + callId
                + ", callCreator=" + callCreator
                + ", netMedium=" + netMedium
                + ", capabilities=" + capabilities
                + ", audioCodecs=" + audioCodecs
                + ", videoCodecs=" + videoCodecs
                + ", encKeys=" + encKeys
                + ", media=" + media
                + ", encOptions=" + encOptions
                + ", transport=" + transport
                + ", relay=" + relay + ']';
    }
}
