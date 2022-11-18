package it.auties.whatsapp.model.signal.sender;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.Objects;

@AllArgsConstructor
@Builder
@Jacksonized
public class SenderKeyRecord implements ProtobufMessage {
    private final LinkedHashMap<Integer, SenderKeyState> states;
    public SenderKeyRecord(){
        this.states = new LinkedHashMap<>();
    }

    public SenderKeyState headState() {
        return states.values()
                .stream()
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Cannot get head state for empty record"));
    }

    public SenderKeyState findStateById(int keyId) {
        return Objects.requireNonNull(states.get(keyId), "Cannot find state with id %s".formatted(keyId));
    }

    public void addState(int id, int iteration, byte[] seed, byte[] signatureKey) {
        addState(id, iteration, seed, SignalKeyPair.of(signatureKey));
    }

    public void addState(int id, int iteration, byte[] seed, SignalKeyPair signingKey) {
        var state = new SenderKeyState(id, iteration, seed, signingKey);
        if(states.containsKey(id)){
            System.out.println("Ignoring state");
            return;
        }

        states.put(id, state);
    }

    public boolean isEmpty() {
        return states.isEmpty();
    }

    public boolean equals(Object object) {
        return object instanceof SenderKeyRecord that
                && Objects.equals(this.states, that.states);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.states);
    }
}
