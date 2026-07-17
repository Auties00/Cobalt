package com.github.auties00.cobalt.message.receive.crypto;

import com.github.auties00.cobalt.exception.linked.WhatsAppMessageException;
import com.github.auties00.cobalt.message.MessageEncryptionType;
import com.github.auties00.cobalt.message.MessageFixtures;
import com.github.auties00.cobalt.message.TestSignalSession;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.message.crypto.SignalCryptoLocks;
import com.github.auties00.libsignal.SignalSessionCipher;
import com.github.auties00.libsignal.groups.SignalGroupCipher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link MessageDecryption} device-to-device decryption: the PKMSG round-trip,
 * inbound session installation, malformed-ciphertext and null-argument handling, and
 * the {@link MessageDecryption#hasSessionWith(Jid)} probe. The sender side is driven by
 * {@link MessageEncryption} over a real libsignal session installed by
 * {@link TestSignalSession#establishSession}, and plaintext arguments are raw bytes so
 * assertions do not depend on protobuf encoding.
 */
@DisplayName("MessageDecryption")
class MessageDecryptionTest {

    private static final Jid SENDER_JID = Jid.of("12025550100:0@s.whatsapp.net");
    private static final Jid RECIPIENT_JID = Jid.of("19254863482:0@s.whatsapp.net");

    @Test
    @DisplayName("decryptFromDevice(PKMSG): recipient recovers the exact plaintext after the sender's first send")
    void roundTripPkmsg() {
        var sender = MessageFixtures.temporaryStore(SENDER_JID, null);
        var recipient = MessageFixtures.temporaryStore(RECIPIENT_JID, null);
        TestSignalSession.establishSession(sender, RECIPIENT_JID, recipient);

        var plaintext = "hello".getBytes();
        var payload = encryption(sender).encryptForDevice(RECIPIENT_JID, plaintext);

        var decryption = decryption(recipient);
        var recovered = decryption.decryptFromDevice(payload.ciphertext(), SENDER_JID, MessageEncryptionType.PKMSG);
        assertArrayEquals(plaintext, recovered,
                "decryptFromDevice strips Signal-protocol padding then output equals input byte-for-byte");
    }

    @Test
    @DisplayName("decryptFromDevice(PKMSG): installs the session on the recipient side (subsequent hasSessionWith returns true)")
    void pkmsgInstallsSession() {
        var sender = MessageFixtures.temporaryStore(SENDER_JID, null);
        var recipient = MessageFixtures.temporaryStore(RECIPIENT_JID, null);
        TestSignalSession.establishSession(sender, RECIPIENT_JID, recipient);

        var decryption = decryption(recipient);
        assertTrue(!decryption.hasSessionWith(SENDER_JID),
                "before any decrypt the recipient has no session with the sender");

        var payload = encryption(sender).encryptForDevice(RECIPIENT_JID, "first".getBytes());
        decryption.decryptFromDevice(payload.ciphertext(), SENDER_JID, MessageEncryptionType.PKMSG);

        assertTrue(decryption.hasSessionWith(SENDER_JID),
                "PKMSG decryption installs the inbound session then hasSessionWith must now be true");
    }

    @Test
    @DisplayName("decryptFromDevice: malformed ciphertext throws WhatsAppMessageException.Receive.InvalidMessage")
    void malformedCiphertextThrows() {
        var recipient = MessageFixtures.temporaryStore(RECIPIENT_JID, null);
        var decryption = decryption(recipient);
        var bogus = new byte[]{0x00, 0x01, 0x02, 0x03};
        assertThrows(WhatsAppMessageException.Receive.InvalidMessage.class,
                () -> decryption.decryptFromDevice(bogus, SENDER_JID, MessageEncryptionType.PKMSG));
    }

    @Test
    @DisplayName("decryptFromDevice: null arguments throw NullPointerException")
    void decryptFromDeviceNullArgs() {
        var recipient = MessageFixtures.temporaryStore(RECIPIENT_JID, null);
        var decryption = decryption(recipient);
        assertThrows(NullPointerException.class,
                () -> decryption.decryptFromDevice(null, SENDER_JID, MessageEncryptionType.PKMSG));
        assertThrows(NullPointerException.class,
                () -> decryption.decryptFromDevice(new byte[]{0}, null, MessageEncryptionType.PKMSG));
        assertThrows(NullPointerException.class,
                () -> decryption.decryptFromDevice(new byte[]{0}, SENDER_JID, null));
    }

    @Test
    @DisplayName("constructor: null collaborators throw NullPointerException")
    void constructorNullArgs() {
        var store = MessageFixtures.temporaryStore(RECIPIENT_JID, null);
        var session = new SignalSessionCipher(store.signalStore());
        var group = new SignalGroupCipher(store.signalStore());
        var locks = new SignalCryptoLocks();
        assertThrows(NullPointerException.class, () -> new MessageDecryption(null, session, group, locks));
        assertThrows(NullPointerException.class, () -> new MessageDecryption(store, null, group, locks));
        assertThrows(NullPointerException.class, () -> new MessageDecryption(store, session, null, locks));
        assertThrows(NullPointerException.class, () -> new MessageDecryption(store, session, group, null));
    }

    @Test
    @DisplayName("hasSessionWith(null): returns false (no NPE on null device JID)")
    void hasSessionWithNullDoesNotThrow() {
        var recipient = MessageFixtures.temporaryStore(RECIPIENT_JID, null);
        var decryption = decryption(recipient);
        assertTrue(!decryption.hasSessionWith(SENDER_JID),
                "fresh store has no sessions");
    }

    // Sender side: session and group ciphers share the supplied store so both ends keep the same protocol state.
    private static MessageEncryption encryption(LinkedWhatsAppStore store) {
        return new MessageEncryption(store, new SignalSessionCipher(store.signalStore()), new SignalGroupCipher(store.signalStore()), new SignalCryptoLocks());
    }

    // Recipient side: session and group ciphers share the supplied store so both ends keep the same protocol state.
    private static MessageDecryption decryption(LinkedWhatsAppStore store) {
        return new MessageDecryption(store, new SignalSessionCipher(store.signalStore()), new SignalGroupCipher(store.signalStore()), new SignalCryptoLocks());
    }
}
