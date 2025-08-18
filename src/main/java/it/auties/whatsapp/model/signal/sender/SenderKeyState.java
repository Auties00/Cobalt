package it.auties.whatsapp.model.signal.sender;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ProtobufMessage
public final class SenderKeyState {
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    private final int id;
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    private final SignalKeyPair signingKey;
    @ProtobufProperty(index = 3, type = ProtobufType.MAP, mapKeyType = ProtobufType.INT32, mapValueType = ProtobufType.MESSAGE)
    private final ConcurrentHashMap<Integer, SenderMessageKey> messageKeys;
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    private SenderChainKey chainKey;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SenderKeyState(int id, SignalKeyPair signingKey, ConcurrentHashMap<Integer, SenderMessageKey> messageKeys, SenderChainKey chainKey) {
        this.id = id;
        this.signingKey = signingKey;
        this.messageKeys = messageKeys;
        this.chainKey = chainKey;
    }

    public SenderKeyState(int id, SignalKeyPair signingKey, int iteration, byte[] seed) {
        this.id = id;
        this.signingKey = signingKey;
        this.chainKey = new SenderChainKey(iteration, seed);
        this.messageKeys = new ConcurrentHashMap<>();
    }

    public Map<Integer, SenderMessageKey> messageKeys() {
        return Collections.unmodifiableMap(messageKeys);
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
