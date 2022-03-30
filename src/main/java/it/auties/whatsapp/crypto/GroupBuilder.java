package it.auties.whatsapp.crypto;

import it.auties.whatsapp.controller.WhatsappKeys;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.signal.message.SignalDistributionMessage;
import it.auties.whatsapp.model.signal.sender.SenderKeyName;
import it.auties.whatsapp.model.signal.sender.SenderKeyRecord;
import it.auties.whatsapp.util.Keys;
import it.auties.whatsapp.util.SignalSpecification;
import lombok.NonNull;

public record GroupBuilder(@NonNull WhatsappKeys keys) implements SignalSpecification {
    public void process(SenderKeyName name, SignalDistributionMessage message) {
        var senderKeyRecord = keys.findSenderKeyByName(name)
                .orElseGet(() -> createRecord(name));
        senderKeyRecord.addState(message.id(), message.iteration(),
                message.chainKey(), message.signingKey());
        keys.addSenderKey(name, senderKeyRecord);
    }

    public SignalDistributionMessage createMessage(SenderKeyName name) {
        var senderKeyRecord = keys.findSenderKeyByName(name)
                .orElseGet(() -> createRecord(name));
        if (senderKeyRecord.isEmpty()) {
            var keyId = Keys.senderKeyId();
            var senderKey = Keys.senderKey();
            var signingKey = SignalKeyPair.random();
            senderKeyRecord.addState(keyId, 0, senderKey, signingKey.publicKey(), signingKey.privateKey());
            keys.addSenderKey(name, senderKeyRecord);
        }

        var state = senderKeyRecord.currentState();
        return new SignalDistributionMessage(state.id(), state.chainKey().iteration(),
                state.chainKey().seed(), state.signingKeyPublic());
    }

    private SenderKeyRecord createRecord(SenderKeyName name) {
        var record = new SenderKeyRecord();
        keys.addSenderKey(name, record);
        return record;
    }
}
