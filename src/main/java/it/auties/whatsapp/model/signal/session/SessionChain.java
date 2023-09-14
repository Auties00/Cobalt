package it.auties.whatsapp.model.signal.session;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public record SessionChain(AtomicInteger counter, AtomicReference<byte[]> key,
                           ConcurrentHashMap<Integer, byte[]> messageKeys) {
    public SessionChain(int counter, byte @NonNull [] key) {
        this(new AtomicInteger(counter), new AtomicReference<>(key), new ConcurrentHashMap<>());
    }

    public boolean hasMessageKey(int counter) {
        return messageKeys.containsKey(counter);
    }
}