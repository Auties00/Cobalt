package it.auties.whatsapp.model.signal.group.state;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.signal.group.ratchet.SenderChainKeyBuilder;
import it.auties.whatsapp.model.signal.key.SignalPublicKey;

import java.util.*;

@ProtobufMessage
public final class SignalSenderKeyRecord {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final SenderKeyStates senderKeyStates;

    SignalSenderKeyRecord(SenderKeyStates senderKeyStates) {
        this.senderKeyStates = senderKeyStates;
    }

    public SignalSenderKeyRecord() {
        this.senderKeyStates = new SenderKeyStates();
    }

    public boolean isEmpty() {
        return senderKeyStates.isEmpty();
    }

    public Optional<SignalSenderKeyState> findSenderKeyState() {
        return senderKeyStates.get();
    }

    public Optional<SignalSenderKeyState> findSenderKeyStateById(int keyId) {
        return senderKeyStates.get(keyId);
    }

    public void addSenderKeyState(int id, int iteration, byte[] chainKey, SignalPublicKey signatureKey) {
        var senderChainKey = new SenderChainKeyBuilder()
                .iteration(iteration)
                .seed(chainKey)
                .build();
        var senderKeyState = new SenderKeyStateBuilder()
                .id(id)
                .chainKey(senderChainKey)
                .signatureKey(signatureKey)
                .build();
        senderKeyStates.add(senderKeyState);
    }

    public void setSenderKeyState(int id, int iteration, byte[] chainKey, SignalPublicKey signatureKey) {
        senderKeyStates.clear();
        var senderChainKey = new SenderChainKeyBuilder()
                .iteration(iteration)
                .seed(chainKey)
                .build();
        var senderKeyState = new SenderKeyStateBuilder()
                .id(id)
                .chainKey(senderChainKey)
                .signatureKey(signatureKey)
                .build();
        senderKeyStates.add(senderKeyState);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.senderKeyStates);
    }

    public boolean equals(Object object) {
        return object instanceof SignalSenderKeyRecord that
                && Objects.equals(this.senderKeyStates, that.senderKeyStates);
    }

    static final class SenderKeyStates extends AbstractCollection<SignalSenderKeyState> {
        private static final int MAX_STATES = 5;

        private final SequencedMap<Integer, SignalSenderKeyState> backing;

        public SenderKeyStates() {
            this.backing = new LinkedHashMap<>(MAX_STATES, 0.75F, true);
        }

        public Optional<SignalSenderKeyState> get() {
            return backing.isEmpty()
                    ? Optional.empty()
                    : Optional.ofNullable(backing.firstEntry().getValue());
        }

        public Optional<SignalSenderKeyState> get(int keyId) {
            return Optional.ofNullable(backing.get(keyId));
        }

        @Override
        public boolean add(SignalSenderKeyState senderKeyState) {
            if(backing.size() == MAX_STATES) {
                backing.pollLastEntry();
            }
            backing.putFirst(senderKeyState.id(), senderKeyState);
            return true;
        }

        @Override
        public Iterator<SignalSenderKeyState> iterator() {
            return backing.sequencedValues()
                    .iterator();
        }

        @Override
        public int size() {
            return backing.size();
        }
    }
}
