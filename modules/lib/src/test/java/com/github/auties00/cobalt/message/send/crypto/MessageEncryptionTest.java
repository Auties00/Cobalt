package com.github.auties00.cobalt.message.send.crypto;

import com.github.auties00.cobalt.message.MessageEncryptionType;
import com.github.auties00.cobalt.message.MessageFixtures;
import com.github.auties00.cobalt.message.TestSignalSession;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.message.crypto.SignalCryptoLocks;
import com.github.auties00.libsignal.SignalSessionCipher;
import com.github.auties00.libsignal.groups.SignalGroupCipher;
import com.github.auties00.libsignal.protocol.SignalPreKeyMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip and structural tests for {@link MessageEncryption}, the send-side
 * Signal encryption service.
 *
 * <p>Each case sets up a real libsignal session between two
 * {@link MessageFixtures#temporaryStore} instances via
 * {@link TestSignalSession#establishSession}, encrypts on the sender, and where
 * a round-trip is asserted decrypts on the recipient with a fresh
 * {@link SignalSessionCipher} to prove byte equality. There are no mocks; the
 * only synthesis is the pair of stores.
 */
@DisplayName("MessageEncryption")
class MessageEncryptionTest {

    private static final Jid SENDER_JID = Jid.of("12025550100:0@s.whatsapp.net");
    private static final Jid RECIPIENT_JID = Jid.of("19254863482:0@s.whatsapp.net");

    @Test
    @DisplayName("encryptForDevice: first send produces a PKMSG envelope with v=2")
    void firstSendIsPreKeyMessage() throws Exception {
        var sender = MessageFixtures.temporaryStore(SENDER_JID, null);
        var recipient = MessageFixtures.temporaryStore(RECIPIENT_JID, null);
        TestSignalSession.establishSession(sender, RECIPIENT_JID, recipient);

        var encryption = encryption(sender);
        var payload = encryption.encryptForDevice(RECIPIENT_JID, "hello".getBytes());

        assertEquals(MessageEncryptionType.PKMSG, payload.type(),
                "first send to a freshly-established session is a PKMSG envelope");
        assertTrue(payload.isPreKeyMessage());
        assertEquals(2, MessageEncryption.CIPHERTEXT_VERSION,
                "CIPHERTEXT_VERSION is the v=2 wire attribute");
    }

    @Test
    @DisplayName("encryptForDevice: ciphertext is non-empty and longer than plaintext")
    void ciphertextLargerThanPlaintext() {
        var sender = MessageFixtures.temporaryStore(SENDER_JID, null);
        var recipient = MessageFixtures.temporaryStore(RECIPIENT_JID, null);
        TestSignalSession.establishSession(sender, RECIPIENT_JID, recipient);

        var encryption = encryption(sender);
        var plaintext = "hello world".getBytes();
        var payload = encryption.encryptForDevice(RECIPIENT_JID, plaintext);

        assertTrue(payload.ciphertext().length > plaintext.length,
                "ciphertext must be larger than plaintext (random padding + Signal protocol overhead)");
    }

    @Test
    @DisplayName("encryptForDevice round-trip: recipient decrypts to plaintext modulo padding")
    void roundTripDecrypt() throws Exception {
        var sender = MessageFixtures.temporaryStore(SENDER_JID, null);
        var recipient = MessageFixtures.temporaryStore(RECIPIENT_JID, null);
        TestSignalSession.establishSession(sender, RECIPIENT_JID, recipient);

        var encryption = encryption(sender);
        var plaintext = "hello".getBytes();
        var payload = encryption.encryptForDevice(RECIPIENT_JID, plaintext);

        // ofSerialized strips the 1-byte version prefix first; SignalPreKeyMessageSpec.decode would read it as protobuf and reject the wire type
        var preKey = SignalPreKeyMessage.ofSerialized(payload.ciphertext());
        var recipientCipher = new SignalSessionCipher(recipient.signalStore());
        var recovered = recipientCipher.decrypt(SENDER_JID.toSignalAddress(), preKey);

        var padding = recovered.length - plaintext.length;
        assertTrue(padding >= 1 && padding <= 16,
                "MessageEncryption.addPadding adds 1..16 bytes; got " + padding);
        var stripped = new byte[plaintext.length];
        System.arraycopy(recovered, 0, stripped, 0, plaintext.length);
        assertArrayEquals(plaintext, stripped,
                "decrypted plaintext must equal the input (modulo trailing padding)");
    }

    @Test
    @DisplayName("encryptForDevice: each call samples fresh padding and ratchet, producing different ciphertexts")
    void ciphertextIsFresh() {
        var sender = MessageFixtures.temporaryStore(SENDER_JID, null);
        var recipient = MessageFixtures.temporaryStore(RECIPIENT_JID, null);
        TestSignalSession.establishSession(sender, RECIPIENT_JID, recipient);

        var encryption = encryption(sender);
        var first = encryption.encryptForDevice(RECIPIENT_JID, "x".getBytes());
        var second = encryption.encryptForDevice(RECIPIENT_JID, "x".getBytes());
        assertNotEquals(toHex(first.ciphertext()), toHex(second.ciphertext()),
                "Signal ratchet advances per encrypt; identical plaintext yields different ciphertext");
    }

    @Test
    @DisplayName("encryptForDevice: type stays PKMSG until the recipient processes the prekey")
    void typeSwitchesAfterFirstAck() {
        var sender = MessageFixtures.temporaryStore(SENDER_JID, null);
        var recipient = MessageFixtures.temporaryStore(RECIPIENT_JID, null);
        TestSignalSession.establishSession(sender, RECIPIENT_JID, recipient);
        var encryption = encryption(sender);

        var first = encryption.encryptForDevice(RECIPIENT_JID, "first".getBytes());
        assertEquals(MessageEncryptionType.PKMSG, first.type());

        // Encrypting alone never advances past PKMSG; libsignal tracks "recipient processed our prekey?" in session metadata, which only flips on a reply
        var secondBeforeAck = encryption.encryptForDevice(RECIPIENT_JID, "second".getBytes());
        assertEquals(MessageEncryptionType.PKMSG, secondBeforeAck.type(),
                "encrypt-without-ack stays in PKMSG until the recipient processes the prekey");
    }

    @Test
    @DisplayName("encryptForDevice: matching device JIDs are required (null throws NullPointerException)")
    void encryptForDeviceNullArgs() {
        var sender = MessageFixtures.temporaryStore(SENDER_JID, null);
        var encryption = encryption(sender);
        assertThrows(NullPointerException.class,
                () -> encryption.encryptForDevice(null, new byte[]{1}));
        assertThrows(NullPointerException.class,
                () -> encryption.encryptForDevice(RECIPIENT_JID, null));
    }

    @Test
    @DisplayName("CIPHERTEXT_VERSION constant: pinned at 2; drives the v=2 attribute on every <enc>")
    void ciphertextVersionPinned() {
        assertEquals(2, MessageEncryption.CIPHERTEXT_VERSION,
                "v=2 is the wire-byte invariant relied on by every stanza builder");
    }

    @Test
    @DisplayName("constructor: null collaborators throw NullPointerException")
    void constructorNullArgs() {
        var store = MessageFixtures.temporaryStore(SENDER_JID, null);
        var session = new SignalSessionCipher(store.signalStore());
        var group = new SignalGroupCipher(store.signalStore());
        var locks = new SignalCryptoLocks();
        assertThrows(NullPointerException.class, () -> new MessageEncryption(null, session, group, locks));
        assertThrows(NullPointerException.class, () -> new MessageEncryption(store, null, group, locks));
        assertThrows(NullPointerException.class, () -> new MessageEncryption(store, session, null, locks));
        assertThrows(NullPointerException.class, () -> new MessageEncryption(store, session, group, null));
    }

    @Test
    @DisplayName("payload classification: PKMSG flagged as preKey, not senderKey")
    void payloadFlagsConsistent() {
        var sender = MessageFixtures.temporaryStore(SENDER_JID, null);
        var recipient = MessageFixtures.temporaryStore(RECIPIENT_JID, null);
        TestSignalSession.establishSession(sender, RECIPIENT_JID, recipient);
        var payload = encryption(sender).encryptForDevice(RECIPIENT_JID, "x".getBytes());
        assertTrue(payload.isPreKeyMessage(),
                "PKMSG payloads must self-identify as preKey for the identity-on-PKMSG branch");
        assertFalse(payload.isSenderKeyMessage(),
                "PKMSG is not a sender-key message");
    }

    private static MessageEncryption encryption(LinkedWhatsAppStore store) {
        return new MessageEncryption(store, new SignalSessionCipher(store.signalStore()), new SignalGroupCipher(store.signalStore()), new SignalCryptoLocks());
    }

    private static String toHex(byte[] bytes) {
        var sb = new StringBuilder(bytes.length * 2);
        for (var b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
