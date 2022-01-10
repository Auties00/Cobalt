package it.auties.whatsapp.protobuf.signal.session;

import it.auties.whatsapp.protobuf.signal.keypair.SignalPreKeyPair;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
public class SessionChain {
    private int counter;

    @NonNull
    private byte[] key;

    @NonNull
    private byte[] owner;

    @NonNull
    private Map<Integer, SignalPreKeyPair> messageKeys;

    public SessionChain(int counter, @NonNull byte[] key, @NonNull byte[] owner) {
        this(counter, key, owner, new HashMap<>());
    }

    public boolean hasMessageKey(int counter){
        return messageKeys.containsKey(counter);
    }

    public void incrementCounter(){
        this.counter++;
    }
}