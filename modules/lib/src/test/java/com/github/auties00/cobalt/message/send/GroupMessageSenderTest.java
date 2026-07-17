package com.github.auties00.cobalt.message.send;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.StubDeviceService;
import com.github.auties00.cobalt.message.MessageFixtures;
import com.github.auties00.cobalt.message.TestSignalSession;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.message.send.senderkey.SenderKeyDistribution;
import com.github.auties00.cobalt.message.send.stanza.BizStanza;
import com.github.auties00.cobalt.message.send.stanza.BotStanza;
import com.github.auties00.cobalt.message.send.stanza.MetaStanza;
import com.github.auties00.cobalt.message.send.stanza.ReportingStanza;
import com.github.auties00.cobalt.message.send.bot.BotProtobufTransform;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupMetadataBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
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

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the {@link GroupMessageSender} wire-stanza shape across three send
 * regimes: steady-state SKMSG (a bare {@code <enc type="skmsg">} with no
 * {@code <participants>}), first-time distribution ({@code <participants>}
 * wrapping per-device PKMSG envelopes plus a sibling {@code <device-identity>}),
 * and PN-addressed groups ({@code addressing_mode="pn"} on the outer message).
 * The sender runs against a stubbed {@link StubDeviceService} and a
 * {@link TestWhatsAppClient} that captures the first emitted {@link Stanza} into
 * an {@link AtomicReference}.
 */
@DisplayName("GroupMessageSender")
class GroupMessageSenderTest {

    private static final Jid SELF_PN = Jid.of("12025550100@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("258252122116273@lid");
    private static final Jid GROUP = Jid.of("120363023250764418@g.us");
    private static final Jid PARTICIPANT_LID = Jid.of("83116928594056:0@lid");

    @Test
    @DisplayName("steady-state: bare <enc type=\"skmsg\">, no <participants>")
    void steadyStateSkmsg() {
        var senderStore = MessageFixtures.temporaryStore(SELF_PN, SELF_LID);
        seedGroupMetadata(senderStore, /*lidAddressing*/ true);
        senderStore.signalStore().markSenderKeyDistributed(GROUP, PARTICIPANT_LID);

        var captured = new AtomicReference<Stanza>();
        var client = clientWithCapture(senderStore, captured);
        var sender = groupMessageSender(client, senderStore,
                StubDeviceService.create().withGroupFanout(
                        g -> Set.of(PARTICIPANT_LID)));

        sender.send(GROUP, chatMessage("3EB0GRP0001", GROUP, LinkedMessageContainer.of("hi")));

        var stanza = captured.get();
        assertEquals("message", stanza.description());
        assertEquals(GROUP.toString(), stanza.getAttributeAsString("to").orElseThrow());
        assertEquals("text", stanza.getAttributeAsString("type").orElseThrow());
        assertEquals("lid", stanza.getAttributeAsString("addressing_mode").orElseThrow());
        assertEquals("2:hash", stanza.getAttributeAsString("phash").orElseThrow());

        var enc = stanza.getChild("enc").orElseThrow();
        assertEquals("skmsg", enc.getAttributeAsString("type").orElseThrow());
        assertEquals("2", enc.getAttributeAsString("v").orElseThrow());
        assertFalse(stanza.getChild("participants").isPresent(),
                "steady-state SKMSG must not carry <participants>");
    }

    @Test
    @DisplayName("first-time distribution: <participants> wraps PKMSG envelopes plus the bare SKMSG sibling")
    void firstTimeDistribution() {
        var senderStore = MessageFixtures.temporaryStore(SELF_PN, SELF_LID);
        var participantStore = MessageFixtures.temporaryStore(SELF_PN, SELF_LID);
        TestSignalSession.establishSession(senderStore, PARTICIPANT_LID, participantStore);
        seedGroupMetadata(senderStore, /*lidAddressing*/ true);
        // The participant is intentionally NOT marked as already-distributed
        // so the first-time distribution branch fires.

        var captured = new AtomicReference<Stanza>();
        var client = clientWithCapture(senderStore, captured);
        var sender = groupMessageSender(client, senderStore,
                StubDeviceService.create()
                        .withGroupFanout(
                                g -> Set.of(PARTICIPANT_LID))
                        .withEnsureSessions(devices -> 0));

        sender.send(GROUP, chatMessage("3EB0GRP0002", GROUP, LinkedMessageContainer.of("hello group")));

        var stanza = captured.get();
        assertEquals("text", stanza.getAttributeAsString("type").orElseThrow());
        assertEquals("lid", stanza.getAttributeAsString("addressing_mode").orElseThrow());

        // Bare SKMSG sibling.
        var enc = stanza.getChild("enc").orElseThrow();
        assertEquals("skmsg", enc.getAttributeAsString("type").orElseThrow());

        // <participants> wrapping per-device PKMSG payloads.
        var participants = stanza.getChild("participants").orElseThrow(
                () -> new AssertionError("first-distribution must wrap PKMSG payloads in <participants>"));
        var tos = participants.streamChildren("to").toList();
        assertEquals(1, tos.size(), "fanout is just one participant in this test");
        var participantEnc = tos.getFirst().getChild("enc").orElseThrow();
        assertEquals("pkmsg", participantEnc.getAttributeAsString("type").orElseThrow(),
                "SK distribution payload is wrapped as PKMSG");

        // <device-identity> sibling for PKMSG verification.
        assertTrue(stanza.getChild("device-identity").isPresent(),
                "first-distribution carries <device-identity> since PKMSG is present");
    }

    @Test
    @DisplayName("PN-addressing group: addressing_mode=\"pn\" propagates to outer <message>")
    void pnAddressingMode() {
        var senderStore = MessageFixtures.temporaryStore(SELF_PN, SELF_LID);
        var participantPn = Jid.of("19254863482:0@s.whatsapp.net");
        seedGroupMetadata(senderStore, /*lidAddressing*/ false);
        senderStore.signalStore().markSenderKeyDistributed(GROUP, participantPn);

        var captured = new AtomicReference<Stanza>();
        var client = clientWithCapture(senderStore, captured);
        var sender = groupMessageSender(client, senderStore,
                StubDeviceService.create().withGroupFanout(
                        g -> Set.of(participantPn)));

        sender.send(GROUP, chatMessage("3EB0GRPPN01", GROUP, LinkedMessageContainer.of("hi")));

        var stanza = captured.get();
        assertEquals("pn", stanza.getAttributeAsString("addressing_mode").orElseThrow(),
                "non-LID group emits addressing_mode=pn");
    }

    // Seeds group metadata so the sender resolves the addressing mode locally
    // instead of going through the live group-metadata fetch path.
    private static void seedGroupMetadata(LinkedWhatsAppStore store, boolean lidAddressing) {
        var metadata = new GroupMetadataBuilder()
                .jid(GROUP)
                .subject("test group")
                .isLidAddressingMode(lidAddressing)
                .build();
        store.chatStore().addChatMetadata(metadata);
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

    private static GroupMessageSender groupMessageSender(TestWhatsAppClient client, LinkedWhatsAppStore store, StubDeviceService deviceService) {
        var ab = client.abPropsService();
        var encryption = new MessageEncryption(store,
                new SignalSessionCipher(store.signalStore()),
                new SignalGroupCipher(store.signalStore()),
                new SignalCryptoLocks());
        var wamService = new LiveWamService(client, ab);
        var skDistribution = new SenderKeyDistribution(encryption, deviceService, store);
        var bot = new BotStanza(encryption, new BotProtobufTransform(store));
        var biz = new BizStanza(store);
        var meta = new MetaStanza(store);
        var reporting = new ReportingStanza(ab);
        return new GroupMessageSender(client, encryption, deviceService, ab,
                skDistribution, bot, biz, meta, reporting, wamService);
    }

    private static ChatMessageInfo chatMessage(
            String id, Jid groupJid, LinkedMessageContainer container) {
        var key = new MessageKeyBuilder()
                .id(id)
                .parentJid(groupJid)
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
