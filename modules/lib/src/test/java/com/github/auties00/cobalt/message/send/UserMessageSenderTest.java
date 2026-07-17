package com.github.auties00.cobalt.message.send;
import com.github.auties00.cobalt.migration.LiveLidMigrationService;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.StubDeviceService;
import com.github.auties00.cobalt.message.MessageFixtures;
import com.github.auties00.cobalt.message.TestSignalSession;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.message.send.stanza.BizStanza;
import com.github.auties00.cobalt.message.send.stanza.BotStanza;
import com.github.auties00.cobalt.message.send.stanza.CtwaAttributionStanza;
import com.github.auties00.cobalt.message.send.stanza.MetaStanza;
import com.github.auties00.cobalt.message.send.stanza.ReportingStanza;
import com.github.auties00.cobalt.message.send.stanza.TcTokenStanza;
import com.github.auties00.cobalt.message.send.bot.BotProtobufTransform;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.privacy.LiveTrustedContactTokenService;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.wam.LiveWamService;
import com.github.auties00.cobalt.message.crypto.SignalCryptoLocks;
import com.github.auties00.libsignal.SignalSessionCipher;
import com.github.auties00.libsignal.groups.SignalGroupCipher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the {@link UserMessageSender} wire-stanza shape across a stubbed
 * multi-device fanout. The load-bearing cell is the 479 invariant: when the
 * chat is LID-addressed, every {@code <to jid="...">} under
 * {@code <participants>} must carry an LID-form recipient, since a PN entry
 * would trip the server's 479 recipient-addressing-mismatch nack. The sender
 * runs against a captured {@link TestWhatsAppClient} with separate per-device
 * recipient stores approximating the production one-store-per-device identity.
 */
@DisplayName("UserMessageSender")
class UserMessageSenderTest {

    private static final Jid SELF_PN = Jid.of("12025550100@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("258252122116273@lid");
    private static final Jid PEER_LID = Jid.of("83116928594056@lid");
    private static final Jid PEER_DEVICE_PRIMARY = Jid.of("83116928594056:0@lid");
    private static final Jid PEER_DEVICE_COMPANION = Jid.of("83116928594056:1@lid");

    @Test
    @DisplayName("send: 1:1 LID fanout produces <message to=lid> with <participants> wrapping per-device <enc> nodes")
    void lidFanoutShape() {
        var senderStore = MessageFixtures.temporaryStore(SELF_PN, SELF_LID);
        // Two separate recipient stores so each device has its own identity.
        // In real life it's one store per device but we approximate here.
        var recipientPrimary = MessageFixtures.temporaryStore(SELF_PN, SELF_LID);
        var recipientCompanion = MessageFixtures.temporaryStore(SELF_PN, SELF_LID);
        TestSignalSession.establishSession(senderStore, PEER_DEVICE_PRIMARY, recipientPrimary);
        TestSignalSession.establishSession(senderStore, PEER_DEVICE_COMPANION, recipientCompanion);

        var captured = new AtomicReference<Stanza>();
        var client = clientWithCapture(senderStore, captured);
        var sender = userMessageSender(client, senderStore,
                StubDeviceService.create()
                        .withUserFanout(chat -> List.of(PEER_DEVICE_PRIMARY, PEER_DEVICE_COMPANION))
                        .withEnsureSessions(devices -> 0));

        var info = chatMessage("3EB0USR0001", PEER_LID, LinkedMessageContainer.of("hi"));
        sender.send(PEER_LID, info);

        var stanza = captured.get();
        assertNotNull(stanza, "exactly one <message> must be sent");
        assertEquals("message", stanza.description());
        assertEquals("3EB0USR0001", stanza.getAttributeAsString("id").orElseThrow());
        assertEquals(PEER_LID.toString(), stanza.getAttributeAsString("to").orElseThrow(),
                "outer to must be the chat JID (LID-form)");

        // Multi-device fanout wraps every <enc> under <participants>.
        var participants = stanza.getChild("participants").orElseThrow(
                () -> new AssertionError("multi-device fanout must wrap in <participants>"));
        var participantTos = participants.streamChildren("to").toList();
        assertEquals(2, participantTos.size(),
                "exactly one <to jid=...> per fanout device");
        for (var to : participantTos) {
            var enc = to.getChild("enc").orElseThrow();
            assertEquals("2", enc.getAttributeAsString("v").orElseThrow());
            assertEquals("pkmsg", enc.getAttributeAsString("type").orElseThrow(),
                    "first send to a session yields PKMSG");
        }
    }

    @Test
    @DisplayName("479 invariant: every participant <to jid=...> carries @lid when chat is @lid")
    void participantsAreLidWhenChatIsLid() {
        var senderStore = MessageFixtures.temporaryStore(SELF_PN, SELF_LID);
        var recipientPrimary = MessageFixtures.temporaryStore(SELF_PN, SELF_LID);
        var recipientCompanion = MessageFixtures.temporaryStore(SELF_PN, SELF_LID);
        TestSignalSession.establishSession(senderStore, PEER_DEVICE_PRIMARY, recipientPrimary);
        TestSignalSession.establishSession(senderStore, PEER_DEVICE_COMPANION, recipientCompanion);

        var captured = new AtomicReference<Stanza>();
        var client = clientWithCapture(senderStore, captured);
        var sender = userMessageSender(client, senderStore,
                StubDeviceService.create()
                        .withUserFanout(chat -> List.of(PEER_DEVICE_PRIMARY, PEER_DEVICE_COMPANION)));

        sender.send(PEER_LID, chatMessage("3EB0479001", PEER_LID, LinkedMessageContainer.of("479 guard")));

        var stanza = captured.get();
        var participants = stanza.getChild("participants").orElseThrow();
        for (var to : participants.streamChildren("to").toList()) {
            var jid = to.getAttributeAsString("jid").orElseThrow();
            assertTrue(jid.endsWith("@lid"),
                    "479 invariant: every participant <to jid=...> must be @lid for an @lid chat, got " + jid);
            assertFalse(jid.endsWith("@s.whatsapp.net"),
                    "479 invariant: NO participant may be @s.whatsapp.net when outer is @lid: " + jid);
        }
    }

    @Test
    @DisplayName("send: <device-identity> sibling is emitted when any payload is PKMSG")
    void identityNodeOnPkmsg() {
        var senderStore = MessageFixtures.temporaryStore(SELF_PN, SELF_LID);
        var recipientPrimary = MessageFixtures.temporaryStore(SELF_PN, SELF_LID);
        TestSignalSession.establishSession(senderStore, PEER_DEVICE_PRIMARY, recipientPrimary);

        var captured = new AtomicReference<Stanza>();
        var client = clientWithCapture(senderStore, captured);
        var sender = userMessageSender(client, senderStore,
                StubDeviceService.create().withUserFanout(chat -> List.of(PEER_DEVICE_PRIMARY)));

        sender.send(PEER_LID, chatMessage("3EB0IDENT01", PEER_LID, LinkedMessageContainer.of("hi")));

        var stanza = captured.get();
        assertTrue(stanza.getChild("device-identity").isPresent(),
                "PKMSG fanout must carry <device-identity> for recipient identity-key verification");
    }

    @Test
    @DisplayName("send: deviceService.ensureSessions is invoked with the fanout devices")
    void ensureSessionsReceivesFanout() {
        var senderStore = MessageFixtures.temporaryStore(SELF_PN, SELF_LID);
        var recipientPrimary = MessageFixtures.temporaryStore(SELF_PN, SELF_LID);
        TestSignalSession.establishSession(senderStore, PEER_DEVICE_PRIMARY, recipientPrimary);

        var ensureCalls = new ArrayList<Collection<Jid>>();
        var client = clientWithCapture(senderStore, new AtomicReference<>());
        var sender = userMessageSender(client, senderStore,
                StubDeviceService.create()
                        .withUserFanout(chat -> List.of(PEER_DEVICE_PRIMARY))
                        .withEnsureSessions(devices -> {
                            ensureCalls.add(devices);
                            return 0;
                        }));

        sender.send(PEER_LID, chatMessage("3EB0ENS001", PEER_LID, LinkedMessageContainer.of("hi")));

        assertEquals(1, ensureCalls.size(), "ensureSessions must be called exactly once per send");
        assertTrue(ensureCalls.getFirst().contains(PEER_DEVICE_PRIMARY),
                "ensureSessions must receive the fanout device list");
    }

    // The returned ack carries only the t attribute, which AckParser reads as
    // a success result.
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

    private static UserMessageSender userMessageSender(TestWhatsAppClient client, LinkedWhatsAppStore store, StubDeviceService deviceService) {
        var ab = client.abPropsService();
        var encryption = new MessageEncryption(store,
                new SignalSessionCipher(store.signalStore()),
                new SignalGroupCipher(store.signalStore()),
                new SignalCryptoLocks());
        var wamService = new LiveWamService(client, ab);
        var lidMigrationService = new LiveLidMigrationService(client, ab, wamService);
        var bot = new BotStanza(encryption, new BotProtobufTransform(store));
        var biz = new BizStanza(store);
        var meta = new MetaStanza(store);
        var reporting = new ReportingStanza(ab);
        var ctwa = new CtwaAttributionStanza(store, ab);
        var tcToken = new TcTokenStanza(store, ab, new LiveTrustedContactTokenService(ab));
        return new UserMessageSender(client, encryption, deviceService, lidMigrationService, ab,
                bot, biz, meta, reporting, ctwa, tcToken, wamService);
    }

    private static ChatMessageInfo chatMessage(
            String id, Jid chatJid, LinkedMessageContainer container) {
        var key = new MessageKeyBuilder()
                .id(id)
                .parentJid(chatJid)
                .fromMe(true)
                .senderJid(SELF_LID)
                .build();
        return new ChatMessageInfoBuilder()
                .key(key)
                .senderJid(SELF_LID)
                .message(container)
                .messageSecret(new byte[32])
                .build();
    }
}
