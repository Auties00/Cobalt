package com.github.auties00.cobalt.calls.engine;

import com.github.auties00.cobalt.calls.engine.context.CallContext.CallDirection;
import com.github.auties00.cobalt.calls.engine.context.CallContext.CallRole;
import com.github.auties00.cobalt.calls.engine.IncomingMessageRouter.DedupState;
import com.github.auties00.cobalt.calls.engine.IncomingMessageRouter.RoutingClass;
import com.github.auties00.cobalt.calls.signaling.session.OfferStanza;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.auties00.cobalt.calls.engine.state.CallStateMachine;
import com.github.auties00.cobalt.calls.engine.state.CallLifecycleState;
import com.github.auties00.cobalt.calls.engine.context.CallContext;
import com.github.auties00.cobalt.calls.engine.context.CallManager;

/**
 * Adversarial verification of the dual-call manager (SPEC section 4.6): while one call is active a second
 * incoming call is held in the secondary context, the by-call-id resolver checks the secondary before the
 * primary, a third call is refused, and teardown ends the secondary before the primary. This drives the
 * scenario as a real second-incoming-offer admission rather than the bare slot-allocation the implementer's
 * own manager test covers: the second call enters through a state transition while the first is in
 * {@link CallLifecycleState#CALL_ACTIVE}, and the message router resolves each call independently.
 */
@DisplayName("calls SPEC 4.6 dual-call manager (second incoming held as secondary)")
class DualCallReplayTest {
    // First (primary, connected) call: captured 1:1 identities.
    private static final String PRIMARY_CALL_ID = "006454CB35389E8C2BE8C5AAAF1CC4E5";
    private static final Jid PRIMARY_PEER = Jid.of("258252122116273:2@lid");
    private static final Jid SELF = Jid.of("39110693621863:0@lid");

    // Second (secondary, incoming-while-busy) call from a different peer.
    private static final String SECONDARY_CALL_ID = "00AABBCCDDEEFF00112233445566778899";
    private static final Jid SECONDARY_PEER = Jid.of("44444444:7@lid");

    private final CallManager manager = new CallManager();
    private final CallStateMachine machine = new CallStateMachine(manager);
    private final IncomingMessageRouter<CallContext> router = new IncomingMessageRouter<>();

    private CallContext startPrimaryActive() {
        var primary = new CallContext(PRIMARY_CALL_ID, CallRole.PRIMARY, CallDirection.INCOMING,
                PRIMARY_PEER, PRIMARY_PEER, SELF, PRIMARY_PEER.toUserJid(), false, false);
        manager.startCall(primary);
        // Bring the primary to the connected state, the busy condition under which a second offer arrives.
        primary.state(CallLifecycleState.ACCEPT_SENT);
        machine.transition(primary.callId(), CallLifecycleState.CALL_ACTIVE);
        return primary;
    }

    private CallContext admitSecondary() {
        var secondary = new CallContext(SECONDARY_CALL_ID, CallRole.SECONDARY,
                CallDirection.INCOMING, SECONDARY_PEER, SECONDARY_PEER, SELF,
                SECONDARY_PEER.toUserJid(), false, false);
        return manager.startDualCall(secondary);
    }

    private static OfferStanza offer(String callId, Jid creator) {
        return new OfferStanza(callId, creator, null, null, null, null, null, null, true, false, null, -1, 3,
                List.of(), List.of(), List.of(), List.of(), null, null, null, null, null, null, null, List.of(),
                null);
    }

    @Nested
    @DisplayName("admission of the second incoming call")
    class Admission {
        @Test
        @DisplayName("a second incoming call while the primary is active is held in the secondary slot")
        void secondHeldAsSecondary() {
            var primary = startPrimaryActive();
            assertSame(CallLifecycleState.CALL_ACTIVE, primary.state());

            var secondary = admitSecondary();
            assertSame(CallRole.SECONDARY, secondary.role());
            assertEquals(2, manager.callCount());
            assertSame(primary, manager.primary().orElseThrow());
            assertSame(secondary, manager.secondary().orElseThrow());
        }

        @Test
        @DisplayName("the by-call-id resolver checks the secondary slot before the primary")
        void resolvesSecondaryFirst() {
            startPrimaryActive();
            var secondary = admitSecondary();
            assertSame(secondary, manager.getByCallId(SECONDARY_CALL_ID).orElseThrow());
            assertTrue(manager.contains(PRIMARY_CALL_ID));
            assertTrue(manager.contains(SECONDARY_CALL_ID));
        }

        @Test
        @DisplayName("a third concurrent call is refused beyond the primary+secondary ceiling")
        void thirdCallRefused() {
            startPrimaryActive();
            admitSecondary();
            assertThrows(IllegalStateException.class, () ->
                    manager.startDualCall(CallDirection.INCOMING, Jid.of("55555555:1@lid"),
                            Jid.of("55555555:1@lid"), SELF, Jid.of("55555555@lid"), false, false));
        }

        @Test
        @DisplayName("a dual call cannot be started with no primary present")
        void noSecondaryWithoutPrimary() {
            assertThrows(IllegalStateException.class, DualCallReplayTest.this::admitSecondary);
        }
    }

    @Nested
    @DisplayName("independent routing and state of the two contexts")
    class IndependentContexts {
        @Test
        @DisplayName("the message router resolves each call by id through the manager lookup")
        void routerResolvesEachCall() {
            startPrimaryActive();
            admitSecondary();

            var primaryVerdict = router.route(offer(PRIMARY_CALL_ID, PRIMARY_PEER), PRIMARY_PEER,
                    DedupState.INITIAL, id -> manager.getByCallId(id).orElse(null));
            // An offer for the already-known primary re-rings.
            assertSame(RoutingClass.OFFER_RERING, primaryVerdict.routingClass());
            assertSame(PRIMARY_CALL_ID, primaryVerdict.context().orElseThrow().callId());

            var secondaryVerdict = router.route(offer(SECONDARY_CALL_ID, SECONDARY_PEER), SECONDARY_PEER,
                    DedupState.INITIAL, id -> manager.getByCallId(id).orElse(null));
            assertSame(RoutingClass.OFFER_RERING, secondaryVerdict.routingClass());
            assertSame(SECONDARY_CALL_ID, secondaryVerdict.context().orElseThrow().callId());
        }

        @Test
        @DisplayName("a transition on the secondary leaves the active primary's state untouched")
        void secondaryTransitionIsolated() {
            var primary = startPrimaryActive();
            var secondary = admitSecondary();

            secondary.state(CallLifecycleState.RECEIVED_CALL);
            machine.transition(secondary.callId(), CallLifecycleState.ACCEPT_SENT);

            assertSame(CallLifecycleState.ACCEPT_SENT, secondary.state());
            assertSame(CallLifecycleState.CALL_ACTIVE, primary.state());
        }

        @Test
        @DisplayName("each context owns its own lock instance")
        void distinctLocks() {
            var primary = startPrimaryActive();
            var secondary = admitSecondary();
            assertTrue(primary.lock() != secondary.lock());
        }
    }

    @Nested
    @DisplayName("teardown ends the secondary before the primary")
    class Teardown {
        @Test
        @DisplayName("endCall on the secondary id clears only the secondary slot")
        void endSecondaryKeepsPrimary() {
            var primary = startPrimaryActive();
            admitSecondary();
            assertSame(SECONDARY_CALL_ID, manager.endCall(SECONDARY_CALL_ID).orElseThrow().callId());
            assertEquals(1, manager.callCount());
            assertSame(primary, manager.primary().orElseThrow());
            assertTrue(manager.secondary().isEmpty());
        }

        @Test
        @DisplayName("endAll tears down both slots, the secondary first")
        void endAllClearsBoth() {
            startPrimaryActive();
            admitSecondary();
            manager.endAll();
            assertEquals(0, manager.callCount());
            assertTrue(manager.primary().isEmpty());
            assertTrue(manager.secondary().isEmpty());
        }

        @Test
        @DisplayName("after the secondary ends a fresh dual call may be admitted again")
        void readmitAfterSecondaryEnds() {
            startPrimaryActive();
            admitSecondary();
            manager.endCall(SECONDARY_CALL_ID);
            var readmitted = manager.startDualCall(CallDirection.INCOMING, Jid.of("66666666:9@lid"),
                    Jid.of("66666666:9@lid"), SELF, Jid.of("66666666@lid"), false, false);
            assertSame(CallRole.SECONDARY, readmitted.role());
            assertEquals(2, manager.callCount());
        }
    }
}
