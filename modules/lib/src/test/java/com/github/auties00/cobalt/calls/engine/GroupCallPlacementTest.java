package com.github.auties00.cobalt.calls.engine;

import com.github.auties00.cobalt.calls.crypto.CallRekeyEnvelope;
import com.github.auties00.cobalt.calls.signaling.session.CallCapability;
import com.github.auties00.cobalt.calls.signaling.group.GroupInfoStanza;
import com.github.auties00.cobalt.calls.signaling.group.GroupUpdateStanza;
import com.github.auties00.cobalt.calls.engine.participant.CallParticipantPlatform;
import com.github.auties00.cobalt.calls.engine.participant.CallParticipantUserNode;
import com.github.auties00.cobalt.wire.linked.call.CallEndReason;
import com.github.auties00.cobalt.wire.linked.call.CallState;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.auties00.cobalt.calls.engine.timer.CallTimerKind;
import com.github.auties00.cobalt.calls.engine.mediaplane.MediaStreams;

/**
 * Adversarial verification of the P9 outbound group-call placement on the real
 * {@link LifecycleController}: that {@code startGroupCall} sends a group offer with a roster and no
 * per-device key fanout, mints exactly one thirty-two-byte call key, fans that one key out as a unicast
 * per-participant-device {@code <enc_rekey>}, brings up the group media plane, and arms the group timers;
 * and that a membership change drives a fresh rekey and the active-versus-lonely re-decision; and that a
 * lonely/ringing timeout teardown ends the call with {@link CallEndReason#TIMEOUT}.
 *
 * <p>The controller is driven through {@link ControllerHarness}, which wires it over a real state
 * guard and call manager plus deterministic recording fakes for the host, crypto, media plane, timers, and
 * event sink. A test that needs the offer-ack to echo a connected roster (so the placement's rekey fanout
 * has device recipients) constructs the harness with {@link ControllerHarness#ackNodeWithRoster}. The
 * per-participant fanout shape (one {@code <enc>} per recipient device, a single thirty-two-byte key,
 * addressed unicast) is the {@code group-rekey.json} finding; the placement-offer shape (group JID, roster,
 * no key fanout) is the {@code group-sfu.json} finding.
 */
@DisplayName("calls group-call placement, key fanout, and lonely teardown")
class GroupCallPlacementTest {
    private static final Jid SELF = Jid.of("39110693621863:58@lid");
    private static final Jid GROUP_JID = Jid.of("120363012345678901@g.us");
    private static final Jid PEER_A = Jid.of("258252122116273@lid");
    private static final Jid PEER_B = Jid.of("83116928594056@lid");
    private static final Jid PEER_A_DEVICE = Jid.of("258252122116273:2@lid");
    private static final Jid PEER_B_DEVICE = Jid.of("83116928594056:3@lid");

    @Nested
    @DisplayName("startGroupCall offer and key fanout")
    class Placement {
        @Test
        @DisplayName("places a group call: group offer sent, no per-device key in the offer, media plane up")
        void placesGroupCall() {
            var harness = new ControllerHarness();
            var call = harness.controller().startGroupCall(SELF, List.of(PEER_A, PEER_B), GROUP_JID, false, MediaStreams.none());

            assertTrue(call.isGroup(), "the placed call must be a group call");
            assertEquals(GROUP_JID, call.chatJid(), "the call's chat JID must be the group");

            // The group offer carries the group_info roster and NOT a per-device <enc> fanout (the group
            // key arrives post-join as enc_rekey, not in the offer): the crypto offer-fanout encryptor is
            // never called for a group placement.
            assertTrue(harness.crypto().offerFanouts().isEmpty(),
                    "a group offer must not fan the call key out per device");
            // The sent offer carries a <group_info> roster and no per-device <destination> key fanout. The
            // offer rides the synchronous offer-ack seam, so it is captured there rather than on the host.
            var offers = harness.sentOffers();
            assertEquals(1, offers.size(), "exactly one group offer is sent");
            assertTrue(offers.getFirst().getChild("group_info").isPresent(),
                    "the group offer carries a <group_info> roster");
            assertTrue(offers.getFirst().streamChildren("destination").findAny().isEmpty(),
                    "the group offer carries no per-device <destination> key fanout");

            // The media plane was brought up from the ack relay as the caller (group SFU path), with audio.
            var bringUps = harness.mediaPlane().bringUps();
            assertEquals(1, bringUps.size(), "the group placement must bring up the media plane once");
            assertTrue(bringUps.getFirst().isCaller(), "the placing side is the caller");
            assertFalse(bringUps.getFirst().video(), "an audio group call brings up audio-only media");

            // The group timers are armed: the watchdog, the group heartbeat, and the SFrame key-rotation tick.
            var callId = call.callId();
            assertTrue(harness.timers().isArmed(callId, CallTimerKind.PERIODIC));
            assertTrue(harness.timers().isArmed(callId, CallTimerKind.HEARTBEAT));
            assertTrue(harness.timers().isArmed(callId, CallTimerKind.UPDATE_ENCRYPTION_KEY));
        }

        @Test
        @DisplayName("mints exactly one 32-byte call key and fans it as one unicast enc_rekey per peer device")
        void fansOneKeyPerParticipantDevice() {
            // The offer-ack echoes a connected roster (each peer with one connected device) so the placement
            // rekey fanout has device recipients.
            var ackRoster = connectedRoster(
                    connectedUserWithDevice(PEER_A, PEER_A_DEVICE),
                    connectedUserWithDevice(PEER_B, PEER_B_DEVICE));
            var harness = new ControllerHarness(ControllerHarness.ackNodeWithRoster(ackRoster));
            harness.controller().startGroupCall(SELF, List.of(PEER_A, PEER_B), GROUP_JID, false, MediaStreams.none());

            // Exactly one call key was minted for the whole placement (SPEC 8: one 32B raw key).
            assertEquals(1, harness.crypto().mintedKeys().size(), "a group placement mints exactly one call key");
            assertEquals(32, harness.crypto().mintedKeys().getFirst().length, "the minted call key is 32 bytes");
            assertArrayEquals(ControllerHarness.CALL_KEY, harness.crypto().mintedKeys().getFirst());

            // The key was fanned out once, to exactly the two connected peer devices (unicast per device),
            // never to the local self device.
            var rekeyFanouts = harness.crypto().rekeyFanouts();
            assertEquals(1, rekeyFanouts.size(), "the placement fans the key out exactly once");
            var recipients = rekeyFanouts.getFirst();
            assertEquals(2, recipients.size(), "one rekey recipient per connected peer device");
            assertTrue(recipients.contains(PEER_A_DEVICE));
            assertTrue(recipients.contains(PEER_B_DEVICE));
            assertFalse(recipients.stream().anyMatch(jid -> jid.toUserJid().equals(SELF.toUserJid())),
                    "the local participant is never a rekey recipient");

            // On the wire each recipient gets its own <call to=device><enc_rekey> with exactly ONE <enc>
            // child carrying the single wrapped key (the single-32B unicast shape from group-rekey.json).
            var encRekeys = harness.host().actionsTagged(CallRekeyEnvelope.ELEMENT);
            assertEquals(2, encRekeys.size(), "one enc_rekey stanza per recipient device");
            for (var rekey : encRekeys) {
                assertEquals(1, rekey.streamChildren("enc").count(),
                        "each enc_rekey carries exactly one <enc> (single key)");
            }
        }

        @Test
        @DisplayName("the wrapped rekey plaintext is the single minted call key, not three per-domain masters")
        void rekeyCarriesSingleKey() {
            var ackRoster = connectedRoster(connectedUserWithDevice(PEER_A, PEER_A_DEVICE));
            var harness = new ControllerHarness(ControllerHarness.ackNodeWithRoster(ackRoster));
            harness.controller().startGroupCall(SELF, List.of(PEER_A), GROUP_JID, false, MediaStreams.none());

            // The plaintext wrapped for the fanout is the wrapping of the one 32-byte key (SPEC 7.3 / Q3:
            // the wire enc_rekey is a single raw key; the three audio/video/appdata keys are derived
            // locally, not transmitted), so the wrapped plaintext embeds exactly the minted key bytes.
            var wrapped = harness.crypto().lastWrappedPlaintext();
            var embedded = java.util.Arrays.copyOfRange(wrapped, wrapped.length - 32, wrapped.length);
            assertArrayEquals(ControllerHarness.CALL_KEY, embedded,
                    "the rekey plaintext wraps the single 32-byte call key");
        }

        @Test
        @DisplayName("a membership change re-fans the key and re-decides active vs lonely")
        void membershipChangeRefansAndRedecides() {
            var ackRoster = connectedRoster(connectedUserWithDevice(PEER_A, PEER_A_DEVICE));
            var harness = new ControllerHarness(ControllerHarness.ackNodeWithRoster(ackRoster));
            var call = harness.controller().startGroupCall(SELF, List.of(PEER_A), GROUP_JID, false, MediaStreams.none());
            // Drive the call to a connected in-call state so the group update's active-vs-lonely decision and
            // re-fan path runs.
            harness.controller().onMediaConnected(call.callId());

            var rekeyCountBeforeUpdate = harness.crypto().rekeyFanouts().size();

            // An inbound group_update that adds a second connected peer device reconciles membership and,
            // because a caller re-shares its key on a membership change, fans the key out again.
            var updatedRoster = connectedRoster(
                    connectedUserWithDevice(PEER_A, PEER_A_DEVICE),
                    connectedUserWithDevice(PEER_B, PEER_B_DEVICE));
            var groupUpdate = new GroupUpdateStanza(call.callId(), SELF, null, false, false, updatedRoster,
                    List.of());
            harness.controller().handleIncomingMessage(groupUpdate, PEER_B);

            assertTrue(harness.crypto().rekeyFanouts().size() > rekeyCountBeforeUpdate,
                    "a membership change must trigger a fresh per-participant key re-fan");
            // The most recent fan-out reaches the newly added peer device.
            assertTrue(harness.crypto().rekeyFanouts().getLast().contains(PEER_B_DEVICE),
                    "the re-fan must reach the newly connected peer device");
        }
    }

    @Nested
    @DisplayName("lonely / ringing timeout teardown")
    class TimerTeardown {
        @Test
        @DisplayName("endCall(TIMEOUT) on a ringing call ends it with CallEndReason.TIMEOUT and CallState.ENDED")
        void timeoutEndsCall() {
            // This is the teardown a fired caller-lonely / connected-lonely timeout drives: the timer
            // callback calls controller.endCall(callId, TIMEOUT). Drive a placed ringing group call and end
            // it the way the lonely-timeout callback does.
            var harness = new ControllerHarness();
            var call = harness.controller().startGroupCall(SELF, List.of(PEER_A), GROUP_JID, false, MediaStreams.none());
            var callId = call.callId();
            assertFalse(call.state() == CallState.ENDED, "the call must not start ended");

            harness.controller().endCall(callId, CallEndReason.TIMEOUT);

            assertSame(CallState.ENDED, call.state(), "a timed-out call must end");
            assertEquals(Optional.of(CallEndReason.TIMEOUT), call.endReason(),
                    "the lonely-timeout teardown must record CallEndReason.TIMEOUT");
            // The teardown cancels every per-call timer and the manager no longer holds the call.
            assertTrue(harness.timers().cancelledAll().contains(callId),
                    "teardown must cancel all per-call timers");
            assertTrue(harness.manager().getByCallId(callId).isEmpty(),
                    "the manager slot must be freed on teardown");
        }

        @Test
        @DisplayName("the teardown closes the media plane that the placement brought up")
        void teardownClosesMediaPlane() {
            var ackRoster = connectedRoster(connectedUserWithDevice(PEER_A, PEER_A_DEVICE));
            var harness = new ControllerHarness(ControllerHarness.ackNodeWithRoster(ackRoster));
            var call = harness.controller().startGroupCall(SELF, List.of(PEER_A), GROUP_JID, false, MediaStreams.none());
            var session = harness.mediaPlane().bringUps().getFirst().session();
            assertFalse(session.closed(), "the session is live while the call is up");

            harness.controller().endCall(call.callId(), CallEndReason.TIMEOUT);
            assertTrue(session.closed(), "the lonely-timeout teardown must close the media plane");
        }
    }

    // ---- fixtures ---------------------------------------------------------------------------------------

    /**
     * Builds a connected {@code <user>} entry carrying one connected device, the roster shape the relay
     * echoes for an in-call participant (state="connected" plus a {@code <device>} child).
     */
    private static CallParticipantUserNode connectedUserWithDevice(Jid user, Jid device) {
        var deviceNode = new CallParticipantUserNode.Device(device, Optional.of(CallParticipantPlatform.WEB), -1,
                Optional.<CallCapability>empty());
        return new CallParticipantUserNode(CallParticipantUserNode.ChildForm.USER, user,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.of("connected"), -1,
                Optional.empty(), -1, Optional.empty(), false, Optional.empty(), false, List.of(deviceNode));
    }

    private static GroupInfoStanza connectedRoster(CallParticipantUserNode... users) {
        var nodes = java.util.Arrays.stream(users).map(CallParticipantUserNode::toNode).toList();
        return GroupInfoStanza.ofUsers(null, 32, nodes);
    }
}
