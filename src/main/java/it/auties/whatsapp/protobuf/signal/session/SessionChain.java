package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.protobuf.signal.keypair.SignalPreKeyPair;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Builder
@Jacksonized
@Data
@Accessors(fluent = true)
public class SessionChain {
    @JsonProperty("counter")
    private int counter;

    @JsonProperty("key")
    private byte @NonNull [] key;

    @JsonProperty("owner")
    private byte @NonNull [] owner;

    @JsonProperty("message_keys")
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