package it.auties.whatsapp.model.signal.sender;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
@Builder
@Jacksonized
@Data
@Accessors(fluent = true)
public class SenderKeyState implements ProtobufMessage {
    private final int id;
    private final SignalKeyPair signingKey;
    private final ConcurrentHashMap<Integer, SenderMessageKey> messageKeys;
    private SenderChainKey chainKey;

    public SenderKeyState(int id, int iteration, byte[] seed, SignalKeyPair signingKey) {
        this.id = id;
        this.chainKey = new SenderChainKey(iteration, seed);
        this.signingKey = signingKey;
        this.messageKeys = new ConcurrentHashMap<>();
    }

    public void addSenderMessageKey(SenderMessageKey senderMessageKey) {
        messageKeys.put(senderMessageKey.iteration(), senderMessageKey);
    }

    public Optional<SenderMessageKey> findSenderMessageKey(int iteration) {
        return Optional.ofNullable(messageKeys.get(iteration));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id());
    }

    public boolean equals(Object other) {
        return other instanceof SenderKeyState that && Objects.equals(this.id(), that.id());
    }
}
