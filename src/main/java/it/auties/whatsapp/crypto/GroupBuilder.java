package it.auties.whatsapp.crypto;

import it.auties.whatsapp.controller.WhatsappKeys;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.signal.message.SignalDistributionMessage;
import it.auties.whatsapp.model.signal.sender.SenderKeyName;
import it.auties.whatsapp.util.Keys;
import it.auties.whatsapp.util.SignalSpecification;
import lombok.NonNull;

public record GroupBuilder(@NonNull WhatsappKeys keys) implements SignalSpecification {
    public byte[] createOutgoing(SenderKeyName name) {
        var record = keys.findSenderKeyByName(name);
        if (record.isEmpty()) {
            record.addState(
                    Keys.senderKeyId(),
                    0,
                    Keys.senderKey(),
                    SignalKeyPair.random()
            );
        }

        var state = record.headState();
        var message = new SignalDistributionMessage(
                state.id(),
                state.chainKey().iteration(),
                state.chainKey().seed(),
                state.signingKey().encodedPublicKey()
        );

        return message.serialized();
    }

    public void createIncoming(SenderKeyName name, SignalDistributionMessage message) {
        var record = keys.findSenderKeyByName(name);
        record.addState(message.id(), message.iteration(),
                message.chainKey(), message.signingKey());
    }
}
