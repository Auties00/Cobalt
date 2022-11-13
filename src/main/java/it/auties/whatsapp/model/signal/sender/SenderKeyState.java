package it.auties.whatsapp.model.signal.sender;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.util.SignalSpecification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.UINT32;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class SenderKeyState implements ProtobufMessage, SignalSpecification {
    @ProtobufProperty(index = 1, type = UINT32)
    private Integer id;

    @ProtobufProperty(index = 2, type = MESSAGE, implementation = SenderChainKey.class)
    private SenderChainKey chainKey;

    @ProtobufProperty(index = 3, type = MESSAGE, implementation = SenderSigningKey.class)
    private SenderSigningKey signingKey;

    @ProtobufProperty(index = 4, type = MESSAGE, implementation = SenderMessageKey.class, repeated = true)
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
        if (messageKeys.size() <= MAX_MESSAGES) {
            return;
        }

        messageKeys.remove(0);
    }

    public SenderMessageKey removeSenderMessageKey(int iteration) {
        return messageKeys.stream()
                .filter(key -> key.iteration() == iteration)
                .findFirst()
                .orElse(null);
    }

    public boolean equals(Object other) {
        return other instanceof SenderKeyState that && Objects.equals(this.id(), that.id());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id());
    }

    @SuppressWarnings("unused")
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
