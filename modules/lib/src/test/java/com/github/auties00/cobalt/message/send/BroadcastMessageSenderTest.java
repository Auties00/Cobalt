package com.github.auties00.cobalt.message.send;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.StubDeviceService;
import com.github.auties00.cobalt.message.MessageFixtures;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.message.send.senderkey.SenderKeyDistribution;
import com.github.auties00.cobalt.message.send.stanza.MetaStanza;
import com.github.auties00.cobalt.message.send.stanza.ReportingStanza;
import com.github.auties00.cobalt.wire.linked.business.BroadcastListParticipantBuilder;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastListBuilder;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
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

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers the {@link BroadcastMessageSender} SKMSG send shape. Unlike a status post, a business
 * broadcast carries a {@code phash} on the outer {@code <message>} (WA Web's
 * {@code encryptAndSendBroadcastMsg} sets {@code phash = phashV2([...devices, meLid])}). The cell
 * drives a real send against a captured {@link TestWhatsAppClient} and a stubbed
 * {@link StubDeviceService}, with the recipient roster seeded as a local broadcast list.
 */
@DisplayName("BroadcastMessageSender")
class BroadcastMessageSenderTest {

    private static final Jid SELF_PN = Jid.of("12025550100@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("258252122116273@lid");
    private static final String LIST_ID = "12345";
    private static final Jid BROADCAST = Jid.of(LIST_ID + "@broadcast");
    private static final Jid RECIPIENT_LID = Jid.of("83116928594056@lid");
    private static final Jid RECIPIENT_DEVICE = Jid.of("83116928594056:0@lid");

    @Test
    @DisplayName("broadcast send carries phash on the outer <message> (mirrors phashV2([...devices, meLid]))")
    void broadcastCarriesPhash() {
        var senderStore = MessageFixtures.temporaryStore(SELF_PN, SELF_LID);
        senderStore.businessStore().putBusinessBroadcastList(new BusinessBroadcastListBuilder()
                .id(LIST_ID)
                .participants(List.of(new BroadcastListParticipantBuilder().lidJid(RECIPIENT_LID).build()))
                .build());
        // Steady-state: the recipient device already holds the sender key, so no per-device crypto.
        senderStore.signalStore().markSenderKeyDistributed(BROADCAST, RECIPIENT_DEVICE);

        var captured = new AtomicReference<Stanza>();
        var client = clientWithCapture(senderStore, captured);
        var sender = broadcastMessageSender(client, senderStore,
                StubDeviceService.create()
                        .withBroadcastFanout(recipients -> Set.of(RECIPIENT_DEVICE))
                        .withGroupPhash((devices, self) -> "2:bcast"));

        sender.send(BROADCAST, broadcastMessage("3EB0BCAST01", LinkedMessageContainer.of("broadcast body")));

        var stanza = captured.get();
        assertEquals(BROADCAST.toString(), stanza.getAttributeAsString("to").orElseThrow());
        assertEquals("2:bcast", stanza.getAttributeAsString("phash").orElseThrow(),
                "broadcast <message> must carry the device-list phash");
    }

    // The returned ack carries only the t attribute, which AckParser reads as a success result.
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

    private static BroadcastMessageSender broadcastMessageSender(TestWhatsAppClient client, LinkedWhatsAppStore store, StubDeviceService deviceService) {
        var ab = client.abPropsService();
        var encryption = new MessageEncryption(store,
                new SignalSessionCipher(store.signalStore()),
                new SignalGroupCipher(store.signalStore()),
                new SignalCryptoLocks());
        var wamService = new LiveWamService(client, ab);
        var skDistribution = new SenderKeyDistribution(encryption, deviceService, store);
        var meta = new MetaStanza(store);
        var reporting = new ReportingStanza(ab);
        return new BroadcastMessageSender(client, encryption, deviceService, ab, skDistribution,
                meta, reporting, wamService);
    }

    private static ChatMessageInfo broadcastMessage(String id, LinkedMessageContainer container) {
        var key = new MessageKeyBuilder()
                .id(id)
                .parentJid(BROADCAST)
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
