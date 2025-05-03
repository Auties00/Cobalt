package it.auties.whatsapp.model.signal.session;


import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ProtobufMessage
public final class SessionChain {
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    int counter;
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] key;
    @ProtobufProperty(index = 3, type = ProtobufType.MAP, mapKeyType = ProtobufType.INT32, mapValueType = ProtobufType.BYTES)
    final ConcurrentHashMap<Integer, byte[]> messageKeys;

    SessionChain(int counter, byte[] key, ConcurrentHashMap<Integer, byte[]> messageKeys) {
        this.counter = counter;
        this.key = key;
        this.messageKeys = messageKeys;
    }

    public SessionChain(int counter, byte[] key) {
        this(counter, key, new ConcurrentHashMap<>());
    }

    public boolean hasMessageKey(int counter) {
        return messageKeys.containsKey(counter);
    }

    public int counter() {
        return counter;
    }

    public byte[] key() {
        return key;
    }

    public void addMessageKey(int id, byte[] key) {
        messageKeys.put(id, key);
    }

    public boolean removeMessageKey(int id) {
        return messageKeys.remove(id) != null;
    }

    public Optional<byte[]> getMessageKey(int id) {
        return Optional.ofNullable(messageKeys.get(id));
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

    public void close() {
        this.key = null;
    }
}