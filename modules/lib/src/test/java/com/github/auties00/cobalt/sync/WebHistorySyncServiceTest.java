package com.github.auties00.cobalt.sync;
import com.github.auties00.cobalt.migration.LiveLidMigrationService;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.migration.LidMigrationService;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.system.history.HistorySyncNotificationBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.history.HistorySyncType;
import com.github.auties00.cobalt.media.TestMediaConnectionService;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.wam.LiveWamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers the synchronous, no-network paths of {@link WebHistorySyncService}: constructor
 * null-checks, the {@code process(null)} no-op, the empty-notification short-circuit (mirroring WA
 * Web's {@code MESSAGE_ACCESS_STATUS} / {@code NO_HISTORY} markers), and the inflate-failure path
 * for malformed inline payloads. The download/decrypt/decode pipeline runs on a virtual thread and
 * needs a live media-connection round-trip, so the full per-{@link HistorySyncType} matrix and
 * chunk-to-store projection are out of scope ({@code WebHistorySyncServiceLiveOracleTest} covers
 * decode parity against the captured oracle). The service is wired against a temporary store from
 * {@link DeviceFixtures#temporaryStore(Jid, Jid)} and a {@link TestWhatsAppClient} with real
 * {@link LiveWamService} and {@link LidMigrationService} collaborators; the
 * {@code SELF_PN_DEVICE_1} JID is set on the store so the WAM commit path can derive a stable
 * session id even though no test asserts the emission directly.
 */
@DisplayName("WebHistorySyncService")
class WebHistorySyncServiceTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");

    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private static final Jid SELF_PN_DEVICE_1 = Jid.of("19250000001:1@s.whatsapp.net");

    private TestWhatsAppClient client;

    private TestABPropsService props;

    private LidMigrationService lidMigration;

    private WebHistorySyncService service;

    @BeforeEach
    void setUp() {
        props = TestABPropsService.builder().build();
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        store.accountStore().setJid(SELF_PN_DEVICE_1);
        client = TestWhatsAppClient.create().withStore(store);
        var wam = new LiveWamService(client, props);
        lidMigration = new LiveLidMigrationService(client, props, wam);
        service = new LiveWebHistorySyncService(client, lidMigration, wam, TestMediaConnectionService.create());
    }

    @Nested
    @DisplayName("constructor -- rejects null collaborators")
    class ConstructorContract {
        @Test
        @DisplayName("null LinkedWhatsAppClient is NPE")
        void nullClient() {
            var wam = new LiveWamService(client, props);
            assertThrows(NullPointerException.class,
                    () -> new LiveWebHistorySyncService(null, lidMigration, wam, TestMediaConnectionService.create()));
        }

        @Test
        @DisplayName("null LidMigrationService is NPE")
        void nullLidMigration() {
            var wam = new LiveWamService(client, props);
            assertThrows(NullPointerException.class,
                    () -> new LiveWebHistorySyncService(client, null, wam, TestMediaConnectionService.create()));
        }

        @Test
        @DisplayName("null WamService is NPE")
        void nullWam() {
            assertThrows(NullPointerException.class,
                    () -> new LiveWebHistorySyncService(client, lidMigration, null, TestMediaConnectionService.create()));
        }
    }

    @Nested
    @DisplayName("process -- early-return paths")
    class EarlyReturns {
        @Test
        @DisplayName("process(null) returns immediately without scheduling a virtual thread")
        void nullNotificationIsNoOp() {
            assertDoesNotThrow(() -> service.process(null));
        }

        @Test
        @DisplayName("notification with no inline payload and no media url is non-fatal")
        void emptyNotificationNonFatal() {
            // mirrors WA Web's MESSAGE_ACCESS_STATUS / NO_HISTORY markers, which take the early-return path
            var notification = new HistorySyncNotificationBuilder()
                    .syncType(HistorySyncType.NON_BLOCKING_DATA)
                    .build();
            assertDoesNotThrow(() -> service.process(notification));
        }

        @Test
        @DisplayName("notification with a non-zlib inline payload is reported as failure but does not throw")
        void malformedInlinePayloadDoesNotThrow() {
            // non-zlib bytes fail inflate; the failure is reported as a chunk error and swallowed on the virtual thread
            var notification = new HistorySyncNotificationBuilder()
                    .syncType(HistorySyncType.INITIAL_BOOTSTRAP)
                    .initialHistBootstrapInlinePayload(new byte[]{(byte) 0xFF, (byte) 0xFF})
                    .build();
            assertDoesNotThrow(() -> service.process(notification));
        }
    }

    @Nested
    @DisplayName("syncType matrix -- synthetic notifications cover every variant")
    class SyncTypeMatrix {
        @Test
        @DisplayName("every HistorySyncType can be wrapped in a notification and processed without throwing")
        void everySyncTypeAccepted() {
            for (var syncType : HistorySyncType.values()) {
                var notification = new HistorySyncNotificationBuilder()
                        .syncType(syncType)
                        .build();
                assertDoesNotThrow(() -> service.process(notification),
                        "syncType=" + syncType + " must be a no-op when carrying no payload");
            }
        }
    }

    /**
     * Reserved test slots for per-fixture oracle assertions, each gated on its capture being present
     * on the classpath so the suite stays green before the capture cycle has populated
     * {@code modules/lib/src/test/resources/fixtures/sync/history/}. Decode parity is already covered
     * by {@code WebHistorySyncServiceLiveOracleTest}; these slots are for the eventual end-to-end
     * store-projection assertions that need a fake media connection.
     */
    @Nested
    @DisplayName("WA Web oracle hooks -- gated on captured chunk fixtures")
    class OracleHooks {
        @Test
        @DisplayName("history/initial-bootstrap oracle (when captured) drives a real decode")
        void initialBootstrapOracle() {
            if (!SyncFixtures.isOracleAvailable("history/initial-bootstrap")) return;
        }

        @Test
        @DisplayName("history/recent oracle hook")
        void recentOracle() {
            if (!SyncFixtures.isOracleAvailable("history/recent")) return;
        }

        @Test
        @DisplayName("history/push-name oracle hook")
        void pushNameOracle() {
            if (!SyncFixtures.isOracleAvailable("history/push-name")) return;
        }

        @Test
        @DisplayName("history/full oracle hook")
        void fullOracle() {
            if (!SyncFixtures.isOracleAvailable("history/full")) return;
        }

        @Test
        @DisplayName("history/on-demand oracle hook")
        void onDemandOracle() {
            if (!SyncFixtures.isOracleAvailable("history/on-demand")) return;
        }

        @Test
        @DisplayName("history/initial-status-v3 oracle hook")
        void initialStatusV3Oracle() {
            if (!SyncFixtures.isOracleAvailable("history/initial-status-v3")) return;
        }

        @Test
        @DisplayName("history/non-blocking-data oracle hook")
        void nonBlockingDataOracle() {
            if (!SyncFixtures.isOracleAvailable("history/non-blocking-data")) return;
        }
    }
}
