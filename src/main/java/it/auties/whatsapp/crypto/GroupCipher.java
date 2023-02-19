package it.auties.whatsapp.crypto;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.model.signal.message.SenderKeyMessage;
import it.auties.whatsapp.model.signal.sender.SenderKeyName;
import it.auties.whatsapp.model.signal.sender.SenderKeyState;
import it.auties.whatsapp.model.signal.sender.SenderMessageKey;
import lombok.NonNull;

import java.util.NoSuchElementException;

import static java.util.Map.of;

public record GroupCipher(@NonNull SenderKeyName name, @NonNull Keys keys) {
    public Node encrypt(byte[] data) {
        var currentState = keys.findSenderKeyByName(name).findState();
        var messageKey = currentState.chainKey().toMessageKey();
        var ciphertext = AesCbc.encrypt(messageKey.iv(), data, messageKey.cipherKey());
        var senderKeyMessage = new SenderKeyMessage(currentState.id(), messageKey.iteration(), ciphertext, currentState.signingKey()
                .privateKey());
        var next = currentState.chainKey().next();
        currentState.chainKey(next);
        return Node.of("enc", of("v", "2", "type", "skmsg"), senderKeyMessage.serialized());
    }

    public byte[] decrypt(byte[] data) {
        var record = keys.findSenderKeyByName(name);
        var senderKeyMessage = SenderKeyMessage.ofSerialized(data);
        var senderKeyStates = record.findStateById(senderKeyMessage.id());
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
                    .orElseThrow(() -> new NoSuchElementException("Received message with old counter: got %s, expected > %s".formatted(iteration, senderKeyState.chainKey()
                            .iteration())));
        }
        var lastChainKey = senderKeyState.chainKey();
        while (lastChainKey.iteration() < iteration) {
            senderKeyState.addSenderMessageKey(lastChainKey.toMessageKey());
            lastChainKey = lastChainKey.next();
        }
        senderKeyState.chainKey(lastChainKey.next());
        return lastChainKey.toMessageKey();
    }
}
