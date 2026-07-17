package com.github.auties00.cobalt.migration;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.wam.LiveWamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the two LID-rotation entry points
 * {@link LidMigrationService#changeLid(Jid, Jid, Jid)} and
 * {@link LidMigrationService#registerOriginalLid(Jid, Jid)}. {@code changeLid}
 * keeps the primary cache, the bidirectional store mapping, the contact LID,
 * and any phone-keyed chat in sync when a contact's LID rotates;
 * {@code registerOriginalLid} populates the fallback cache the migration
 * cascade consults when neither the primary mapping nor a local mapping is
 * known. Each test runs against an isolated store so rotations do not bleed
 * across cases.
 */
@DisplayName("LidMigrationService.changeLid / registerOriginalLid")
class LidMigrationServiceChangeLidTest {

    private static final Jid SELF_PN = Jid.of("19254863482@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594056@lid");
    private static final Jid PEER_PN = Jid.of("393495089819@s.whatsapp.net");
    private static final Jid PEER_LID_OLD = Jid.of("258252122116273@lid");
    private static final Jid PEER_LID_NEW = Jid.of("999999999999999@lid");

    private record Harness(TestWhatsAppClient client, LiveLidMigrationService service) {}

    private static Harness build() {
        var props = TestABPropsService.builder().build();
        var store = MigrationFixtures.temporaryStore(SELF_PN, SELF_LID);
        var client = TestWhatsAppClient.create().withStore(store);
        var wamService = new LiveWamService(client, props);
        var service = new LiveLidMigrationService(client, props, wamService);
        return new Harness(client, service);
    }

    @Test
    @DisplayName("changeLid is a no-op when phoneJid is null")
    void changeLidNullPhoneJid() {
        var h = build();
        h.service.changeLid(null, PEER_LID_NEW, PEER_LID_OLD);
        // Nothing throws; nothing populated.
        assertFalse(h.client.store().contactStore().findLidByPhone(PEER_PN).isPresent());
    }

    @Test
    @DisplayName("changeLid is a no-op when newLid is null")
    void changeLidNullNewLid() {
        var h = build();
        h.service.changeLid(PEER_PN, null, PEER_LID_OLD);
        assertFalse(h.client.store().contactStore().findLidByPhone(PEER_PN).isPresent());
    }

    @Test
    @DisplayName("changeLid mirrors the new LID onto store, contact, chat, and lookupLid cache")
    void changeLidPropagatesNewLid() {
        var h = build();
        var store = h.client.store();

        // Pre-existing state: contact + chat under the phone JID.
        store.contactStore().addNewContact(PEER_PN);
        store.chatStore().addNewChat(PEER_PN);

        h.service.changeLid(PEER_PN, PEER_LID_NEW, PEER_LID_OLD);

        assertEquals(PEER_LID_NEW, store.contactStore().findLidByPhone(PEER_PN).orElseThrow());

        var contact = store.contactStore().findContactByJid(PEER_PN).orElseThrow();
        assertEquals(PEER_LID_NEW, contact.lid().orElseThrow());

        var chat = store.chatStore().findChatByJid(PEER_PN).orElseThrow();
        assertEquals(PEER_LID_NEW, chat.lid().orElseThrow());
        assertEquals(PEER_PN, chat.phoneNumberJid().orElseThrow());

        // lookupLid reads the primary cache first; the cache must contain the new LID.
        assertEquals(PEER_LID_NEW, h.service.lookupLid(PEER_PN).orElseThrow());
    }

    @Test
    @DisplayName("changeLid still works when only the contact is pre-existing (no chat)")
    void changeLidContactOnly() {
        var h = build();
        h.client.store().contactStore().addNewContact(PEER_PN);

        h.service.changeLid(PEER_PN, PEER_LID_NEW, null);

        assertEquals(PEER_LID_NEW, h.client.store().contactStore().findLidByPhone(PEER_PN).orElseThrow());
        assertEquals(PEER_LID_NEW, h.client.store().contactStore().findContactByJid(PEER_PN).orElseThrow().lid().orElseThrow());
        assertFalse(h.client.store().chatStore().findChatByJid(PEER_PN).isPresent());
    }

    @Test
    @DisplayName("registerOriginalLid is a no-op when phoneJid is null")
    void registerOriginalLidNullPhone() {
        var h = build();
        h.service.registerOriginalLid(null, PEER_LID_OLD);
    }

    @Test
    @DisplayName("registerOriginalLid is a no-op when lid is null")
    void registerOriginalLidNullLid() {
        var h = build();
        h.service.registerOriginalLid(PEER_PN, null);
    }

    @Test
    @DisplayName("registerOriginalLid populates the originalLidCache for use by resolveThread")
    void registerOriginalLidPopulatesFallback() {
        var h = build();
        var store = h.client.store();

        // PN-only chat with no primary cache hit and no localLid, so the fallback cache is consulted.
        store.chatStore().addNewChat(PEER_PN);
        h.service.registerOriginalLid(PEER_PN, PEER_LID_OLD);

        var chat = store.chatStore().findChatByJid(PEER_PN).orElseThrow();
        var resolution = h.service.resolveThread(chat);

        assertInstanceOfMigrate(resolution, PEER_LID_OLD.toUserJid());
    }

    private static void assertInstanceOfMigrate(LidMigrationResolution resolution, Jid expectedTargetLid) {
        assertTrue(resolution instanceof LidMigrationResolution.Migrate,
                "expected Migrate, got " + resolution);
        var migrate = (LidMigrationResolution.Migrate) resolution;
        assertEquals(expectedTargetLid, migrate.targetLid(),
                "originalLidCache fallback returns the cached LID at user level");
    }
}
