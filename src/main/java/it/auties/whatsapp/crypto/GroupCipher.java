package it.auties.whatsapp.crypto;

import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.protobuf.signal.sender.SenderKeyMessage;
import it.auties.whatsapp.protobuf.signal.sender.SenderKeyName;
import it.auties.whatsapp.protobuf.signal.sender.SenderKeyState;
import it.auties.whatsapp.protobuf.signal.sender.SenderMessageKey;
import it.auties.whatsapp.util.Validate;
import lombok.NonNull;

import java.util.NoSuchElementException;

public record GroupCipher(@NonNull SenderKeyName name, @NonNull WhatsappKeys keys) {
    public byte[] encrypt(byte[] data) {
        var record = keys.findSenderKeyByName(name)
                .orElseThrow(() -> new NoSuchElementException("Missing record for name: %s".formatted(name)));
        var messageKey = record.currentState().chainKey().toSenderMessageKey();
        var ciphertext = AesCbc.encrypt(
                messageKey.iv(),
                data,
                messageKey.cipherKey()
        );

        var senderKeyMessage = new SenderKeyMessage(
                record.currentState().id(),
                messageKey.iteration(),
                ciphertext,
                record.currentState().signingKeyPrivate()
        );

        record.currentState().chainKey(record.currentState().chainKey().next());
        keys.addSenderKey(name, record);
        return senderKeyMessage.serialized();
    }

    public byte[] decrypt(byte[] data) {
        var record = keys.findSenderKeyByName(name)
                .orElseThrow(() -> new NoSuchElementException("Missing record for name: %s".formatted(name)));
        var senderKeyMessage = SenderKeyMessage.ofEncoded(data);
        var senderKeyState = record.findStateById(senderKeyMessage.id());
        var senderKey = getSenderKey(senderKeyState, senderKeyMessage.iteration());
        var plaintext = AesCbc.decrypt(
                senderKey.iv(),
                senderKeyMessage.cipherText(),
                senderKey.cipherKey()
        );

        keys.addSenderKey(name, record);
        return plaintext;
    }

    private SenderMessageKey getSenderKey(SenderKeyState senderKeyState, int iteration) {
        if (senderKeyState.chainKey().iteration() > iteration) {
            Validate.isTrue(senderKeyState.hasSenderMessageKey(iteration),
                    "Received message with old counter: %s, %s",
                    senderKeyState.chainKey().iteration(), iteration);
            return senderKeyState.removeSenderMessageKey(iteration);
        }

        Validate.isTrue(senderKeyState.chainKey().iteration() - iteration <= 2000,
                "Message overflow: expected <= 2000, got %s", senderKeyState.chainKey().iteration() - iteration);

        var lastChainKey = senderKeyState.chainKey();
        while (lastChainKey.iteration() < iteration) {
            senderKeyState.addSenderMessageKey(lastChainKey.toSenderMessageKey());
            lastChainKey = lastChainKey.next();
        }

        senderKeyState.chainKey(lastChainKey.next());
        return lastChainKey.toSenderMessageKey();
    }
}
