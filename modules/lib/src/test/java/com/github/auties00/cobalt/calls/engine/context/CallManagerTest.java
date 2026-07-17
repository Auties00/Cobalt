package com.github.auties00.cobalt.calls.engine.context;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CallManager dual-call manager")
class CallManagerTest {
    private static final Jid PEER = Jid.of("258252122116273:94@lid");
    private static final Jid PEER_2 = Jid.of("44444444:7@lid");
    private static final Jid CREATOR = Jid.of("258252122116273:94@lid");
    private static final Jid SELF = Jid.of("39110693621863:0@lid");
    private static final Jid CHAT = Jid.of("258252122116273@lid");

    private CallContext startPrimary(CallManager manager) {
        return manager.startCall(CallContext.CallDirection.OUTGOING, PEER, CREATOR, SELF, CHAT,
                false, false);
    }

    private CallContext startSecondary(CallManager manager) {
        return manager.startDualCall(CallContext.CallDirection.INCOMING, PEER_2, PEER_2, SELF,
                Jid.of("44444444@lid"), false, false);
    }

    @Nested
    @DisplayName("slot allocation")
    class Allocation {
        @Test
        @DisplayName("a fresh manager is idle")
        void idle() {
            var manager = new CallManager();
            assertEquals(0, manager.callCount());
            assertFalse(manager.hasPrimary());
            assertTrue(manager.primary().isEmpty());
            assertTrue(manager.secondary().isEmpty());
        }

        @Test
        @DisplayName("starting a call fills the primary slot")
        void startPrimaryFillsSlot() {
            var manager = new CallManager();
            var primary = startPrimary(manager);
            assertSame(primary, manager.primary().orElseThrow());
            assertSame(CallContext.CallRole.PRIMARY, primary.role());
            assertEquals(1, manager.callCount());
        }

        @Test
        @DisplayName("starting a second primary call is refused")
        void secondPrimaryRefused() {
            var manager = new CallManager();
            startPrimary(manager);
            assertThrows(IllegalStateException.class, () -> startPrimary(manager));
        }

        @Test
        @DisplayName("a dual call fills the secondary slot while the primary is busy")
        void dualFillsSecondary() {
            var manager = new CallManager();
            startPrimary(manager);
            var secondary = startSecondary(manager);
            assertSame(secondary, manager.secondary().orElseThrow());
            assertSame(CallContext.CallRole.SECONDARY, secondary.role());
            assertEquals(2, manager.callCount());
        }

        @Test
        @DisplayName("a dual call with no primary is refused")
        void dualWithoutPrimaryRefused() {
            var manager = new CallManager();
            assertThrows(IllegalStateException.class, () -> startSecondary(manager));
        }

        @Test
        @DisplayName("a second dual call is refused")
        void secondDualRefused() {
            var manager = new CallManager();
            startPrimary(manager);
            startSecondary(manager);
            assertThrows(IllegalStateException.class, () -> startSecondary(manager));
        }

        @Test
        @DisplayName("adopting a context with the wrong role is refused")
        void wrongRoleRefused() {
            var manager = new CallManager();
            var secondaryRole = new CallContext(CallContext.CallRole.SECONDARY,
                    CallContext.CallDirection.OUTGOING, PEER, CREATOR, SELF, CHAT, false, false);
            assertThrows(IllegalArgumentException.class, () -> manager.startCall(secondaryRole));
        }
    }

    @Nested
    @DisplayName("getByCallId")
    class Resolve {
        @Test
        @DisplayName("resolves the primary call by its id")
        void resolvesPrimary() {
            var manager = new CallManager();
            var primary = startPrimary(manager);
            assertSame(primary, manager.getByCallId(primary.callId()).orElseThrow());
            assertTrue(manager.contains(primary.callId()));
        }

        @Test
        @DisplayName("resolves the secondary call by its id")
        void resolvesSecondary() {
            var manager = new CallManager();
            startPrimary(manager);
            var secondary = startSecondary(manager);
            assertSame(secondary, manager.getByCallId(secondary.callId()).orElseThrow());
        }

        @Test
        @DisplayName("an unknown call id resolves to empty")
        void unknownEmpty() {
            var manager = new CallManager();
            startPrimary(manager);
            assertTrue(manager.getByCallId("00000000000000000000000000000000").isEmpty());
            assertFalse(manager.contains("00000000000000000000000000000000"));
        }
    }

    @Nested
    @DisplayName("teardown")
    class Teardown {
        @Test
        @DisplayName("ending the primary by id clears its slot")
        void endPrimary() {
            var manager = new CallManager();
            var primary = startPrimary(manager);
            assertSame(primary, manager.endCall(primary.callId()).orElseThrow());
            assertFalse(manager.hasPrimary());
            assertEquals(0, manager.callCount());
        }

        @Test
        @DisplayName("ending the primary leaves a present secondary in place")
        void endPrimaryKeepsSecondary() {
            var manager = new CallManager();
            var primary = startPrimary(manager);
            var secondary = startSecondary(manager);
            manager.endCall(primary.callId());
            assertTrue(manager.primary().isEmpty());
            assertSame(secondary, manager.secondary().orElseThrow());
        }

        @Test
        @DisplayName("ending an unknown call id reports not found")
        void endUnknown() {
            var manager = new CallManager();
            startPrimary(manager);
            assertTrue(manager.endCall("00000000000000000000000000000000").isEmpty());
            assertTrue(manager.hasPrimary());
        }

        @Test
        @DisplayName("endAll tears down the secondary before the primary")
        void endAllSecondaryFirst() {
            var manager = new CallManager();
            var order = new ArrayList<CallContext.CallRole>();
            var primary = startPrimary(manager);
            var secondary = startSecondary(manager);
            primary.attachResource(() -> order.add(CallContext.CallRole.PRIMARY));
            secondary.attachResource(() -> order.add(CallContext.CallRole.SECONDARY));
            manager.endAll();
            assertEquals(java.util.List.of(CallContext.CallRole.SECONDARY,
                    CallContext.CallRole.PRIMARY), order);
            assertEquals(0, manager.callCount());
        }

        @Test
        @DisplayName("ending a call closes the context resources")
        void endClosesResources() {
            var manager = new CallManager();
            var primary = startPrimary(manager);
            var closed = new AtomicBoolean();
            primary.attachResource(() -> closed.set(true));
            manager.endCall(primary.callId());
            assertTrue(closed.get());
        }
    }

    @Test
    @DisplayName("the allocation seam observes each newly allocated context")
    void allocationSeam() {
        var manager = new CallManager();
        var allocated = new ArrayList<String>();
        manager.onContextAllocated(ctx -> allocated.add(ctx.callId()));
        var primary = startPrimary(manager);
        var secondary = startSecondary(manager);
        assertEquals(java.util.List.of(primary.callId(), secondary.callId()), allocated);
    }
}
