package it.auties.whatsapp.model.signal.sender;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class SenderKeyState {
    private final int id;
    private final SignalKeyPair signingKey;
    private final ConcurrentHashMap<Integer, SenderMessageKey> messageKeys;
    private SenderChainKey chainKey;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
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

    public int id() {
        return id;
    }

    public SignalKeyPair signingKey() {
        return signingKey;
    }

    public SenderChainKey chainKey() {
        return chainKey;
    }

    public void setChainKey(SenderChainKey chainKey) {
        this.chainKey = chainKey;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id());
    }

    public boolean equals(Object other) {
        return other instanceof SenderKeyState that && Objects.equals(this.id(), that.id());
    }
}
