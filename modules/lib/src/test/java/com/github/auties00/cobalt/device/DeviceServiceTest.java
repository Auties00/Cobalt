package com.github.auties00.cobalt.device;
import com.github.auties00.cobalt.sync.LiveSnapshotRecoveryService;
import com.github.auties00.cobalt.sync.LiveWebAppStateService;
import com.github.auties00.cobalt.migration.LiveLidMigrationService;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.fanout.DevicePhashCalculator;
import com.github.auties00.cobalt.device.fanout.DevicePhashVersion;
import com.github.auties00.cobalt.migration.LidMigrationService;
import com.github.auties00.cobalt.wire.linked.device.info.DeviceInfo;
import com.github.auties00.cobalt.wire.linked.device.info.DeviceList;
import com.github.auties00.cobalt.wire.linked.device.info.DeviceListBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.media.TestMediaConnectionService;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.sync.SnapshotRecoveryService;
import com.github.auties00.cobalt.sync.WebAppStateService;
import com.github.auties00.cobalt.wam.LiveWamService;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.libsignal.SignalSessionCipher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests for {@link DeviceService} exercising the read-from-cache paths that do not require a
 * real USync round-trip: peer fanout for a cached peer, ICDC computation, self-fanout (which must
 * strip the sender's own device), the group-fanout phash byte-equality KAT against the captured WA
 * Web oracle, and the alt-device merging mode of {@link DeviceService#getDeviceLists}.
 *
 * <p>Each test builds the full collaborator graph ({@link WamService}, {@link SnapshotRecoveryService},
 * {@link WebAppStateService}, {@link DeviceService}) on top of a {@link TestWhatsAppClient}. The
 * USync-round-trip cases (the {@code sendNode} path) are deliberately left to
 * {@link com.github.auties00.cobalt.device.stanza.DeviceUSyncResponseParserTest}, which covers them
 * against captured fixtures.
 */
@DisplayName("DeviceService")
class DeviceServiceTest {
    private static final Jid SELF_PN = Jid.of("19254863482@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594056@lid");
    private static final Jid SELF_PN_DEVICE_1 = Jid.of("19254863482:1@s.whatsapp.net");
    private static final Jid SELF_LID_DEVICE_1 = Jid.of("83116928594056:1@lid");
    private static final Jid PEER = Jid.of("393495089819@s.whatsapp.net");

    private record Harness(
            TestWhatsAppClient client,
            TestABPropsService props,
            DeviceService deviceService) {
    }

    /**
     * Builds a fresh harness with all collaborators wired against a temporary store, with the local
     * own JID set to {@code <pn>:1}.
     *
     * @return the constructed harness
     */
    private static Harness build() {
        var props = TestABPropsService.builder().build();
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        store.accountStore().setJid(SELF_PN_DEVICE_1);
        store.accountStore().setLid(SELF_LID_DEVICE_1);

        var client = TestWhatsAppClient.create().withStore(store);

        var wamService = new LiveWamService(client, props);
        var lidMigration = new LiveLidMigrationService(client, props, wamService);
        var snapshotRecovery = new LiveSnapshotRecoveryService(client, props, wamService);
        var webAppState = new LiveWebAppStateService(client, props, lidMigration, snapshotRecovery, wamService, TestMediaConnectionService.create());
        var sessionCipher = new SignalSessionCipher(store.signalStore());
        var deviceService = new LiveDeviceService(client, webAppState, props, sessionCipher, wamService);

        return new Harness(client, props, deviceService);
    }

    @Test
    @DisplayName("getUserFanout returns the cached companions for a known peer")
    void cachedPeerFanout() {
        var h = build();
        var peerCompanion = Jid.of("393495089819:5@s.whatsapp.net");

        var peerList = new DeviceListBuilder()
                .userJid(PEER)
                .devices(List.of(DeviceInfo.ofE2EE(0, 0), DeviceInfo.ofE2EE(5, 1)))
                .timestamp(Instant.now())
                .currentIndex(0)
                .validIndexes(new LinkedHashSet<>())
                .build();
        h.client.store().contactStore().addDeviceList(peerList);

        var selfList = new DeviceListBuilder()
                .userJid(SELF_PN)
                .devices(List.of(DeviceInfo.ofE2EE(0, 0), DeviceInfo.ofE2EE(1, 1)))
                .timestamp(Instant.now())
                .currentIndex(0)
                .validIndexes(new LinkedHashSet<>())
                .build();
        h.client.store().contactStore().addDeviceList(selfList);

        var fanout = h.deviceService.getUserFanout(PEER, null);

        assertTrue(fanout.contains(PEER.toUserJid()),
                "fanout should include the peer's primary device (id=0)");
        assertTrue(fanout.contains(peerCompanion),
                "fanout should include the peer's companion device id=5");
        assertFalse(fanout.contains(SELF_PN_DEVICE_1),
                "fanout must never include the sender's own device");
    }

    // Regression guard: the sender's own device must never appear in its own fanout, even when the chat is with self
    @Test
    @DisplayName("getUserFanout for self-send strips the sender's own device")
    void selfFanoutStripsSender() {
        var h = build();

        var selfList = new DeviceListBuilder()
                .userJid(SELF_PN)
                .devices(List.of(DeviceInfo.ofE2EE(0, 0), DeviceInfo.ofE2EE(1, 1)))
                .timestamp(Instant.now())
                .currentIndex(0)
                .validIndexes(new LinkedHashSet<>())
                .build();
        h.client.store().contactStore().addDeviceList(selfList);

        var fanout = h.deviceService.getUserFanout(SELF_PN, null);

        assertTrue(fanout.contains(SELF_PN.toUserJid()),
                "self-send fanout must include the other own device (id=0, the primary phone)");
        assertFalse(fanout.contains(SELF_PN_DEVICE_1),
                "self-send fanout must not include the sending device itself");
    }

    @Test
    @DisplayName("computeIcdc delegates to IcdcComputer and returns an Optional")
    void computeIcdcWires() {
        var h = build();
        var peerList = new DeviceListBuilder()
                .userJid(PEER)
                .devices(List.of(DeviceInfo.ofE2EE(0, 0)))
                .timestamp(Instant.now())
                .currentIndex(0)
                .validIndexes(new LinkedHashSet<>())
                .build();
        h.client.store().contactStore().addDeviceList(peerList);

        var icdc = h.deviceService.computeIcdc(PEER);
        assertTrue(icdc.isPresent(), "computeIcdc should produce a result for a cached peer");
        assertEquals(peerList.timestamp(), icdc.get().timestamp().orElse(null),
                "timestamp should propagate from the cached device list");
    }

    // Parity KAT against the captured group-phashes oracle; fails if WA Web changes its phashV2 derivation
    @Test
    @DisplayName("computeGroupPhash matches WA Web's phashV2 oracle (byte-equality, three group sizes)")
    void groupPhashMatchesLiveOracle() throws Exception {
        var raw = DeviceFixtures.loadExpected("group-phashes").getJSONObject("result").getString("value");
        var samples = JSON.parseArray(raw);

        var h = build();
        for (var i = 0; i < samples.size(); i++) {
            var sample = samples.getJSONObject(i);
            var groupId = sample.getString("groupId");
            var expectedPhash = sample.getString("phashV2");
            var devices = sample.getJSONArray("devices");

            var participantJids = new ArrayList<Jid>(devices.size());
            for (var j = 0; j < devices.size(); j++) {
                participantJids.add(Jid.of(devices.getString(j)));
            }
            for (var pjid : participantJids) {
                h.client.store().contactStore().addDeviceList(new DeviceListBuilder()
                        .userJid(pjid.toUserJid())
                        .devices(List.of(DeviceInfo.ofE2EE(0, 0)))
                        .timestamp(Instant.now())
                        .currentIndex(0)
                        .validIndexes(new LinkedHashSet<>())
                        .build());
            }

            var calculator = new DevicePhashCalculator(h.props);
            var deviceSet = new LinkedHashSet<>(participantJids);
            var actualPhash = calculator.calculate(deviceSet,
                    DevicePhashVersion.V2, true);

            assertEquals(expectedPhash, actualPhash,
                    "group " + groupId + " (n=" + devices.size() + "): Cobalt phashV2 must match WA Web's oracle byte-for-byte");
        }
    }

    // Guards the status-send regression: getStatusFanout must resolve the audience's devices from
    // cache without any group-metadata query (it takes audience JIDs, never status@broadcast).
    @Test
    @DisplayName("getStatusFanout resolves audience devices from cache and excludes the sending device")
    void statusFanoutResolvesAudienceDevices() {
        var h = build();
        var audienceCompanion = Jid.of("393495089819:5@s.whatsapp.net");
        h.client.store().contactStore().addDeviceList(new DeviceListBuilder()
                .userJid(PEER)
                .devices(List.of(DeviceInfo.ofE2EE(0, 0), DeviceInfo.ofE2EE(5, 1)))
                .timestamp(Instant.now())
                .currentIndex(0)
                .validIndexes(new LinkedHashSet<>())
                .build());

        var fanout = h.deviceService.getStatusFanout(List.of(PEER));

        assertTrue(fanout.contains(PEER.toUserJid()),
                "status fanout must include the audience's primary device");
        assertTrue(fanout.contains(audienceCompanion),
                "status fanout must include the audience's companion device");
        assertFalse(fanout.contains(SELF_LID_DEVICE_1),
                "status fanout must exclude the sending device");
    }

    // Guards the broadcast-send fix: getBroadcastFanout must resolve recipient devices from cache
    // (no group-metadata query) and exclude the sending device, mirroring status.
    @Test
    @DisplayName("getBroadcastFanout resolves recipient devices from cache and excludes the sending device")
    void broadcastFanoutResolvesRecipientDevices() {
        var h = build();
        var recipientCompanion = Jid.of("393495089819:5@s.whatsapp.net");
        h.client.store().contactStore().addDeviceList(new DeviceListBuilder()
                .userJid(PEER)
                .devices(List.of(DeviceInfo.ofE2EE(0, 0), DeviceInfo.ofE2EE(5, 1)))
                .timestamp(Instant.now())
                .currentIndex(0)
                .validIndexes(new LinkedHashSet<>())
                .build());

        var fanout = h.deviceService.getBroadcastFanout(Jid.of("12345@broadcast"), List.of(PEER));

        assertTrue(fanout.contains(PEER.toUserJid()),
                "broadcast fanout must include the recipient's primary device");
        assertTrue(fanout.contains(recipientCompanion),
                "broadcast fanout must include the recipient's companion device");
        assertFalse(fanout.contains(SELF_LID_DEVICE_1),
                "broadcast fanout must exclude the sending device");
    }

    @Test
    @DisplayName("getDeviceLists with shouldMergeAltDevices=true augments device ids without collapsing entries")
    void shouldMergeAltDevicesAugmentsDeviceIds() {
        var h = build();
        var peerPn = Jid.of("393495089819@s.whatsapp.net");
        var peerLid = Jid.of("72104938291847@lid");

        h.client.store().contactStore().registerLidMapping(peerPn, peerLid);

        var pnList = new DeviceListBuilder()
                .userJid(peerPn)
                .devices(List.of(DeviceInfo.ofE2EE(0, 0), DeviceInfo.ofE2EE(1, 1)))
                .timestamp(Instant.now())
                .currentIndex(0)
                .validIndexes(new LinkedHashSet<>())
                .build();
        var lidList = new DeviceListBuilder()
                .userJid(peerLid)
                .devices(List.of(DeviceInfo.ofE2EE(0, 0), DeviceInfo.ofE2EE(2, 1)))
                .timestamp(Instant.now())
                .currentIndex(0)
                .validIndexes(new LinkedHashSet<>())
                .build();
        h.client.store().contactStore().addDeviceList(pnList);
        h.client.store().contactStore().addDeviceList(lidList);

        var unmerged = h.deviceService.getDeviceLists(List.of(peerPn, peerLid), "message", null, false);
        assertEquals(2, unmerged.size(),
                "without shouldMergeAltDevices, PN and LID lists are returned independently");

        var merged = h.deviceService.getDeviceLists(List.of(peerPn, peerLid), "message", null, true);
        assertEquals(2, merged.size(),
                "with shouldMergeAltDevices, PN and LID lists are preserved separately so each carries its own addressing mode");

        var mergedByJid = merged.stream()
                .collect(Collectors.toMap(DeviceList::userJid, Function.identity()));
        var pnDevices = mergedByJid.get(peerPn).devices().stream().map(DeviceInfo::id).sorted().toList();
        var lidDevices = mergedByJid.get(peerLid).devices().stream().map(DeviceInfo::id).sorted().toList();
        assertEquals(List.of(0, 1, 2), pnDevices,
                "PN-keyed list must carry the union of its own device ids and the LID-keyed list's");
        assertEquals(List.of(0, 1, 2), lidDevices,
                "LID-keyed list must carry the union of its own device ids and the PN-keyed list's");
    }
}
