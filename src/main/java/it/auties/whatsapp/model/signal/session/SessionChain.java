package it.auties.whatsapp.model.signal.session;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record SessionChain(AtomicInteger counter, AtomicReference<byte[]> key,
                           ConcurrentHashMap<Integer, byte[]> messageKeys) {

  public SessionChain(int counter, byte @NonNull [] key) {
    this(new AtomicInteger(counter), new AtomicReference<>(key), new ConcurrentHashMap<>());
  }

  public boolean hasMessageKey(int counter) {
    return messageKeys.containsKey(counter);
  }
}