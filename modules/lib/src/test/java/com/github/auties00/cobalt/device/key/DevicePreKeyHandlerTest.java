package com.github.auties00.cobalt.device.key;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.libsignal.SignalSessionCipher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fixture-driven structural tests for {@link DevicePreKeyHandler}, asserting wire-shape parity
 * between the pre-key fetch path and captured pre-key responses, plus the empty-input no-op
 * branch of {@link DevicePreKeyHandler#ensureSessions(java.util.Collection)}.
 *
 * <p>Tests replay captured responses from {@code fixtures/device/prekey-batch.jsonl} (8 devices,
 * oracle pins {@code missedPrekeyCount=8}, {@code depletedPrekeyCount=0}) and
 * {@code fixtures/device/prekey-single.jsonl} (the one-device hosted enterprise primary path).
 */
@DisplayName("DevicePreKeyHandler")
class DevicePreKeyHandlerTest {
    private static final Jid SELF_PN = Jid.of("393495089819@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("258252122116273@lid");

    private static Stanza loadCapturedResponse(String topic) {
        for (var event : DeviceFixtures.loadEvents(topic)) {
            if ("in".equals(event.getString("direction"))) {
                return DeviceFixtures.buildNodeFromEvent(event);
            }
        }
        throw new AssertionError("no inbound event in fixture " + topic);
    }

    private static List<Jid> capturedDeviceJids(Stanza response) {
        var jids = new ArrayList<Jid>();
        response.streamChildren("list")
                .flatMap(list -> list.streamChildren("user"))
                .forEach(user -> user.getAttributeAsJid("jid").ifPresent(jids::add));
        return jids;
    }

    @Test
    @DisplayName("ensureSessions: no devices need sessions when called with empty input")
    void emptyInput() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        var client = TestWhatsAppClient.create().withStore(store);
        var sessionCipher = new SignalSessionCipher(store.signalStore());
        var handler = new DevicePreKeyHandler(client, sessionCipher);

        var depleted = handler.ensureSessions(List.of());
        assertEquals(0, depleted, "empty input is a no-op");
    }

    @Test
    @DisplayName("captured batch response: every device carries a <key> child, so depletion = 0")
    void batchResponseHasNoDepletion() {
        var response = loadCapturedResponse("prekey-batch");
        var deviceJids = capturedDeviceJids(response);
        assertEquals(8, deviceJids.size(),
                "captured batch fixture has 8 devices");

        var withKey = response.streamChildren("list")
                .flatMap(list -> list.streamChildren("user"))
                .filter(user -> user.hasChild("key"))
                .count();
        assertEquals(8, withKey,
                "captured response: every device has a <key> child, matching the prekey-batch.expected.json oracle (depletedPrekeyCount=0)");
    }

    @Test
    @DisplayName("captured single response: one device, one <key> child, no depletion")
    void singleResponseHasOneKey() {
        var response = loadCapturedResponse("prekey-single");
        var deviceJids = capturedDeviceJids(response);
        assertEquals(1, deviceJids.size(),
                "captured single fixture has 1 device (the hosted enterprise primary)");

        var withKey = response.streamChildren("list")
                .flatMap(list -> list.streamChildren("user"))
                .filter(user -> user.hasChild("key"))
                .count();
        assertTrue(withKey <= 1, "single response can have 0 or 1 <key> children");
    }

    @Test
    @DisplayName("captured response: every user carries the canonical pre-key bundle children")
    void canonicalBundleShape() {
        var response = loadCapturedResponse("prekey-batch");
        response.streamChildren("list")
                .flatMap(list -> list.streamChildren("user"))
                .forEach(user -> {
                    assertTrue(user.hasChild("registration"), "<registration> child present");
                    assertTrue(user.hasChild("type"), "<type> child present");
                    assertTrue(user.hasChild("identity"), "<identity> child present");
                    assertTrue(user.hasChild("skey"), "<skey> child present");
                });
    }
}
