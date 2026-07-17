package com.github.auties00.cobalt.calls.engine;

import com.github.auties00.cobalt.calls.engine.context.CallContext.CallDirection;
import com.github.auties00.cobalt.calls.engine.context.CallContext.CallRole;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.auties00.cobalt.calls.engine.state.CallLifecycleState;
import com.github.auties00.cobalt.calls.engine.state.CallStateMachine;
import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.calls.engine.context.CallContext;
import com.github.auties00.cobalt.calls.engine.context.CallManager;

/**
 * Adversarial verification of the fifteen-state transition guard against the consolidated transition
 * table in SPEC section 4.4 and the guard rules in 4.3. Drives {@link CallStateMachine} through
 * every lifecycle edge the table names (asserting acceptance and the event-emission decision) and through
 * a sample of edges the closed in-call sets forbid (asserting rejection without mutation), then checks the
 * duration accounting on leaving the two in-call states. This is an independent re-derivation from the
 * SPEC, not a re-use of the implementer's own state-machine test.
 */
@DisplayName("calls SPEC 4.4 transition-table replay")
class TransitionTableReplayTest {
    // Captured LID identities from re/calls2-spec/captures/CAPTURE-FINDINGS.md (the 1:1 call).
    private static final Jid CALLER = Jid.of("39110693621863:58@lid");
    private static final Jid CALLEE = Jid.of("258252122116273:2@lid");
    private static final Jid SELF = Jid.of("39110693621863:0@lid");
    private static final Jid CHAT = Jid.of("258252122116273@lid");

    private final CallManager manager = new CallManager();
    private final CallStateMachine machine = new CallStateMachine(manager);

    private static CallContext context(CallDirection direction) {
        return new CallContext(CallRole.PRIMARY, direction, CALLER, CALLEE, SELF, CHAT, false, false);
    }

    // Drives the context into a starting state without the guard so an edge can be exercised from any
    // origin; the production code reaches these states through the guard but the table is verified edge by
    // edge here.
    private static CallContext from(CallLifecycleState origin) {
        var context = context(CallDirection.OUTGOING);
        context.state(origin);
        return context;
    }

    @Nested
    @DisplayName("outbound caller leg (None -> Calling -> PreacceptReceived/AcceptReceived -> CallActive)")
    class CallerLeg {
        @Test
        @DisplayName("None -> Calling on start_call is accepted and fires the state-changed event")
        void startCall() {
            var transition = machine.transition(from(CallLifecycleState.NONE), CallLifecycleState.CALLING);
            assertTrue(transition.accepted());
            assertTrue(transition.shouldEmitEvent());
            assertSame(CallEventType.CALL_STATE_CHANGED, transition.event().orElseThrow());
        }

        @Test
        @DisplayName("Calling -> PreacceptReceived on inbound Preaccept(13) is accepted")
        void preacceptReceived() {
            assertTrue(machine.transition(from(CallLifecycleState.CALLING),
                    CallLifecycleState.PREACCEPT_RECEIVED).accepted());
        }

        @Test
        @DisplayName("Calling -> Calling on OfferAck(7) is an accepted no-op carrying no event")
        void offerAckIsNoOp() {
            var transition = machine.transition(from(CallLifecycleState.CALLING), CallLifecycleState.CALLING);
            assertTrue(transition.accepted());
            assertFalse(transition.changedState());
            assertFalse(transition.shouldEmitEvent());
        }

        @TestFactory
        @DisplayName("Calling and PreCalling -> AcceptReceived on inbound Accept(3)/AcceptReceipt(14)")
        Stream<DynamicTest> acceptReceived() {
            return Stream.of(CallLifecycleState.CALLING, CallLifecycleState.PRE_CALLING)
                    .map(origin -> DynamicTest.dynamicTest(origin + " -> ACCEPT_RECEIVED", () ->
                            assertTrue(machine.transition(from(origin),
                                    CallLifecycleState.ACCEPT_RECEIVED).accepted())));
        }

        @TestFactory
        @DisplayName("AcceptSent and AcceptReceived -> CallActive when peer media is present")
        Stream<DynamicTest> bringUpToActive() {
            return Stream.of(CallLifecycleState.ACCEPT_SENT, CallLifecycleState.ACCEPT_RECEIVED)
                    .map(origin -> DynamicTest.dynamicTest(origin + " -> CALL_ACTIVE", () -> {
                        var transition = machine.transition(from(origin), CallLifecycleState.CALL_ACTIVE);
                        assertTrue(transition.accepted());
                        assertTrue(transition.shouldEmitEvent());
                    }));
        }
    }

    @Nested
    @DisplayName("inbound callee leg (None -> ReceivedCall -> AcceptSent -> CallActive)")
    class CalleeLeg {
        @TestFactory
        @DisplayName("None -> ReceivedCall / ReceivedCallWithoutOffer on inbound Offer(1)")
        Stream<DynamicTest> ring() {
            return Stream.of(CallLifecycleState.RECEIVED_CALL, CallLifecycleState.RECEIVED_CALL_WITHOUT_OFFER)
                    .map(target -> DynamicTest.dynamicTest("None -> " + target, () ->
                            assertTrue(machine.transition(from(CallLifecycleState.NONE), target).accepted())));
        }

        @TestFactory
        @DisplayName("ReceivedCall and ...WithoutOffer and PreacceptReceived -> AcceptSent on user accept")
        Stream<DynamicTest> userAccept() {
            return Stream.of(CallLifecycleState.RECEIVED_CALL, CallLifecycleState.RECEIVED_CALL_WITHOUT_OFFER,
                            CallLifecycleState.PREACCEPT_RECEIVED)
                    .map(origin -> DynamicTest.dynamicTest(origin + " -> ACCEPT_SENT", () ->
                            assertTrue(machine.transition(from(origin), CallLifecycleState.ACCEPT_SENT).accepted())));
        }

        @TestFactory
        @DisplayName("ReceivedCall and Calling -> CallActiveElsewhere when answered elsewhere")
        Stream<DynamicTest> answeredElsewhere() {
            return Stream.of(CallLifecycleState.RECEIVED_CALL, CallLifecycleState.CALLING)
                    .map(origin -> DynamicTest.dynamicTest(origin + " -> CALL_ACTIVE_ELSEWHERE", () ->
                            assertTrue(machine.transition(from(origin),
                                    CallLifecycleState.CALL_ACTIVE_ELSEWHERE).accepted())));
        }
    }

    @Nested
    @DisplayName("group lonely leg (AcceptSent -> ConnectedLonely <-> CallActive)")
    class GroupLonelyLeg {
        @Test
        @DisplayName("AcceptSent -> ConnectedLonely when no peer connected yet is accepted")
        void enterLonelyFromAccept() {
            assertTrue(machine.transition(from(CallLifecycleState.ACCEPT_SENT),
                    CallLifecycleState.CONNECTED_LONELY).accepted());
        }

        @Test
        @DisplayName("CallActive -> ConnectedLonely when the last peer leaves is accepted (closed set)")
        void lastPeerLeaves() {
            assertTrue(machine.transition(from(CallLifecycleState.CALL_ACTIVE),
                    CallLifecycleState.CONNECTED_LONELY).accepted());
        }

        @Test
        @DisplayName("ConnectedLonely -> CallActive when a peer reconnects is accepted (closed set)")
        void peerReconnects() {
            assertTrue(machine.transition(from(CallLifecycleState.CONNECTED_LONELY),
                    CallLifecycleState.CALL_ACTIVE).accepted());
        }
    }

    @Nested
    @DisplayName("link, rejoin, broadcast and teardown legs (silent transitions)")
    class SilentLegs {
        @Test
        @DisplayName("None -> Link on join_ongoing_call applies the state but fires no event (silent)")
        void joinLink() {
            var transition = machine.transition(from(CallLifecycleState.NONE), CallLifecycleState.LINK);
            assertTrue(transition.accepted());
            assertFalse(transition.shouldEmitEvent());
            assertTrue(transition.event().isEmpty());
        }

        @TestFactory
        @DisplayName("Link -> CallActive or ConnectedLonely once the join completes")
        Stream<DynamicTest> linkCompletes() {
            return Stream.of(CallLifecycleState.CALL_ACTIVE, CallLifecycleState.CONNECTED_LONELY)
                    .map(target -> DynamicTest.dynamicTest("LINK -> " + target, () ->
                            assertTrue(machine.transition(from(CallLifecycleState.LINK), target).accepted())));
        }

        @Test
        @DisplayName("None -> BCallStarting on BCallStart(45) is accepted")
        void broadcastStart() {
            assertTrue(machine.transition(from(CallLifecycleState.NONE),
                    CallLifecycleState.BCALL_STARTING).accepted());
        }

        @Test
        @DisplayName("Ending -> None on end_call teardown is silent")
        void endingTeardown() {
            var transition = machine.transition(from(CallLifecycleState.ENDING), CallLifecycleState.NONE);
            assertTrue(transition.accepted());
            assertFalse(transition.shouldEmitEvent());
            assertSame(CallLifecycleState.NONE, transition.newState());
        }

        @TestFactory
        @DisplayName("setup legs -> Ending on inbound Terminate(5) are silent into Ending")
        Stream<DynamicTest> terminateFromSetup() {
            return Stream.of(CallLifecycleState.CALLING, CallLifecycleState.RECEIVED_CALL,
                            CallLifecycleState.PREACCEPT_RECEIVED, CallLifecycleState.ACCEPT_SENT,
                            CallLifecycleState.ACCEPT_RECEIVED, CallLifecycleState.REJOINING)
                    .map(origin -> DynamicTest.dynamicTest(origin + " -> ENDING", () -> {
                        var transition = machine.transition(from(origin), CallLifecycleState.ENDING);
                        assertTrue(transition.accepted());
                        assertFalse(transition.shouldEmitEvent());
                    }));
        }

        @Test
        @DisplayName("any in-call -> Rejoining on network-path loss is accepted from a setup leg")
        void networkPathLost() {
            assertTrue(machine.transition(from(CallLifecycleState.ACCEPT_RECEIVED),
                    CallLifecycleState.REJOINING).accepted());
        }
    }

    @Nested
    @DisplayName("illegal edges out of the two closed in-call sets are rejected without mutation")
    class IllegalEdges {
        @ParameterizedTest
        @EnumSource(value = CallLifecycleState.class,
                names = {"NONE", "CALL_ACTIVE", "CONNECTED_LONELY"}, mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("CallActive -> any state other than {None, ConnectedLonely} is rejected")
        void illegalFromActive(CallLifecycleState target) {
            var context = from(CallLifecycleState.CALL_ACTIVE);
            var transition = machine.transition(context, target);
            assertFalse(transition.accepted());
            assertSame(CallLifecycleState.CALL_ACTIVE, context.state());
        }

        @ParameterizedTest
        @EnumSource(value = CallLifecycleState.class,
                names = {"NONE", "CALL_ACTIVE", "CONNECTED_LONELY"}, mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("ConnectedLonely -> any state other than {None, CallActive} is rejected")
        void illegalFromLonely(CallLifecycleState target) {
            var context = from(CallLifecycleState.CONNECTED_LONELY);
            var transition = machine.transition(context, target);
            assertFalse(transition.accepted());
            assertSame(CallLifecycleState.CONNECTED_LONELY, context.state());
        }

        @Test
        @DisplayName("CallActive -> Ending is rejected: the closed-set check precedes the silent-ending path")
        void activeToEndingRejected() {
            // SPEC 4.3: from CallActive only {None, ConnectedLonely} are legal; the silent-Ending shortcut
            // in 4.3 does not exempt CallActive, so a direct CallActive -> Ending must be refused. The
            // lifecycle teardown therefore relies on the legal CallActive -> None edge, not on Ending.
            var context = from(CallLifecycleState.CALL_ACTIVE);
            assertFalse(machine.transition(context, CallLifecycleState.ENDING).accepted());
            assertTrue(machine.transition(context, CallLifecycleState.NONE).accepted());
        }
    }

    @Nested
    @DisplayName("duration accounting on leaving the in-call states")
    class DurationAccounting {
        @Test
        @DisplayName("leaving CallActive adds the elapsed segment to the active accumulator")
        void activeAccumulates() {
            var context = from(CallLifecycleState.ACCEPT_SENT);
            machine.transition(context, CallLifecycleState.CALL_ACTIVE, 10_000L);
            machine.transition(context, CallLifecycleState.CONNECTED_LONELY, 25_000L);
            assertEquals(15_000L, context.activeDurationMillis());
        }

        @Test
        @DisplayName("leaving ConnectedLonely adds the elapsed segment to the lonely accumulator")
        void lonelyAccumulates() {
            var context = from(CallLifecycleState.ACCEPT_SENT);
            machine.transition(context, CallLifecycleState.CONNECTED_LONELY, 1_000L);
            machine.transition(context, CallLifecycleState.CALL_ACTIVE, 4_500L);
            assertEquals(3_500L, context.lonelyDurationMillis());
        }

        @Test
        @DisplayName("the active and lonely accumulators total across an interleaved active/lonely flap")
        void interleavedFlap() {
            var context = from(CallLifecycleState.ACCEPT_SENT);
            machine.transition(context, CallLifecycleState.CALL_ACTIVE, 0L);            // active opens
            machine.transition(context, CallLifecycleState.CONNECTED_LONELY, 2_000L);   // +2000 active
            machine.transition(context, CallLifecycleState.CALL_ACTIVE, 5_000L);        // +3000 lonely
            machine.transition(context, CallLifecycleState.CONNECTED_LONELY, 6_000L);   // +1000 active
            machine.transition(context, CallLifecycleState.NONE, 10_000L);              // +4000 lonely
            assertEquals(3_000L, context.activeDurationMillis());
            assertEquals(7_000L, context.lonelyDurationMillis());
        }
    }

    @Test
    @DisplayName("the full caller happy path None->Calling->AcceptReceived->CallActive->None is all accepted")
    void fullCallerHappyPath() {
        var context = from(CallLifecycleState.NONE);
        var edges = List.of(CallLifecycleState.CALLING, CallLifecycleState.ACCEPT_RECEIVED,
                CallLifecycleState.CALL_ACTIVE, CallLifecycleState.NONE);
        for (var edge : edges) {
            assertTrue(machine.transition(context, edge).accepted(), "edge to " + edge + " must be accepted");
            assertSame(edge, context.state());
        }
    }
}
