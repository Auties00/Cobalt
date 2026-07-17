package com.github.auties00.cobalt.message.receive;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.message.MessageFixtures;
import com.github.auties00.cobalt.message.TestSignalSession;
import com.github.auties00.cobalt.message.receive.crypto.MessageDecryption;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerSpec;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.wam.TestWamService;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.message.crypto.SignalCryptoLocks;
import com.github.auties00.libsignal.SignalSessionCipher;
import com.github.auties00.libsignal.groups.SignalGroupCipher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers {@link MessageReceivingService} dispatch: inbound {@code <message>} nodes route to the
 * newsletter receiver when the {@code from} JID is on the {@code @newsletter} server and to the
 * chat receiver otherwise. Synthetic inbound stanzas are built via {@link StanzaBuilder}; the
 * chat-dispatch tests install a full sender-recipient libsignal pair via
 * {@link TestSignalSession#establishSession} so the decryption path runs end-to-end.
 */
@DisplayName("MessageReceivingService")
class MessageReceivingServiceTest {

    private static final Jid SENDER_PRIMARY = Jid.of("12025550100:0@s.whatsapp.net");
    private static final Jid RECIPIENT_BARE = Jid.of("19254863482@s.whatsapp.net");
    private static final Jid NEWSLETTER = Jid.of("120363402045452944@newsletter");

    private static final WamService wamService = TestWamService.create(TestWhatsAppClient.create());

    @Test
    @DisplayName("constructor: null store throws NullPointerException")
    void nullStoreThrows() {
        var store = MessageFixtures.temporaryStore(RECIPIENT_BARE, null);
        var decryption = decryption(store);
        assertThrows(NullPointerException.class,
                () -> new LiveMessageReceivingService(null, decryption, wamService));
    }

    @Test
    @DisplayName("process: chat dispatch; non-newsletter from JID routes to ChatMessageReceiver and decrypts the payload")
    void chatDispatch() {
        var senderStore = MessageFixtures.temporaryStore(Jid.of("12025550100@s.whatsapp.net"), null);
        var recipientStore = MessageFixtures.temporaryStore(RECIPIENT_BARE, null);
        TestSignalSession.establishSession(senderStore, Jid.of("19254863482:0@s.whatsapp.net"), recipientStore);

        var senderEncryption = new MessageEncryption(senderStore,
                new SignalSessionCipher(senderStore.signalStore()),
                new SignalGroupCipher(senderStore.signalStore()),
                new SignalCryptoLocks());
        var payload = senderEncryption.encryptForDevice(
                Jid.of("19254863482:0@s.whatsapp.net"),
                LinkedMessageContainerSpec.encode(LinkedMessageContainer.of("dispatched via chat")));

        var inbound = buildInbound("3EB0RCV0001", SENDER_PRIMARY, "pkmsg", payload.ciphertext());
        var service = new LiveMessageReceivingService(recipientStore, decryption(recipientStore), wamService);

        var info = service.process(inbound);

        assertInstanceOf(ChatMessageInfo.class, info,
                "non-newsletter dispatch returns a ChatMessageInfo");
        assertEquals("3EB0RCV0001", info.key().id().orElseThrow());
    }

    // PreKey ciphertext is single-use so the same PKMSG cannot be replayed here; true concurrent
    // in-flight dedup is exercised at the unit level by MessageDedupTest.
    @Test
    @DisplayName("process: dedup key is released in the finally block so a follow-up message processes independently")
    void dedupKeyReleasedAfterProcessing() {
        var senderStore = MessageFixtures.temporaryStore(Jid.of("12025550100@s.whatsapp.net"), null);
        var recipientStore = MessageFixtures.temporaryStore(RECIPIENT_BARE, null);
        TestSignalSession.establishSession(senderStore, Jid.of("19254863482:0@s.whatsapp.net"), recipientStore);

        var senderEncryption = new MessageEncryption(senderStore,
                new SignalSessionCipher(senderStore.signalStore()),
                new SignalGroupCipher(senderStore.signalStore()),
                new SignalCryptoLocks());

        var firstPayload = senderEncryption.encryptForDevice(
                Jid.of("19254863482:0@s.whatsapp.net"),
                LinkedMessageContainerSpec.encode(LinkedMessageContainer.of("first")));
        var firstInbound = buildInbound("3EB0DEDUP01", SENDER_PRIMARY, "pkmsg", firstPayload.ciphertext());

        var secondPayload = senderEncryption.encryptForDevice(
                Jid.of("19254863482:0@s.whatsapp.net"),
                LinkedMessageContainerSpec.encode(LinkedMessageContainer.of("second")));
        var secondInbound = buildInbound("3EB0DEDUP02", SENDER_PRIMARY,
                secondPayload.type().protocolValue(), secondPayload.ciphertext());

        var service = new LiveMessageReceivingService(recipientStore, decryption(recipientStore), wamService);

        var first = service.process(firstInbound);
        assertNotNull(first, "first message processes successfully");
        var second = service.process(secondInbound);
        assertNotNull(second, "second distinct message processes; dedup key from first was released");
    }

    @Test
    @DisplayName("process: null stanza throws NullPointerException")
    void nullNodeThrows() {
        var store = MessageFixtures.temporaryStore(RECIPIENT_BARE, null);
        var service = new LiveMessageReceivingService(store, decryption(store), wamService);
        assertThrows(NullPointerException.class, () -> service.process(null));
    }

    @Test
    @DisplayName("clearPendingMessages: safe to call on a fresh service (idempotent no-op)")
    void clearPendingMessagesIdempotent() {
        var store = MessageFixtures.temporaryStore(RECIPIENT_BARE, null);
        var service = new LiveMessageReceivingService(store, decryption(store), wamService);
        Assertions.assertDoesNotThrow(service::clearPendingMessages);
        Assertions.assertDoesNotThrow(service::clearPendingMessages);
    }

    // The chat-decryption path would throw on the missing <enc>; a null return is the fingerprint
    // of a newsletter dispatch.
    @Test
    @DisplayName("process: newsletter-server JID routes to NewsletterMessageReceiver; missing <plaintext> returns null")
    void newsletterDispatchRecognised() {
        var store = MessageFixtures.temporaryStore(RECIPIENT_BARE, null);
        var service = new LiveMessageReceivingService(store, decryption(store), wamService);

        var inbound = new StanzaBuilder()
                .description("message")
                .attribute("id", "3EB0NL0001")
                .attribute("from", NEWSLETTER)
                .attribute("t", 1700000000L)
                .attribute("server_id", 1)
                .attribute("type", "text")
                .build();

        var info = service.process(inbound);
        Assertions.assertNull(info,
                "newsletter dispatch + no <plaintext> then null, not a chat-decryption throw");
    }

    private static MessageDecryption decryption(LinkedWhatsAppStore store) {
        return new MessageDecryption(store,
                new SignalSessionCipher(store.signalStore()),
                new SignalGroupCipher(store.signalStore()),
                new SignalCryptoLocks());
    }

    private static Stanza buildInbound(String id, Jid fromJid, String encType, byte[] ciphertext) {
        return new StanzaBuilder()
                .description("message")
                .attribute("id", id)
                .attribute("from", fromJid)
                // pins a deterministic stanza age so the receiver's expired-status branch never fires
                .attribute("t", 1700000000L)
                .attribute("type", "text")
                .content(new StanzaBuilder()
                        .description("enc")
                        .attribute("v", "2")
                        .attribute("type", encType)
                        .content(ciphertext)
                        .build())
                .build();
    }
}
