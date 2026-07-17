package com.github.auties00.cobalt.calls.signaling;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryptedPayload;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import com.github.auties00.cobalt.calls.signaling.group.DestinationStanza;
import com.github.auties00.cobalt.calls.signaling.group.GroupInfoStanza;
import com.github.auties00.cobalt.calls.signaling.group.GroupUpdateStanza;
import com.github.auties00.cobalt.calls.signaling.group.HeartbeatStanza;
import com.github.auties00.cobalt.calls.signaling.incall.DtmfStanza;
import com.github.auties00.cobalt.calls.signaling.incall.ExtensionStanza;
import com.github.auties00.cobalt.calls.signaling.incall.FlowControlStanza;
import com.github.auties00.cobalt.calls.signaling.incall.InCallActionStanza;
import com.github.auties00.cobalt.calls.signaling.incall.InterruptionStanza;
import com.github.auties00.cobalt.calls.signaling.incall.MuteV2Stanza;
import com.github.auties00.cobalt.calls.signaling.incall.NotifyStanza;
import com.github.auties00.cobalt.calls.signaling.incall.PeerStateStanza;
import com.github.auties00.cobalt.calls.signaling.incall.RaiseHandStanza;
import com.github.auties00.cobalt.calls.signaling.incall.ReconfigureBotStanza;
import com.github.auties00.cobalt.calls.signaling.incall.RekeyStanza;
import com.github.auties00.cobalt.calls.signaling.incall.ScreenShareStanza;
import com.github.auties00.cobalt.calls.signaling.incall.VideoStateStanza;
import com.github.auties00.cobalt.calls.signaling.link.LinkCreateStanza;
import com.github.auties00.cobalt.calls.signaling.link.LinkEditStanza;
import com.github.auties00.cobalt.calls.signaling.link.LinkJoinStanza;
import com.github.auties00.cobalt.calls.signaling.link.LinkQueryStanza;
import com.github.auties00.cobalt.calls.signaling.receive.CallReceiver;
import com.github.auties00.cobalt.calls.signaling.receive.CallSignalingRouter;
import com.github.auties00.cobalt.calls.signaling.relay.RelayLatencyStanza;
import com.github.auties00.cobalt.calls.signaling.session.AcceptStanza;
import com.github.auties00.cobalt.calls.signaling.session.OfferStanza;
import com.github.auties00.cobalt.calls.signaling.session.PreacceptStanza;
import com.github.auties00.cobalt.calls.signaling.session.RejectStanza;
import com.github.auties00.cobalt.calls.signaling.session.RingingStanza;
import com.github.auties00.cobalt.calls.signaling.session.TerminateStanza;
import com.github.auties00.cobalt.calls.signaling.session.TransportStanza;
import com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomAdmitStanza;
import com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomDenyStanza;
import com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomLeaveStanza;
import com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomToggleStanza;
import com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomUpdateStanza;

/**
 * Builds and parses the WhatsApp voip {@code <call>} signaling stanza.
 *
 * <p>Every call action is exactly one child element inside a top level {@code <call>} stanza:
 * {@snippet lang="xml" :
 * <call to="<peerJid>" id="<dispatcher-assigned>">
 *   <ACTION call-id="..." call-creator="...">...</ACTION>
 * </call>
 * }
 * This factory is the two way bridge between a typed {@link CallMessage} and that wire shape. On the
 * send side {@link #toCall(CallMessage, Jid, String)} wraps a message's action stanza in the envelope with
 * the recipient and the stanza id the dispatcher assigns; on the receive side {@link #parse(Stanza)} maps an
 * already unwrapped {@code <call>} child element to the typed record that owns its wire tag.
 *
 * <p>The {@link #offer(Jid, Jid, String, boolean, byte[], byte[], java.util.List, byte[], Jid, java.util.Collection, Jid, Stanza)
 * offer} factory is the one send side action this class builds directly rather than through a typed
 * {@link CallMessage}: the outbound offer is the leaner thin shape the caller emits (codec
 * advertisement, per device call key fanout, and device identity only), which the server enriches before
 * forwarding, whereas {@link OfferStanza} models the rich server delivered offer the callee parses. It
 * returns the {@code <call>} envelope builder with the {@code id} left unset for the dispatcher.
 *
 * <p>Inbound dispatch keys on the single child element tag, which is the rule the engine follows: the
 * {@code <call>} envelope carries no action discriminator of its own, so the action is identified purely
 * by the name of its one child. {@link #parse(Stanza)} therefore reads {@link Stanza#description()} and
 * looks the tag up in a fixed table of per tag decoders rather than branching on
 * {@link SignalingType}, because a few action elements ({@link RingingStanza},
 * {@link RaiseHandStanza}) name a {@code <call>} child yet carry no taxonomy ordinal and so cannot be
 * routed through the numeric table. The table covers every {@link CallMessage} permitted subtype,
 * including the in call actions grouped under {@link InCallActionStanza}; a tag the table does not know
 * yields an empty result so the caller can drop or buffer the stanza without throwing.
 *
 * <p>The waiting room actions and the {@code extension} action are mapped exactly as their owning
 * records build themselves, so a round trip is symmetric. The waiting room tags are read from
 * {@link SignalingType#wireTag()} for the same {@link SignalingType} the records stamp,
 * and the shared {@code extension} tag resolves to {@link ExtensionStanza} decoded as an
 * {@link SignalingType#ADD_EXTENSION add extension} message, the canonical owner
 * {@link SignalingType#ofWireTag(String)} assigns it; a remove extension is distinguished
 * downstream by message id, not by the wire tag, so it is not separately routable here.
 *
 * @implNote This implementation maps each {@code <call>} element directly onto a {@link Stanza} tree
 * rather than through a separate voip node model, because the binary XMPP codec already yields the parsed
 * tree, so a {@link Stanza} already is the parsed element. The send side wraps each serialized action in
 * the {@code <call to id>} envelope the dispatcher forwards; inbound dispatch switches on the single child
 * element tag and returns an empty {@link Optional} for a tag with no registered decoder rather than
 * throwing. This factory does not validate the universal {@code call-id}/{@code call-creator} header, which
 * {@link CallSignalingRouter} checks before {@link #parse(Stanza)} is reached; each record's {@code of}
 * decoder enforces the attributes it requires.
 */
public final class CallStanza {
    /**
     * The logger for {@link CallStanza}.
     */
    private static final System.Logger LOGGER = Log.get(CallStanza.class);

    /**
     * The wire element tag of the call signaling envelope.
     */
    public static final String ELEMENT = "call";

    /**
     * The wire attribute naming the recipient on an outgoing {@code <call>} envelope.
     */
    private static final String TO_ATTRIBUTE = "to";

    /**
     * The wire attribute naming the stanza identifier the dispatcher assigns on a {@code <call>} envelope.
     */
    private static final String ID_ATTRIBUTE = "id";

    /**
     * The wire attribute naming the call identifier on a {@code <call>} child element.
     */
    private static final String CALL_ID_ATTRIBUTE = "call-id";

    /**
     * The wire attribute naming the call creator on a {@code <call>} child element.
     */
    private static final String CALL_CREATOR_ATTRIBUTE = "call-creator";

    /**
     * The wire attribute naming the group JID on a group offer.
     */
    private static final String GROUP_JID_ATTRIBUTE = "group-jid";

    /**
     * The wire element tag for the trusted contact token block on a one to one offer.
     */
    private static final String PRIVACY_ELEMENT = "privacy";

    /**
     * The wire element tag for an offered audio codec advertisement.
     */
    private static final String AUDIO_ELEMENT = "audio";

    /**
     * The wire element tag for the network medium advertisement.
     */
    private static final String NET_ELEMENT = "net";

    /**
     * The wire element tag for the capability advertisement.
     */
    private static final String CAPABILITY_ELEMENT = "capability";

    /**
     * The wire element tag for the per device key fanout block.
     */
    private static final String DESTINATION_ELEMENT = "destination";

    /**
     * The wire element tag for one per device fanout slot.
     */
    private static final String TO_ELEMENT = "to";

    /**
     * The wire element tag for one Signal encrypted call key blob inside a fanout slot.
     */
    private static final String ENC_ELEMENT = "enc";

    /**
     * The wire element tag advertising a video call.
     */
    private static final String VIDEO_ELEMENT = "video";

    /**
     * The wire element tag for the SRTP key generation options.
     */
    private static final String ENCOPT_ELEMENT = "encopt";

    /**
     * The wire element tag for the ADV device identity envelope.
     */
    private static final String DEVICE_IDENTITY_ELEMENT = "device-identity";

    /**
     * The wire attribute naming the codec on an {@code <audio>} or {@code <enc>} element.
     */
    private static final String ENC_ATTRIBUTE = "enc";

    /**
     * The wire attribute naming the sample rate on an {@code <audio>} element.
     */
    private static final String RATE_ATTRIBUTE = "rate";

    /**
     * The wire attribute naming the medium on the {@code <net>} element.
     */
    private static final String MEDIUM_ATTRIBUTE = "medium";

    /**
     * The wire attribute naming the version on the {@code <capability>} element.
     */
    private static final String VERSION_ATTRIBUTE = "ver";

    /**
     * The wire attribute naming the recipient device on a {@code <to>} fanout slot.
     */
    private static final String JID_ATTRIBUTE = "jid";

    /**
     * The wire attribute naming the cipher version on an {@code <enc>} blob.
     */
    private static final String ENC_VERSION_ATTRIBUTE = "v";

    /**
     * The wire attribute naming the Signal envelope type on an {@code <enc>} blob.
     */
    private static final String ENC_TYPE_ATTRIBUTE = "type";

    /**
     * The wire attribute naming the message count on an {@code <enc>} blob.
     */
    private static final String ENC_COUNT_ATTRIBUTE = "count";

    /**
     * The wire attribute naming the key generation scheme on the {@code <encopt>} element.
     */
    private static final String KEYGEN_ATTRIBUTE = "keygen";

    /**
     * The canonical capability advertisement carried on every one to one offer when the caller supplies
     * none. The seven bytes encode the device's supported call features for the standard Opus and
     * DTLS/SCTP profile.
     */
    private static final byte[] DEFAULT_CAPABILITY_BYTES = {
            (byte) 0x01, (byte) 0x05, (byte) 0xF7, (byte) 0x09,
            (byte) 0xE4, (byte) 0xBB, (byte) 0x13
    };

    /**
     * The {@code opus} codec name advertised on every offered {@code <audio>} element.
     */
    private static final String AUDIO_CODEC_OPUS = "opus";

    /**
     * The lower sample rate advertised on the offer's first {@code <audio>} element.
     */
    private static final String OFFERED_AUDIO_RATE_LOW = "8000";

    /**
     * The higher sample rate advertised on the offer's second {@code <audio>} element.
     */
    private static final String OFFERED_AUDIO_RATE_HIGH = "16000";

    /**
     * The network medium classification advertised on an outgoing offer, the placer's classification
     * before the relay election runs.
     */
    private static final String OFFER_NET_MEDIUM = "3";

    /**
     * The capability advertisement version carried on the {@code <capability>} element.
     */
    private static final String CAPABILITY_VERSION = "1";

    /**
     * The cipher version stamped on every {@code <enc>} blob inside the per device fanout, matching the
     * standard Signal {@code v="2"} envelope of the regular message fanout.
     */
    private static final String ENC_CIPHERTEXT_VERSION = "2";

    /**
     * The message count stamped on every {@code <enc>} blob inside the per device fanout.
     */
    private static final String ENC_COUNT_ZERO = "0";

    /**
     * The SRTP master key derivation scheme stamped on every {@code <encopt>} element; scheme
     * {@code "2"} is the standard scheme observed on every offer.
     */
    private static final String ENCOPT_KEYGEN = "2";

    /**
     * Maps each {@code <call>} child element tag to the decoder that turns the element into its typed
     * {@link CallMessage} record.
     *
     * <p>The table is keyed on the wire child tag exactly as each record renders it through
     * {@link CallMessage#toStanza()}, so {@link #parse(Stanza)} is the inverse of {@link CallMessage#toStanza()}
     * for every permitted subtype. The records whose {@code of} decoder returns an {@link Optional} are
     * adapted to the same {@code Function<Stanza, CallMessage>} shape by mapping their empty result to a
     * {@code null} the lookup treats as an undecodable element. The {@code extension} tag decodes as an
     * add extension message, the canonical owner of the shared tag.
     */
    private static final Map<String, Function<Stanza, CallMessage>> DECODERS = buildDecoders();

    /**
     * Prevents instantiation of this stateless factory.
     *
     * @throws AssertionError always, since this class is not instantiable
     */
    private CallStanza() {
        throw new AssertionError("CallStanza is not instantiable");
    }

    /**
     * Builds the per tag decoder table covering every {@link CallMessage} permitted subtype.
     *
     * <p>Each entry pairs a wire child tag with the static {@code of} decoder of the record that owns
     * the tag. The waiting room tags are taken from {@link SignalingType#wireTag()} for the same
     * type the corresponding record stamps, so the table cannot drift from the records' own emission.
     * The two records whose decoder returns an {@link Optional} ({@link GroupInfoStanza},
     * {@link DestinationStanza}) are adapted by unwrapping the optional to a nullable reference, which
     * the lookup in {@link #parse(Stanza)} treats as an undecodable element.
     *
     * @return the immutable tag to decoder table
     */
    private static Map<String, Function<Stanza, CallMessage>> buildDecoders() {
        return Map.ofEntries(
                Map.entry(OfferStanza.ELEMENT, OfferStanza::of),
                Map.entry(AcceptStanza.ELEMENT, AcceptStanza::of),
                Map.entry(PreacceptStanza.ELEMENT, PreacceptStanza::of),
                Map.entry(RejectStanza.ELEMENT, RejectStanza::of),
                Map.entry(TerminateStanza.ELEMENT, TerminateStanza::of),
                Map.entry(RingingStanza.ELEMENT, RingingStanza::of),
                Map.entry(HeartbeatStanza.ELEMENT, HeartbeatStanza::of),
                Map.entry(TransportStanza.ELEMENT, TransportStanza::of),
                Map.entry(RelayLatencyStanza.ELEMENT, RelayLatencyStanza::of),
                Map.entry(GroupInfoStanza.ELEMENT, node -> GroupInfoStanza.of(node).orElse(null)),
                Map.entry(GroupUpdateStanza.ELEMENT, GroupUpdateStanza::of),
                Map.entry(DestinationStanza.ELEMENT, node -> DestinationStanza.of(node).orElse(null)),
                Map.entry(RekeyStanza.ELEMENT, RekeyStanza::of),
                Map.entry(LinkCreateStanza.ELEMENT, LinkCreateStanza::of),
                Map.entry(LinkQueryStanza.ELEMENT, LinkQueryStanza::of),
                Map.entry(LinkJoinStanza.ELEMENT, LinkJoinStanza::of),
                Map.entry(LinkEditStanza.ELEMENT, LinkEditStanza::of),
                Map.entry(waitingRoomTag(SignalingType.WAITING_ROOM_LEAVE), WaitingRoomLeaveStanza::of),
                Map.entry(waitingRoomTag(SignalingType.WAITING_ROOM_TOGGLE), WaitingRoomToggleStanza::of),
                Map.entry(waitingRoomTag(SignalingType.WAITING_ROOM_ADMIT), WaitingRoomAdmitStanza::of),
                Map.entry(waitingRoomTag(SignalingType.WAITING_ROOM_DENY), WaitingRoomDenyStanza::of),
                Map.entry(waitingRoomTag(SignalingType.WAITING_ROOM_UPDATE), WaitingRoomUpdateStanza::of),
                Map.entry(InterruptionStanza.ELEMENT, InterruptionStanza::of),
                Map.entry(MuteV2Stanza.ELEMENT, MuteV2Stanza::of),
                Map.entry(RaiseHandStanza.ELEMENT, RaiseHandStanza::of),
                Map.entry(ScreenShareStanza.ELEMENT, ScreenShareStanza::of),
                Map.entry(NotifyStanza.ELEMENT, NotifyStanza::of),
                Map.entry(VideoStateStanza.ELEMENT, VideoStateStanza::of),
                Map.entry(PeerStateStanza.ELEMENT, PeerStateStanza::of),
                Map.entry(FlowControlStanza.ELEMENT, FlowControlStanza::of),
                Map.entry(DtmfStanza.ELEMENT, DtmfStanza::of),
                Map.entry(ReconfigureBotStanza.ELEMENT, ReconfigureBotStanza::of),
                Map.entry(ExtensionStanza.ELEMENT, node -> ExtensionStanza.of(node, false)));
    }

    /**
     * Resolves the wire child tag a waiting room signaling type names.
     *
     * <p>The waiting room records build themselves from {@link SignalingType#wireTag()} rather
     * than from a local element constant, so the decoder table reads the tag from the same source to keep
     * parse and build symmetric.
     *
     * @param type the waiting room signaling type
     * @return the wire child tag the type names
     * @throws java.util.NoSuchElementException if the type names no wire child tag
     */
    private static String waitingRoomTag(SignalingType type) {
        return type.wireTag().orElseThrow();
    }

    /**
     * Wraps a call message's action stanza in the {@code <call>} envelope addressed to a recipient.
     *
     * <p>The envelope carries the recipient and the stanza id the dispatcher assigns, and nests the
     * message's {@link CallMessage#toStanza() action stanza} as its single child. The action stanza already
     * carries the universal {@code call-id}/{@code call-creator} header and the action's own attributes
     * and tree, so this method adds only the envelope addressing.
     *
     * @param message the call message to wrap
     * @param to      the recipient of the {@code <call>} stanza
     * @param id      the stanza identifier the dispatcher assigns
     * @return the {@code <call to id>} envelope nesting the action stanza
     * @throws NullPointerException if {@code message}, {@code to}, or {@code id} is {@code null}
     */
    public static Stanza toCall(CallMessage message, Jid to, String id) {
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(to, "to cannot be null");
        Objects.requireNonNull(id, "id cannot be null");
        if (Log.DEBUG)
            LOGGER.log(Level.DEBUG, "call stanza built: id={0} type={1} to={2}", id, message.type(), to);
        return new StanzaBuilder()
                .description(ELEMENT)
                .attribute(TO_ATTRIBUTE, to)
                .attribute(ID_ATTRIBUTE, id)
                .content(message.toStanza())
                .build();
    }

    /**
     * Builds the thin outbound {@code <call><offer>} envelope a caller ships to place a one to one or
     * group call, leaving the {@code id} attribute for the dispatcher to assign.
     *
     * <p>This is the send side offer the engine emits, which is structurally leaner than the rich offer
     * {@link OfferStanza} models on the receive side: the caller advertises only the {@code <audio>}
     * codec set, the {@code <net>} medium, the {@code <capability>} bitfield, the per device
     * Signal encrypted call key fanout, and the ADV {@code <device-identity>} envelope. The server
     * enriches the forwarded copy with the {@code <relay>} transport block, the engine parameter
     * bundles, and the A/B test metadata before delivering it to the callee, so the caller does not send
     * those. The returned envelope wraps {@snippet lang="xml" :
     * <call to="<peer-user-lid>">
     *   <offer call-id="..." call-creator="<self-lid:device@lid>" [group-jid="..."]>
     *     [<privacy>[token bytes]</privacy>]
     *     <audio enc="opus" rate="8000"/>
     *     <audio enc="opus" rate="16000"/>
     *     <net medium="3"/>
     *     [<capability ver="1">[7 bytes]</capability>]
     *     [<destination>
     *       <to jid="<peer-device>"><enc v="2" type="msg|pkmsg" count="0">[ciphertext]</enc></to> ...
     *     </destination>]
     *     [<video/>]
     *     [<encopt keygen="2"/>]
     *     [<device-identity>[ADV bytes]</device-identity>]
     *   </offer>
     * </call>
     * }
     *
     * <p>A one to one offer ({@code groupJid} {@code null}) carries the per peer {@code <privacy>}
     * trusted contact token, the offer level {@code <capability>} and {@code <encopt>}, and the
     * {@code <device-identity>}; a group offer ({@code groupJid} not {@code null}) omits all of those
     * because the server builds the per member fanout and the relay block arrives after the join, and it
     * leads instead with the prebuilt {@code <group_info>} roster the server fans out on. The
     * {@code <destination>} block is emitted only when {@code destinationPayloads} is not empty; the
     * group offer ships no call key in the offer and so passes an empty list.
     *
     * @apiNote Callers precompute every byte buffer this factory takes; it performs no Signal
     * encryption, no ADV signing, and no privacy or capability bitfield assembly. The returned
     * {@link StanzaBuilder} is left unbuilt so the caller can pass it to
     * {@link com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient#sendNode(StanzaBuilder)} when
     * the offer ACK that carries the relay tokens must be captured.
     *
     * @param target              the peer user LID placed on {@code <call to>}; never {@code null}
     * @param creator             the local self device JID placed on {@code call-creator}; never
     *                            {@code null}
     * @param callId              the locally generated call identifier; never {@code null}
     * @param video               whether to advertise this as a video call (emits {@code <video/>})
     * @param privacy             the one to one trusted contact token bytes, or {@code null} for a group
     *                            offer or when no token is due
     * @param capability          the capability bitfield bytes, or {@code null} to use
     *                            {@link #DEFAULT_CAPABILITY_BYTES}
     * @param destinationPayloads one {@link MessageEncryptedPayload} per peer device receiving the call
     *                            key; never {@code null}, empty for a group offer
     * @param deviceIdentity      the ADV encoded device identity envelope bytes; never {@code null}
     * @param groupJid            the group JID for a group offer, or {@code null} for a one to one offer
     * @param groupParticipants   currently unused; retained for parity with the caller wiring
     * @param callerPn            the caller's phone number JID for {@code caller_pn} on a group offer, or
     *                            {@code null}
     * @param groupInfo           the prebuilt {@code <group_info>} roster child for a group offer, or
     *                            {@code null}
     * @return the {@code <call>} stanza builder ready for dispatch
     * @throws NullPointerException if {@code target}, {@code creator}, {@code callId},
     *                              {@code destinationPayloads}, or {@code deviceIdentity} is
     *                              {@code null}
     */
    public static StanzaBuilder offer(Jid target,
                                      Jid creator,
                                      String callId,
                                      boolean video,
                                      byte[] privacy,
                                      byte[] capability,
                                      List<MessageEncryptedPayload> destinationPayloads,
                                      byte[] deviceIdentity,
                                      Jid groupJid,
                                      Collection<Jid> groupParticipants,
                                      Jid callerPn,
                                      Stanza groupInfo) {
        Objects.requireNonNull(target, "target cannot be null");
        Objects.requireNonNull(creator, "creator cannot be null");
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(destinationPayloads, "destinationPayloads cannot be null");
        Objects.requireNonNull(deviceIdentity, "deviceIdentity cannot be null");

        if (Log.DEBUG)
            LOGGER.log(Level.DEBUG,
                    "call offer built: id={0} target={1} video={2} group={3} destinations={4}",
                    callId, target, video, groupJid != null, destinationPayloads.size());

        var capabilityBytes = capability != null ? capability : DEFAULT_CAPABILITY_BYTES;

        var offerChildren = new ArrayList<Stanza>(11);
        if (groupInfo != null) {
            offerChildren.add(groupInfo);
        }
        // A one to one offer carries the peer's trusted contact token in <privacy>; a group offer carries
        // no such per peer token and omits the element entirely, so a null privacy skips it.
        if (privacy != null) {
            offerChildren.add(new StanzaBuilder()
                    .description(PRIVACY_ELEMENT)
                    .content(privacy)
                    .build());
        }
        offerChildren.add(new StanzaBuilder()
                .description(AUDIO_ELEMENT)
                .attribute(ENC_ATTRIBUTE, AUDIO_CODEC_OPUS)
                .attribute(RATE_ATTRIBUTE, OFFERED_AUDIO_RATE_LOW)
                .build());
        offerChildren.add(new StanzaBuilder()
                .description(AUDIO_ELEMENT)
                .attribute(ENC_ATTRIBUTE, AUDIO_CODEC_OPUS)
                .attribute(RATE_ATTRIBUTE, OFFERED_AUDIO_RATE_HIGH)
                .build());
        offerChildren.add(new StanzaBuilder()
                .description(NET_ELEMENT)
                .attribute(MEDIUM_ATTRIBUTE, OFFER_NET_MEDIUM)
                .build());
        // A one to one offer carries offer level <capability> and <encopt>; a group offer carries neither
        // at offer level (capability rides inside each <group_info> device), so the group offer omits both.
        if (groupJid == null) {
            offerChildren.add(new StanzaBuilder()
                    .description(CAPABILITY_ELEMENT)
                    .attribute(VERSION_ATTRIBUTE, CAPABILITY_VERSION)
                    .content(capabilityBytes)
                    .build());
        }
        // A group offer carries no per device <destination> fanout (the call key is not shipped in the
        // offer for groups); the one to one offer always does. Skip it when there are no payloads.
        if (!destinationPayloads.isEmpty()) {
            offerChildren.add(buildDestination(destinationPayloads));
        }
        if (video) {
            offerChildren.add(new StanzaBuilder()
                    .description(VIDEO_ELEMENT)
                    .build());
        }
        if (groupJid == null) {
            offerChildren.add(new StanzaBuilder()
                    .description(ENCOPT_ELEMENT)
                    .attribute(KEYGEN_ATTRIBUTE, ENCOPT_KEYGEN)
                    .build());
        }
        // A one to one offer carries <device identity> for the server to authenticate the caller; a group
        // offer carries no device identity at all, so the group offer omits it.
        if (groupJid == null) {
            offerChildren.add(new StanzaBuilder()
                    .description(DEVICE_IDENTITY_ELEMENT)
                    .content(deviceIdentity)
                    .build());
        }

        var offerBuilder = new StanzaBuilder()
                .description(OfferStanza.ELEMENT)
                .attribute(CALL_ID_ATTRIBUTE, callId)
                .attribute(CALL_CREATOR_ATTRIBUTE, creator);
        // A group offer carries only group jid at offer level; caller_pn and joinable are added by the
        // server on the copy fanned out to members, not sent by the caller.
        if (groupJid != null) {
            offerBuilder.attribute(GROUP_JID_ATTRIBUTE, groupJid);
        }
        offerBuilder.content(offerChildren);

        return new StanzaBuilder()
                .description(ELEMENT)
                .attribute(TO_ATTRIBUTE, target)
                .content(offerBuilder.build());
    }

    /**
     * Builds the thin outbound offer envelope with an optional caller phone number JID and a prebuilt
     * {@code <group_info>} child, defaulting the {@code <group_info>} to {@code null}.
     *
     * @param target              the peer user LID placed on {@code <call to>}; never {@code null}
     * @param creator             the local self device JID placed on {@code call-creator}; never
     *                            {@code null}
     * @param callId              the locally generated call identifier; never {@code null}
     * @param video               whether to advertise this as a video call
     * @param privacy             the one to one trusted contact token bytes, or {@code null}
     * @param capability          the capability bitfield bytes, or {@code null} to use the default
     * @param destinationPayloads one {@link MessageEncryptedPayload} per peer device; never {@code null}
     * @param deviceIdentity      the ADV encoded device identity envelope bytes; never {@code null}
     * @param groupJid            the group JID for a group offer, or {@code null} for a one to one offer
     * @param groupParticipants   currently unused; retained for parity with the caller wiring
     * @param callerPn            the caller's phone number JID for a group offer, or {@code null}
     * @return the {@code <call>} stanza builder ready for dispatch
     * @throws NullPointerException if any required reference argument is {@code null}
     * @see #offer(Jid, Jid, String, boolean, byte[], byte[], List, byte[], Jid, Collection, Jid, Stanza)
     */
    public static StanzaBuilder offer(Jid target,
                                      Jid creator,
                                      String callId,
                                      boolean video,
                                      byte[] privacy,
                                      byte[] capability,
                                      List<MessageEncryptedPayload> destinationPayloads,
                                      byte[] deviceIdentity,
                                      Jid groupJid,
                                      Collection<Jid> groupParticipants,
                                      Jid callerPn) {
        return offer(target, creator, callId, video, privacy, capability, destinationPayloads,
                deviceIdentity, groupJid, groupParticipants, callerPn, null);
    }

    /**
     * Builds the thin outbound offer envelope, defaulting both the caller phone number JID and the
     * {@code <group_info>} child to {@code null}.
     *
     * @param target              the peer user LID placed on {@code <call to>}; never {@code null}
     * @param creator             the local self device JID placed on {@code call-creator}; never
     *                            {@code null}
     * @param callId              the locally generated call identifier; never {@code null}
     * @param video               whether to advertise this as a video call
     * @param privacy             the one to one trusted contact token bytes, or {@code null}
     * @param capability          the capability bitfield bytes, or {@code null} to use the default
     * @param destinationPayloads one {@link MessageEncryptedPayload} per peer device; never {@code null}
     * @param deviceIdentity      the ADV encoded device identity envelope bytes; never {@code null}
     * @param groupJid            the group JID for a group offer, or {@code null} for a one to one offer
     * @param groupParticipants   currently unused; retained for parity with the caller wiring
     * @return the {@code <call>} stanza builder ready for dispatch
     * @throws NullPointerException if any required reference argument is {@code null}
     * @see #offer(Jid, Jid, String, boolean, byte[], byte[], List, byte[], Jid, Collection, Jid, Stanza)
     */
    public static StanzaBuilder offer(Jid target,
                                      Jid creator,
                                      String callId,
                                      boolean video,
                                      byte[] privacy,
                                      byte[] capability,
                                      List<MessageEncryptedPayload> destinationPayloads,
                                      byte[] deviceIdentity,
                                      Jid groupJid,
                                      Collection<Jid> groupParticipants) {
        return offer(target, creator, callId, video, privacy, capability, destinationPayloads,
                deviceIdentity, groupJid, groupParticipants, null, null);
    }

    /**
     * Builds the {@code <destination>} child carrying one {@code <to>} fanout slot per peer device.
     *
     * <p>Each {@link MessageEncryptedPayload} with a ciphertext produces a {@code <to jid="<device>">
     * <enc v="2" type="msg|pkmsg" count="0">[ciphertext]</enc></to>} slot. A
     * {@link MessageEncryptedPayload#bareDestination(Jid) bare destination} marker (a {@code null}
     * ciphertext) produces a keyless {@code <to jid="<device>"/>} slot instead, the all or nothing
     * fallback that still rings every device when per device encryption failed for any device. Payloads
     * with a {@code null} {@link MessageEncryptedPayload#recipientJid() recipient} are skipped.
     *
     * @param payloads the per device call key fanout, or per device bare destination markers
     * @return the {@code <destination>} stanza; never {@code null}
     */
    private static Stanza buildDestination(List<MessageEncryptedPayload> payloads) {
        var toNodes = new ArrayList<Stanza>(payloads.size());
        for (var payload : payloads) {
            if (payload.recipientJid() == null) {
                continue;
            }
            var toBuilder = new StanzaBuilder()
                    .description(TO_ELEMENT)
                    .attribute(JID_ATTRIBUTE, payload.recipientJid());
            if (payload.ciphertext() != null) {
                toBuilder.content(new StanzaBuilder()
                        .description(ENC_ELEMENT)
                        .attribute(ENC_VERSION_ATTRIBUTE, ENC_CIPHERTEXT_VERSION)
                        .attribute(ENC_TYPE_ATTRIBUTE, payload.type().protocolValue())
                        .attribute(ENC_COUNT_ATTRIBUTE, ENC_COUNT_ZERO)
                        .content(payload.ciphertext())
                        .build());
            }
            toNodes.add(toBuilder.build());
        }
        return new StanzaBuilder()
                .description(DESTINATION_ELEMENT)
                .content(toNodes)
                .build();
    }

    /**
     * Returns whether a {@code <call>} child element tag names an action this factory can decode.
     *
     * <p>This reports membership in the same per tag decoder table {@link #parse(Stanza)} dispatches on, so
     * it is the authoritative set of routable {@code <call>} child tags including the few actions that
     * name a child element yet carry no {@link SignalingType} taxonomy ordinal
     * ({@link RingingStanza}, {@link RaiseHandStanza}). {@link CallSignalingRouter} consults it to route a
     * tag that {@link SignalingType#ofWireTag(String)} does not resolve, so the router and the
     * parser agree on which inbound elements are deliverable. A tag in this set is not guaranteed to
     * decode for a specific stanza: a decoder whose body is structurally optional still yields an empty
     * {@link #parse(Stanza)} result when that body is absent, and a decoder finding a required attribute
     * missing throws; this method reports only that a decoder exists for the tag.
     *
     * @param tag the {@code <call>} child element tag, or {@code null}
     * @return {@code true} when a decoder is registered for the tag, {@code false} otherwise
     */
    public static boolean isKnownTag(String tag) {
        return tag != null && DECODERS.containsKey(tag);
    }

    /**
     * Parses an unwrapped {@code <call>} child element into its typed {@link CallMessage}.
     *
     * <p>The supplied stanza is the action element itself, the single child of a {@code <call>} envelope,
     * not the envelope: {@link CallReceiver} reads the envelope, takes its child, and passes that
     * child here. Dispatch keys on the child element tag through {@link Stanza#description()}; a tag the
     * decoder table does not know, and an element a known decoder cannot decode because a structurally
     * optional body is absent, both yield an empty result so the caller drops the stanza. A decoder that
     * finds a required attribute missing throws, matching each record's {@code of} contract; callers
     * that route untrusted input should validate the universal header through {@link CallSignalingRouter}
     * first, which {@link CallReceiver} does.
     *
     * @param payload the unwrapped {@code <call>} child element
     * @return the decoded message, or an empty result when the tag is unknown or the element is
     *         undecodable
     * @throws NullPointerException if {@code payload} is {@code null}
     */
    public static Optional<CallMessage> parse(Stanza payload) {
        Objects.requireNonNull(payload, "payload cannot be null");
        var tag = payload.description();
        var decoder = DECODERS.get(tag);
        if (decoder == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call stanza tag not decodable: {0}", tag);
            return Optional.empty();
        }
        var decoded = decoder.apply(payload);
        if (decoded == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call stanza element undecodable: {0}", tag);
        } else if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "call stanza parsed: tag={0} type={1}", tag, decoded.type());
        }
        return Optional.ofNullable(decoded);
    }
}
