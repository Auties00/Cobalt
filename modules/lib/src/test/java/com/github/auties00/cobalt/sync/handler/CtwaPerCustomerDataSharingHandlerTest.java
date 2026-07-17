package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.wam.TestWamService;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.linked.business.ctwa.CtwaDataSharingPreferenceBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.business.CtwaPerCustomerDataSharingAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.CtwaPerCustomerDataSharingActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.media.FavoritesActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.CtwaPerCustomerDataSharingMutationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link CtwaPerCustomerDataSharingHandler} - Cobalt's adapter for
 * {@code WAWebCtwaPerCustomerDataSharingSync}.
 *
 * <p>Unique among the User-prefs bucket because it explicitly supports REMOVE
 * in addition to SET. The matrix covers metadata, the SET and REMOVE
 * branches, the malformed-index and malformed-value paths, the
 * {@code getCtwaPerCustomerDataSharingMutation} builder, and the default
 * timestamp-based conflict resolution.
 */
@DisplayName("CtwaPerCustomerDataSharingHandler")
class CtwaPerCustomerDataSharingHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final String CUSTOMER_LID = "12025550100@lid";
    private static final Jid CUSTOMER_LID_JID = Jid.of(CUSTOMER_LID);

    private LinkedWhatsAppClient client;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    private static DecryptedMutation.Trusted ctwaMutation(String accountLid, Boolean enabled, SyncdOperation op, Instant ts) {
        var action = new CtwaPerCustomerDataSharingActionBuilder()
                .isCtwaPerCustomerDataSharingEnabled(enabled)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(ts)
                .ctwaPerCustomerDataSharingAction(action)
                .build();
        var index = accountLid == null
                ? "[\"ctwaPerCustomerDataSharing\"]"
                : "[\"ctwaPerCustomerDataSharing\",\"" + accountLid + "\"]";
        return new DecryptedMutation.Trusted(index, value, op, ts, 1);
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName() returns 'ctwaPerCustomerDataSharing'")
        void actionName() {
            assertEquals(CtwaPerCustomerDataSharingAction.ACTION_NAME, new CtwaPerCustomerDataSharingHandler(TestWamService.create(client)).actionName());
            assertEquals("ctwaPerCustomerDataSharing", new CtwaPerCustomerDataSharingHandler(TestWamService.create(client)).actionName());
        }

        @Test
        @DisplayName("collectionName() is SyncPatchType.REGULAR_HIGH")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_HIGH, new CtwaPerCustomerDataSharingHandler(TestWamService.create(client)).collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version (1)")
        void version() {
            assertEquals(CtwaPerCustomerDataSharingAction.ACTION_VERSION, new CtwaPerCustomerDataSharingHandler(TestWamService.create(client)).version());
            assertEquals(1, new CtwaPerCustomerDataSharingHandler(TestWamService.create(client)).version());
        }
    }

    @Nested
    @DisplayName("applyMutation: SET stores a preference keyed by accountLid")
    class SetHappy {
        @Test
        @DisplayName("isEnabled=true writes a CtwaDataSharingPreference into the store")
        void writesEnabled() {
            var result = new CtwaPerCustomerDataSharingHandler(TestWamService.create(client)).applyMutation(
                    client, ctwaMutation(CUSTOMER_LID, Boolean.TRUE, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var pref = client.store().businessStore().findCtwaDataSharing(CUSTOMER_LID).orElseThrow();
            assertEquals(CUSTOMER_LID, pref.accountLid());
            assertTrue(pref.enabled());
        }

        @Test
        @DisplayName("isEnabled=false also writes a record (with enabled=false)")
        void writesDisabled() {
            var result = new CtwaPerCustomerDataSharingHandler(TestWamService.create(client)).applyMutation(
                    client, ctwaMutation(CUSTOMER_LID, Boolean.FALSE, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var pref = client.store().businessStore().findCtwaDataSharing(CUSTOMER_LID).orElseThrow();
            assertFalse(pref.enabled());
        }
    }

    @Nested
    @DisplayName("applyMutation: REMOVE clears the per-customer preference")
    class RemoveHappy {
        @Test
        @DisplayName("REMOVE deletes the entry from the store")
        void removeDeletes() {
            client.store().businessStore().putCtwaDataSharing(
                    new CtwaDataSharingPreferenceBuilder().accountLid(CUSTOMER_LID).enabled(true).build());
            assertTrue(client.store().businessStore().findCtwaDataSharing(CUSTOMER_LID).isPresent());

            var result = new CtwaPerCustomerDataSharingHandler(TestWamService.create(client)).applyMutation(
                    client, ctwaMutation(CUSTOMER_LID, null, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(client.store().businessStore().findCtwaDataSharing(CUSTOMER_LID).isEmpty(),
                    "REMOVE must drop the preference from the store");
        }

        @Test
        @DisplayName("REMOVE with no entry is still SUCCESS (no-op on the store)")
        void removeNoOp() {
            var result = new CtwaPerCustomerDataSharingHandler(TestWamService.create(client)).applyMutation(
                    client, ctwaMutation(CUSTOMER_LID, null, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }

        @Test
        @DisplayName("REMOVE with a null accountLid is SUCCESS (store treats null as no-op)")
        void removeNullAccount() {
            var result = new CtwaPerCustomerDataSharingHandler(TestWamService.create(client)).applyMutation(
                    client, ctwaMutation(null, null, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation: malformed paths")
    class Malformed {
        @Test
        @DisplayName("SET with a missing accountLid in the index is MALFORMED")
        void setMissingAccount() {
            var result = new CtwaPerCustomerDataSharingHandler(TestWamService.create(client)).applyMutation(
                    client, ctwaMutation(null, Boolean.TRUE, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("SET with a non-CTWA action value is MALFORMED")
        void wrongActionType() {
            var wrongValue = new SyncActionValueBuilder()
                    .timestamp(Instant.now())
                    .favoritesAction(new FavoritesActionBuilder().favorites(List.of()).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"ctwaPerCustomerDataSharing\",\"" + CUSTOMER_LID + "\"]",
                    wrongValue, SyncdOperation.SET, Instant.now(), 1);

            var result = new CtwaPerCustomerDataSharingHandler(TestWamService.create(client)).applyMutation(client, mutation);

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("getCtwaPerCustomerDataSharingMutation - builder helper")
    class Builder {
        @Test
        @DisplayName("emits a SET pending mutation with the requested accountLid and flag")
        void buildsPending() {
            var pending = new CtwaPerCustomerDataSharingMutationFactory()
                    .getCtwaPerCustomerDataSharingMutation(CUSTOMER_LID_JID, true);

            assertEquals(SyncdOperation.SET, pending.mutation().operation());
            assertEquals(CtwaPerCustomerDataSharingAction.ACTION_VERSION, pending.mutation().actionVersion());
            assertEquals("[\"ctwaPerCustomerDataSharing\",\"" + CUSTOMER_LID_JID + "\"]",
                    pending.mutation().index());
            var action = (CtwaPerCustomerDataSharingAction) pending.mutation().value().flatMap(sav -> sav.action()).orElseThrow();
            assertTrue(action.isCtwaPerCustomerDataSharingEnabled());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp-based")
    class ResolveConflicts {
        @Test
        @DisplayName("remote with the later timestamp wins")
        void remoteWins() {
            var local  = ctwaMutation(CUSTOMER_LID, Boolean.FALSE, SyncdOperation.SET, Instant.ofEpochSecond(1700000000L));
            var remote = ctwaMutation(CUSTOMER_LID, Boolean.TRUE,  SyncdOperation.SET, Instant.ofEpochSecond(1700000010L));

            var resolution = new CtwaPerCustomerDataSharingHandler(TestWamService.create(client)).resolveConflicts(local, remote);

            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }
    }

    @Nested
    @DisplayName("applyMutationBatch - n/a, default implementation")
    class BatchNa {
        @Test
        @DisplayName("default applyMutationBatch delegates per mutation")
        void perItem() {
            var batch = List.of(
                    ctwaMutation(CUSTOMER_LID,            Boolean.TRUE, SyncdOperation.SET,    Instant.now()),
                    ctwaMutation("12025550101@lid",       null,         SyncdOperation.REMOVE, Instant.now()));

            var results = new CtwaPerCustomerDataSharingHandler(TestWamService.create(client)).applyMutationBatch(client, batch);

            assertEquals(2, results.size());
            assertEquals(SyncActionState.SUCCESS, results.get(0).actionState());
            assertEquals(SyncActionState.SUCCESS, results.get(1).actionState());
        }
    }

}
