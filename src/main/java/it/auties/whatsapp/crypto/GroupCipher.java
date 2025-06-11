package it.auties.whatsapp.crypto;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.model.signal.message.SenderKeyMessage;
import it.auties.whatsapp.model.signal.sender.SenderKeyName;
import it.auties.whatsapp.model.signal.sender.SenderKeyState;
import it.auties.whatsapp.model.signal.sender.SenderMessageKey;
import it.auties.whatsapp.util.SignalConstants;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.NoSuchElementException;

import static it.auties.whatsapp.util.SignalConstants.CURRENT_VERSION;

public final class GroupCipher {
    private final SenderKeyName name;
    private final Keys keys;

    public GroupCipher(SenderKeyName name, Keys keys) {
        this.name = name;
        this.keys = keys;
    }

    public CipheredMessageResult encrypt(byte[] data) {
        try {
            var currentState = keys.findSenderKeyByName(name).firstState();
            var messageKey = currentState.chainKey().toMessageKey();
            var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            var keySpec = new SecretKeySpec(messageKey.cipherKey(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(messageKey.iv()));
            var ciphertext = cipher.doFinal(data);
            var senderKeyMessage = new SenderKeyMessage(
                    CURRENT_VERSION,
                    currentState.id(),
                    messageKey.iteration(),
                    ciphertext,
                    currentState.signingKey().privateKey()
            );
            var next = currentState.chainKey().next();
            currentState.setChainKey(next);
            return new CipheredMessageResult(senderKeyMessage.serialized(), SignalConstants.SKMSG);
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot encrypt data", exception);
        }
    }

    public byte[] decrypt(byte[] data) {
        var record = keys.findSenderKeyByName(name);
        var senderKeyMessage = SenderKeyMessage.ofSerialized(data);
        var senderKeyStates = record.findStatesById(senderKeyMessage.id());
        for (var senderKeyState : senderKeyStates) {
            try {
                var senderKey = getSenderKey(senderKeyState, senderKeyMessage.iteration());
                try {
                    var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    var keySpec = new SecretKeySpec(senderKey.cipherKey(), "AES");
                    cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(senderKey.iv()));
                    return cipher.doFinal(senderKeyMessage.cipherText());
                } catch (GeneralSecurityException exception) {
                    throw new IllegalArgumentException("Cannot decrypt data", exception);
                }
            } catch (Throwable ignored) {

            }
        }
        throw new RuntimeException("Cannot decode message with any session");
    }

    private SenderMessageKey getSenderKey(SenderKeyState senderKeyState, int iteration) {
        if (senderKeyState.chainKey().iteration() > iteration) {
            return senderKeyState.findSenderMessageKey(iteration)
                    .orElseThrow(() -> new NoSuchElementException("Received message with old counter: got %s, expected more than %s".formatted(iteration, senderKeyState.chainKey().iteration())));
        }
        var lastChainKey = senderKeyState.chainKey();
        while (lastChainKey.iteration() < iteration) {
            senderKeyState.addSenderMessageKey(lastChainKey.toMessageKey());
            lastChainKey = lastChainKey.next();
        }
        senderKeyState.setChainKey(lastChainKey.next());
        return lastChainKey.toMessageKey();
    }
}
