package it.auties.whatsapp.model.signal.sender;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.util.Validate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class SenderKeyRecord implements ProtobufMessage {
    private static final int MAX_STATES = 5;

    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = SenderKeyState.class, repeated = true)
    @Default
    private ConcurrentLinkedDeque<SenderKeyState> states = new ConcurrentLinkedDeque<>();

    public SenderKeyState headState() {
        Validate.isTrue(!isEmpty(), "Cannot get head state for empty record", NoSuchElementException.class);
        return states.getFirst();
    }

    public SenderKeyState findStateById(int keyId) {
        return states().stream()
                .filter(key -> key.id() == keyId)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Cannot find state with id %s".formatted(keyId)));
    }

    public void addState(int id, int iteration, byte[] seed, byte[] signatureKey) {
        addState(id, iteration, seed, SignalKeyPair.of(signatureKey));
    }

    public void addState(int id, int iteration, byte[] seed, SignalKeyPair signingKey) {
        var state = new SenderKeyState(id, iteration, seed, signingKey);
        states.add(state);
    }

    public boolean isEmpty() {
        return states.isEmpty();
    }

    public boolean equals(Object object) {
        return object instanceof SenderKeyRecord that && Objects.equals(this.states(), that.states());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.states());
    }

    @SuppressWarnings("unused")
    public static class SenderKeyRecordBuilder {
        public SenderKeyRecordBuilder states(ConcurrentLinkedDeque<SenderKeyState> states) {
            if (!this.states$set) {
                this.states$value = states;
                this.states$set = true;
                return this;
            }

            this.states$value.addAll(states);
            return this;
        }
    }
}
