package it.auties.whatsapp.model.signal.sender;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@ProtobufMessage
public final class SenderKeyRecord {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<SenderKeyState> states;

    public SenderKeyRecord(List<SenderKeyState> states) {
        this.states = states;
    }

    public SenderKeyRecord() {
        this.states = new ArrayList<>();
    }

    public SenderKeyState firstState() {
        if(states.isEmpty()) {
            throw new NoSuchElementException("Cannot get head state for empty record");
        }

        return states.getFirst();
    }

    public List<SenderKeyState> findStatesById(int keyId) {
        return states.stream()
                .filter(entry -> entry.id() == keyId)
                .toList();
    }

    public void addState(int id, byte[] signatureKey, int iteration, byte[] seed) {
        addState(id, SignalKeyPair.of(signatureKey), iteration, seed);
    }

    public void addState(int id, SignalKeyPair signingKey, int iteration, byte[] seed) {
        var state = new SenderKeyState(id, signingKey, iteration, seed);
        states.add(state);
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
