package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.PaymentInfoAction;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.PaymentInfoActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Covers {@link PaymentInfoHandler}: the SMB platform gate, the
 * {@link ABProp#ORDER_DETAILS_PAYMENT_INSTRUCTIONS_SYNC_ENABLED} AB-prop gate, the
 * {@link SyncdOperation#SET} path that persists the CPI string via
 * {@link LinkedWhatsAppBusinessStore#setPaymentInstructionCpi(String)}, the malformed-value
 * classification when {@link PaymentInfoAction#cpi()} is missing, and the
 * {@link SyncActionState#UNSUPPORTED} classification for non-{@code SET} operations and
 * gate failures. Each test builds its own mutation and opts into the platform and AB prop
 * explicitly so the gating dimension under test is declarative.
 */
@DisplayName("PaymentInfoHandler")
class PaymentInfoHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppStore store;
    private TestWhatsAppClient testClient;
    private TestABPropsService props;
    private PaymentInfoHandler handler;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        props = TestABPropsService.builder().build();
        testClient = TestWhatsAppClient.create()
                .withStore(store)
                .withAbPropsService(props);
        handler = new PaymentInfoHandler(props);
    }

    private void smbPlatform() {
        store.accountStore().setDevice(store.accountStore().device().withPlatform(ClientPlatformType.IOS_BUSINESS));
    }

    private void enableSync() {
        props.set(ABProp.ORDER_DETAILS_PAYMENT_INSTRUCTIONS_SYNC_ENABLED, true);
    }

    // Passing action == null omits the paymentInfoAction sub-message so the malformed-value branch can be exercised.
    private static DecryptedMutation.Trusted setMutation(PaymentInfoAction action) {
        var ts = Instant.ofEpochSecond(1_700_000_000L);
        var builder = new SyncActionValueBuilder().timestamp(ts);
        if (action != null) builder.paymentInfoAction(action);
        return new DecryptedMutation.Trusted("[\"payment_info\"]", builder.build(),
                SyncdOperation.SET, ts, PaymentInfoAction.ACTION_VERSION);
    }

    @Nested
    @DisplayName("metadata Ã¢â‚¬â€ wire constants")
    class Metadata {
        @Test
        @DisplayName("actionName() is payment_info")
        void actionName() {
            assertEquals(PaymentInfoAction.ACTION_NAME, handler.actionName());
            assertEquals("payment_info", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() is REGULAR_LOW")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_LOW, handler.collectionName());
        }

        @Test
        @DisplayName("version() is 7")
        void version() {
            assertEquals(7, handler.version());
            assertEquals(PaymentInfoAction.ACTION_VERSION, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation Ã¢â‚¬â€ platform gating")
    class PlatformGating {
        @Test
        @DisplayName("default WEB platform short-circuits to UNSUPPORTED")
        void unsupportedOnNonSmb() {
            var action = new PaymentInfoActionBuilder().cpi("cpi-1234").build();
            assertEquals(SyncActionState.UNSUPPORTED,
                    handler.applyMutation(testClient, setMutation(action)).actionState());
        }

        @Test
        @DisplayName("ANDROID_BUSINESS platform is accepted")
        void androidBusinessAccepted() {
            store.accountStore().setDevice(store.accountStore().device().withPlatform(ClientPlatformType.ANDROID_BUSINESS));
            enableSync();
            var action = new PaymentInfoActionBuilder().cpi("cpi-1234").build();
            assertEquals(SyncActionState.SUCCESS,
                    handler.applyMutation(testClient, setMutation(action)).actionState());
        }

        @Test
        @DisplayName("IOS_BUSINESS platform is accepted")
        void iosBusinessAccepted() {
            store.accountStore().setDevice(store.accountStore().device().withPlatform(ClientPlatformType.IOS_BUSINESS));
            enableSync();
            var action = new PaymentInfoActionBuilder().cpi("cpi-1234").build();
            assertEquals(SyncActionState.SUCCESS,
                    handler.applyMutation(testClient, setMutation(action)).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation Ã¢â‚¬â€ AB-prop gate")
    class AbPropGating {
        @Test
        @DisplayName("disabled order_details_payment_instructions_sync_enabled AB prop returns UNSUPPORTED")
        void abPropDisabled() {
            smbPlatform();
            // props builder defaults the flag to "false"
            var action = new PaymentInfoActionBuilder().cpi("cpi-1234").build();
            assertEquals(SyncActionState.UNSUPPORTED,
                    handler.applyMutation(testClient, setMutation(action)).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation Ã¢â‚¬â€ non-SET operation")
    class RemoveBranch {
        @Test
        @DisplayName("REMOVE operation past the platform/AB-prop gates is UNSUPPORTED")
        void removeUnsupported() {
            smbPlatform();
            enableSync();
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var action = new PaymentInfoActionBuilder().cpi("cpi-1234").build();
            var value = new SyncActionValueBuilder().timestamp(ts).paymentInfoAction(action).build();
            var mutation = new DecryptedMutation.Trusted("[\"payment_info\"]", value,
                    SyncdOperation.REMOVE, ts, 7);
            assertEquals(SyncActionState.UNSUPPORTED,
                    handler.applyMutation(testClient, mutation).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation Ã¢â‚¬â€ malformed value")
    class MalformedValue {
        @Test
        @DisplayName("missing paymentInfoAction sub-message is MALFORMED")
        void missingActionMessage() {
            smbPlatform();
            enableSync();
            assertEquals(SyncActionState.MALFORMED,
                    handler.applyMutation(testClient, setMutation(null)).actionState());
        }

        @Test
        @DisplayName("present sub-message with null cpi is MALFORMED")
        void missingCpi() {
            smbPlatform();
            enableSync();
            var action = new PaymentInfoActionBuilder().build(); // no cpi
            assertEquals(SyncActionState.MALFORMED,
                    handler.applyMutation(testClient, setMutation(action)).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation Ã¢â‚¬â€ happy SET path")
    class HappySet {
        @Test
        @DisplayName("SET persists the cpi to the store and reports SUCCESS")
        void persistsCpi() {
            smbPlatform();
            enableSync();
            var action = new PaymentInfoActionBuilder().cpi("cpi-1234").build();
            var result = handler.applyMutation(testClient, setMutation(action));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertEquals("cpi-1234", store.businessStore().paymentInstructionCpi().orElseThrow(
                    () -> new AssertionError("cpi must be persisted")));
        }
    }

    @Nested
    @DisplayName("applyMutation Ã¢â‚¬â€ malformed index (n/a)")
    class MalformedIndex {
        @Test
        @DisplayName("the handler does not parse indexParts beyond position 0, so index malformations are not exercised")
        void notExercised() {
            // The wire index is literally ["payment_info"]; the handler does not consult any
            // other element. Even a wildly malformed index reaches the success path when every
            // other precondition is satisfied. This @Nested makes the absence of the malformed-
            // index branch explicit per the per-handler matrix.
            smbPlatform();
            enableSync();
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var action = new PaymentInfoActionBuilder().cpi("cpi-malformed-index").build();
            var value = new SyncActionValueBuilder().timestamp(ts).paymentInfoAction(action).build();
            var mutation = new DecryptedMutation.Trusted("garbage-index", value, SyncdOperation.SET, ts, 7);
            assertEquals(SyncActionState.SUCCESS,
                    handler.applyMutation(testClient, mutation).actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts Ã¢â‚¬â€ default timestamp tiebreaker")
    class ResolveConflicts {
        @Test
        @DisplayName("remote with later timestamp wins (APPLY_REMOTE_DROP_LOCAL)")
        void remoteLater() {
            var local = setMutation(new PaymentInfoActionBuilder().cpi("A").build());
            var remoteTs = Instant.ofEpochSecond(1_700_000_010L);
            var remoteValue = new SyncActionValueBuilder().timestamp(remoteTs)
                    .paymentInfoAction(new PaymentInfoActionBuilder().cpi("B").build())
                    .build();
            var remote = new DecryptedMutation.Trusted("[\"payment_info\"]", remoteValue,
                    SyncdOperation.SET, remoteTs, 7);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(local, remote).state());
        }
    }

    @Nested
    @DisplayName("applyMutationBatch Ã¢â‚¬â€ default per-item dispatch (n/a override)")
    class BatchDispatch {
        @Test
        @DisplayName("the handler does not override applyMutationBatch")
        void defaultDispatchPreserved() {
            smbPlatform();
            enableSync();
            var batch = List.of(setMutation(new PaymentInfoActionBuilder().cpi("cpi-1234").build()));
            var results = handler.applyMutationBatch(testClient, batch);
            assertEquals(1, results.size());
            assertEquals(SyncActionState.SUCCESS, results.get(0).actionState());
        }
    }

}
