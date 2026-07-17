package com.github.auties00.cobalt.calls.engine.state;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.auties00.cobalt.calls.engine.context.CallContext;
import com.github.auties00.cobalt.calls.engine.context.CallManager;
import com.github.auties00.cobalt.calls.engine.event.CallEventType;

@DisplayName("CallStateMachine transition guard (change_call_state_no_event)")
class CallStateMachineTest {
    private static final Jid PEER = Jid.of("258252122116273:94@lid");
    private static final Jid CREATOR = Jid.of("258252122116273:94@lid");
    private static final Jid SELF = Jid.of("39110693621863:0@lid");
    private static final Jid CHAT = Jid.of("258252122116273@lid");

    private final CallManager manager = new CallManager();
    private final CallStateMachine machine = new CallStateMachine(manager);

    private static CallContext context(CallContext.CallDirection direction) {
        return new CallContext(CallContext.CallRole.PRIMARY, direction,
                PEER, CREATOR, SELF, CHAT, false, false);
    }

    // Forces the context into a state directly, bypassing the guard, so a transition rule can be tested
    // from any starting state.
    private static void force(CallContext context, CallLifecycleState state) {
        context.state(state);
    }

    @Nested
    @DisplayName("self-transition")
    class SelfTransition {
        @ParameterizedTest
        @EnumSource(CallLifecycleState.class)
        @DisplayName("to the current state is an accepted no-op carrying no event")
        void selfTransitionIsNoOp(CallLifecycleState state) {
            var context = context(CallContext.CallDirection.OUTGOING);
            force(context, state);
            var transition = machine.transition(context, state);
            assertTrue(transition.accepted());
            assertFalse(transition.changedState());
            assertFalse(transition.shouldEmitEvent());
            assertSame(state, context.state());
        }
    }

    @Nested
    @DisplayName("ConnectedLonely closed transition set")
    class FromConnectedLonely {
        @Test
        @DisplayName("to None is legal")
        void toNone() {
            var context = context(CallContext.CallDirection.OUTGOING);
            force(context, CallLifecycleState.CONNECTED_LONELY);
            assertTrue(machine.transition(context, CallLifecycleState.NONE).accepted());
        }

        @Test
        @DisplayName("to CallActive is legal and emits the state-changed event")
        void toCallActive() {
            var context = context(CallContext.CallDirection.OUTGOING);
            force(context, CallLifecycleState.CONNECTED_LONELY);
            var transition = machine.transition(context, CallLifecycleState.CALL_ACTIVE);
            assertTrue(transition.accepted());
            assertTrue(transition.shouldEmitEvent());
            assertSame(CallEventType.CALL_STATE_CHANGED, transition.event().orElseThrow());
            assertSame(CallLifecycleState.CALL_ACTIVE, context.state());
        }

        @ParameterizedTest
        @EnumSource(value = CallLifecycleState.class, names = {"NONE", "CALL_ACTIVE", "CONNECTED_LONELY"},
                mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("to any other state is rejected without mutation")
        void otherTargetsRejected(CallLifecycleState target) {
            var context = context(CallContext.CallDirection.OUTGOING);
            force(context, CallLifecycleState.CONNECTED_LONELY);
            var transition = machine.transition(context, target);
            assertFalse(transition.accepted());
            assertSame(CallLifecycleState.CONNECTED_LONELY, context.state());
        }
    }

    @Nested
    @DisplayName("CallActive closed transition set")
    class FromCallActive {
        @Test
        @DisplayName("to None is legal")
        void toNone() {
            var context = context(CallContext.CallDirection.OUTGOING);
            force(context, CallLifecycleState.CALL_ACTIVE);
            assertTrue(machine.transition(context, CallLifecycleState.NONE).accepted());
        }

        @Test
        @DisplayName("to ConnectedLonely is legal")
        void toConnectedLonely() {
            var context = context(CallContext.CallDirection.OUTGOING);
            force(context, CallLifecycleState.CALL_ACTIVE);
            assertTrue(machine.transition(context, CallLifecycleState.CONNECTED_LONELY).accepted());
        }

        @Test
        @DisplayName("to Ending is rejected because the legal-set check precedes the silent-ending shortcut")
        void toEndingRejected() {
            var context = context(CallContext.CallDirection.OUTGOING);
            force(context, CallLifecycleState.CALL_ACTIVE);
            var transition = machine.transition(context, CallLifecycleState.ENDING);
            assertFalse(transition.accepted());
            assertSame(CallLifecycleState.CALL_ACTIVE, context.state());
        }

        @ParameterizedTest
        @EnumSource(value = CallLifecycleState.class, names = {"NONE", "CALL_ACTIVE", "CONNECTED_LONELY"},
                mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("to any other state is rejected without mutation")
        void otherTargetsRejected(CallLifecycleState target) {
            var context = context(CallContext.CallDirection.OUTGOING);
            force(context, CallLifecycleState.CALL_ACTIVE);
            var transition = machine.transition(context, target);
            assertFalse(transition.accepted());
            assertSame(CallLifecycleState.CALL_ACTIVE, context.state());
        }
    }

    @Nested
    @DisplayName("silent transitions")
    class Silent {
        @Test
        @DisplayName("into Link applies the state but emits no event")
        void enterLink() {
            var context = context(CallContext.CallDirection.OUTGOING);
            force(context, CallLifecycleState.CALLING);
            var transition = machine.transition(context, CallLifecycleState.LINK);
            assertTrue(transition.accepted());
            assertFalse(transition.shouldEmitEvent());
            assertSame(CallLifecycleState.LINK, context.state());
        }

        @Test
        @DisplayName("Link to None is a silent teardown")
        void linkTeardown() {
            var context = context(CallContext.CallDirection.OUTGOING);
            force(context, CallLifecycleState.LINK);
            var transition = machine.transition(context, CallLifecycleState.NONE);
            assertTrue(transition.accepted());
            assertFalse(transition.shouldEmitEvent());
            assertSame(CallLifecycleState.NONE, context.state());
        }

        @Test
        @DisplayName("into Ending applies the state but emits no event")
        void enterEnding() {
            var context = context(CallContext.CallDirection.OUTGOING);
            force(context, CallLifecycleState.CALLING);
            var transition = machine.transition(context, CallLifecycleState.ENDING);
            assertTrue(transition.accepted());
            assertFalse(transition.shouldEmitEvent());
            assertSame(CallLifecycleState.ENDING, context.state());
        }

        @Test
        @DisplayName("Ending to None is a silent teardown")
        void endingTeardown() {
            var context = context(CallContext.CallDirection.OUTGOING);
            force(context, CallLifecycleState.ENDING);
            var transition = machine.transition(context, CallLifecycleState.NONE);
            assertTrue(transition.accepted());
            assertFalse(transition.shouldEmitEvent());
            assertSame(CallLifecycleState.NONE, context.state());
        }
    }

    @Nested
    @DisplayName("duration accounting")
    class DurationAccounting {
        @Test
        @DisplayName("leaving CallActive accumulates the active segment elapsed time")
        void activeSegmentAccumulates() {
            var context = context(CallContext.CallDirection.OUTGOING);
            force(context, CallLifecycleState.CONNECTED_LONELY);
            machine.transition(context, CallLifecycleState.CALL_ACTIVE, 1_000L);
            machine.transition(context, CallLifecycleState.CONNECTED_LONELY, 6_000L);
            assertEquals(5_000L, context.activeDurationMillis());
        }

        @Test
        @DisplayName("leaving ConnectedLonely accumulates the lonely segment elapsed time")
        void lonelySegmentAccumulates() {
            var context = context(CallContext.CallDirection.OUTGOING);
            // Enter ConnectedLonely from a setup leg so the lonely segment opens, then leave to CallActive.
            force(context, CallLifecycleState.ACCEPT_SENT);
            machine.transition(context, CallLifecycleState.CONNECTED_LONELY, 2_000L);
            machine.transition(context, CallLifecycleState.CALL_ACTIVE, 9_000L);
            assertEquals(7_000L, context.lonelyDurationMillis());
        }

        @Test
        @DisplayName("active and lonely durations accumulate across multiple segments")
        void accumulatesAcrossSegments() {
            var context = context(CallContext.CallDirection.OUTGOING);
            force(context, CallLifecycleState.ACCEPT_SENT);
            machine.transition(context, CallLifecycleState.CALL_ACTIVE, 0L);          // active opens
            machine.transition(context, CallLifecycleState.CONNECTED_LONELY, 1_000L); // +1000 active, lonely opens
            machine.transition(context, CallLifecycleState.CALL_ACTIVE, 3_000L);      // +2000 lonely, active opens
            machine.transition(context, CallLifecycleState.NONE, 6_000L);             // +3000 active
            assertEquals(4_000L, context.activeDurationMillis());
            assertEquals(2_000L, context.lonelyDurationMillis());
        }
    }

    @Nested
    @DisplayName("CallStateTransition seam (by call id)")
    class ByCallId {
        @Test
        @DisplayName("an accepted transition returns the prior state")
        void acceptedReturnsPriorState() {
            var primary = manager.startCall(CallContext.CallDirection.OUTGOING,
                    PEER, CREATOR, SELF, CHAT, false, false);
            primary.state(CallLifecycleState.CALLING);
            var prior = machine.transition(primary.callId(), CallLifecycleState.ACCEPT_RECEIVED);
            assertSame(CallLifecycleState.CALLING, prior.orElseThrow());
            assertSame(CallLifecycleState.ACCEPT_RECEIVED, primary.state());
        }

        @Test
        @DisplayName("a no-op self-transition returns the unchanged current state")
        void selfTransitionReturnsCurrent() {
            var primary = manager.startCall(CallContext.CallDirection.OUTGOING,
                    PEER, CREATOR, SELF, CHAT, false, false);
            primary.state(CallLifecycleState.CALL_ACTIVE);
            assertSame(CallLifecycleState.CALL_ACTIVE,
                    machine.transition(primary.callId(), CallLifecycleState.CALL_ACTIVE).orElseThrow());
        }

        @Test
        @DisplayName("a rejected transition returns empty and leaves the state unchanged")
        void rejectedReturnsEmpty() {
            var primary = manager.startCall(CallContext.CallDirection.OUTGOING,
                    PEER, CREATOR, SELF, CHAT, false, false);
            primary.state(CallLifecycleState.CALL_ACTIVE);
            assertTrue(machine.transition(primary.callId(), CallLifecycleState.RECEIVED_CALL).isEmpty());
            assertSame(CallLifecycleState.CALL_ACTIVE, primary.state());
        }

        @Test
        @DisplayName("an unknown call id returns empty")
        void unknownCallIdReturnsEmpty() {
            assertTrue(machine.transition("00000000000000000000000000000000",
                    CallLifecycleState.CALL_ACTIVE).isEmpty());
        }
    }

    @Test
    @DisplayName("an accepted state-changing transition reports the prior and new states")
    void reportsEndpoints() {
        var context = context(CallContext.CallDirection.OUTGOING);
        force(context, CallLifecycleState.CALLING);
        var transition = machine.transition(context, CallLifecycleState.ACCEPT_RECEIVED);
        assertSame(CallLifecycleState.CALLING, transition.oldState());
        assertSame(CallLifecycleState.ACCEPT_RECEIVED, transition.newState());
        assertTrue(transition.changedState());
    }
}
