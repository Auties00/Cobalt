package it.auties.whatsapp.crypto;

import it.auties.whatsapp.controller.WhatsappKeys;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.model.signal.sender.SenderKeyMessage;
import it.auties.whatsapp.model.signal.sender.SenderKeyName;
import it.auties.whatsapp.model.signal.sender.SenderKeyState;
import it.auties.whatsapp.model.signal.sender.SenderMessageKey;
import it.auties.whatsapp.util.SignalSpecification;
import it.auties.whatsapp.util.Validate;
import lombok.NonNull;

import java.util.NoSuchElementException;
import java.util.concurrent.Semaphore;

import static it.auties.whatsapp.model.request.Node.with;
import static java.util.Map.of;

public record GroupCipher(@NonNull SenderKeyName name, @NonNull WhatsappKeys keys) implements SignalSpecification{
    private static final Semaphore ENCRYPTION_SEMAPHORE  = new Semaphore(1);
    public Node encrypt(byte[] data) {
        try {
            ENCRYPTION_SEMAPHORE.acquire();
            var currentState = keys.findSenderKeyByName(name)
                    .headState();
            var messageKey = currentState.chainKey()
                    .toSenderMessageKey();
            var ciphertext = AesCbc.encrypt(
                    messageKey.iv(),
                    data,
                    messageKey.cipherKey()
            );

            var senderKeyMessage = new SenderKeyMessage(
                    currentState.id(),
                    messageKey.iteration(),
                    ciphertext,
                    currentState.signingKey().privateKey()
            );

            var next = currentState.chainKey().next();
            currentState.chainKey(next);
            return with("enc", of("v", "2", "type", "skmsg"),
                    senderKeyMessage.serialized());
        }catch (Throwable throwable){
            throw new RuntimeException("Cannot encrypt message: an exception occured", throwable);
        }finally {
            ENCRYPTION_SEMAPHORE.release();
        }
    }

    public byte[] decrypt(byte[] data) {
        var record = keys.findSenderKeyByName(name);
        var senderKeyMessage = SenderKeyMessage.ofSerialized(data);
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
