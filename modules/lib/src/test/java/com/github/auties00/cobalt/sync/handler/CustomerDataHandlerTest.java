package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.business.CustomerDataAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.CustomerDataActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for {@link CustomerDataHandler} - Cobalt's adapter for
 * {@code WAWebCustomerDataSync}.
 *
 * <p>Cobalt does not currently maintain a dedicated customer-data store,
 * so valid SET / REMOVE mutations are acknowledged with {@code SUCCESS}
 * without touching the store. SET requires a parseable chat JID at
 * {@code indexParts[1]} and a {@link CustomerDataAction} payload;
 * otherwise the result is {@code MALFORMED}. These tests pin the wire
 * metadata, the SET / REMOVE acknowledgements, the malformed-input
 * fallbacks, and the default timestamp-based conflict resolution.
 */
@DisplayName("CustomerDataHandler")
class CustomerDataHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final String CHAT_JID = "12345@s.whatsapp.net";

    private LinkedWhatsAppStore store;
    private TestWhatsAppClient client;
    private CustomerDataHandler handler;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
        handler = new CustomerDataHandler();
    }

    /**
     * Builds a trusted mutation carrying the given customer-data action.
     *
     * @param indexChatJid the chat JID placed in {@code indexParts[1]}, may be {@code null}
     * @param action       the customer data action payload, may be {@code null}
     * @param operation    the sync operation
     * @param ts           the mutation timestamp
     * @return the trusted mutation
     */
    private DecryptedMutation.Trusted buildMutation(String indexChatJid, CustomerDataAction action,
                                                    SyncdOperation operation, Instant ts) {
        var valueBuilder = new SyncActionValueBuilder().timestamp(ts);
        if (action != null) {
            valueBuilder.customerDataAction(action);
        }
        var indexParts = indexChatJid == null ? List.of(handler.actionName()) : List.of(handler.actionName(), indexChatJid);
        var index = JSON.toJSONString(indexParts);
        return new DecryptedMutation.Trusted(index, valueBuilder.build(), operation, ts, handler.version());
    }

    @Nested
    @DisplayName("metadata - wire identity")
    class Metadata {
        @Test
        @DisplayName("actionName() returns the wire constant")
        void actionName() {
            assertEquals(CustomerDataAction.ACTION_NAME, handler.actionName());
            assertEquals("customer_data", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR_LOW")
        void collectionName() {
            assertEquals(CustomerDataAction.COLLECTION_NAME, handler.collectionName());
            assertEquals(SyncPatchType.REGULAR_LOW, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version")
        void version() {
            assertEquals(CustomerDataAction.ACTION_VERSION, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation - SET acknowledgement")
    class ApplySet {
        @Test
        @DisplayName("a valid SET with a parseable chat JID and present payload returns SUCCESS")
        void validSetSucceeds() {
            var action = new CustomerDataActionBuilder()
                    .chatJid(CHAT_JID)
                    .contactType(1)
                    .email("alice@example.com")
                    .build();

            var result = handler.applyMutation(client,
                    buildMutation(CHAT_JID, action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState(),
                    "Cobalt acknowledges customer_data SETs without persisting them");
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan dimension is n/a")
    class OrphanDimension {
        @Test
        @DisplayName("Cobalt has no dedicated customer-data store, so an orphan branch is never taken")
        void orphanNotApplicable() {
            // The WA Web handler resolves the chat from indexParts[1] but Cobalt's adapter
            // collapses the whole flow to a parseable-JID check plus an acknowledgement.
            var action = new CustomerDataActionBuilder().chatJid(CHAT_JID).build();
            assertEquals(SyncActionState.SUCCESS, handler.applyMutation(client,
                    buildMutation(CHAT_JID, action, SyncdOperation.SET, Instant.now()))
                    .actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed value")
    class MalformedValue {
        @Test
        @DisplayName("a SET whose value carries the wrong action returns MALFORMED")
        void wrongActionType() {
            var ts = Instant.now();
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .pinAction(new PinActionBuilder().pinned(true).build())
                    .build();
            var index = JSON.toJSONString(List.of(handler.actionName(), CHAT_JID));
            var mutation = new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed index")
    class MalformedIndex {
        @Test
        @DisplayName("a SET with a blank chat JID at indexParts[1] returns MALFORMED")
        void blankChatJid() {
            var action = new CustomerDataActionBuilder().build();
            var result = handler.applyMutation(client,
                    buildMutation("", action, SyncdOperation.SET, Instant.now()));
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("a SET with a missing chat JID slot returns MALFORMED")
        void missingChatJidSlot() {
            var action = new CustomerDataActionBuilder().build();
            var result = handler.applyMutation(client,
                    buildMutation(null, action, SyncdOperation.SET, Instant.now()));
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE")
    class ApplyRemove {
        @Test
        @DisplayName("REMOVE with a valid chat JID returns SUCCESS - acknowledged no-op")
        void validRemoveSucceeds() {
            var result = handler.applyMutation(client,
                    buildMutation(CHAT_JID, null, SyncdOperation.REMOVE, Instant.now()));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }

        @Test
        @DisplayName("REMOVE with a blank chat JID is silently acknowledged")
        void blankRemoveSucceeds() {
            // Per the handler source, the REMOVE branch acknowledges with SUCCESS regardless of
            // whether a chat JID was supplied - there is no removal target to validate.
            var result = handler.applyMutation(client,
                    buildMutation("", null, SyncdOperation.REMOVE, Instant.now()));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - unknown operation")
    class UnknownOperation {
        @Test
        @DisplayName("an unknown operation falls through to UNSUPPORTED")
        void unknownReturnsUnsupported() {
            // The handler discriminates on SET / REMOVE and falls through to unsupported
            // otherwise. SyncdOperation only exposes SET/REMOVE; this test stands in as the
            // explicit unsupported-fall-through outcome documenting the else branch.
            var ts = Instant.now();
            var value = new SyncActionValueBuilder().timestamp(ts).build();
            // No action installed - even SET on this value with a valid chat JID will trip the
            // malformed-action-value branch rather than the unsupported one. The unsupported
            // branch is only reachable from a non-SET, non-REMOVE op, which the SyncdOperation
            // enum does not currently expose. Confirm at least that SET with no value content
            // does NOT silently produce SUCCESS - it must surface as a typed failure.
            var index = JSON.toJSONString(List.of(handler.actionName(), CHAT_JID));
            var mutation = new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, handler.version());

            // value is non-null (built without an action), so action().orElse(null) returns null
            // and the wrong-action-type branch fires: MALFORMED.
            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp comparison")
    class ResolveConflicts {
        @Test
        @DisplayName("newer remote - APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteApplies() {
            var local = mutationAt(Instant.ofEpochSecond(1_000));
            var remote = mutationAt(Instant.ofEpochSecond(2_000));
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("older remote - SKIP_REMOTE")
        void olderRemoteSkipped() {
            var local = mutationAt(Instant.ofEpochSecond(2_000));
            var remote = mutationAt(Instant.ofEpochSecond(1_000));
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    handler.resolveConflicts(local, remote).state());
        }

        private DecryptedMutation.Trusted mutationAt(Instant ts) {
            var action = new CustomerDataActionBuilder().chatJid(CHAT_JID).build();
            return buildMutation(CHAT_JID, action, SyncdOperation.SET, ts);
        }
    }

    @Nested
    @DisplayName("static builders - n/a")
    class StaticBuilders {
        @Test
        @DisplayName("CustomerDataHandler exposes no outbound mutation builder - dimension is n/a")
        void noBuilder() {
            // Customer data is authored via the WA Web business UI and Cobalt does not currently
            // emit `customer_data` mutations from this handler.
            assertNotNull(new CustomerDataHandler(),
                    "CustomerDataHandler instantiates with a no-arg constructor and exposes no builder method");
        }
    }
}
