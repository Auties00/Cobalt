package it.auties.whatsapp.socket.message.signal;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.model.signal.SignalAddress;
import it.auties.whatsapp.model.signal.key.SignalKeyDirection;
import it.auties.whatsapp.model.signal.key.SignalPublicKey;
import it.auties.whatsapp.model.signal.message.SignalMessage;
import it.auties.whatsapp.model.signal.message.SignalPreKeySignalMessage;
import it.auties.whatsapp.model.signal.ratchet.SignalChainKey;
import it.auties.whatsapp.model.signal.ratchet.SignalMessageKey;
import it.auties.whatsapp.model.signal.state.SignalSessionRecord;
import it.auties.whatsapp.model.signal.state.SignalSessionState;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.*;

public final class SignalSessionCipher {
    private static final int MAX_MESSAGE_KEYS = 2000;

    private final Keys keys;
    private final SignalSessionBuilder sessionBuilder;
    private final SignalAddress remoteAddress;

    public SignalSessionCipher(Keys keys, SignalSessionBuilder sessionBuilder, SignalAddress remoteAddress) {
        this.keys = keys;
        this.sessionBuilder = sessionBuilder;
        this.remoteAddress = remoteAddress;
    }

    public byte[] encrypt(byte[] paddedMessage) {
        var sessionRecord = keys.findSessionByAddress(remoteAddress).orElseGet(() -> {
            var record = new SignalSessionRecord();
            keys.addSession(remoteAddress, record);
            return record;
        });
        var sessionState = sessionRecord.sessionState();
        var chainKey = sessionState.senderChain().chainKey();
        var messageKeys = chainKey.toMessageKeys();
        var senderEphemeral = sessionState.senderChain().senderRatchetKey();
        int previousCounter = sessionState.previousCounter();
        int sessionVersion = sessionState.sessionVersion();

        byte[] ciphertextBody = getCiphertext(messageKeys, paddedMessage);
        var ciphertextMessage = new SignalMessage(
                sessionVersion,
                senderEphemeral,
                chainKey.index(),
                previousCounter,
                ciphertextBody,
                sessionState.localIdentityPublic(),
                sessionState.remoteIdentityPublic(),
                messageKeys.macKey()
        );

        if (sessionState.hasUnacknowledgedPreKeyMessage()) {
            var items = sessionState.getUnacknowledgedPreKeyMessageItems();
            int localRegistrationId = sessionState.localRegistrationId();

            ciphertextMessage = new SignalPreKeySignalMessage(
                    sessionVersion,
                    localRegistrationId,
                    items.preKeyId(),
                    items.signedKeyId(),
                    items.baseKey(),
                    sessionState.localIdentityPublic(),
                    ciphertextMessage
            );
        }

        sessionState.setSenderChain(chainKey.next());

        if (!keys.hasTrust(remoteAddress, sessionState.remoteIdentityPublic(), SignalKeyDirection.SENDING)) {
            throw new SecurityException("Untrusted identity: " + remoteAddress.name());
        }

        return ciphertextMessage.serialized();
    }

    public byte[] decrypt(SignalPreKeySignalMessage ciphertext) {
        var sessionRecord = keys.findSessionByAddress(remoteAddress).orElseGet(() -> {
            var record = new SignalSessionRecord();
            keys.addSession(remoteAddress, record);
            return record;
        });
        var unsignedPreKeyId = sessionBuilder.process(sessionRecord, ciphertext);
        byte[] plaintext = decrypt(sessionRecord, ciphertext.signalMessage());

        if (unsignedPreKeyId.isPresent()) {
            keys.removePreKey(unsignedPreKeyId.getAsInt());
        }

        return plaintext;
    }

    public byte[] decrypt(SignalMessage ciphertext) {
        if (!keys.hasSession(remoteAddress)) {
            throw new SecurityException("No session for: " + remoteAddress);
        }

        var sessionRecord = keys.findSessionByAddress(remoteAddress).orElseGet(() -> {
            var record = new SignalSessionRecord();
            keys.addSession(remoteAddress, record);
            return record;
        });
        byte[] plaintext = decrypt(sessionRecord, ciphertext);

        if (!keys.hasTrust(remoteAddress, sessionRecord.sessionState().remoteIdentityPublic(), SignalKeyDirection.INCOMING)) {
            throw new SecurityException("Untrusted identity: " + remoteAddress.name());
        }

        return plaintext;
    }

    private byte[] decrypt(SignalSessionRecord sessionRecord, SignalMessage ciphertext) {
        var previousStates = sessionRecord.previousSessionStates().iterator();
        var exceptions = new ArrayList<Exception>();

        try {
            var sessionState = sessionRecord.sessionState();
            byte[] plaintext = decrypt(sessionState, ciphertext);

            sessionRecord.setState(sessionState);
            return plaintext;
        } catch (RuntimeException e) {
            exceptions.add(e);
        }

        while (previousStates.hasNext()) {
            try {
                var promotedState = previousStates.next();
                byte[] plaintext = decrypt(promotedState, ciphertext);

                previousStates.remove();
                sessionRecord.promoteState(promotedState);

                return plaintext;
            } catch (RuntimeException e) {
                exceptions.add(e);
            }
        }

        throw new SecurityException("No valid sessions. Errors: " + exceptions.size());
    }

    private byte[] decrypt(SignalSessionState sessionState, SignalMessage ciphertextMessage) {
        if (!sessionState.hasSenderChain()) {
            throw new SecurityException("Uninitialized session!");
        }

        if (!Objects.equals(ciphertextMessage.version(), sessionState.sessionVersion())) {
            throw new SecurityException(String.format("Message version %d, but session version %d",
                    ciphertextMessage.version(),
                    sessionState.sessionVersion()));
        }

        var theirEphemeral = ciphertextMessage.senderRatchetKey();
        int counter = ciphertextMessage.counter();
        var chainKey = getOrCreateChainKey(sessionState, theirEphemeral);
        var messageKeys = getOrCreateMessageKeys(sessionState, theirEphemeral, chainKey, counter);

        // Verify MAC using available methods from SignalMessage
        // This would need to be implemented based on the actual SignalMessage API

        byte[] plaintext = getPlaintext(messageKeys, ciphertextMessage.ciphertext());

        sessionState.clearUnacknowledgedPreKeyMessage();

        return plaintext;
    }

    private SignalChainKey getOrCreateChainKey(SignalSessionState sessionState, SignalPublicKey theirEphemeral) {
        try {
            if (sessionState.hasReceiverChain(theirEphemeral)) {
                return sessionState.getReceiverChainKey(theirEphemeral);
            } else {
                var rootKey = sessionState.getRootKey();
                var ourEphemeral = sessionState.getSenderRatchetKeyPair();
                var receiverChain = rootKey.createChain(theirEphemeral, ourEphemeral);
                var ourNewEphemeral = SignalKeyPair.random();
                var senderChain = receiverChain.first().createChain(theirEphemeral, ourNewEphemeral);

                sessionState.setRootKey(senderChain.first());
                sessionState.addReceiverChain(theirEphemeral, receiverChain.second());
                sessionState.setPreviousCounter(Math.max(sessionState.getSenderChainKey().index() - 1, 0));
                sessionState.setSenderChain(ourNewEphemeral, senderChain.second());

                return receiverChain.second();
            }
        } catch (Exception e) {
            throw new SecurityException("Invalid key", e);
        }
    }

    private SignalMessageKey getOrCreateMessageKeys(SignalSessionState sessionState,
                                                    SignalPublicKey theirEphemeral,
                                                    SignalChainKey chainKey, int counter) {
        if (chainKey.index() > counter) {
            if (sessionState.hasMessageKeys(theirEphemeral, counter)) {
                return sessionState.removeMessageKeys(theirEphemeral, counter);
            } else {
                throw new SecurityException("Received message with old counter: " +
                        chainKey.index() + " , " + counter);
            }
        }

        if (counter - chainKey.index() > MAX_MESSAGE_KEYS) {
            throw new SecurityException("Over " + MAX_MESSAGE_KEYS + " messages into the future!");
        }

        while (chainKey.index() < counter) {
            var messageKeys = chainKey.toMessageKeys();
            sessionState.setMessageKeys(theirEphemeral, messageKeys);
            chainKey = chainKey.next();
        }

        sessionState.setReceiverChainKey(theirEphemeral, chainKey.next());
        return chainKey.toMessageKeys();
    }

    private byte[] getCiphertext(SignalMessageKey messageKeys, byte[] plaintext) {
        try {
            var cipher = getCipher(Cipher.ENCRYPT_MODE, messageKeys.cipherKey(), messageKeys.iv());
            return cipher.doFinal(plaintext);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    private byte[] getPlaintext(SignalMessageKey messageKeys, byte[] cipherText) {
        try {
            var cipher = getCipher(Cipher.DECRYPT_MODE, messageKeys.cipherKey(), messageKeys.iv());
            return cipher.doFinal(cipherText);
        } catch (GeneralSecurityException e) {
            throw new SecurityException("Decryption failed", e);
        }
    }

    private Cipher getCipher(int mode, SecretKeySpec key, IvParameterSpec iv) {
        try {
            var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(mode, key, iv);
            return cipher;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Cannot initialize cipher", e);
        }
    }
}