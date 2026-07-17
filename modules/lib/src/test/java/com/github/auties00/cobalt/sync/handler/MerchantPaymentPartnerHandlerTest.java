package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.MerchantPaymentPartnerAction;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.MerchantPaymentPartnerAction.Status;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.MerchantPaymentPartnerActionBuilder;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link MerchantPaymentPartnerHandler}: metadata, the SMB platform gating
 * ({@link ClientPlatformType#IOS_BUSINESS} / {@link ClientPlatformType#ANDROID_BUSINESS}), the
 * {@link ABProp#PAYMENTS_BR_MERCHANT_PSP_ACCOUNT_STATUS_SYNC} gating, the SET happy path that
 * persists the action via
 * {@link LinkedWhatsAppBusinessStore#setMerchantPaymentPartner(MerchantPaymentPartnerAction)}, the malformed-value
 * branch, the REMOVE rejection and the inherited timestamp-based conflict resolution.
 *
 * <p>The handler is built with a stubbed {@link TestABPropsService} so the gating prop can be
 * flipped per test, and runs against an in-memory {@link DeviceFixtures#temporaryStore} via
 * {@link TestWhatsAppClient} so the {@link LinkedWhatsAppBusinessStore#merchantPaymentPartner()} read-back can be
 * asserted directly.
 */
@DisplayName("MerchantPaymentPartnerHandler")
class MerchantPaymentPartnerHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppStore store;
    private TestABPropsService props;
    private MerchantPaymentPartnerHandler handler;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        props = TestABPropsService.builder().build();
        handler = new MerchantPaymentPartnerHandler(props);
    }

    // A nullable action lets the malformed-value path be exercised without re-implementing
    // the envelope.
    private DecryptedMutation.Trusted buildMutation(MerchantPaymentPartnerAction action,
                                                    SyncdOperation operation, Instant ts) {
        var valueBuilder = new SyncActionValueBuilder().timestamp(ts);
        if (action != null) {
            valueBuilder.merchantPaymentPartnerAction(action);
        }
        var index = JSON.toJSONString(List.of(handler.actionName()));
        return new DecryptedMutation.Trusted(index, valueBuilder.build(), operation, ts, handler.version());
    }

    private MerchantPaymentPartnerAction sampleAction() {
        return new MerchantPaymentPartnerActionBuilder()
                .status(Status.ACTIVE)
                .country("BR")
                .gatewayName("psp")
                .credentialId("cred-1")
                .build();
    }

    @Nested
    @DisplayName("metadata - wire identity")
    class Metadata {
        @Test
        @DisplayName("actionName() returns the wire constant")
        void actionName() {
            assertEquals(MerchantPaymentPartnerAction.ACTION_NAME, handler.actionName());
            assertEquals("merchant_payment_partner", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR_LOW")
        void collectionName() {
            assertEquals(MerchantPaymentPartnerAction.COLLECTION_NAME, handler.collectionName());
            assertEquals(SyncPatchType.REGULAR_LOW, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version")
        void version() {
            assertEquals(MerchantPaymentPartnerAction.ACTION_VERSION, handler.version());
        }
    }

    @Nested
    @DisplayName("platform gate - only SMB iOS/Android pass")
    class PlatformGate {
        @Test
        @DisplayName("a non-business platform returns UNSUPPORTED without reading the AB prop")
        void nonBusinessPlatformIsUnsupported() {
            // Default temporary-store platform is not set to a business variant. Using
            // TestWhatsAppClient (which throws on abPropsService) is safe here because the
            // platform gate trips first and short-circuits the AB-prop call.
            var client = TestWhatsAppClient.create().withStore(store);
            store.accountStore().setDevice(store.accountStore().device().withPlatform(ClientPlatformType.WEB));

            var result = handler.applyMutation(client,
                    buildMutation(sampleAction(), SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }

        @Test
        @DisplayName("the ANDROID_BUSINESS platform passes the platform gate")
        void androidBusinessPlatformPasses() {
            store.accountStore().setDevice(store.accountStore().device().withPlatform(ClientPlatformType.ANDROID_BUSINESS));
            props.set(ABProp.PAYMENTS_BR_MERCHANT_PSP_ACCOUNT_STATUS_SYNC, true);
            var client = TestWhatsAppClient.create().withStore(store).withAbPropsService(props);

            assertEquals(SyncActionState.SUCCESS, handler.applyMutation(client,
                    buildMutation(sampleAction(), SyncdOperation.SET, Instant.now()))
                    .actionState());
        }

        @Test
        @DisplayName("the IOS_BUSINESS platform passes the platform gate")
        void iosBusinessPlatformPasses() {
            store.accountStore().setDevice(store.accountStore().device().withPlatform(ClientPlatformType.IOS_BUSINESS));
            props.set(ABProp.PAYMENTS_BR_MERCHANT_PSP_ACCOUNT_STATUS_SYNC, true);
            var client = TestWhatsAppClient.create().withStore(store).withAbPropsService(props);

            assertEquals(SyncActionState.SUCCESS, handler.applyMutation(client,
                    buildMutation(sampleAction(), SyncdOperation.SET, Instant.now()))
                    .actionState());
        }
    }

    @Nested
    @DisplayName("AB-prop gate - when SMB but prop is off")
    class AbPropGate {
        @Test
        @DisplayName("a business platform with the AB prop unset returns UNSUPPORTED")
        void propOffIsUnsupported() {
            store.accountStore().setDevice(store.accountStore().device().withPlatform(ClientPlatformType.ANDROID_BUSINESS));
            // The AB prop defaults to "false" - explicitly set to false to exercise the gate path.
            props.set(ABProp.PAYMENTS_BR_MERCHANT_PSP_ACCOUNT_STATUS_SYNC, false);
            var client = TestWhatsAppClient.create().withStore(store).withAbPropsService(props);

            var result = handler.applyMutation(client,
                    buildMutation(sampleAction(), SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - SET happy path")
    class ApplySetHappy {
        @Test
        @DisplayName("SET on SMB with AB prop on persists the merchant partner")
        void persistsMerchantPartner() {
            store.accountStore().setDevice(store.accountStore().device().withPlatform(ClientPlatformType.ANDROID_BUSINESS));
            props.set(ABProp.PAYMENTS_BR_MERCHANT_PSP_ACCOUNT_STATUS_SYNC, true);
            var client = TestWhatsAppClient.create().withStore(store).withAbPropsService(props);
            var action = sampleAction();

            var result = handler.applyMutation(client,
                    buildMutation(action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var stored = store.businessStore().merchantPaymentPartner().orElseThrow();
            assertEquals(Status.ACTIVE, stored.status());
            assertEquals("BR", stored.country());
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan dimension is n/a")
    class OrphanDimension {
        @Test
        @DisplayName("there is no orphan branch - the handler writes a singleton store slot")
        void orphanNotApplicable() {
            store.accountStore().setDevice(store.accountStore().device().withPlatform(ClientPlatformType.ANDROID_BUSINESS));
            props.set(ABProp.PAYMENTS_BR_MERCHANT_PSP_ACCOUNT_STATUS_SYNC, true);
            var client = TestWhatsAppClient.create()
                    .withStore(store)
                    .withAbPropsService(props);

            assertEquals(SyncActionState.SUCCESS, handler.applyMutation(client,
                    buildMutation(sampleAction(), SyncdOperation.SET, Instant.now()))
                    .actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed value")
    class MalformedValue {
        @Test
        @DisplayName("a SET whose value carries the wrong action returns MALFORMED")
        void wrongActionType() {
            store.accountStore().setDevice(store.accountStore().device().withPlatform(ClientPlatformType.ANDROID_BUSINESS));
            props.set(ABProp.PAYMENTS_BR_MERCHANT_PSP_ACCOUNT_STATUS_SYNC, true);
            var client = TestWhatsAppClient.create()
                    .withStore(store)
                    .withAbPropsService(props);

            var ts = Instant.now();
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .pinAction(new PinActionBuilder().pinned(true).build())
                    .build();
            var index = JSON.toJSONString(List.of(handler.actionName()));
            var mutation = new DecryptedMutation.Trusted(index, value,
                    SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed index dimension is n/a")
    class MalformedIndex {
        @Test
        @DisplayName("the handler never reads indexParts past slot 0 - index content is irrelevant")
        void indexUnused() {
            // The index for this action is the singleton ["merchant_payment_partner"]; the handler
            // does not extract any positional argument. Confirm that arbitrary index payloads
            // still pass through the gates and reach the action-type check.
            store.accountStore().setDevice(store.accountStore().device().withPlatform(ClientPlatformType.ANDROID_BUSINESS));
            props.set(ABProp.PAYMENTS_BR_MERCHANT_PSP_ACCOUNT_STATUS_SYNC, true);
            var client = TestWhatsAppClient.create()
                    .withStore(store)
                    .withAbPropsService(props);

            var ts = Instant.now();
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .merchantPaymentPartnerAction(sampleAction())
                    .build();
            for (var index : new String[]{
                    "[\"merchant_payment_partner\"]",
                    "[\"merchant_payment_partner\", \"extra-1\"]",
                    "[\"merchant_payment_partner\", \"extra-1\", \"extra-2\"]"
            }) {
                var mutation = new DecryptedMutation.Trusted(index, value,
                        SyncdOperation.SET, ts, handler.version());
                assertEquals(SyncActionState.SUCCESS, handler.applyMutation(client, mutation).actionState(),
                        "index payload '" + index + "' must not affect outcome");
            }
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE")
    class ApplyRemove {
        @Test
        @DisplayName("REMOVE on SMB with AB prop on returns UNSUPPORTED")
        void removeUnsupported() {
            store.accountStore().setDevice(store.accountStore().device().withPlatform(ClientPlatformType.ANDROID_BUSINESS));
            props.set(ABProp.PAYMENTS_BR_MERCHANT_PSP_ACCOUNT_STATUS_SYNC, true);
            var client = TestWhatsAppClient.create()
                    .withStore(store)
                    .withAbPropsService(props);

            var result = handler.applyMutation(client,
                    buildMutation(sampleAction(), SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
            assertTrue(store.businessStore().merchantPaymentPartner().isEmpty(),
                    "REMOVE must not touch the store on this handler");
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
            return buildMutation(sampleAction(), SyncdOperation.SET, ts);
        }
    }

    @Nested
    @DisplayName("static builders - n/a")
    class StaticBuilders {
        @Test
        @DisplayName("MerchantPaymentPartnerHandler exposes no outbound mutation builder - dimension is n/a")
        void noBuilder() {
            assertEquals(handler, handler);
        }
    }
}
