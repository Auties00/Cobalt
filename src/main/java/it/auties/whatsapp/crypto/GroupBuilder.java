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
    private static final Semaphore ENCRYPTION_SEMAPHORE = new Semaphore(1);

    public SignalDistributionMessage createOutgoing(SenderKeyName name) {
        try {
            ENCRYPTION_SEMAPHORE.acquire();
            var senderKeyRecord = keys.findSenderKeyByName(name)
                    .orElseGet(() -> createRecord(name));
            if (senderKeyRecord.isEmpty()) {
                var signingKey = SignalKeyPair.random();
                senderKeyRecord.addState(
                        Keys.senderKeyId(),
                        0,
                        Keys.senderKey(),
                        signingKey.publicKey(),
                        signingKey.privateKey()
                );
            }

            var state = senderKeyRecord.currentState();
            return new SignalDistributionMessage(
                    state.id(),
                    state.chainKey().iteration(),
                    state.chainKey().seed(),
                    state.signingKey().publicKey()
            );
        }catch (Throwable throwable){
            throw new RuntimeException("Cannot create outgoing: an exception occured", throwable);
        }finally {
            ENCRYPTION_SEMAPHORE.release();
        }
    }

    public void createIncoming(SenderKeyName name, SignalDistributionMessage message) {
        var senderKeyRecord = keys.findSenderKeyByName(name)
                .orElseGet(() -> createRecord(name));
        senderKeyRecord.addState(message.id(), message.iteration(),
                message.chainKey(), message.signingKey());
        keys.addSenderKey(name, senderKeyRecord);
    }
    private SenderKeyRecord createRecord(SenderKeyName name) {
        var record = new SenderKeyRecord();
        keys.addSenderKey(name, record);
        return record;
    }
}
