package it.auties.whatsapp.model.signal.sender;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;

import java.util.*;

public final class SenderKeyRecord {
    private final LinkedHashMap<Integer, List<SenderKeyState>> states;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SenderKeyRecord(LinkedHashMap<Integer, List<SenderKeyState>> states) {
        this.states = states;
    }

    public SenderKeyRecord() {
        this.states = new LinkedHashMap<>();
    }

    public SenderKeyState findState() {
        return states.values()
                .stream()
                .flatMap(Collection::stream)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Cannot get head state for empty record"));
    }

    public List<SenderKeyState> findStateById(int keyId) {
        return Objects.requireNonNull(states.get(keyId), "Cannot find state with id %s".formatted(keyId));
    }

    public void addState(int id, int iteration, byte[] seed, byte[] signatureKey) {
        addState(id, iteration, seed, SignalKeyPair.of(signatureKey));
    }

    public void addState(int id, int iteration, byte[] seed, SignalKeyPair signingKey) {
        var state = new SenderKeyState(id, iteration, seed, signingKey);
        var oldList = states.getOrDefault(id, new ArrayList<>());
        oldList.add(state);
        states.put(id, oldList);
    }

    public boolean isEmpty() {
        return states.isEmpty();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.states);
    }

    public boolean equals(Object object) {
        return object instanceof SenderKeyRecord that && Objects.equals(this.states, that.states);
    }
}
