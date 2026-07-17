package com.github.auties00.cobalt.message.send;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.StubDeviceService;
import com.github.auties00.cobalt.message.MessageFixtures;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.message.send.senderkey.SenderKeyDistribution;
import com.github.auties00.cobalt.message.send.stanza.MetaStanza;
import com.github.auties00.cobalt.message.send.stanza.ReportingStanza;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.linked.contact.ContactBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.linked.privacy.StatusPrivacyMode;
import com.github.auties00.cobalt.wire.linked.privacy.StatusPrivacySettingBuilder;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.wam.LiveWamService;
import com.github.auties00.cobalt.message.crypto.SignalCryptoLocks;
import com.github.auties00.libsignal.SignalSessionCipher;
import com.github.auties00.libsignal.groups.SignalGroupCipher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the {@link StatusMessageSender} SKMSG send shape. Status broadcasts
 * share the group-send SKMSG pipeline but address the {@code status@broadcast}
 * JID and carry a {@code <meta status_setting=...>} child mirroring the user's
 * status privacy preference. The cells exercise the steady-state SKMSG branch
 * and the no-privacy meta-shape branch through a captured
 * {@link TestWhatsAppClient} and a stubbed {@link StubDeviceService}, neither
 * of which needs a real status audience pipeline.
 */
@DisplayName("StatusMessageSender")
class StatusMessageSenderTest {

    private static final Jid SELF_PN = Jid.of("12025550100@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("258252122116273@lid");
    private static final Jid STATUS = Jid.statusBroadcastAccount();
    private static final Jid AUDIENCE_DEVICE = Jid.of("19254863482:0@s.whatsapp.net");

    @Test
    @DisplayName("steady-state text status: outer to=status@broadcast, <enc type=skmsg>, no PKMSG distribution")
    void steadyStateStatus() {
        var senderStore = MessageFixtures.temporaryStore(SELF_PN, SELF_LID);
        // Pre-mark the audience device as already having the sender key so the
        // steady-state branch is taken.
        senderStore.signalStore().markSenderKeyDistributed(STATUS, AUDIENCE_DEVICE);

        var captured = new AtomicReference<Stanza>();
        var client = clientWithCapture(senderStore, captured);
        var sender = statusMessageSender(client, senderStore,
                StubDeviceService.create().withStatusFanout(
                        audience -> Set.of(AUDIENCE_DEVICE)));

        sender.send(STATUS, statusMessage("3EB0STAT001", LinkedMessageContainer.of("status body")));

        var stanza = captured.get();
        assertEquals("message", stanza.description());
        assertEquals(STATUS.toString(), stanza.getAttributeAsString("to").orElseThrow(),
                "outer to must be the canonical status@broadcast JID");
        assertEquals("text", stanza.getAttributeAsString("type").orElseThrow());

        // The bare SKMSG <enc> sibling carries the encrypted body.
        var enc = stanza.getChild("enc").orElseThrow();
        assertEquals("skmsg", enc.getAttributeAsString("type").orElseThrow());
        assertEquals("2", enc.getAttributeAsString("v").orElseThrow());

        // Steady-state: no PKMSG distribution, though a <participants> echo
        // may still exist for the receipt list with empty <to jid="..."> entries.
        stanza.getChild("participants").ifPresent(participants -> {
            var pkmsgCount = participants.streamChildren("to")
                    .flatMap(to -> to.streamChild("enc"))
                    .filter(e -> "pkmsg".equals(e.getAttributeAsString("type").orElse(null)))
                    .count();
            assertEquals(0, pkmsgCount,
                    "steady-state status must NOT carry any PKMSG distribution");
        });
    }

    @Test
    @DisplayName("text status: <meta> child is emitted when present (status_setting omitted when no privacy entry seeded)")
    void metaShapeWhenNoPrivacy() {
        var senderStore = MessageFixtures.temporaryStore(SELF_PN, SELF_LID);
        senderStore.signalStore().markSenderKeyDistributed(STATUS, AUDIENCE_DEVICE);

        var captured = new AtomicReference<Stanza>();
        var client = clientWithCapture(senderStore, captured);
        var sender = statusMessageSender(client, senderStore,
                StubDeviceService.create().withStatusFanout(
                        audience -> Set.of(AUDIENCE_DEVICE)));

        sender.send(STATUS, statusMessage("3EB0STAT002", LinkedMessageContainer.of("status with no privacy entry")));

        var stanza = captured.get();
        var meta = stanza.getChild("meta").orElse(null);
        if (meta != null) {
            assertTrue(meta.getAttribute("status_setting").isEmpty(),
                    "no privacy entry means no status_setting attribute on <meta>");
        }
    }

    @Test
    @DisplayName("audience derives from status privacy: WHITELIST -> allowlist; CONTACTS_EXCEPT -> contacts minus blocklist")
    void audienceFromStatusPrivacy() {
        var a = Jid.of("111111111111@s.whatsapp.net");
        var b = Jid.of("222222222222@s.whatsapp.net");
        var c = Jid.of("333333333333@s.whatsapp.net");

        var whitelistAudience = captureStatusAudience(store -> {
            store.contactStore().addContact(new ContactBuilder().jid(a).build());
            store.contactStore().addContact(new ContactBuilder().jid(b).build());
            store.contactStore().addContact(new ContactBuilder().jid(c).build());
            store.settingsStore().setStatusPrivacy(new StatusPrivacySettingBuilder()
                    .mode(StatusPrivacyMode.WHITELIST).jids(List.of(a)).build());
        });
        assertEquals(Set.of(a), Set.copyOf(whitelistAudience),
                "WHITELIST audience must be exactly the allowlist, ignoring contacts");

        var exceptAudience = captureStatusAudience(store -> {
            store.contactStore().addContact(new ContactBuilder().jid(a).build());
            store.contactStore().addContact(new ContactBuilder().jid(b).build());
            store.contactStore().addContact(new ContactBuilder().jid(c).build());
            store.settingsStore().setStatusPrivacy(new StatusPrivacySettingBuilder()
                    .mode(StatusPrivacyMode.CONTACTS_EXCEPT).jids(List.of(b)).build());
        });
        assertEquals(Set.of(a, c), Set.copyOf(exceptAudience),
                "CONTACTS_EXCEPT audience must be all contacts minus the blocklist");
    }

    // Runs a status send after the given store seeding and returns the audience the sender handed
    // to getStatusFanout. AUDIENCE_DEVICE is pre-marked so the send takes the steady-state branch
    // and never reaches the per-device sender-key crypto.
    private static Collection<Jid> captureStatusAudience(Consumer<LinkedWhatsAppStore> seed) {
        var senderStore = MessageFixtures.temporaryStore(SELF_PN, SELF_LID);
        senderStore.signalStore().markSenderKeyDistributed(STATUS, AUDIENCE_DEVICE);
        seed.accept(senderStore);

        var captured = new AtomicReference<Stanza>();
        var client = clientWithCapture(senderStore, captured);
        var audienceRef = new AtomicReference<Collection<Jid>>();
        var sender = statusMessageSender(client, senderStore,
                StubDeviceService.create().withStatusFanout(audience -> {
                    audienceRef.set(audience);
                    return Set.of(AUDIENCE_DEVICE);
                }));

        sender.send(STATUS, statusMessage("3EB0STATAUD", LinkedMessageContainer.of("audience probe")));
        return audienceRef.get();
    }

    // The returned ack carries only the t attribute, which AckParser reads as
    // a success result with no error code.
    private static TestWhatsAppClient clientWithCapture(LinkedWhatsAppStore store, AtomicReference<Stanza> capturedStanza) {
        return TestWhatsAppClient.create()
                .withStore(store)
                .withAbPropsService(TestABPropsService.builder().build())
                .withSendNodeHandler(nb -> {
                    capturedStanza.set(nb.build());
                    return new StanzaBuilder()
                            .description("ack")
                            .attribute("t", 1700000000L)
                            .build();
                });
    }

    private static StatusMessageSender statusMessageSender(TestWhatsAppClient client, LinkedWhatsAppStore store, StubDeviceService deviceService) {
        var ab = client.abPropsService();
        var encryption = new MessageEncryption(store,
                new SignalSessionCipher(store.signalStore()),
                new SignalGroupCipher(store.signalStore()),
                new SignalCryptoLocks());
        var wamService = new LiveWamService(client, ab);
        var skDistribution = new SenderKeyDistribution(encryption, deviceService, store);
        var meta = new MetaStanza(store);
        var reporting = new ReportingStanza(ab);
        return new StatusMessageSender(client, encryption, deviceService, ab, skDistribution,
                meta, reporting, wamService);
    }

    private static ChatMessageInfo statusMessage(
            String id, LinkedMessageContainer container) {
        var key = new MessageKeyBuilder()
                .id(id)
                .parentJid(STATUS)
                .fromMe(true)
                .senderJid(SELF_LID)
                .build();
        return new ChatMessageInfoBuilder()
                .key(key)
                .senderJid(SELF_LID)
                .message(container)
                .messageSecret(new byte[32])
                .broadcast(true)
                .build();
    }
}
