package com.github.auties00.cobalt.message.send;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.StubDeviceService;
import com.github.auties00.cobalt.message.MessageFixtures;
import com.github.auties00.cobalt.message.TestSignalSession;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.wam.LiveWamService;
import com.github.auties00.cobalt.message.crypto.SignalCryptoLocks;
import com.github.auties00.libsignal.SignalSessionCipher;
import com.github.auties00.libsignal.groups.SignalGroupCipher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the {@link PeerMessageSender} wire-stanza shape. Peer messages target
 * one of the user's own devices, encrypt per-device via the Signal session
 * cipher, and carry {@code category="peer"} plus {@code push_priority="high"};
 * the first send on a fresh session yields a {@code PKMSG} envelope with a
 * sibling {@code <device-identity>}. The sender runs against a captured
 * {@link TestWhatsAppClient} and a pre-established {@link TestSignalSession} so
 * the encryption stage runs against a real Signal session.
 */
@DisplayName("PeerMessageSender")
class PeerMessageSenderTest {

    private static final Jid SELF_PRIMARY = Jid.of("12025550100:0@s.whatsapp.net");
    private static final Jid SELF_COMPANION = Jid.of("12025550100:73@s.whatsapp.net");

    @Test
    @DisplayName("send: emits <message category=\"peer\" push_priority=\"high\"> with <enc> and <device-identity>")
    void sendShape() {
        var senderStore = MessageFixtures.temporaryStore(Jid.of("12025550100@s.whatsapp.net"), null);
        var recipientStore = MessageFixtures.temporaryStore(Jid.of("12025550100@s.whatsapp.net"), null);
        TestSignalSession.establishSession(senderStore, SELF_COMPANION, recipientStore);

        var capturedStanza = new AtomicReference<Stanza>();
        var client = TestWhatsAppClient.create()
                .withStore(senderStore)
                .withAbPropsService(TestABPropsService.builder().build())
                .withSendNodeHandler(node -> {
                    capturedStanza.set(node.build());
                    // Return a success ack so the sender's AckParser is satisfied.
                    return new StanzaBuilder()
                            .description("ack")
                            .attribute("t", 1700000000L)
                            .build();
                });
        var wamService = new LiveWamService(client, client.abPropsService());
        var encryption = new MessageEncryption(senderStore,
                new SignalSessionCipher(senderStore.signalStore()),
                new SignalGroupCipher(senderStore.signalStore()),
                new SignalCryptoLocks());
        var deviceService = StubDeviceService.create();
        var sender = new PeerMessageSender(client, encryption, deviceService, client.abPropsService(), wamService);

        var messageInfo = peerMessageInfo("3EB0CAFEBABE", SELF_COMPANION);
        var ack = sender.send(SELF_COMPANION, messageInfo);

        var stanza = capturedStanza.get();
        assertNotNull(stanza, "PeerMessageSender must emit exactly one outbound <message>");
        assertEquals("message", stanza.description());

        assertEquals("3EB0CAFEBABE", stanza.getAttributeAsString("id").orElseThrow());
        assertEquals(SELF_COMPANION.toString(), stanza.getAttributeAsString("to").orElseThrow(),
                "outer to must be the target device JID");
        assertEquals("peer", stanza.getAttributeAsString("category").orElseThrow(),
                "category=peer routes the stanza outside the normal chat fanout pipeline");
        assertEquals("high", stanza.getAttributeAsString("push_priority").orElseThrow(),
                "push_priority=high asks the server to deliver promptly");

        assertFalse(stanza.getChild("participants").isPresent(),
                "peer fanout has no <participants> wrapper");

        var enc = stanza.getChild("enc").orElseThrow();
        assertEquals("2", enc.getAttributeAsString("v").orElseThrow());
        assertEquals("pkmsg", enc.getAttributeAsString("type").orElseThrow(),
                "first send carries PKMSG since the recipient hasn't processed the prekey yet");

        assertTrue(stanza.getChild("device-identity").isPresent(),
                "PKMSG peer sends must include <device-identity> for the recipient to verify the identity key");

        assertTrue(ack.isSuccess(), "stub returned a t-only ack so AckParser reports success");
    }

    @Test
    @DisplayName("send: invokes deviceService.ensureSessions exactly once with the target device")
    void ensureSessionsCalled() {
        var senderStore = MessageFixtures.temporaryStore(Jid.of("12025550100@s.whatsapp.net"), null);
        var recipientStore = MessageFixtures.temporaryStore(Jid.of("12025550100@s.whatsapp.net"), null);
        TestSignalSession.establishSession(senderStore, SELF_COMPANION, recipientStore);

        var ensureCalls = new ArrayList<Collection<Jid>>();
        var client = TestWhatsAppClient.create()
                .withStore(senderStore)
                .withAbPropsService(TestABPropsService.builder().build())
                .withSendNodeHandler(node -> new StanzaBuilder()
                        .description("ack")
                        .attribute("t", 1700000000L)
                        .build());
        var wamService = new LiveWamService(client, client.abPropsService());
        var encryption = new MessageEncryption(senderStore,
                new SignalSessionCipher(senderStore.signalStore()),
                new SignalGroupCipher(senderStore.signalStore()),
                new SignalCryptoLocks());
        var deviceService = StubDeviceService.create()
                .withEnsureSessions(devices -> {
                    ensureCalls.add(devices);
                    return 0;
                });
        var sender = new PeerMessageSender(client, encryption, deviceService, client.abPropsService(), wamService);

        sender.send(SELF_COMPANION, peerMessageInfo("3EB0PEER01", SELF_COMPANION));

        assertEquals(1, ensureCalls.size(),
                "ensureSessions must be called exactly once per peer send");
        assertTrue(ensureCalls.getFirst().contains(SELF_COMPANION),
                "ensureSessions must receive the target device JID");
    }

    @Test
    @DisplayName("send: stanza type is derived from the payload (text content -> type=\"text\")")
    void stanzaTypeFromContent() {
        var senderStore = MessageFixtures.temporaryStore(Jid.of("12025550100@s.whatsapp.net"), null);
        var recipientStore = MessageFixtures.temporaryStore(Jid.of("12025550100@s.whatsapp.net"), null);
        TestSignalSession.establishSession(senderStore, SELF_COMPANION, recipientStore);

        var capturedStanza = new AtomicReference<Stanza>();
        var client = TestWhatsAppClient.create()
                .withStore(senderStore)
                .withAbPropsService(TestABPropsService.builder().build())
                .withSendNodeHandler(node -> {
                    capturedStanza.set(node.build());
                    return new StanzaBuilder().description("ack").attribute("t", 1700000000L).build();
                });
        var wamService = new LiveWamService(client, client.abPropsService());
        var encryption = new MessageEncryption(senderStore,
                new SignalSessionCipher(senderStore.signalStore()),
                new SignalGroupCipher(senderStore.signalStore()),
                new SignalCryptoLocks());
        var sender = new PeerMessageSender(client, encryption, StubDeviceService.create(), client.abPropsService(), wamService);

        sender.send(SELF_COMPANION, peerMessageInfo("3EB0TYPE01", SELF_COMPANION));

        var stanza = capturedStanza.get();
        assertEquals("text", stanza.getAttributeAsString("type").orElseThrow(),
                "ExtendedTextMessage payload must produce a type=\"text\" peer stanza");
    }

    private static ChatMessageInfo peerMessageInfo(
            String id, Jid targetDevice) {
        var key = new MessageKeyBuilder()
                .id(id)
                .parentJid(targetDevice)
                .fromMe(true)
                .senderJid(SELF_PRIMARY)
                .build();
        return new ChatMessageInfoBuilder()
                .key(key)
                .message(LinkedMessageContainer.of("peer payload"))
                .build();
    }
}
