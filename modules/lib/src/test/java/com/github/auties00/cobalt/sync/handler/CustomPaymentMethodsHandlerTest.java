package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.CustomPaymentMethod;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.CustomPaymentMethodBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.CustomPaymentMethodsAction;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.CustomPaymentMethodsActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.CustomPaymentMethodsMutationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link CustomPaymentMethodsHandler}, Cobalt's adapter for
 * {@code WAWebCustomPaymentMethodsSync}.
 *
 * <p>The handler is gated on the SMB platform variants and the
 * {@code payments_br_pix_phase_1_seller_sync_enabled} AB prop. On
 * {@code SET} it overwrites the merchant's custom-payment-methods list via
 * {@code setCustomPaymentMethods}.
 *
 * <p>Matrix:
 * <ul>
 *   <li>Metadata wire constants.</li>
 *   <li>Non-SMB platform short-circuits to {@code UNSUPPORTED}.</li>
 *   <li>AB-prop gate disabled returns {@code UNSUPPORTED}.</li>
 *   <li>Non-{@code SET} operation is {@code UNSUPPORTED}.</li>
 *   <li>Malformed value (missing sub-message) is {@code MALFORMED}.</li>
 *   <li>Happy path: SET stores the payment methods and reports
 *       {@code SUCCESS}.</li>
 *   <li>Default conflict resolution.</li>
 *   <li>Default batch dispatch.</li>
 *   <li>{@code getCustomPaymentMethodSetMutation} builder.</li>
 *   <li>Malformed index (n/a - handler does not parse indexParts).</li>
 * </ul>
 */
@DisplayName("CustomPaymentMethodsHandler")
class CustomPaymentMethodsHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppStore store;
    private TestWhatsAppClient testClient;
    private TestABPropsService props;
    private CustomPaymentMethodsHandler handler;
    private CustomPaymentMethodsMutationFactory factory;

    /**
     * Builds a fresh harness for each test.
     */
    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        props = TestABPropsService.builder().build();
        testClient = TestWhatsAppClient.create()
                .withStore(store)
                .withAbPropsService(props);
        handler = new CustomPaymentMethodsHandler(props);
        factory = new CustomPaymentMethodsMutationFactory();
    }

    /**
     * Sets the local store's platform to {@code IOS_BUSINESS}.
     */
    private void smbPlatform() {
        store.accountStore().setDevice(store.accountStore().device().withPlatform(ClientPlatformType.IOS_BUSINESS));
    }

    /**
     * Enables the {@code payments_br_pix_phase_1_seller_sync_enabled} AB prop.
     */
    private void enableSync() {
        props.set(ABProp.PAYMENTS_BR_PIX_PHASE_1_SELLER_SYNC_ENABLED, true);
    }

    /**
     * Builds a sample {@link CustomPaymentMethod} carrying a credentialId so that
     * the resulting action is observable.
     *
     * @return a populated payment method
     */
    private static CustomPaymentMethod sampleMethod() {
        return new CustomPaymentMethodBuilder()
                .credentialId("cred-123")
                .country("BR")
                .type("PIX")
                .build();
    }

    /**
     * Builds a trusted SET mutation carrying the given action.
     *
     * @param action the custom-payment-methods payload, or {@code null} to omit
     *               the sub-message
     * @return the trusted mutation
     */
    private static DecryptedMutation.Trusted setMutation(CustomPaymentMethodsAction action) {
        var ts = Instant.ofEpochSecond(1_700_000_000L);
        var builder = new SyncActionValueBuilder().timestamp(ts);
        if (action != null) builder.customPaymentMethodsAction(action);
        return new DecryptedMutation.Trusted("[\"custom_payment_methods\"]", builder.build(),
                SyncdOperation.SET, ts, CustomPaymentMethodsAction.ACTION_VERSION);
    }

    @Nested
    @DisplayName("metadata - wire constants")
    class Metadata {
        @Test
        @DisplayName("actionName() is custom_payment_methods")
        void actionName() {
            assertEquals(CustomPaymentMethodsAction.ACTION_NAME,
                    handler.actionName());
            assertEquals("custom_payment_methods", handler.actionName());
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
            assertEquals(CustomPaymentMethodsAction.ACTION_VERSION,
                    handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation - platform gating")
    class PlatformGating {
        @Test
        @DisplayName("default WEB platform short-circuits to UNSUPPORTED")
        void unsupportedOnNonSmb() {
            var action = new CustomPaymentMethodsActionBuilder()
                    .customPaymentMethods(List.of(sampleMethod())).build();
            assertEquals(SyncActionState.UNSUPPORTED,
                    handler.applyMutation(testClient,
                            setMutation(action)).actionState());
        }

        @Test
        @DisplayName("IOS_BUSINESS platform is accepted")
        void iosBusinessAccepted() {
            store.accountStore().setDevice(store.accountStore().device().withPlatform(ClientPlatformType.IOS_BUSINESS));
            enableSync();
            var action = new CustomPaymentMethodsActionBuilder()
                    .customPaymentMethods(List.of(sampleMethod())).build();
            assertEquals(SyncActionState.SUCCESS,
                    handler.applyMutation(testClient,
                            setMutation(action)).actionState());
        }

        @Test
        @DisplayName("ANDROID_BUSINESS platform is accepted")
        void androidBusinessAccepted() {
            store.accountStore().setDevice(store.accountStore().device().withPlatform(ClientPlatformType.ANDROID_BUSINESS));
            enableSync();
            var action = new CustomPaymentMethodsActionBuilder()
                    .customPaymentMethods(List.of(sampleMethod())).build();
            assertEquals(SyncActionState.SUCCESS,
                    handler.applyMutation(testClient,
                            setMutation(action)).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - AB-prop gate")
    class AbPropGating {
        @Test
        @DisplayName("disabled payments_br_pix_phase_1_seller_sync_enabled AB prop returns UNSUPPORTED")
        void abPropDisabled() {
            smbPlatform();
            // AB prop defaults to false
            var action = new CustomPaymentMethodsActionBuilder()
                    .customPaymentMethods(List.of(sampleMethod())).build();
            assertEquals(SyncActionState.UNSUPPORTED,
                    handler.applyMutation(testClient,
                            setMutation(action)).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - non-SET operation")
    class RemoveBranch {
        @Test
        @DisplayName("REMOVE operation past the gates is UNSUPPORTED")
        void removeUnsupported() {
            smbPlatform();
            enableSync();
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var action = new CustomPaymentMethodsActionBuilder()
                    .customPaymentMethods(List.of(sampleMethod())).build();
            var value = new SyncActionValueBuilder().timestamp(ts).customPaymentMethodsAction(action).build();
            var mutation = new DecryptedMutation.Trusted("[\"custom_payment_methods\"]", value,
                    SyncdOperation.REMOVE, ts, 7);
            assertEquals(SyncActionState.UNSUPPORTED,
                    handler.applyMutation(testClient, mutation).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed value")
    class MalformedValue {
        @Test
        @DisplayName("missing customPaymentMethodsAction sub-message is MALFORMED")
        void missingActionMessage() {
            smbPlatform();
            enableSync();
            assertEquals(SyncActionState.MALFORMED,
                    handler.applyMutation(testClient,
                            setMutation(null)).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - happy SET path")
    class HappySet {
        @Test
        @DisplayName("SET stores the payment-method list and reports SUCCESS")
        void persistsMethods() {
            smbPlatform();
            enableSync();
            var action = new CustomPaymentMethodsActionBuilder()
                    .customPaymentMethods(List.of(sampleMethod())).build();
            var result = handler.applyMutation(testClient, setMutation(action));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var persisted = store.businessStore().customPaymentMethods();
            assertEquals(1, persisted.size(),
                    "the stored list must match the action payload arity");
            assertEquals("cred-123", persisted.get(0).credentialId());
        }

        @Test
        @DisplayName("SET with an empty list still reports SUCCESS and clears the store")
        void persistsEmpty() {
            smbPlatform();
            enableSync();
            // Seed a prior entry so we can observe the clear
            store.businessStore().setCustomPaymentMethods(List.of(sampleMethod()));
            var action = new CustomPaymentMethodsActionBuilder()
                    .customPaymentMethods(List.of()).build();
            var result = handler.applyMutation(testClient, setMutation(action));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertEquals(0, store.businessStore().customPaymentMethods().size(),
                    "an empty SET must clear the stored list");
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed index (n/a)")
    class MalformedIndex {
        @Test
        @DisplayName("the handler does not parse indexParts beyond position 0, so index malformations are not exercised")
        void notExercised() {
            // The wire index is literally ["custom_payment_methods"]; the handler ignores every
            // other element. This @Nested makes the absence explicit per the per-handler matrix.
            smbPlatform();
            enableSync();
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var action = new CustomPaymentMethodsActionBuilder()
                    .customPaymentMethods(List.of(sampleMethod())).build();
            var value = new SyncActionValueBuilder().timestamp(ts).customPaymentMethodsAction(action).build();
            var mutation = new DecryptedMutation.Trusted("garbage-index", value, SyncdOperation.SET, ts, 7);
            assertEquals(SyncActionState.SUCCESS,
                    handler.applyMutation(testClient, mutation).actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp tiebreaker")
    class ResolveConflicts {
        @Test
        @DisplayName("remote with later timestamp wins (APPLY_REMOTE_DROP_LOCAL)")
        void remoteLater() {
            var local = setMutation(new CustomPaymentMethodsActionBuilder()
                    .customPaymentMethods(List.of(sampleMethod())).build());
            var remoteTs = Instant.ofEpochSecond(1_700_000_010L);
            var remoteValue = new SyncActionValueBuilder().timestamp(remoteTs)
                    .customPaymentMethodsAction(new CustomPaymentMethodsActionBuilder()
                            .customPaymentMethods(List.of()).build())
                    .build();
            var remote = new DecryptedMutation.Trusted("[\"custom_payment_methods\"]", remoteValue,
                    SyncdOperation.SET, remoteTs, 7);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(local, remote).state());
        }
    }

    @Nested
    @DisplayName("applyMutationBatch - default per-item dispatch (n/a override)")
    class BatchDispatch {
        @Test
        @DisplayName("the handler does not override applyMutationBatch")
        void defaultDispatchPreserved() {
            smbPlatform();
            enableSync();
            var action = new CustomPaymentMethodsActionBuilder()
                    .customPaymentMethods(List.of(sampleMethod())).build();
            var results = handler.applyMutationBatch(testClient,
                    List.of(setMutation(action)));
            assertEquals(1, results.size());
            assertEquals(SyncActionState.SUCCESS, results.get(0).actionState());
        }
    }

    @Nested
    @DisplayName("getCustomPaymentMethodSetMutation - pending mutation builder")
    class Builder {
        @Test
        @DisplayName("builder emits a SET pending mutation at [\"custom_payment_methods\"]")
        void buildsCorrect() {
            var action = new CustomPaymentMethodsActionBuilder()
                    .customPaymentMethods(List.of(sampleMethod())).build();
            var pending = factory.getCustomPaymentMethodSetMutation(action);
            var mutation = pending.mutation();
            assertEquals(SyncdOperation.SET, mutation.operation());
            assertEquals(CustomPaymentMethodsAction.ACTION_VERSION, mutation.actionVersion());
            assertEquals("[\"custom_payment_methods\"]", mutation.index());
            var roundtrip = mutation.value().flatMap(sav -> sav.action()).filter(a -> a instanceof CustomPaymentMethodsAction).map(a -> (CustomPaymentMethodsAction) a).orElseThrow();
            assertEquals(1, roundtrip.customPaymentMethods().size());
        }

        @Test
        @DisplayName("attemptCount of a freshly built pending mutation is zero")
        void freshAttemptCount() {
            var action = new CustomPaymentMethodsActionBuilder()
                    .customPaymentMethods(List.of()).build();
            assertEquals(0,
                    factory.getCustomPaymentMethodSetMutation(action).attemptCount());
        }
    }

}
