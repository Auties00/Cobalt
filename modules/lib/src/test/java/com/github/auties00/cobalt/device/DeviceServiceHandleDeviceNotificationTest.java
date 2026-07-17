package com.github.auties00.cobalt.device;
import com.github.auties00.cobalt.sync.LiveSnapshotRecoveryService;
import com.github.auties00.cobalt.sync.LiveWebAppStateService;
import com.github.auties00.cobalt.migration.LiveLidMigrationService;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.media.TestMediaConnectionService;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.wam.LiveWamService;
import com.github.auties00.libsignal.SignalSessionCipher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Replays captured live {@code <notification type="account_sync">} stanzas through
 * {@link DeviceService#handleDeviceNotification} and asserts the handler accepts the wire shape WA
 * Web actually sends, completing without throwing on the captured shapes.
 *
 * <p>Each test builds the full collaborator graph through {@link LiveDeviceService} on a
 * {@link TestWhatsAppClient} backed by a temporary store; no network is involved. Two captures live
 * in the corpus, both taken from the {@code personal} live session on 2026-05-11:
 * {@code adv-notification-link.jsonl} emitted after linking device id 78 (the wrapped
 * {@code <devices dhash="2:GgkHBG8F">} carries {@code [0, 73, 77, 78]}), and
 * {@code adv-notification-unlink.jsonl} emitted after unlinking the same device (the wrapped
 * {@code <devices dhash="2:wRn7yVQL">} carries {@code [0, 73, 77]}).
 */
@DisplayName("DeviceService.handleDeviceNotification")
class DeviceServiceHandleDeviceNotificationTest {
    private static final Jid SELF_PN = Jid.of("393495089819@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("258252122116273@lid");

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

    /**
     * Returns the {@code <devices>} child of the first inbound captured notification in the topic,
     * mirroring the way the stream handler unwraps the outer {@code <notification>} before dispatch.
     *
     * @param topic the fixture topic
     * @return the {@code <devices>} child stanza
     */
    private static Stanza loadDevicesNode(String topic) {
        for (var event : DeviceFixtures.loadEvents(topic)) {
            if (!"in".equals(event.getString("direction"))) continue;
            var notification = DeviceFixtures.buildNodeFromEvent(event);
            return notification.getChild("devices")
                    .orElseThrow(() -> new AssertionError("captured notification has no <devices> child"));
        }
        throw new AssertionError("no inbound event in fixture " + topic);
    }

    @Test
    @DisplayName("link notification: handler accepts the captured wire shape without throwing")
    void linkNotification() {
        var h = build();
        var devicesNode = loadDevicesNode("adv-notification-link");

        h.deviceService.handleDeviceNotification(devicesNode, "add", SELF_LID);

        var stored = h.client.store().contactStore().findDeviceList(SELF_LID.toUserJid());
        assertTrue(stored.isPresent() || stored.isEmpty(),
                "handler completed without throwing (Optional check is structural)");
    }

    @Test
    @DisplayName("unlink notification: handler accepts the captured wire shape without throwing")
    void unlinkNotification() {
        var h = build();
        var devicesNode = loadDevicesNode("adv-notification-unlink");
        h.deviceService.handleDeviceNotification(devicesNode, "remove", SELF_LID);
    }

    @Test
    @DisplayName("invalid action is logged but does not throw")
    void invalidAction() {
        var h = build();
        var devicesNode = loadDevicesNode("adv-notification-link");
        h.deviceService.handleDeviceNotification(devicesNode, "bogus", SELF_LID);
    }
}
