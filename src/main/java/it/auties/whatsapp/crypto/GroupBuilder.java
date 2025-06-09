package it.auties.whatsapp.crypto;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.signal.message.SignalDistributionMessage;
import it.auties.whatsapp.model.signal.sender.SenderKeyName;
import it.auties.whatsapp.util.Bytes;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import static it.auties.whatsapp.util.SignalConstants.CURRENT_VERSION;

public final class GroupBuilder {
    private final Keys keys;

    public GroupBuilder(Keys keys) {
        this.keys = keys;
    }

    public byte[] createOutgoing(SenderKeyName name) {
        var record = keys.findSenderKeyByName(name);
        if (record.isEmpty()) {
            record.addState(randomId(), SignalKeyPair.random(), 0, Bytes.random(32));
        }
        var state = record.firstState();
        var message = new SignalDistributionMessage(
                CURRENT_VERSION,
                state.id(),
                state.chainKey().iteration(),
                state.chainKey().seed(),
                state.signingKey().signalPublicKey()
        );
        return message.serialized();
    }

    private int randomId() {
        try {
            return SecureRandom.getInstanceStrong()
                    .nextInt();
        }catch (GeneralSecurityException exception) {
            return new SecureRandom()
                    .nextInt();
        }
    }

    public void createIncoming(SenderKeyName name, SignalDistributionMessage message) {
        var record = keys.findSenderKeyByName(name);
        record.addState(message.id(), message.signingKey(), message.iteration(), message.chainKey());
    }
}
