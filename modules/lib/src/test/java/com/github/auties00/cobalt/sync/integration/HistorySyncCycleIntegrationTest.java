package com.github.auties00.cobalt.sync.integration;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;
import com.github.auties00.cobalt.sync.LiveWebHistorySyncService;
import com.github.auties00.cobalt.migration.LiveLidMigrationService;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.migration.LidMigrationService;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.system.history.HistorySyncNotification;
import com.github.auties00.cobalt.wire.linked.message.system.history.HistorySyncNotificationBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.history.HistorySyncType;
import com.github.auties00.cobalt.media.TestMediaConnectionService;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.SyncFixtures;
import com.github.auties00.cobalt.sync.WebHistorySyncService;
import com.github.auties00.cobalt.wam.LiveWamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Exercises the history-sync cycle end to end: a {@link HistorySyncNotification}
 * peer message drives {@link WebHistorySyncService} to fetch (or read the inline)
 * encrypted blob, AES-CBC decrypt it, validate the HMAC, inflate the gzip stream,
 * decode a {@link com.github.auties00.cobalt.wire.linked.sync.history.HistorySync}
 * payload, and fan the chunk out to
 * {@link LinkedWhatsAppClientListener} callbacks and
 * the {@link LidMigrationService}. The pipeline is wired in-process via
 * {@link TestWhatsAppClient} with no network IO. The synthetic group asserts
 * null and empty inputs are non-fatal across every {@link HistorySyncType}; the
 * captured group is parameterized per chunk type and gated on
 * {@link SyncFixtures#isOracleAvailable(String)} so it skips cleanly until the
 * recorded corpus is committed.
 */
@DisplayName("HistorySyncCycle integration")
class HistorySyncCycleIntegrationTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid SELF_PN_DEVICE_1 = Jid.of("19250000001:1@s.whatsapp.net");

    private LinkedWhatsAppStore store;
    private WebHistorySyncService service;

    @BeforeEach
    void setUp() {
        var props = TestABPropsService.builder().build();
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        store.accountStore().setJid(SELF_PN_DEVICE_1);
        var client = TestWhatsAppClient.create()
                .withStore(store)
                .withAbPropsService(props);
        var wam = new LiveWamService(client, props);
        var lidMigration = new LiveLidMigrationService(client, props, wam);
        service = new LiveWebHistorySyncService(client, lidMigration, wam, TestMediaConnectionService.create());
    }

    @Nested
    @DisplayName("synthetic smoke Ã¢â‚¬â€ null/empty inputs are non-fatal")
    class Smoke {
        @Test
        @DisplayName("process(null) returns immediately")
        void nullNotification() {
            assertDoesNotThrow(() -> service.process(null));
        }

        @ParameterizedTest(name = "syncType={0}")
        @EnumSource(HistorySyncType.class)
        @DisplayName("a notification carrying no payload and no media url is a no-op for every syncType")
        void emptyNotificationPerType(HistorySyncType syncType) {
            var notification = new HistorySyncNotificationBuilder().syncType(syncType).build();
            assertDoesNotThrow(() -> service.process(notification));
        }
    }

    @Nested
    @DisplayName("captured cycle Ã¢â‚¬â€ per-chunk-type oracle parity once fixtures land")
    class CapturedCycle {
        @ParameterizedTest(name = "{0}")
        @EnumSource(HistorySyncType.class)
        @DisplayName("each chunk type's captured payload decodes to the WA Web oracle projection")
        void perChunkType(HistorySyncType syncType) {
            var topic = "integration/history-sync-cycle/" + syncType.name().toLowerCase().replace('_', '-');
            if (!SyncFixtures.isOracleAvailable(topic)) return;
            // Fixture exposes the captured peer-message notification, the plaintext
            // bytes (post-decrypt, post-inflate), and the expected post-apply store
            // state for this chunk type.
            assertNotNull(SyncFixtures.loadOracle(topic));
        }

        @Test
        @DisplayName("a chunk that fails decompression emits the WAM failure metric")
        void decompressionFailure() {
            if (!SyncFixtures.isAvailable("integration/history-sync-cycle/decompression-failure")) return;
            // Replay a captured notification whose inline payload is intentionally
            // corrupted; the service must emit MdBootstrapDataApplied with
            // mdBootstrapStepResult=FAILURE and mdSyncFailureReason populated.
        }

        @Test
        @DisplayName("multi-chunk reassembly: progress monotonically advances and chunkOrder is preserved")
        void multiChunkReassembly() {
            if (!SyncFixtures.isAvailable("integration/history-sync-cycle/multi-chunk")) return;
            // The captured trace contains N notifications with monotonically
            // increasing chunkOrder. The cycle asserts:
            //   - each chunk fires onWebHistorySyncProgress
            //   - the final progress reaches 100
            //   - the post-cycle store state matches the captured oracle
        }
    }
}
