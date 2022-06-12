package it.auties.whatsapp.model.signal.sender;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.UINT32;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class SenderKeyState implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = UINT32)
    private Integer id;

    @ProtobufProperty(index = 2, type = MESSAGE, concreteType = SenderChainKey.class)
    private SenderChainKey chainKey;

    @ProtobufProperty(index = 3, type = MESSAGE, concreteType = SenderSigningKey.class)
    private SenderSigningKey signingKey;

    @ProtobufProperty(index = 4, type = MESSAGE, concreteType = SenderMessageKey.class, repeated = true)
    @Default
    private CopyOnWriteArrayList<SenderMessageKey> messageKeys = new CopyOnWriteArrayList<>(); // A map would be better but the proto says otherwise

    public SenderKeyState(int id, int iteration, byte[] seed, SignalKeyPair signingKey) {
        this.id = id;
        this.chainKey = new SenderChainKey(iteration, seed);
        this.signingKey = new SenderSigningKey(signingKey.encodedPublicKey(), signingKey.privateKey());
        this.messageKeys = new CopyOnWriteArrayList<>();
    }

    public boolean hasSenderMessageKey(int iteration) {
        return messageKeys.stream()
                .anyMatch(senderMessageKey -> senderMessageKey.iteration() == iteration);
    }

    public void addSenderMessageKey(SenderMessageKey senderMessageKey) {
        messageKeys.add(senderMessageKey);
        if (messageKeys.size() <= 2000) {
            return;
        }

        messageKeys.remove(0);
    }

    public SenderMessageKey removeSenderMessageKey(int iteration) {
        return messageKeys.stream()
                .filter(key -> key.iteration() == iteration)
                .mapToInt(messageKeys::indexOf)
                .mapToObj(messageKeys::remove)
                .findFirst()
                .orElse(null);
    }

    public void nextChainKey() {
        this.chainKey = chainKey.next();
    }

    public boolean equals(Object other) {
        return other instanceof SenderKeyState that && Objects.equals(this.id(), that.id());
    }

    public static class SenderKeyStateBuilder {
        public SenderKeyStateBuilder messageKeys(CopyOnWriteArrayList<SenderMessageKey> messageKeys) {
            if (!messageKeys$set) {
                this.messageKeys$value = messageKeys;
                this.messageKeys$set = true;
                return this;
            }

            this.messageKeys$value.addAll(messageKeys);
            return this;
        }
    }
}
