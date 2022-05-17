package it.auties.whatsapp.model.signal.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.model.signal.keypair.SignalPreKeyPair;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
@Builder
@Jacksonized
@Data
@Accessors(fluent = true)
public class SessionChain {
    private SessionChainType type;

    private int counter;

    private byte[] key;

    @NonNull
    private ConcurrentHashMap<Integer, byte[]> messageKeys;

    public SessionChain(int counter, byte @NonNull [] key) {
        this(null, counter, key, new ConcurrentHashMap<>());
    }

    public boolean hasMessageKey(int counter){
        return messageKeys.containsKey(counter);
    }

    public void incrementCounter(){
        this.counter++;
    }
}