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

import static it.auties.whatsapp.model.request.Node.with;
import static java.util.Map.of;

public record GroupCipher(@NonNull SenderKeyName name, @NonNull WhatsappKeys keys) implements SignalSpecification{
    public Node encrypt(byte[] data) {
        var currentState = keys.findSenderKeyByName(name)
                .headState();
        var messageKey = currentState.chainKey()
                .toMessageKey();

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
    }

    public byte[] decrypt(byte[] data) {
        var record = keys.findSenderKeyByName(name);
        var senderKeyMessage = SenderKeyMessage.ofSerialized(data);
        var senderKeyState = record.findStateById(senderKeyMessage.id());
        var senderKey = getSenderKey(senderKeyState, senderKeyMessage.iteration());
        return AesCbc.decrypt(
                senderKey.iv(),
                senderKeyMessage.cipherText(),
                senderKey.cipherKey()
        );
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
            senderKeyState.addSenderMessageKey(lastChainKey.toMessageKey());
            lastChainKey = lastChainKey.next();
        }

        senderKeyState.chainKey(lastChainKey.next());
        return lastChainKey.toMessageKey();
    }
}
