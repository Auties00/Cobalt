package it.auties.whatsapp.socket.message.signal;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.model.signal.group.SignalSenderKeyName;
import it.auties.whatsapp.model.signal.group.state.SignalSenderKeyRecord;
import it.auties.whatsapp.model.signal.key.SignalKeyPair;
import it.auties.whatsapp.model.signal.message.SignalSenderKeyDistributionMessage;
import it.auties.whatsapp.model.signal.message.SenderKeyDistributionMessageBuilder;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class SignalGroupSessionBuilder {
    private final Keys keys;

    public SignalGroupSessionBuilder(Keys keys) {
        this.keys = keys;
    }

    public void process(SignalSenderKeyName senderKeyName, SignalSenderKeyDistributionMessage senderKeyDistributionMessage) {
        var senderKeyRecord = keys.findSenderKeyByName(senderKeyName).orElseGet(() -> {
            var record = new SignalSenderKeyRecord();
            keys.addSenderKey(senderKeyName, record);
            return record;
        });
        senderKeyRecord.addSenderKeyState(
                senderKeyDistributionMessage.id(),
                senderKeyDistributionMessage.iteration(),
                senderKeyDistributionMessage.chainKey(),
                senderKeyDistributionMessage.signatureKey()
        );
    }

    public SignalSenderKeyDistributionMessage create(SignalSenderKeyName senderKeyName) throws NoSuchAlgorithmException {
        var senderKeyRecord = keys.findSenderKeyByName(senderKeyName).orElseGet(() -> {
            var record = new SignalSenderKeyRecord();
            keys.addSenderKey(senderKeyName, record);
            return record;
        });

        if (senderKeyRecord.isEmpty()) {
            var random = SecureRandom.getInstanceStrong();
            var senderKeyId = random.nextInt(Integer.MAX_VALUE);
            var senderKey = new byte[32];
            random.nextBytes(senderKey);
            var signatureKey = SignalKeyPair.random().publicKey();
            senderKeyRecord.setSenderKeyState(
                    senderKeyId,
                    0,
                    senderKey,
                    signatureKey
            );
        }

        var state = senderKeyRecord.findSenderKeyState()
                .orElseThrow(() -> new IllegalStateException("No sender key state found"));

        return new SenderKeyDistributionMessageBuilder()
                .id(state.id())
                .iteration(state.chainKey().iteration())
                .chainKey(state.chainKey().seed())
                .signatureKey(state.signatureKey().publicKey())
                .build();
    }
}
