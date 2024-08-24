package it.auties.whatsapp.crypto;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.model.signal.message.SenderKeyMessage;
import it.auties.whatsapp.model.signal.sender.SenderKeyName;
import it.auties.whatsapp.model.signal.sender.SenderKeyState;
import it.auties.whatsapp.model.signal.sender.SenderMessageKey;
import it.auties.whatsapp.util.SignalConstants;

import java.util.NoSuchElementException;

public record GroupCipher(SenderKeyName name, Keys keys) {
    public CipheredMessageResult encrypt(byte[] data) {
        if (data == null) {
            return new CipheredMessageResult(null, SignalConstants.UNAVAILABLE);
        }

        var currentState = keys.findSenderKeyByName(name).firstState();
        var messageKey = currentState.chainKey().toMessageKey();
        var ciphertext = AesCbc.encrypt(messageKey.iv(), data, messageKey.cipherKey());
        var senderKeyMessage = new SenderKeyMessage(currentState.id(), messageKey.iteration(), ciphertext, currentState.signingKey().privateKey());
        var next = currentState.chainKey().next();
        currentState.setChainKey(next);
        return new CipheredMessageResult(senderKeyMessage.serialized(), SignalConstants.SKMSG);
    }

    public byte[] decrypt(byte[] data) {
        var record = keys.findSenderKeyByName(name);
        var senderKeyMessage = SenderKeyMessage.ofSerialized(data);
        var senderKeyStates = record.findStatesById(senderKeyMessage.id());
        for (var senderKeyState : senderKeyStates) {
            try {
                var senderKey = getSenderKey(senderKeyState, senderKeyMessage.iteration());
                return AesCbc.decrypt(senderKey.iv(), senderKeyMessage.cipherText(), senderKey.cipherKey());
            } catch (Throwable ignored) {
            }
        }
        throw new RuntimeException("Cannot decode message with any session");
    }

    private SenderMessageKey getSenderKey(SenderKeyState senderKeyState, int iteration) {
        if (senderKeyState.chainKey().iteration() > iteration) {
            return senderKeyState.findSenderMessageKey(iteration)
                    .orElseThrow(() -> new NoSuchElementException("Received message with old counter: got %s, expected more than %s".formatted(iteration, senderKeyState.chainKey()
                            .iteration())));
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
