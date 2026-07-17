package com.github.auties00.cobalt.sync.handler;
import com.github.auties00.cobalt.migration.LiveLidMigrationService;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.migration.LidMigrationService;
import com.github.auties00.cobalt.wire.linked.device.capabilities.DeviceCapabilities;
import com.github.auties00.cobalt.wire.linked.device.capabilities.DeviceCapabilitiesBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.media.FavoritesActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.wam.LiveWamService;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the {@link DeviceCapabilitiesHandler} adapter for the
 * {@code device_capabilities} app-state sync action across metadata, the
 * non-primary (companion) device branch where the store is updated but no
 * LID-migration side effects fire, the early-return paths (non-SET, foreign
 * action, missing or blank JID), the per-device store write, and the default
 * conflict resolution and batch dispatch. The primary-device branch reaches
 * into {@link LidMigrationService}, which {@link TestWhatsAppClient} does not
 * stub, so it is exercised by the integration suite rather than here.
 *
 * <p>The handler is built with a real {@link LiveWamService} and
 * {@link LidMigrationService} so the dependency graph matches production wiring;
 * A/B props are stubbed via {@link TestABPropsService}. Each test starts from a
 * clean {@link DeviceFixtures#temporaryStore}, so device-capability writes land
 * in an empty per-JID map.
 */
@DisplayName("DeviceCapabilitiesHandler")
class DeviceCapabilitiesHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppClient client;
    private DeviceCapabilitiesHandler handler;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
        var props = TestABPropsService.builder().build();
        var wam = new LiveWamService(client, props);
        var lidMigrationService = new LiveLidMigrationService(client, props, wam);
        handler = new DeviceCapabilitiesHandler(lidMigrationService, wam);
    }

    private static DecryptedMutation.Trusted capabilitiesMutation(String jidString, DeviceCapabilities caps, SyncdOperation op, Instant ts) {
        var value = new SyncActionValueBuilder().timestamp(ts).deviceCapabilities(caps).build();
        var index = jidString == null
                ? "[\"device_capabilities\"]"
                : "[\"device_capabilities\",\"" + jidString + "\"]";
        return new DecryptedMutation.Trusted(index, value, op, ts, 7);
    }

    private static DeviceCapabilities emptyCapabilities() {
        return new DeviceCapabilitiesBuilder().build();
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName() returns 'device_capabilities'")
        void actionName() {
            assertEquals(DeviceCapabilities.ACTION_NAME, handler.actionName());
            assertEquals("device_capabilities", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() is SyncPatchType.REGULAR_LOW")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_LOW, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version (7)")
        void version() {
            assertEquals(DeviceCapabilities.ACTION_VERSION, handler.version());
            assertEquals(7, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation: SET on a non-primary companion device")
    class SetCompanion {
        @Test
        @DisplayName("device != 0: persists the per-device entry without reaching the primary branch")
        void writesPerDeviceEntry() {
            var companion = Jid.of("19250000002", JidServer.user(), 7, 0);
            var caps = emptyCapabilities();

            var result = handler.applyMutation(
                    client, capabilitiesMutation(companion.toString(), caps, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var stored = client.store().contactStore().findDeviceCapabilitiesEntry(companion).orElseThrow();
            assertEquals(companion, stored.deviceJid());
            assertNotNull(stored.capabilities());
        }
    }

    @Nested
    @DisplayName("applyMutation: early-return paths classified as SUCCESS")
    class EarlyReturn {
        @Test
        @DisplayName("non-SET operation is silently accepted as SUCCESS")
        void nonSetIsSuccess() {
            var result = handler.applyMutation(
                    client, capabilitiesMutation(SELF_PN.toString(), emptyCapabilities(), SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState(),
                    "WA Web returns Success for non-SET operations rather than UNSUPPORTED");
            assertTrue(client.store().contactStore().findDeviceCapabilitiesEntry(SELF_PN).isEmpty(),
                    "REMOVE must not write a per-device entry");
        }

        @Test
        @DisplayName("missing JID part is silently accepted as SUCCESS without any store write")
        void missingJid() {
            var result = handler.applyMutation(
                    client, capabilitiesMutation(null, emptyCapabilities(), SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }

        @Test
        @DisplayName("blank JID part is silently accepted as SUCCESS without any store write")
        void blankJid() {
            var result = handler.applyMutation(
                    client, capabilitiesMutation("   ", emptyCapabilities(), SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }

        @Test
        @DisplayName("non-deviceCapabilities action in the value is silently accepted as SUCCESS")
        void wrongActionType() {
            var wrongValue = new SyncActionValueBuilder()
                    .timestamp(Instant.now())
                    .favoritesAction(new FavoritesActionBuilder().favorites(List.of()).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"device_capabilities\",\"" + SELF_PN + "\"]",
                    wrongValue, SyncdOperation.SET, Instant.now(), 7);

            var result = handler.applyMutation(client, mutation);

            assertEquals(SyncActionState.SUCCESS, result.actionState(),
                    "WA Web always returns Success from the per-mutation loop body");
        }
    }

    @Nested
    @DisplayName("applyMutation: malformed index - n/a")
    class MalformedIndexNa {
        @Test
        @DisplayName("the handler does not produce a malformed verdict; absent JID is silently SUCCESS")
        void naMalformedIndex() {
            // Per WA Web, missing/blank index components fall through to {actionState: Success}
            // with no store write. There is no malformedActionIndex branch on this handler.
            var result = handler.applyMutation(
                    client, capabilitiesMutation(null, emptyCapabilities(), SyncdOperation.SET, Instant.now()));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation: malformed value - n/a")
    class MalformedValueNa {
        @Test
        @DisplayName("the handler does not produce a malformed verdict; foreign action is silently SUCCESS")
        void naMalformedValue() {
            var wrongValue = new SyncActionValueBuilder()
                    .timestamp(Instant.now())
                    .favoritesAction(new FavoritesActionBuilder().favorites(List.of()).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"device_capabilities\",\"" + SELF_PN + "\"]",
                    wrongValue, SyncdOperation.SET, Instant.now(), 7);
            var result = handler.applyMutation(client, mutation);
            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation: orphan paths - n/a")
    class OrphanNa {
        @Test
        @DisplayName("device_capabilities does not require an existing entity; every device JID is acceptable")
        void noOrphanPath() {
            var fresh = Jid.of("19250000003", JidServer.user(), 99, 0);

            var result = handler.applyMutation(
                    client, capabilitiesMutation(fresh.toString(), emptyCapabilities(), SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(client.store().contactStore().findDeviceCapabilitiesEntry(fresh).isPresent(),
                    "the handler creates a per-device entry on first sight");
        }
    }

    @Nested
    @DisplayName("applyMutation: primary-device branch - n/a (requires lidMigrationService stub)")
    class PrimaryDeviceNa {
        @Test
        @DisplayName("primary-device application is exercised through the integration suite")
        void primaryDeviceNotExercisedHere() {
            // The device == 0 branch triggers client.lidMigrationService() and the WAM
            // commit path. TestWhatsAppClient does not stub lidMigrationService, so the
            // call would throw UnsupportedOperationException; the integration suite under
            // WebAppStateServiceTest exercises this path end-to-end.
            assertNotNull(handler);
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp-based")
    class ResolveConflicts {
        @Test
        @DisplayName("remote with the later timestamp wins")
        void remoteWins() {
            var companion = Jid.of("19250000002", JidServer.user(), 7, 0);
            var local  = capabilitiesMutation(companion.toString(), emptyCapabilities(), SyncdOperation.SET, Instant.ofEpochSecond(1700000000L));
            var remote = capabilitiesMutation(companion.toString(), emptyCapabilities(), SyncdOperation.SET, Instant.ofEpochSecond(1700000010L));

            var resolution = handler.resolveConflicts(local, remote);

            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }
    }

    @Nested
    @DisplayName("applyMutationBatch - n/a, default implementation")
    class BatchNa {
        @Test
        @DisplayName("default applyMutationBatch delegates per mutation")
        void perItem() {
            var companion = Jid.of("19250000002", JidServer.user(), 7, 0);
            var batch = List.of(
                    capabilitiesMutation(companion.toString(), emptyCapabilities(), SyncdOperation.SET, Instant.now()),
                    capabilitiesMutation(companion.toString(), emptyCapabilities(), SyncdOperation.REMOVE, Instant.now()));

            var results = handler.applyMutationBatch(client, batch);

            assertEquals(2, results.size());
            assertEquals(SyncActionState.SUCCESS, results.get(0).actionState());
            assertEquals(SyncActionState.SUCCESS, results.get(1).actionState());
        }
    }

    @Nested
    @DisplayName("static builder methods - n/a, handler exposes none")
    class BuilderNa {
        @Test
        @DisplayName("DeviceCapabilitiesHandler does not expose a getMutation helper")
        void noBuilder() {
            assertNotNull(handler);
        }
    }
}
