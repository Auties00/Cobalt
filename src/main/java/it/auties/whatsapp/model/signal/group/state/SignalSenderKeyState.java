package it.auties.whatsapp.model.signal.group.state;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.signal.group.ratchet.SignalSenderChainKey;
import it.auties.whatsapp.model.signal.group.ratchet.SignalSenderMessageKey;
import it.auties.whatsapp.model.signal.key.SignalKeyPair;

import java.util.*;

@ProtobufMessage
public final class SignalSenderKeyState {
    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    final int id;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    SignalSenderChainKey chainKey;

    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final SignalKeyPair signatureKey;

    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final MessageKeys messageKeys;

    SignalSenderKeyState(int id, SignalSenderChainKey chainKey, SignalKeyPair signatureKey, MessageKeys messageKeys) {
        this.id = id;
        this.chainKey = chainKey;
        this.signatureKey = signatureKey;
        this.messageKeys = messageKeys;
    }

    public int id() {
        return id;
    }

    public SignalSenderChainKey chainKey() {
        return chainKey;
    }

    public void setChainKey(SignalSenderChainKey chainKey) {
        this.chainKey = chainKey;
    }

    public SignalKeyPair signatureKey() {
        return signatureKey;
    }

    public SequencedCollection<SignalSenderMessageKey> messageKeys() {
        return messageKeys.values();
    }

    public boolean hasMessageKey(int iteration) {
        return messageKeys.contains(iteration);
    }

    public void addMessageKey(SignalSenderMessageKey senderMessageKey) {
        messageKeys.add(senderMessageKey);
    }

    public Optional<SignalSenderMessageKey> removeMessageKey(int iteration) {
        return messageKeys.remove(iteration);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id());
    }

    public boolean equals(Object other) {
        return other instanceof SignalSenderKeyState that && Objects.equals(this.id(), that.id());
    }

    static final class MessageKeys extends AbstractCollection<SignalSenderMessageKey> {
        private static final int MAX_MESSAGE_KEYS = 2000;

        private final SequencedMap<Integer, SignalSenderMessageKey> backing;

        public MessageKeys() {
            this.backing = new LinkedHashMap<>(MAX_MESSAGE_KEYS, 0.75F, true);
        }

        public SequencedCollection<SignalSenderMessageKey> values() {
            return Collections.unmodifiableSequencedCollection(backing.sequencedValues());
        }

        public Optional<SignalSenderMessageKey> remove(int iteration) {
            return Optional.ofNullable(backing.remove(iteration));
        }

        public boolean contains(int iteration) {
            return backing.get(iteration) != null;
        }

        @Override
        public boolean add(SignalSenderMessageKey senderKeyState) {
            if (backing.size() == MAX_MESSAGE_KEYS) {
                backing.pollFirstEntry();
            }

            backing.put(senderKeyState.iteration(), senderKeyState);
            return true;
        }

        @Override
        public Iterator<SignalSenderMessageKey> iterator() {
            return backing.sequencedValues().iterator();
        }

        @Override
        public int size() {
            return backing.size();
        }
    }
}
