package it.auties.whatsapp.crypto;

import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.protobuf.signal.sender.*;
import it.auties.whatsapp.util.Validate;
import lombok.NonNull;

import java.util.NoSuchElementException;

public record GroupCipher(@NonNull SenderKeyName name, @NonNull WhatsappKeys keys) {
    public byte[] encrypt(byte[] data) {
        var record = keys.findSenderKeyByName(name)
                .orElseThrow(() -> new NoSuchElementException("Missing record"));
        var messageKey = record.senderKeyState().chainKey().toSenderMessageKey();
        var ciphertext = AesCbc.cipher(
                messageKey.iv(),
                messageKey.cipherKey(),
                data,
                true
        );

        var senderKeyMessage = new SenderKeyMessage(
                record.senderKeyState().id(),
                messageKey.iteration(),
                ciphertext,
                record.senderKeyState().signingKeyPrivate()
        );

        record.senderKeyState().chainKey(record.senderKeyState().chainKey().next());
        keys.addSenderKey(name, record);
        return senderKeyMessage.serialized();
    }

    public byte[] decrypt(byte[] data) {
        var record = keys.findSenderKeyByName(name)
                .orElseThrow(() -> new NoSuchElementException("Missing record"));
        var senderKeyMessage = SenderKeyMessage.ofEncoded(data);
        var senderKeyState = record.senderKeyState(senderKeyMessage.id());
        var senderKey = getSenderKey(senderKeyState, senderKeyMessage.iteration());
        var plaintext = AesCbc.cipher(
                senderKey.iv(),
                senderKey.cipherKey(),
                senderKeyMessage.cipherText(),
                false
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
