package it.auties.whatsapp.crypto;

import it.auties.whatsapp.controller.WhatsappKeys;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.signal.message.SignalDistributionMessage;
import it.auties.whatsapp.model.signal.sender.SenderKeyName;
import it.auties.whatsapp.model.signal.sender.SenderKeyRecord;
import it.auties.whatsapp.util.Keys;
import it.auties.whatsapp.util.SignalSpecification;
import lombok.NonNull;

import java.util.concurrent.Semaphore;

public record GroupBuilder(@NonNull WhatsappKeys keys) implements SignalSpecification {
    public SignalDistributionMessage createOutgoing(SenderKeyName name) {
        var senderKeyRecord = keys.findSenderKeyByName(name);
        if (senderKeyRecord.isEmpty()) {
            senderKeyRecord.addState(
                    Keys.senderKeyId(),
                    0,
                    Keys.senderKey(),
                    SignalKeyPair.random()
            );
        }

        var state = senderKeyRecord.headState();
        return new SignalDistributionMessage(
                state.id(),
                state.chainKey().iteration(),
                state.chainKey().seed(),
                state.signingKey().encodedPublicKey()
        );
    }

    public void createIncoming(SenderKeyName name, SignalDistributionMessage message) {
        var senderKeyRecord = keys.findSenderKeyByName(name);
        senderKeyRecord.addState(message.id(), message.iteration(),
                message.chainKey(), message.signingKey());
    }
}
