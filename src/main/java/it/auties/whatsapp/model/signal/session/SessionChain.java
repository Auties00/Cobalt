package it.auties.whatsapp.model.signal.session;


import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@ProtobufMessage
public record SessionChain(
        @ProtobufProperty(index = 1, type = ProtobufType.INT32)
        AtomicInteger counter,
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        AtomicReference<byte[]> key,
        @ProtobufProperty(index = 3, type = ProtobufType.MAP, mapKeyType = ProtobufType.INT32, mapValueType = ProtobufType.BYTES)
        ConcurrentHashMap<Integer, byte[]> messageKeys
) {
    public SessionChain(int counter, byte[] key) {
        this(new AtomicInteger(counter), new AtomicReference<>(key), new ConcurrentHashMap<>());
    }

    public boolean hasMessageKey(int counter) {
        return messageKeys.containsKey(counter);
    }
}