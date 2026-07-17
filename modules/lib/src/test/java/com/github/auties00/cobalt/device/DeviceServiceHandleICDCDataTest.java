package com.github.auties00.cobalt.device;
import com.github.auties00.cobalt.sync.LiveSnapshotRecoveryService;
import com.github.auties00.cobalt.sync.LiveWebAppStateService;
import com.github.auties00.cobalt.migration.LiveLidMigrationService;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.migration.LidMigrationService;
import com.github.auties00.cobalt.wire.linked.device.DeviceListMetadataBuilder;
import com.github.auties00.cobalt.wire.linked.device.info.DeviceInfo;
import com.github.auties00.cobalt.wire.linked.device.info.DeviceListBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.media.TestMediaConnectionService;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.sync.SnapshotRecoveryService;
import com.github.auties00.cobalt.sync.WebAppStateService;
import com.github.auties00.cobalt.wam.LiveWamService;
import com.github.auties00.libsignal.SignalSessionCipher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link DeviceService#handleICDCData}, exercising the three branches the handler
 * distinguishes: a newer peer-sender snapshot updates the cached device list's
 * {@code expectedTimestamp}, a missing cached device list is a no-op, and a primary-device sender
 * with timestamp but no key-hash takes the minimal-sync path that bumps the cached timestamp by one
 * second.
 *
 * <p>Each test constructs the {@link com.github.auties00.cobalt.wire.linked.device.DeviceListMetadata}
 * directly rather than capturing an inbound message stanza, since capturing such a message is
 * operator-dependent and the metadata payload is identical to what the Cobalt message receiver
 * pipeline produces after decryption.
 */
@DisplayName("DeviceService.handleICDCData")
class DeviceServiceHandleICDCDataTest {
    private static final Jid SELF_PN = Jid.of("393495089819@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("258252122116273@lid");
    private static final Jid PEER = Jid.of("12025550100@s.whatsapp.net");
    private static final Jid PEER_DEVICE_1 = Jid.of("12025550100:1@s.whatsapp.net");

    private record Harness(TestWhatsAppClient client, DeviceService deviceService) {
    }

    private static Harness build() {
        var props = TestABPropsService.builder().build();
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        var client = TestWhatsAppClient.create().withStore(store);
        var wamService = new LiveWamService(client, props);
        var lidMigration = new LiveLidMigrationService(client, props, wamService);
        var snapshotRecovery = new LiveSnapshotRecoveryService(client, props, wamService);
        var webAppState = new LiveWebAppStateService(client, props, lidMigration, snapshotRecovery, wamService, TestMediaConnectionService.create());
        var sessionCipher = new SignalSessionCipher(store.signalStore());
        var deviceService = new LiveDeviceService(client, webAppState, props, sessionCipher, wamService);
        return new Harness(client, deviceService);
    }

    @Test
    @DisplayName("null metadata is a no-op")
    void nullMetadata() {
        var h = build();
        h.deviceService.handleICDCData(PEER, null, null);
    }

    // Sends from a peer companion (device id != 0) so the handler skips the minimal-sync path; with no cached list, the update loop has nothing to write
    @Test
    @DisplayName("metadata with no cached device list for the sender is a no-op")
    void senderWithNoCachedList() {
        var h = build();
        var metadata = new DeviceListMetadataBuilder()
                .senderTimestamp(Instant.now())
                .senderKeyHash(new byte[]{1, 2, 3})
                .build();

        h.deviceService.handleICDCData(PEER_DEVICE_1, null, metadata);

        assertTrue(h.client.store().contactStore().findDeviceList(PEER).isEmpty(),
                "no cached list before, none after");
    }

    @Test
    @DisplayName("metadata with newer sender timestamp updates the cached expectedTimestamp")
    void newerSenderTimestampUpdatesExpected() {
        var h = build();
        var cachedTimestamp = Instant.parse("2026-01-01T00:00:00Z");
        var newerTimestamp = Instant.parse("2026-01-02T00:00:00Z");

        var cached = new DeviceListBuilder()
                .userJid(PEER)
                .devices(List.of(DeviceInfo.ofE2EE(0, 0), DeviceInfo.ofE2EE(1, 1)))
                .timestamp(cachedTimestamp)
                .currentIndex(0)
                .validIndexes(new LinkedHashSet<>())
                .build();
        h.client.store().contactStore().addDeviceList(cached);

        var metadata = new DeviceListMetadataBuilder()
                .senderTimestamp(newerTimestamp)
                .senderKeyHash(new byte[]{1, 2, 3})
                .build();

        h.deviceService.handleICDCData(PEER_DEVICE_1, null, metadata);

        var updated = h.client.store().contactStore().findDeviceList(PEER).orElseThrow();
        assertEquals(newerTimestamp, updated.expectedTimestamp(),
                "newer sender timestamp lands as the new expectedTimestamp on the cached list");
        assertEquals(cachedTimestamp, updated.timestamp(),
                "cached snapshot timestamp stays put; only expectedTimestamp moves");
    }

    @Test
    @DisplayName("primary-device sender with no key-hash takes the minimal-sync path (+1s bump)")
    void primaryDeviceMinimalSyncBumpsTimestamp() {
        var h = build();
        var cachedTimestamp = Instant.parse("2026-01-01T00:00:00Z");
        var senderTimestamp = Instant.parse("2026-01-02T00:00:00Z");

        var cached = new DeviceListBuilder()
                .userJid(PEER)
                .devices(List.of(DeviceInfo.ofE2EE(0, 0), DeviceInfo.ofE2EE(1, 1)))
                .timestamp(cachedTimestamp)
                .currentIndex(0)
                .validIndexes(new LinkedHashSet<>())
                .build();
        h.client.store().contactStore().addDeviceList(cached);

        var metadata = new DeviceListMetadataBuilder()
                .senderTimestamp(senderTimestamp)
                .build();

        h.deviceService.handleICDCData(PEER, null, metadata);

        var updated = h.client.store().contactStore().findDeviceList(PEER).orElseThrow();
        assertEquals(senderTimestamp.plusSeconds(1), updated.timestamp(),
                "minimal-sync path bumps the timestamp to senderTimestamp+1s");
        assertEquals(1, updated.devices().size(),
                "minimal-sync resets the cached list to primary-only");
        assertEquals(0, updated.devices().getFirst().id(),
                "the remaining device is the primary (id=0)");
    }
}
