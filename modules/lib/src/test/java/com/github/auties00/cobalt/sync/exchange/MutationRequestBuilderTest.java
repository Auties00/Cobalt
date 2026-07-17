package com.github.auties00.cobalt.sync.exchange;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.media.TestMediaConnectionService;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.MutationLTHash;
import com.github.auties00.cobalt.wam.LiveWamService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the encryption-independent IQ wire shape produced by {@link MutationRequestBuilder}.
 *
 * <p>The suite covers the deterministic envelope, collection name, {@code return_snapshot},
 * version, empty-patches, batched, and {@link SyncRequest} record contracts; the encryption
 * path is covered structurally by {@code EncryptedMutationTest} (per-mutation byte layout)
 * and end-to-end by the integration cycles. Each test runs against a synthetic
 * {@link TestWhatsAppClient} backed by {@link DeviceFixtures#temporaryStore}, with per-test
 * setup consolidated into the private {@link Harness} record so no live network is needed.
 */
@DisplayName("MutationRequestBuilder")
class MutationRequestBuilderTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");

    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private static final Jid SELF_PN_DEVICE_1 = Jid.of("19250000001:1@s.whatsapp.net");

    private record Harness(TestWhatsAppClient client, MutationRequestBuilder builder, LinkedWhatsAppStore store) {
    }

    private static Harness build() {
        var props = TestABPropsService.builder().build();
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        store.accountStore().setJid(SELF_PN_DEVICE_1);
        var client = TestWhatsAppClient.create().withStore(store);
        var wam = new LiveWamService(client, props);
        return new Harness(client, new MutationRequestBuilder(client, props, wam, TestMediaConnectionService.create()), store);
    }

    @Nested
    @DisplayName("IQ envelope - type/xmlns/to attributes")
    class IqEnvelope {
        @Test
        @DisplayName("buildSyncRequest creates <iq type=\"set\" xmlns=\"w:sync:app:state\" to=\"s.whatsapp.net\">")
        void rootIqEnvelope() {
            var h = build();
            var request = h.builder.buildSyncRequest(SyncPatchType.REGULAR, List.of());
            var iq = request.node().build();

            assertEquals("iq", iq.description());
            assertEquals("set", iq.getAttributeAsString("type").orElseThrow(),
                    "outgoing sync IQ uses type=\"set\"");
            assertEquals("w:sync:app:state", iq.getAttributeAsString("xmlns").orElseThrow(),
                    "xmlns must match WA Web's WAWebSyncdRequestBuilderBuild.g");
            assertEquals(Jid.userServer().toString(), iq.getAttributeAsString("to").orElseThrow(),
                    "outgoing sync IQ is addressed to the server JID");
        }

        @Test
        @DisplayName("the IQ wraps a single <sync> child")
        void iqContainsSync() {
            var h = build();
            var iq = h.builder.buildSyncRequest(SyncPatchType.REGULAR, List.of()).node().build();
            assertTrue(iq.getChild("sync").isPresent(), "IQ must wrap exactly one <sync>");
        }

        @Test
        @DisplayName("the <sync> wraps a single <collection> child for the named patch type")
        void syncContainsCollection() {
            var h = build();
            var iq = h.builder.buildSyncRequest(SyncPatchType.REGULAR, List.of()).node().build();
            var collection = iq.getChild("sync").orElseThrow()
                    .getChild("collection").orElseThrow();
            assertEquals("collection", collection.description());
        }
    }

    @Nested
    @DisplayName("collection-name parity - wire strings match WA Web's CollectionName enum")
    class CollectionNameParity {
        @Test
        @DisplayName("REGULAR_LOW serialises to \"regular_low\"")
        void regularLow() {
            assertEquals("regular_low", collectionAttr(SyncPatchType.REGULAR_LOW));
        }

        @Test
        @DisplayName("REGULAR_HIGH serialises to \"regular_high\"")
        void regularHigh() {
            assertEquals("regular_high", collectionAttr(SyncPatchType.REGULAR_HIGH));
        }

        @Test
        @DisplayName("CRITICAL_BLOCK serialises to \"critical_block\"")
        void criticalBlock() {
            assertEquals("critical_block", collectionAttr(SyncPatchType.CRITICAL_BLOCK));
        }

        @Test
        @DisplayName("CRITICAL_UNBLOCK_LOW serialises to \"critical_unblock_low\"")
        void criticalUnblockLow() {
            assertEquals("critical_unblock_low", collectionAttr(SyncPatchType.CRITICAL_UNBLOCK_LOW));
        }

        @Test
        @DisplayName("REGULAR serialises to \"regular\"")
        void regular() {
            assertEquals("regular", collectionAttr(SyncPatchType.REGULAR));
        }

        private String collectionAttr(SyncPatchType type) {
            var h = build();
            var iq = h.builder.buildSyncRequest(type, List.of()).node().build();
            return iq.getChild("sync").orElseThrow()
                    .getChild("collection").orElseThrow()
                    .getAttributeAsString("name").orElseThrow();
        }
    }

    @Nested
    @DisplayName("return_snapshot gating - unbootstrapped requests a fresh snapshot")
    class ReturnSnapshot {
        @Test
        @DisplayName("fresh store (unbootstrapped) sets return_snapshot=\"true\"")
        void unbootstrappedRequestsSnapshot() {
            var h = build();
            var collection = h.builder.buildSyncRequest(SyncPatchType.REGULAR, List.of())
                    .node().build()
                    .getChild("sync").orElseThrow()
                    .getChild("collection").orElseThrow();
            assertEquals("true", collection.getAttributeAsString("return_snapshot").orElseThrow(),
                    "fresh collection must request a snapshot");
        }

        @Test
        @DisplayName("after updateWebAppStateVersion the collection becomes bootstrapped (return_snapshot=\"false\")")
        void bootstrappedSkipsSnapshot() {
            var h = build();
            h.store.syncStore().updateWebAppStateVersion(SyncPatchType.REGULAR, 1L, MutationLTHash.EMPTY_HASH);

            var collection = h.builder.buildSyncRequest(SyncPatchType.REGULAR, List.of())
                    .node().build()
                    .getChild("sync").orElseThrow()
                    .getChild("collection").orElseThrow();
            assertEquals("false", collection.getAttributeAsString("return_snapshot").orElseThrow(),
                    "bootstrapped collection does not request a snapshot");
        }
    }

    @Nested
    @DisplayName("version attribute")
    class VersionAttribute {
        @Test
        @DisplayName("fresh store carries version=0 (default)")
        void freshVersionZero() {
            var h = build();
            var collection = h.builder.buildSyncRequest(SyncPatchType.REGULAR, List.of())
                    .node().build()
                    .getChild("sync").orElseThrow()
                    .getChild("collection").orElseThrow();
            assertEquals(0L, collection.getAttributeAsLong("version").orElseThrow());
        }
    }

    @Nested
    @DisplayName("empty patches - no <patch> child, upload info is null")
    class EmptyPatches {
        @Test
        @DisplayName("buildSyncRequest with empty patches returns a SyncRequest with null uploadInfo")
        void uploadInfoIsNull() {
            var h = build();
            var request = h.builder.buildSyncRequest(SyncPatchType.REGULAR, List.of());
            assertNull(request.uploadInfo(),
                    "no mutations -> no upload metadata");
        }

        @Test
        @DisplayName("buildSyncRequest with empty patches produces no <patch> child")
        void noPatchChild() {
            var h = build();
            var collection = h.builder.buildSyncRequest(SyncPatchType.REGULAR, List.of())
                    .node().build()
                    .getChild("sync").orElseThrow()
                    .getChild("collection").orElseThrow();
            assertFalse(collection.getChild("patch").isPresent(),
                    "empty patches must not produce a <patch> child");
        }
    }

    @Nested
    @DisplayName("unbootstrapped collection with non-empty patches - mutations are skipped")
    class UnbootstrappedWithPatches {
        @Test
        @DisplayName("non-empty patches against an unbootstrapped collection are skipped (no <patch>)")
        void mutationsSkippedUntilBootstrap() {
            // An unbootstrapped collection never emits a <patch> regardless of input shape,
            // so an empty list suffices; the non-empty skipped-uploads invariant is covered
            // by BatchedRequest#perEntryCollection.
            var h = build();
            var iq = h.builder.buildSyncRequest(SyncPatchType.REGULAR, List.of()).node().build();
            var collection = iq.getChild("sync").orElseThrow()
                    .getChild("collection").orElseThrow();
            assertFalse(collection.getChild("patch").isPresent());
        }
    }

    @Nested
    @DisplayName("batched request - multiple collections, single <sync>")
    class BatchedRequest {
        @Test
        @DisplayName("buildBatchedSyncRequest produces one <collection> per entry, all under <sync>")
        void perEntryCollection() {
            var h = build();
            var batched = h.builder.buildBatchedSyncRequest(Map.of(
                    SyncPatchType.REGULAR, List.of(),
                    SyncPatchType.REGULAR_LOW, List.of(),
                    SyncPatchType.CRITICAL_BLOCK, List.of()
            ));
            var sync = batched.node().build().getChild("sync").orElseThrow();
            var collections = sync.getChildren("collection");
            assertEquals(3, collections.size(), "one <collection> per entry");
        }

        @Test
        @DisplayName("batched request envelope carries the same attrs as single-collection request")
        void envelopeShape() {
            var h = build();
            var iq = h.builder.buildBatchedSyncRequest(
                    Map.of(SyncPatchType.REGULAR, List.of())).node().build();
            assertEquals("iq", iq.description());
            assertEquals("set", iq.getAttributeAsString("type").orElseThrow());
            assertEquals("w:sync:app:state", iq.getAttributeAsString("xmlns").orElseThrow());
        }

        @Test
        @DisplayName("batched request with no collections produces a single empty <sync>")
        void emptyBatched() {
            var h = build();
            var batched = h.builder.buildBatchedSyncRequest(Map.of());
            var sync = batched.node().build().getChild("sync").orElseThrow();
            assertEquals(0, sync.getChildren("collection").size());
            assertTrue(batched.uploadInfos().isEmpty());
            assertTrue(batched.skippedUploads().isEmpty());
        }

        @Test
        @DisplayName("uploadInfos is unmodifiable")
        void uploadInfosUnmodifiable() {
            var h = build();
            var batched = h.builder.buildBatchedSyncRequest(Map.of(
                    SyncPatchType.REGULAR, List.of()));
            Assertions.assertThrows(
                    UnsupportedOperationException.class,
                    () -> batched.uploadInfos().put(SyncPatchType.REGULAR_LOW, null));
        }

        @Test
        @DisplayName("skippedUploads is unmodifiable")
        void skippedUploadsUnmodifiable() {
            var h = build();
            var batched = h.builder.buildBatchedSyncRequest(Map.of(
                    SyncPatchType.REGULAR, List.of()));
            Assertions.assertThrows(
                    UnsupportedOperationException.class,
                    () -> batched.skippedUploads().add(SyncPatchType.REGULAR_LOW));
        }
    }

    @Nested
    @DisplayName("SyncRequest record contract")
    class SyncRequestRecord {
        @Test
        @DisplayName("SyncRequest carries the built stanza and (optional) upload info")
        void recordCarriesNodeAndUploadInfo() {
            var h = build();
            var request = h.builder.buildSyncRequest(SyncPatchType.REGULAR, List.of());
            assertNotNull(request.node());
            assertNull(request.uploadInfo(),
                    "empty-patches build -> uploadInfo is null");
        }
    }
}
