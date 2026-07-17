package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.PaymentTosAction;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.PaymentTosActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.PaymentTosMutationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers {@link PaymentTosHandler}: the SMB platform gate, the
 * {@link ABProp#PAYMENTS_BR_PIX_ON_WEB} AB-prop gate, the {@link SyncdOperation#SET} path
 * that persists the {@link PaymentTosAction} via {@code LinkedWhatsAppStore.setPaymentTos}, the
 * malformed-value classification when the payload is missing, the
 * {@link SyncActionState#UNSUPPORTED} classification for non-{@code SET} operations and
 * gate failures, and the {@link PaymentTosMutationFactory} pending-mutation builder. Each
 * test builds its own mutation and opts into the platform and AB prop explicitly so the
 * gating dimension under test is declarative.
 */
@DisplayName("PaymentTosHandler")
class PaymentTosHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppStore store;
    private TestWhatsAppClient testClient;
    private TestABPropsService props;
    private PaymentTosHandler handler;
    private PaymentTosMutationFactory factory;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        props = TestABPropsService.builder().build();
        testClient = TestWhatsAppClient.create()
                .withStore(store)
                .withAbPropsService(props);
        handler = new PaymentTosHandler(props);
        factory = new PaymentTosMutationFactory();
    }

    private void smbPlatform() {
        store.accountStore().setDevice(store.accountStore().device().withPlatform(ClientPlatformType.IOS_BUSINESS));
    }

    private void enablePix() {
        props.set(ABProp.PAYMENTS_BR_PIX_ON_WEB, true);
    }

    private static PaymentTosAction validAction() {
        return new PaymentTosActionBuilder()
                .paymentNotice(PaymentTosAction.PaymentNotice.BR_PAY_PRIVACY_POLICY)
                .accepted(true)
                .build();
    }

    // Passing action == null omits the paymentTosAction sub-message so the malformed-value branch can be exercised.
    private static DecryptedMutation.Trusted setMutation(PaymentTosAction action) {
        var ts = Instant.ofEpochSecond(1_700_000_000L);
        var builder = new SyncActionValueBuilder().timestamp(ts);
        if (action != null) builder.paymentTosAction(action);
        return new DecryptedMutation.Trusted("[\"payment_tos\"]", builder.build(),
                SyncdOperation.SET, ts, PaymentTosAction.ACTION_VERSION);
    }

    @Nested
    @DisplayName("metadata â€” wire constants")
    class Metadata {
        @Test
        @DisplayName("actionName() is payment_tos")
        void actionName() {
            assertEquals(PaymentTosAction.ACTION_NAME, handler.actionName());
            assertEquals("payment_tos", handler.actionName());
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
            assertEquals(PaymentTosAction.ACTION_VERSION, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation â€” platform gating")
    class PlatformGating {
        @Test
        @DisplayName("default WEB platform short-circuits to UNSUPPORTED")
        void unsupportedOnNonSmb() {
            assertEquals(SyncActionState.UNSUPPORTED,
                    handler.applyMutation(testClient, setMutation(validAction())).actionState());
        }

        @Test
        @DisplayName("IOS_BUSINESS platform is accepted")
        void iosBusinessAccepted() {
            store.accountStore().setDevice(store.accountStore().device().withPlatform(ClientPlatformType.IOS_BUSINESS));
            enablePix();
            assertEquals(SyncActionState.SUCCESS,
                    handler.applyMutation(testClient, setMutation(validAction())).actionState());
        }

        @Test
        @DisplayName("ANDROID_BUSINESS platform is accepted")
        void androidBusinessAccepted() {
            store.accountStore().setDevice(store.accountStore().device().withPlatform(ClientPlatformType.ANDROID_BUSINESS));
            enablePix();
            assertEquals(SyncActionState.SUCCESS,
                    handler.applyMutation(testClient, setMutation(validAction())).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation â€” AB-prop gate")
    class AbPropGating {
        @Test
        @DisplayName("disabled payments_br_pix_on_web AB prop returns UNSUPPORTED")
        void abPropDisabled() {
            smbPlatform();
            // AB prop defaults to false
            assertEquals(SyncActionState.UNSUPPORTED,
                    handler.applyMutation(testClient, setMutation(validAction())).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation â€” non-SET operation")
    class RemoveBranch {
        @Test
        @DisplayName("REMOVE operation past the gates is UNSUPPORTED")
        void removeUnsupported() {
            smbPlatform();
            enablePix();
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var value = new SyncActionValueBuilder().timestamp(ts).paymentTosAction(validAction()).build();
            var mutation = new DecryptedMutation.Trusted("[\"payment_tos\"]", value,
                    SyncdOperation.REMOVE, ts, 7);
            assertEquals(SyncActionState.UNSUPPORTED,
                    handler.applyMutation(testClient, mutation).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation â€” malformed value")
    class MalformedValue {
        @Test
        @DisplayName("missing paymentTosAction sub-message is MALFORMED")
        void missingActionMessage() {
            smbPlatform();
            enablePix();
            assertEquals(SyncActionState.MALFORMED,
                    handler.applyMutation(testClient, setMutation(null)).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation â€” happy SET path")
    class HappySet {
        @Test
        @DisplayName("SET persists the action to the store and reports SUCCESS")
        void persistsAction() {
            smbPlatform();
            enablePix();
            var action = validAction();
            var result = handler.applyMutation(testClient, setMutation(action));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var persisted = store.businessStore().paymentTos().orElseThrow(
                    () -> new AssertionError("paymentTos must be persisted"));
            assertEquals(PaymentTosAction.PaymentNotice.BR_PAY_PRIVACY_POLICY, persisted.paymentNotice());
            assertEquals(true, persisted.accepted());
        }
    }

    @Nested
    @DisplayName("applyMutation â€” malformed index (n/a)")
    class MalformedIndex {
        @Test
        @DisplayName("the handler does not parse indexParts beyond position 0, so index malformations are not exercised")
        void notExercised() {
            // The wire index is literally ["payment_tos"]; the handler does not consult any
            // other element. This @Nested makes the absence explicit per the per-handler matrix.
            smbPlatform();
            enablePix();
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var value = new SyncActionValueBuilder().timestamp(ts).paymentTosAction(validAction()).build();
            var mutation = new DecryptedMutation.Trusted("garbage-index", value, SyncdOperation.SET, ts, 7);
            assertEquals(SyncActionState.SUCCESS,
                    handler.applyMutation(testClient, mutation).actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts â€” default timestamp tiebreaker")
    class ResolveConflicts {
        @Test
        @DisplayName("remote with later timestamp wins (APPLY_REMOTE_DROP_LOCAL)")
        void remoteLater() {
            var local = setMutation(validAction());
            var remoteTs = Instant.ofEpochSecond(1_700_000_010L);
            var remoteValue = new SyncActionValueBuilder().timestamp(remoteTs)
                    .paymentTosAction(validAction()).build();
            var remote = new DecryptedMutation.Trusted("[\"payment_tos\"]", remoteValue,
                    SyncdOperation.SET, remoteTs, 7);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(local, remote).state());
        }
    }

    @Nested
    @DisplayName("applyMutationBatch â€” default per-item dispatch (n/a override)")
    class BatchDispatch {
        @Test
        @DisplayName("the handler does not override applyMutationBatch")
        void defaultDispatchPreserved() {
            smbPlatform();
            enablePix();
            var batch = List.of(setMutation(validAction()));
            var results = handler.applyMutationBatch(testClient, batch);
            assertEquals(1, results.size());
            assertEquals(SyncActionState.SUCCESS, results.get(0).actionState());
        }
    }

    @Nested
    @DisplayName("getPaymentTosSetMutation â€” pending mutation builder")
    class Builder {
        @Test
        @DisplayName("builder emits a SET pending mutation at [\"payment_tos\"]")
        void buildsCorrect() {
            var action = validAction();
            var pending = factory.getPaymentTosSetMutation(action);
            var mutation = pending.mutation();
            assertEquals(SyncdOperation.SET, mutation.operation());
            assertEquals(PaymentTosAction.ACTION_VERSION, mutation.actionVersion());
            assertEquals("[\"payment_tos\"]", mutation.index());
            var roundtrip = mutation.value().flatMap(sav -> sav.action()).filter(a -> a instanceof PaymentTosAction).map(a -> (PaymentTosAction) a).orElseThrow();
            assertEquals(PaymentTosAction.PaymentNotice.BR_PAY_PRIVACY_POLICY, roundtrip.paymentNotice());
        }

        @Test
        @DisplayName("attemptCount of a freshly built pending mutation is zero")
        void freshAttemptCount() {
            assertEquals(0,
                    factory.getPaymentTosSetMutation(validAction()).attemptCount());
        }
    }

}
