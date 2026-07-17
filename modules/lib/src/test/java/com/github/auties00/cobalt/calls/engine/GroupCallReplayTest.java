package com.github.auties00.cobalt.calls.engine;

import com.github.auties00.cobalt.calls.crypto.CallRekeyEnvelope;
import com.github.auties00.cobalt.calls.signaling.session.CallCapability;
import com.github.auties00.cobalt.calls.signaling.group.GroupInfoStanza;
import com.github.auties00.cobalt.calls.signaling.group.GroupUpdateStanza;
import com.github.auties00.cobalt.calls.signaling.session.OfferStanza;
import com.github.auties00.cobalt.calls.signaling.incall.RekeyStanza;
import com.github.auties00.cobalt.calls.engine.participant.CallParticipantPlatform;
import com.github.auties00.cobalt.calls.engine.participant.CallParticipantUserNode;
import com.github.auties00.cobalt.message.MessageEncryptionType;
import com.github.auties00.cobalt.wire.linked.call.CallState;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.calls.engine.state.CallLifecycleState;
import com.github.auties00.cobalt.calls.engine.mediaplane.MediaStreams;
import com.github.auties00.cobalt.calls.engine.participant.CallMembership;
import com.github.auties00.cobalt.calls.signaling.session.CallCodecDescriptor;
import com.github.auties00.cobalt.calls.signaling.session.CallMediaDescriptor;

/**
 * Adversarial replay of the captured group-call lifecycle through the real {@link LifecycleController}
 * inbound path: an inbound group {@code <offer>} rings a new group call, an inbound {@code <group_update>}
 * carrying the connected roster reconciles the call's
 * {@link com.github.auties00.cobalt.calls.engine.participant.CallMembership}, the active-versus-lonely
 * decision settles the call by connected-peer count, the captured group events fire, and the captured
 * {@code <enc_rekey>} is a processable stanza. The fixtures reproduce the {@code group-stanzas-peer.jsonl}
 * call-id and the {@code group-sfu.json} roster shape (a {@code <user state="connected">} with a
 * {@code <device>}), rendered through each record's own {@code toNode()} and fed back in through the typed
 * records so the wire tags and the universal header are the ones the records emit.
 *
 * <p>The active-versus-lonely settle is driven from {@code startGroupCall} (the caller side) plus
 * {@code onMediaConnected} (so the call sits in a connected in-call leg the decision applies to) and then an
 * inbound {@code group_update} reconcile, because the active decision only transitions a call already in
 * {@code CALL_ACTIVE}/{@code CONNECTED_LONELY}. The inbound-offer group path now seeds its
 * {@link com.github.auties00.cobalt.calls.engine.participant.CallMembership} from the offer's
 * {@code <group_info>}, so a later {@code group_update} reconciles against a populated roster rather than
 * being dropped; that closed gap is covered by {@link InboundOfferMembership}.
 */
@DisplayName("calls group-call replay (inbound offer -> group_update -> connected)")
class GroupCallReplayTest {
    // Group capture identities (CAPTURE-FINDINGS.md group section).
    private static final String GROUP_CALL_ID = "0023CDF8FF5621A306A3337E63C9F0B5";
    private static final Jid GROUP_HOST_LID_DEVICE = Jid.of("83116928594056:2@lid");
    private static final Jid GROUP_JID = Jid.of("120363012345678901@g.us");
    private static final Jid SELF = Jid.of("39110693621863:58@lid");
    private static final Jid PEER = Jid.of("258252122116273@lid");
    private static final Jid PEER_DEVICE = Jid.of("258252122116273:2@lid");

    @Nested
    @DisplayName("inbound group offer rings a new group call")
    class GroupOffer {
        @Test
        @DisplayName("a captured group offer rings: RECEIVED_CALL, public RINGING, no call key recovered")
        void groupOfferRings() {
            var harness = new ControllerHarness();
            var incoming = harness.controller().handleIncomingOffer(groupOffer(), GROUP_HOST_LID_DEVICE);

            assertTrue(incoming.isPresent(), "an inbound group offer must surface an IncomingCall");
            assertTrue(incoming.get().group(), "the incoming call must be flagged as a group call");
            assertEquals(GROUP_JID, incoming.get().chatJid());

            var context = harness.manager().getByCallId(GROUP_CALL_ID).orElseThrow();
            // A group offer carries a media descriptor (audio codecs), so the ring state is RECEIVED_CALL.
            assertSame(CallLifecycleState.RECEIVED_CALL, context.state());
            assertSame(CallState.RINGING, context.call().state());
            // A group offer ships NO call key in the offer (it arrives post-join as enc_rekey), so the
            // crypto fanout encryptor was never used for this offer.
            assertTrue(harness.crypto().offerFanouts().isEmpty());
            assertTrue(harness.events().count(CallEventType.CALL_OFFER_RECEIVED) >= 1,
                    "the offer ring must fire CALL_OFFER_RECEIVED");
        }
    }

    @Nested
    @DisplayName("inbound group_update reconciles membership and decides active vs lonely")
    class GroupUpdateReconcile {
        @Test
        @DisplayName("a connected-roster group_update drives a connected group call to CALL_ACTIVE")
        void reachesActiveOnConnectedPeer() {
            var harness = new ControllerHarness();
            // Establish a group call with membership (the placement path), then connect its media plane so
            // the call sits in a connected in-call leg the active-vs-lonely decision applies to.
            var call = harness.controller().startGroupCall(SELF, List.of(PEER), GROUP_JID, false, MediaStreams.none());
            harness.controller().onMediaConnected(call.callId());
            var beforeUpdate = harness.manager().getByCallId(call.callId()).orElseThrow().state();
            assertTrue(beforeUpdate == CallLifecycleState.CONNECTED_LONELY || beforeUpdate == CallLifecycleState.CALL_ACTIVE,
                    "a connected group call sits in an in-call leg before the roster reconcile");

            // Replay an inbound group_update carrying the connected roster (re-parsed from its own wire stanza,
            // the round trip the inbound receiver does): membership reconciles and, with a peer connected,
            // the call reaches CALL_ACTIVE.
            var update = new GroupUpdateStanza(call.callId(), SELF, null, false, false, connectedRoster(),
                    List.of());
            harness.controller().handleIncomingMessage(GroupUpdateStanza.of(update.toStanza()), GROUP_HOST_LID_DEVICE);

            var context = harness.manager().getByCallId(call.callId()).orElseThrow();
            assertSame(CallLifecycleState.CALL_ACTIVE, context.state(),
                    "a connected peer in the reconciled roster must drive CALL_ACTIVE");
            assertSame(CallState.ACTIVE, context.call().state());
            assertTrue(harness.events().count(CallEventType.GROUP_INFO_CHANGED) >= 1,
                    "a membership change must fire GROUP_INFO_CHANGED");
        }

        @Test
        @DisplayName("the last peer leaving drives the connected group call back to CONNECTED_LONELY")
        void lastPeerLeavingReturnsLonely() {
            var harness = new ControllerHarness();
            var call = harness.controller().startGroupCall(SELF, List.of(PEER), GROUP_JID, false, MediaStreams.none());
            harness.controller().onMediaConnected(call.callId());

            // First a connected peer -> CALL_ACTIVE.
            var connected = new GroupUpdateStanza(call.callId(), SELF, null, false, false, connectedRoster(),
                    List.of());
            harness.controller().handleIncomingMessage(GroupUpdateStanza.of(connected.toStanza()), GROUP_HOST_LID_DEVICE);
            assertSame(CallLifecycleState.CALL_ACTIVE,
                    harness.manager().getByCallId(call.callId()).orElseThrow().state());

            // Then an empty roster (the peer left) -> back to CONNECTED_LONELY (the closed-set edge).
            var emptyRoster = GroupInfoStanza.ofUsers(null, 32, List.of());
            var left = new GroupUpdateStanza(call.callId(), SELF, null, false, false, emptyRoster, List.of());
            harness.controller().handleIncomingMessage(GroupUpdateStanza.of(left.toStanza()), GROUP_HOST_LID_DEVICE);
            assertSame(CallLifecycleState.CONNECTED_LONELY,
                    harness.manager().getByCallId(call.callId()).orElseThrow().state(),
                    "with the last peer gone the group call returns to the lonely state");
        }

        @Test
        @DisplayName("the captured group_update tags decode and carry the connected roster")
        void groupUpdateDecodes() {
            var update = new GroupUpdateStanza(GROUP_CALL_ID, GROUP_HOST_LID_DEVICE, null, false, false,
                    connectedRoster(), List.of());
            var decoded = GroupUpdateStanza.of(update.toStanza());
            assertEquals(GROUP_CALL_ID, decoded.callId().orElseThrow());
            assertTrue(decoded.groupInfoValue().isPresent(), "the decoded update must carry a group_info roster");
            var roster = decoded.groupInfoValue().orElseThrow();
            var members = roster.entries().stream()
                    .map(CallParticipantUserNode::of)
                    .flatMap(Optional::stream)
                    .toList();
            assertEquals(1, members.size());
            assertEquals(Optional.of("connected"), members.getFirst().state(),
                    "the captured roster member is in the connected server-state");
            assertEquals(1, members.getFirst().devices().size(),
                    "the connected member carries its device list");
        }
    }

    @Nested
    @DisplayName("inbound-offer group call seeds membership from the offer's group_info")
    class InboundOfferMembership {
        @Test
        @DisplayName("a group_update on a group call rung via handleIncomingOffer reconciles the seeded roster")
        void inboundOfferGroupUpdateReconciles() {
            // handleIncomingOffer attaches a CallMembership to a group call and seeds it from the offer's
            // <group_info>, so a subsequent inbound group_update reconciles against the populated roster
            // rather than being dropped by handleGroupUpdate's `membership == null` guard. The offer roster
            // carries PEER connected, so an empty group_update removes that member: a non-empty diff that
            // fires GROUP_INFO_CHANGED, the observable proof the membership was seeded and reconciled.
            var harness = new ControllerHarness();
            harness.controller().handleIncomingOffer(groupOffer(), GROUP_HOST_LID_DEVICE);
            var groupInfoBefore = harness.events().count(CallEventType.GROUP_INFO_CHANGED);

            var emptyRoster = GroupInfoStanza.ofUsers(null, 32, List.of());
            var update = new GroupUpdateStanza(GROUP_CALL_ID, GROUP_HOST_LID_DEVICE, null, false, false,
                    emptyRoster, List.of());
            harness.controller().handleIncomingMessage(GroupUpdateStanza.of(update.toStanza()), GROUP_HOST_LID_DEVICE);

            // The seeded peer is removed by the empty roster: a non-empty reconcile diff fires
            // GROUP_INFO_CHANGED, which a membership-less call could never do.
            assertEquals(groupInfoBefore + 1, harness.events().count(CallEventType.GROUP_INFO_CHANGED),
                    "an inbound-offer group call reconciles its seeded roster on a group_update");
            // The reconcile does not itself advance the ring state; the active decision only acts on a call
            // already in an in-call leg (driven by acceptCall + onMediaConnected on the live join path).
            assertSame(CallLifecycleState.RECEIVED_CALL,
                    harness.manager().getByCallId(GROUP_CALL_ID).orElseThrow().state());
        }
    }

    @Nested
    @DisplayName("captured enc_rekey is a well-formed processable stanza")
    class EncRekey {
        @Test
        @DisplayName("the captured single-32B unicast enc_rekey envelope renders the expected wire shape")
        void encRekeyWireShape() {
            // The captured group enc_rekey (group-rekey.json): a unicast <call to=device><enc_rekey> with
            // exactly one <enc> child carrying a single 32-byte key. Build it via the P9 outbound envelope
            // record and confirm the wire shape is the single-key unicast form.
            var ciphertext = new byte[64];
            for (var i = 0; i < ciphertext.length; i++) {
                ciphertext[i] = (byte) (i + 1);
            }
            var envelope = new CallRekeyEnvelope(PEER_DEVICE, MessageEncryptionType.PKMSG, ciphertext,
                    new byte[]{7, 7, 7});
            var rekeyNode = envelope.toNode(GROUP_CALL_ID, GROUP_HOST_LID_DEVICE, 25);

            assertEquals(RekeyStanza.ELEMENT, rekeyNode.description(), "the wire tag is <enc_rekey>");
            assertEquals(GROUP_CALL_ID, rekeyNode.getAttributeAsString("call-id").orElseThrow());
            assertEquals(25, rekeyNode.getAttributeAsInt("transaction-id", -1),
                    "the rekey carries the captured rotation transaction id");
            // Exactly one <enc> child (the single-key unicast shape), the encopt sibling, and the
            // device-identity (this envelope is a pkmsg, so the identity is attached).
            assertEquals(1, rekeyNode.streamChildren("enc").count(),
                    "a captured enc_rekey carries exactly one <enc> (single key)");
            assertTrue(rekeyNode.getChild("device-identity").isPresent(),
                    "a pkmsg enc_rekey attaches the device identity");
            assertTrue(rekeyNode.getChild("encopt").isPresent(), "the enc_rekey carries an <encopt> sibling");
        }
    }

    // ---- fixtures -------------------------------------------------------------------------------------

    /**
     * Builds the captured inbound group {@code <offer>}: a group JID, joinable, audio codecs (so a media
     * descriptor is present), a {@code <group_info>} roster, and a {@code <relay>} block. No per-device
     * {@code <enc>} fanout (a group offer ships no key).
     */
    private static OfferStanza groupOffer() {
        var relay = new StanzaBuilder()
                .description("relay")
                .attribute("uuid", "0123456789ABCDEF")
                .attribute("participant_uuid", "ABCDEF01")
                .content(new StanzaBuilder()
                        .description("hbh_key")
                        .content("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
                        .build())
                .build();
        var audioCodecs = List.of(
                com.github.auties00.cobalt.calls.signaling.session.CallCodecDescriptor.audio("opus", 16000));
        var media = new com.github.auties00.cobalt.calls.signaling.session.CallMediaDescriptor(2, 16000);
        return new OfferStanza(GROUP_CALL_ID, GROUP_HOST_LID_DEVICE, null, null, null, GROUP_JID, null, null,
                true, false, null, -1, 3, List.of(), audioCodecs, List.of(), List.of(), media,
                null, connectedRoster().toStanza(), null, null, relay, null, List.of(), null);
    }

    /**
     * Builds the captured connected roster: a single peer carrying {@code state="connected"} and one
     * {@code <device>}, the {@code group-sfu.json} in-call participant shape.
     */
    private static GroupInfoStanza connectedRoster() {
        var deviceNode = new CallParticipantUserNode.Device(PEER_DEVICE, Optional.of(CallParticipantPlatform.WEB),
                -1, Optional.<CallCapability>empty());
        var user = new CallParticipantUserNode(CallParticipantUserNode.ChildForm.USER, PEER,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.of("connected"), -1,
                Optional.empty(), -1, Optional.empty(), false, Optional.empty(), false, List.of(deviceNode));
        return GroupInfoStanza.ofUsers(null, 32, List.of(user.toNode()));
    }
}
