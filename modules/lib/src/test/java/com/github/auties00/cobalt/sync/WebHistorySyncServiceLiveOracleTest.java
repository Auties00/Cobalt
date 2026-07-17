package com.github.auties00.cobalt.sync;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.linked.message.system.history.HistorySyncType;
import com.github.auties00.cobalt.wire.linked.sync.history.HistorySync;
import it.auties.protobuf.stream.ProtobufInputStream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parity suite asserting that Cobalt's {@link HistorySync} decoder reproduces the value WhatsApp Web
 * decodes from the same inflated chunk bytes. Every captured fixture under
 * {@code modules/lib/src/test/resources/fixtures/sync/history/<slug>/} carries the inflated protobuf
 * bytes, the parsed value oracle, and the {@code HistorySyncNotification} that introduced the chunk;
 * each {@code @Nested} block covers one {@link HistorySyncType} variant. Tests gate themselves on
 * {@link SyncFixtures#isHistoryChunkAvailable(String)} via {@link Assumptions#assumeTrue(boolean)}
 * so the suite stays green before fixtures are captured and exercises new fixtures the moment they
 * land. Only the structural invariants common to both the {@code HistorySync.Full} and
 * {@code HistorySync.Light} variants ({@code syncType}, {@code chunkOrder}, {@code progress}, list
 * sizes) are asserted; per-element field checks are out of scope.
 *
 * <p>Three chunk-type slugs ({@code full}, {@code recent}, {@code on-demand}) are not captured at
 * this revision: {@code full} on-demand history sync is not supported on web and the request is
 * dropped, {@code recent} is server-gated and was not delivered to the freshly-paired session that
 * drove the corpus, and {@code on-demand} is dropped by the primary when the companion already has
 * every eligible message for the target chat. Their {@code @Nested} blocks remain so a future
 * capture run that does land them activates the tests without code changes.
 */
@DisplayName("WebHistorySyncService -- live-oracle parity")
class WebHistorySyncServiceLiveOracleTest {

    /**
     * Asserts that the decoder produces the same key fields as the captured WA Web oracle for one
     * fixture slug, skipping cleanly via {@link Assumptions#assumeTrue(boolean)} when the fixture is
     * not on the classpath.
     *
     * <p>Always invokes {@link HistorySync#ofFull(ProtobufInputStream)}; the
     * {@code HistorySync.Light} variant shares the same wire layout for the asserted fields, so the
     * full decoder suffices for parity. The {@code conversations} and {@code statusV3Messages} list
     * sizes are asserted because the full decoder populates them and they are key inputs to Cobalt's
     * chat-and-status fan-out in {@link WebHistorySyncService}.
     *
     * @param typeSlug         the per-syncType fixture slug (for
     *                         example {@code "initial-bootstrap"})
     * @param expectedSyncType the {@link HistorySyncType} the slug
     *                         refers to
     */
    private static void assertChunkDecodesToOracle(String typeSlug, HistorySyncType expectedSyncType) {
        Assumptions.assumeTrue(SyncFixtures.isHistoryChunkAvailable(typeSlug),
                "history fixture not captured: " + typeSlug);
        var bytes = SyncFixtures.loadHistoryChunkBytes(typeSlug);
        var oracle = SyncFixtures.loadHistoryExpected(typeSlug);
        var decoded = HistorySync.ofFull(ProtobufInputStream.fromBytes(bytes));
        assertNotNull(decoded, "decoder returned null for " + typeSlug);
        assertEquals(expectedSyncType.index(), oracle.getIntValue("syncType"),
                "oracle syncType mismatch for " + typeSlug);
        assertEquals(expectedSyncType, decoded.syncType(),
                "decoded syncType mismatch for " + typeSlug);
        if (oracle.containsKey("chunkOrder")) {
            assertEquals(oracle.getIntValue("chunkOrder"),
                    decoded.chunkOrder().orElseThrow(() -> new AssertionError("chunkOrder missing in decoded")),
                    "chunkOrder mismatch for " + typeSlug);
        }
        if (oracle.containsKey("progress")) {
            assertEquals(oracle.getIntValue("progress"),
                    decoded.progress().orElseThrow(() -> new AssertionError("progress missing in decoded")),
                    "progress mismatch for " + typeSlug);
        }
        assertListSize(oracle, "pushnames", decoded.pushnames().size(), typeSlug);
        assertListSize(oracle, "phoneNumberToLidMappings",
                decoded.phoneNumberToLidMappings().size(), typeSlug);
        assertListSize(oracle, "pastParticipants",
                decoded.pastParticipants().size(), typeSlug);
        assertListSize(oracle, "recentStickers",
                decoded.recentStickers().size(), typeSlug);
        assertListSize(oracle, "accounts", decoded.accounts().size(), typeSlug);
        assertListSize(oracle, "conversations", decoded.chats().size(), typeSlug);
        assertListSize(oracle, "statusV3Messages",
                decoded.statusV3Messages().size(), typeSlug);
    }

    /**
     * Asserts that the oracle's named list and the decoded list have the same size.
     *
     * <p>A missing list on the oracle side is treated as empty, matching the protobuf default for
     * repeated fields.
     *
     * @param oracle      the oracle JSON document
     * @param fieldName   the protobuf field name to look up on the
     *                    oracle
     * @param decodedSize the size reported by the decoded
     *                    {@link HistorySync}
     * @param typeSlug    the slug used in the assertion message
     */
    private static void assertListSize(JSONObject oracle, String fieldName, int decodedSize, String typeSlug) {
        var array = oracle.getJSONArray(fieldName);
        var expected = array == null ? 0 : array.size();
        assertEquals(expected, decodedSize,
                fieldName + " count mismatch for " + typeSlug);
    }

    /**
     * Asserts that the captured notification's {@code syncType} field matches the slug, guarding
     * that the fixture slug matches the runtime payload that produced it.
     *
     * @param typeSlug         the per-syncType fixture slug
     * @param expectedSyncType the {@link HistorySyncType} the slug
     *                         refers to
     */
    private static void assertNotificationSyncType(String typeSlug, HistorySyncType expectedSyncType) {
        Assumptions.assumeTrue(SyncFixtures.isHistoryChunkAvailable(typeSlug),
                "history fixture not captured: " + typeSlug);
        var notification = SyncFixtures.loadHistoryNotification(typeSlug);
        assertEquals(expectedSyncType.index(), notification.getIntValue("syncType"),
                "notification.syncType mismatch for " + typeSlug);
        var inner = notification.getJSONObject("notification");
        assertNotNull(inner, "notification.notification missing for " + typeSlug);
        assertEquals(expectedSyncType.index(), inner.getIntValue("syncType"),
                "notification.notification.syncType mismatch for " + typeSlug);
    }

    /**
     * Asserts that the captured notification carries either an inline payload or a complete CDN
     * handle pair.
     *
     * <p>Mirrors the WA Web invariant that any non-{@code MESSAGE_ACCESS_STATUS} chunk must be
     * downloadable either inline or via the CDN, since the WA Web notification handler gates its work
     * on the presence of {@code downloadOptions}.
     *
     * @param typeSlug the per-syncType fixture slug
     */
    private static void assertNotificationDeliverable(String typeSlug) {
        Assumptions.assumeTrue(SyncFixtures.isHistoryChunkAvailable(typeSlug),
                "history fixture not captured: " + typeSlug);
        var notification = SyncFixtures.loadHistoryNotification(typeSlug)
                .getJSONObject("notification");
        assertNotNull(notification, "notification.notification missing for " + typeSlug);
        var hasInline = notification.containsKey("initialHistBootstrapInlinePayload");
        var downloadOptions = notification.getJSONObject("downloadOptions");
        var hasCdn = downloadOptions != null
                && downloadOptions.containsKey("directPath")
                && downloadOptions.containsKey("mediaKey");
        assertTrue(hasInline || hasCdn,
                "notification must carry either inline payload or CDN download options for " + typeSlug);
    }

    @Nested
    @DisplayName("INITIAL_BOOTSTRAP")
    class InitialBootstrap {
        private static final String SLUG = "initial-bootstrap";

        @Test
        @DisplayName("decoder matches WA Web oracle")
        void decoderMatchesOracle() {
            assertChunkDecodesToOracle(SLUG, HistorySyncType.INITIAL_BOOTSTRAP);
        }

        @Test
        @DisplayName("notification syncType is INITIAL_BOOTSTRAP")
        void notificationSyncType() {
            assertNotificationSyncType(SLUG, HistorySyncType.INITIAL_BOOTSTRAP);
        }

        @Test
        @DisplayName("notification is deliverable (inline payload or CDN handle)")
        void notificationDeliverable() {
            assertNotificationDeliverable(SLUG);
        }
    }

    @Nested
    @DisplayName("INITIAL_STATUS_V3")
    class InitialStatusV3 {
        private static final String SLUG = "initial-status-v3";

        @Test
        @DisplayName("decoder matches WA Web oracle")
        void decoderMatchesOracle() {
            assertChunkDecodesToOracle(SLUG, HistorySyncType.INITIAL_STATUS_V3);
        }

        @Test
        @DisplayName("notification syncType is INITIAL_STATUS_V3")
        void notificationSyncType() {
            assertNotificationSyncType(SLUG, HistorySyncType.INITIAL_STATUS_V3);
        }

        @Test
        @DisplayName("notification is deliverable (inline payload or CDN handle)")
        void notificationDeliverable() {
            assertNotificationDeliverable(SLUG);
        }
    }

    @Nested
    @DisplayName("FULL")
    class Full {
        private static final String SLUG = "full";

        @Test
        @DisplayName("decoder matches WA Web oracle")
        void decoderMatchesOracle() {
            assertChunkDecodesToOracle(SLUG, HistorySyncType.FULL);
        }

        @Test
        @DisplayName("notification syncType is FULL")
        void notificationSyncType() {
            assertNotificationSyncType(SLUG, HistorySyncType.FULL);
        }

        @Test
        @DisplayName("notification is deliverable (inline payload or CDN handle)")
        void notificationDeliverable() {
            assertNotificationDeliverable(SLUG);
        }
    }

    @Nested
    @DisplayName("RECENT")
    class Recent {
        private static final String SLUG = "recent";

        @Test
        @DisplayName("decoder matches WA Web oracle")
        void decoderMatchesOracle() {
            assertChunkDecodesToOracle(SLUG, HistorySyncType.RECENT);
        }

        @Test
        @DisplayName("notification syncType is RECENT")
        void notificationSyncType() {
            assertNotificationSyncType(SLUG, HistorySyncType.RECENT);
        }

        @Test
        @DisplayName("notification is deliverable (inline payload or CDN handle)")
        void notificationDeliverable() {
            assertNotificationDeliverable(SLUG);
        }
    }

    @Nested
    @DisplayName("PUSH_NAME")
    class PushName {
        private static final String SLUG = "push-name";

        @Test
        @DisplayName("decoder matches WA Web oracle")
        void decoderMatchesOracle() {
            assertChunkDecodesToOracle(SLUG, HistorySyncType.PUSH_NAME);
        }

        @Test
        @DisplayName("notification syncType is PUSH_NAME")
        void notificationSyncType() {
            assertNotificationSyncType(SLUG, HistorySyncType.PUSH_NAME);
        }

        @Test
        @DisplayName("notification is deliverable (inline payload or CDN handle)")
        void notificationDeliverable() {
            assertNotificationDeliverable(SLUG);
        }
    }

    @Nested
    @DisplayName("NON_BLOCKING_DATA")
    class NonBlockingData {
        private static final String SLUG = "non-blocking-data";

        @Test
        @DisplayName("decoder matches WA Web oracle")
        void decoderMatchesOracle() {
            assertChunkDecodesToOracle(SLUG, HistorySyncType.NON_BLOCKING_DATA);
        }

        @Test
        @DisplayName("notification syncType is NON_BLOCKING_DATA")
        void notificationSyncType() {
            assertNotificationSyncType(SLUG, HistorySyncType.NON_BLOCKING_DATA);
        }

        @Test
        @DisplayName("notification is deliverable (inline payload or CDN handle)")
        void notificationDeliverable() {
            assertNotificationDeliverable(SLUG);
        }
    }

    @Nested
    @DisplayName("ON_DEMAND")
    class OnDemand {
        private static final String SLUG = "on-demand";

        @Test
        @DisplayName("decoder matches WA Web oracle")
        void decoderMatchesOracle() {
            assertChunkDecodesToOracle(SLUG, HistorySyncType.ON_DEMAND);
        }

        @Test
        @DisplayName("notification syncType is ON_DEMAND")
        void notificationSyncType() {
            assertNotificationSyncType(SLUG, HistorySyncType.ON_DEMAND);
        }

        @Test
        @DisplayName("notification is deliverable (inline payload or CDN handle)")
        void notificationDeliverable() {
            assertNotificationDeliverable(SLUG);
        }
    }
}
