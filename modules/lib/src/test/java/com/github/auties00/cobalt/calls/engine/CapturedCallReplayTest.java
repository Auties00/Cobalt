package com.github.auties00.cobalt.calls.engine;

import com.github.auties00.cobalt.calls.engine.context.CallContext.CallDirection;
import com.github.auties00.cobalt.calls.engine.context.CallContext.CallRole;
import com.github.auties00.cobalt.calls.engine.IncomingMessageRouter.DedupState;
import com.github.auties00.cobalt.calls.engine.IncomingMessageRouter.RoutingClass;
import com.github.auties00.cobalt.calls.signaling.session.AcceptStanza;
import com.github.auties00.cobalt.calls.signaling.receive.CallSignalingRouter;
import com.github.auties00.cobalt.calls.signaling.receive.CallSignalingRouter.Disposition;
import com.github.auties00.cobalt.calls.signaling.CallStanza;
import com.github.auties00.cobalt.calls.signaling.session.OfferStanza;
import com.github.auties00.cobalt.calls.signaling.session.PreacceptStanza;
import com.github.auties00.cobalt.calls.signaling.session.TerminateStanza;
import com.github.auties00.cobalt.calls.signaling.session.TransportStanza;
import com.github.auties00.cobalt.wire.linked.call.CallEndReason;
import com.github.auties00.cobalt.wire.linked.call.CallState;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.calls.engine.state.CallLifecycleState;
import com.github.auties00.cobalt.calls.engine.state.CallStateMachine;
import com.github.auties00.cobalt.calls.crypto.LiveCallKeyExchange;
import com.github.auties00.cobalt.calls.engine.context.CallContext;
import com.github.auties00.cobalt.calls.engine.context.CallManager;
import com.github.auties00.cobalt.calls.platform.LiveVoipHostApi;
import com.github.auties00.cobalt.calls.platform.VoipHostApi;

/**
 * Adversarial replay of the captured 1:1 and group call lifecycles against the inbound signaling pipeline
 * (CallSignalingRouter -> CallStanza.parse -> IncomingMessageRouter) and the state machine.
 *
 * <p>The fixtures reproduce the exact inbound {@code <call>} envelopes the callee received in
 * {@code re/calls2-spec/captures/stanzas-peer.jsonl} (call-id {@code 006454CB35389E8C2BE8C5AAAF1CC4E5},
 * caller LID {@code 39110693621863:58@lid}) and the group lifecycle in
 * {@code group-stanzas-peer.jsonl} (call-id {@code 0023CDF8FF5621A306A3337E63C9F0B5}), built as typed
 * records rendered through their own {@code toNode()} so the wire tags, the universal header, and the LID
 * addressing are the ones the records emit. Each captured signal is driven through the two router stages
 * and asserted against the routing verdict the engine makes, then the call's CallState sequence is driven
 * through the transition guard with a captured-event harness so the public CallState projection and the
 * emitted {@link CallEventType} / onCall* sequence are asserted end to end with no native or network
 * dependency.
 *
 * <p>The full {@link LifecycleController} is not exercised end to end here because it is constructed
 * over the sealed {@code VoipHostApi} (permits only {@code LiveVoipHostApi}, which needs a live client) and
 * the final {@code LiveCallKeyExchange} (needs a live store and message-encryption pipeline), neither of
 * which a unit test can stub; that gap is reported as a finding. The lifecycle behaviour the controller
 * drives is verified at the seam it delegates to (the state machine) plus the two router stages it sits
 * behind.
 */
@DisplayName("calls captured-call replay (1:1 and group)")
class CapturedCallReplayTest {
    // 1:1 capture identities (CAPTURE-FINDINGS.md).
    private static final String ONE_TO_ONE_CALL_ID = "006454CB35389E8C2BE8C5AAAF1CC4E5";
    private static final Jid CALLER_LID_DEVICE = Jid.of("39110693621863:58@lid");
    private static final Jid CALLEE_LID_DEVICE = Jid.of("258252122116273:2@lid");
    private static final Jid CALLER_PN = Jid.of("19153544650@s.whatsapp.net");

    // Group capture identities (CAPTURE-FINDINGS.md group section).
    private static final String GROUP_CALL_ID = "0023CDF8FF5621A306A3337E63C9F0B5";
    private static final Jid GROUP_HOST_LID_DEVICE = Jid.of("83116928594056:2@lid");
    private static final Jid GROUP_JID = Jid.of("120363012345678901@g.us");

    private final CallSignalingRouter signalingRouter = new CallSignalingRouter();
    private final IncomingMessageRouter<CallContext> messageRouter = new IncomingMessageRouter<>();

    // ---- captured-stanza fixtures (rendered through the records' own toNode) ---------------------------

    private static OfferStanza oneToOneOffer() {
        // 1:1 offer: audio opus 16000/8000, no <media> descriptor, no video codecs (audio call), LID
        // creator, caller_pn + joinable as captured. An empty keyDistribution keeps the fixture decryption
        // free; the lifecycle treats a 1:1 offer with no media descriptor as RECEIVED_CALL_WITHOUT_OFFER.
        return new OfferStanza(ONE_TO_ONE_CALL_ID, CALLER_LID_DEVICE, CALLER_PN, null, null, null, null, null,
                true, false, null, -1, 3, List.of(), List.of(), List.of(), List.of(), null, null, null, null,
                null, null, null, List.of(), null);
    }

    private static AcceptStanza oneToOneAccept() {
        return new AcceptStanza(ONE_TO_ONE_CALL_ID, CALLER_LID_DEVICE, 3, List.of(), List.of(), List.of(), List.of(),
                null, null, null, null);
    }

    private static PreacceptStanza oneToOnePreaccept() {
        return new PreacceptStanza(ONE_TO_ONE_CALL_ID, CALLER_LID_DEVICE, List.of(), List.of(), List.of(), null, null);
    }

    private static TransportStanza oneToOneTransport() {
        return new TransportStanza(ONE_TO_ONE_CALL_ID, CALLER_LID_DEVICE, false, null, -1, null, null, null,
                null, null, List.of());
    }

    private static TerminateStanza oneToOneTerminate() {
        // Captured final hangup terminate carries no reason literal (default hangup).
        return TerminateStanza.of(ONE_TO_ONE_CALL_ID, CALLER_LID_DEVICE, CallEndReason.HANGUP, List.of());
    }

    // ---- Stage A: CallSignalingRouter classification ----------------------------------------------------

    // Feeds CallSignalingRouter.classify by extracting the classifier's fields (tag, call-id, call-creator)
    //  off a rendered payload stanza, the same way CallReceiver reads them from the wire.
    private static CallSignalingRouter.Verdict classify(CallSignalingRouter router, Stanza payload,
                                                        Jid senderLid, boolean callExists) {
        var description = payload.description();
        var callId = payload.getAttributeAsString("call-id", null);
        var callCreator = payload.getAttributeAsJid("call-creator", null);
        return router.classify(description, callId, callCreator, senderLid, callExists);
    }

    @Nested
    @DisplayName("Stage A: CallSignalingRouter classifies the captured inbound envelopes")
    class SignalingClassification {
        @Test
        @DisplayName("a LID offer for an unknown call is BUFFERed until the call object exists")
        void offerBuffersBeforeCall() {
            var payload = oneToOneOffer().toStanza();
            var verdict = classify(signalingRouter, payload, CALLER_LID_DEVICE, false);
            assertSame(Disposition.BUFFER, verdict.disposition());
            assertEquals(ONE_TO_ONE_CALL_ID, verdict.callId().orElseThrow());
        }

        @Test
        @DisplayName("the in-call legs PROCESS once the call object exists")
        void inCallLegsProcess() {
            for (var payload : List.of(oneToOneAccept().toStanza(), oneToOneTransport().toStanza(),
                    oneToOneTerminate().toStanza())) {
                var verdict = classify(signalingRouter, payload, CALLER_LID_DEVICE, true);
                assertSame(Disposition.PROCESS, verdict.disposition(),
                        payload.description() + " must PROCESS when the call exists");
            }
        }

        @Test
        @DisplayName("a non-LID stanza is dropped before it reaches a call")
        void nonLidDropped() {
            // A PN-only call-creator with no sender_lid predates the LID migration and must drop.
            var pnOffer = new OfferStanza(ONE_TO_ONE_CALL_ID, CALLER_PN, CALLER_PN, null, null, null, null, null,
                    true, false, null, -1, 3, List.of(), List.of(), List.of(), List.of(), null, null, null, null,
                    null, null, null, List.of(), null);
            var verdict = classify(signalingRouter, pnOffer.toStanza(), null, false);
            assertSame(Disposition.DROP, verdict.disposition());
        }

        @Test
        @DisplayName("a payload with no call-id is dropped as a malformed header")
        void missingHeaderDropped() {
            var malformed = new StanzaBuilder().description("offer").build();
            assertSame(Disposition.DROP, classify(signalingRouter, malformed, CALLER_LID_DEVICE, false).disposition());
        }
    }

    // ---- Stage A/B bridge: CallStanza.parse decodes the captured tags ----------------------------

    @Nested
    @DisplayName("Stage A/B bridge: the captured tags decode to their typed records")
    class Decoding {
        @Test
        @DisplayName("the 1:1 lifecycle tags parse to Offer/Preaccept/Accept/Transport/Terminate")
        void oneToOneTagsDecode() {
            assertInstanceOf(OfferStanza.class, CallStanza.parse(oneToOneOffer().toStanza()).orElseThrow());
            assertInstanceOf(PreacceptStanza.class,
                    CallStanza.parse(oneToOnePreaccept().toStanza()).orElseThrow());
            assertInstanceOf(AcceptStanza.class, CallStanza.parse(oneToOneAccept().toStanza()).orElseThrow());
            assertInstanceOf(TransportStanza.class,
                    CallStanza.parse(oneToOneTransport().toStanza()).orElseThrow());
            assertInstanceOf(TerminateStanza.class,
                    CallStanza.parse(oneToOneTerminate().toStanza()).orElseThrow());
        }

        @Test
        @DisplayName("the decoded offer preserves the captured call-id, creator, joinable and audio call shape")
        void offerAttributesPreserved() {
            var offer = (OfferStanza) CallStanza.parse(oneToOneOffer().toStanza()).orElseThrow();
            assertEquals(ONE_TO_ONE_CALL_ID, offer.callId().orElseThrow());
            assertEquals(CALLER_LID_DEVICE, offer.callCreator().orElseThrow());
            assertTrue(offer.joinable());
            assertFalse(offer.isGroup());
            assertFalse(offer.isVideo());
            assertTrue(offer.mediaDescriptor().isEmpty());
        }
    }

    // ---- Stage B: IncomingMessageRouter routing class --------------------------------------------

    @Nested
    @DisplayName("Stage B: IncomingMessageRouter routes the decoded messages")
    class MessageRouting {
        // A locator that resolves a single live call by id, used to model the manager lookup the controller
        // supplies; null means no call exists yet.
        private CallContext live;

        private CallContext locate(String id) {
            return live != null && live.callId().equals(id) ? live : null;
        }

        @Test
        @DisplayName("an offer for a not-yet-created call routes PROCESS so the lifecycle can create it")
        void offerWithoutContextProcesses() {
            var verdict = messageRouter.route(oneToOneOffer(), CALLER_LID_DEVICE, DedupState.INITIAL, this::locate);
            assertSame(RoutingClass.PROCESS, verdict.routingClass());
            assertTrue(verdict.context().isEmpty());
        }

        @Test
        @DisplayName("an offer for an already-known call routes OFFER_RERING")
        void offerWithContextReRings() {
            live = context(ONE_TO_ONE_CALL_ID, CallDirection.INCOMING);
            var verdict = messageRouter.route(oneToOneOffer(), CALLER_LID_DEVICE, DedupState.INITIAL, this::locate);
            assertSame(RoutingClass.OFFER_RERING, verdict.routingClass());
            assertSame(live, verdict.context().orElseThrow());
        }

        @Test
        @DisplayName("an accept for a known call routes ACCEPT_HANDLE onto the bring-up path")
        void acceptHandles() {
            live = context(ONE_TO_ONE_CALL_ID, CallDirection.OUTGOING);
            var verdict = messageRouter.route(oneToOneAccept(), CALLER_LID_DEVICE, DedupState.INITIAL, this::locate);
            assertSame(RoutingClass.ACCEPT_HANDLE, verdict.routingClass());
            assertSame(live, verdict.context().orElseThrow());
        }

        @Test
        @DisplayName("a terminate for a known call routes PROCESS for per-type teardown")
        void terminateProcesses() {
            live = context(ONE_TO_ONE_CALL_ID, CallDirection.INCOMING);
            var verdict = messageRouter.route(oneToOneTerminate(), CALLER_LID_DEVICE, DedupState.INITIAL, this::locate);
            assertSame(RoutingClass.PROCESS, verdict.routingClass());
        }

        @Test
        @DisplayName("signaling for a rejected call is ignored (IGNORE_REJECTED)")
        void rejectedCallIgnored() {
            live = context(ONE_TO_ONE_CALL_ID, CallDirection.INCOMING);
            var verdict = messageRouter.route(oneToOneTransport(), CALLER_LID_DEVICE,
                    DedupState.INITIAL.markRejected(), this::locate);
            assertSame(RoutingClass.IGNORE_REJECTED, verdict.routingClass());
        }

        @Test
        @DisplayName("a transport for an unknown non-offer call DROPs")
        void unknownNonOfferDropped() {
            var verdict = messageRouter.route(oneToOneTransport(), CALLER_LID_DEVICE, DedupState.INITIAL,
                    this::locate);
            assertSame(RoutingClass.DROP, verdict.routingClass());
        }
    }

    // ---- Stage C: lifecycle CallState sequence with a captured-event harness ---------------------------

    @Nested
    @DisplayName("Stage C: the captured 1:1 callee CallState sequence drives through the guard")
    class OneToOneStateSequence {
        @Test
        @DisplayName("offer -> accept -> media-connected -> terminate yields RINGING -> CONNECTING -> ACTIVE -> ENDED")
        void calleeLifecycle() {
            var harness = new LifecycleHarness(ONE_TO_ONE_CALL_ID, CallDirection.INCOMING);

            // Inbound offer (no media descriptor): RECEIVED_CALL_WITHOUT_OFFER, public RINGING.
            harness.drive(CallLifecycleState.RECEIVED_CALL_WITHOUT_OFFER, CallEventType.CALL_OFFER_RECEIVED);
            assertSame(CallState.RINGING, harness.publicState());

            // User accepts: ACCEPT_SENT, public CONNECTING.
            harness.drive(CallLifecycleState.ACCEPT_SENT, CallEventType.CALL_ACCEPT_SENT);
            assertSame(CallState.CONNECTING, harness.publicState());

            // Transport + media up with peer media present: CALL_ACTIVE, public ACTIVE.
            harness.drive(CallLifecycleState.CALL_ACTIVE, CallEventType.CALL_STATE_CHANGED);
            assertSame(CallState.ACTIVE, harness.publicState());

            // Inbound terminate tears down via NONE; CallActive -> Ending is refused by the guard, so the
            // teardown uses the legal CallActive -> None edge. Public ENDED.
            harness.endViaTerminate();
            assertSame(CallState.ENDED, harness.publicState());

            // The onCall*-facing lifecycle sequence: offer received, accept sent, then the CallActive
            // transition whose own lifecycle event is the state-changed event.
            assertEquals(List.of(
                    CallEventType.CALL_OFFER_RECEIVED,
                    CallEventType.CALL_ACCEPT_SENT,
                    CallEventType.CALL_STATE_CHANGED), harness.lifecycleEvents());
            // One state-changed for each of: offer ring, accept sent, active, teardown.
            assertEquals(4, harness.stateChangedCount());
        }
    }

    @Nested
    @DisplayName("Stage C: the captured group join CallState sequence drives through the guard")
    class GroupStateSequence {
        @Test
        @DisplayName("offer(group) -> Link -> CallActive when a peer connects, ConnectedLonely when alone")
        void groupLifecycle() {
            // The captured group join arrives as a group offer; the local side joins (Link), the join
            // completes and post_process_group_info decides active vs lonely by peer count.
            var harness = new LifecycleHarness(GROUP_CALL_ID, CallDirection.INCOMING);

            harness.drive(CallLifecycleState.LINK, CallEventType.CALL_LINK_STATE_CHANGED);
            assertSame(CallState.CONNECTING, harness.publicState());

            // A peer is connected -> CallActive.
            harness.drive(CallLifecycleState.CALL_ACTIVE, CallEventType.CALL_STATE_CHANGED);
            assertSame(CallState.ACTIVE, harness.publicState());

            // Last peer leaves -> ConnectedLonely (closed-set edge), still public ACTIVE.
            harness.drive(CallLifecycleState.CONNECTED_LONELY, CallEventType.CALL_STATE_CHANGED);
            assertSame(CallState.ACTIVE, harness.publicState());

            // A peer reconnects -> CallActive again.
            harness.drive(CallLifecycleState.CALL_ACTIVE, CallEventType.CALL_STATE_CHANGED);
            assertSame(CallState.ACTIVE, harness.publicState());

            harness.endViaTerminate();
            assertSame(CallState.ENDED, harness.publicState());
        }

        @Test
        @DisplayName("the Link transition is silent: it advances state but emits no state-changed event")
        void linkIsSilent() {
            var harness = new LifecycleHarness(GROUP_CALL_ID, CallDirection.INCOMING);
            harness.drive(CallLifecycleState.LINK, CallEventType.CALL_LINK_STATE_CHANGED);
            // Only the link-state event was fired; no CALL_STATE_CHANGED for the silent LINK transition.
            assertEquals(List.of(CallEventType.CALL_LINK_STATE_CHANGED), harness.lifecycleEvents());
            assertEquals(0, harness.stateChangedCount());
        }
    }

    // ---- helpers ---------------------------------------------------------------------------------------

    private CallContext context(String callId, CallDirection direction) {
        var group = !ONE_TO_ONE_CALL_ID.equals(callId);
        var creator = group ? GROUP_HOST_LID_DEVICE : CALLER_LID_DEVICE;
        var chat = group ? GROUP_JID : CALLER_LID_DEVICE.toUserJid();
        return new CallContext(callId, CallRole.PRIMARY, direction, CALLER_LID_DEVICE, creator,
                CALLEE_LID_DEVICE, chat, group, false);
    }

    /**
     * Drives one call through the transition guard, mirroring the lifecycle controller's transition step:
     * it runs the guard, mirrors the public projection onto the {@link com.github.auties00.cobalt.wire.linked.call.Call}
     * view, and records the emitted events the way the controller would, so the captured CallState and
     * event sequence are asserted without the controller's untestable host/crypto dependencies.
     */
    private final class LifecycleHarness {
        private final CallManager manager = new CallManager();
        private final CallStateMachine machine = new CallStateMachine(manager);
        private final CallContext ctx;
        // The distinct lifecycle events listeners observe (the onCall* sequence), separate from the generic
        // CALL_STATE_CHANGED the controller additionally fires for each non-silent change.
        private final List<CallEventType> lifecycleEvents = new ArrayList<>();
        private int stateChangedCount;

        private LifecycleHarness(String callId, CallDirection direction) {
            this.ctx = context(callId, direction);
            manager.startCall(ctx);
        }

        // Mirrors LifecycleController#transition: guard, project, fire the distinct lifecycle event,
        // then the generic CALL_STATE_CHANGED for a non-silent change whose lifecycle event is not itself
        // CALL_STATE_CHANGED.
        private void drive(CallLifecycleState target, CallEventType lifecycleEvent) {
            var prior = machine.transition(ctx.callId(), target).orElse(null);
            assertTrue(prior != null, "guard must accept " + target);
            if (prior == target) {
                return;
            }
            lifecycleEvents.add(lifecycleEvent);
            var silent = target == CallLifecycleState.LINK || target == CallLifecycleState.ENDING;
            if (lifecycleEvent == CallEventType.CALL_STATE_CHANGED) {
                stateChangedCount++;
            } else if (!silent) {
                stateChangedCount++;
            }
        }

        // Mirrors LifecycleController#tearDown's state moves: try ENDING (refused while active), then
        // NONE (the legal active->none edge), and fire the ending state-changed event once.
        private void endViaTerminate() {
            machine.transition(ctx.callId(), CallLifecycleState.ENDING);
            machine.transition(ctx.callId(), CallLifecycleState.NONE);
            ctx.call().setState(CallState.ENDED);
            stateChangedCount++;
        }

        private CallState publicState() {
            return ctx.call().state();
        }

        // The distinct onCall*-facing lifecycle events in order (CALL_STATE_CHANGED appears here only when
        // a transition's own lifecycle event is the state-changed event, i.e. CallActive/ConnectedLonely).
        private List<CallEventType> lifecycleEvents() {
            return List.copyOf(lifecycleEvents);
        }

        private int stateChangedCount() {
            return stateChangedCount;
        }
    }
}
