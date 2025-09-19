package it.auties.whatsapp.model.signal.state;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.signal.key.SignalPrivateKey;
import it.auties.whatsapp.model.signal.key.SignalPublicKey;
import it.auties.whatsapp.model.signal.ratchet.SignalChainKey;
import it.auties.whatsapp.model.signal.ratchet.SignalMessageKey;

import java.util.*;

@ProtobufMessage
public final class SignalSessionChain {
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    final SignalPublicKey senderRatchetKey;

    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    final SignalPrivateKey senderRatchetKeyPrivate;

    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final SignalChainKey chainKey;

    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final MessageKeys messageKeys;

    SignalSessionChain(SignalPublicKey senderRatchetKey, SignalPrivateKey senderRatchetKeyPrivate, SignalChainKey chainKey, MessageKeys messageKeys) {
        this.senderRatchetKey = senderRatchetKey;
        this.senderRatchetKeyPrivate = senderRatchetKeyPrivate;
        this.chainKey = chainKey;
        this.messageKeys = messageKeys;
    }

    public SignalPublicKey senderRatchetKey() {
        return senderRatchetKey;
    }

    public SignalPrivateKey senderRatchetKeyPrivate() {
        return senderRatchetKeyPrivate;
    }

    public SignalChainKey chainKey() {
        return chainKey;
    }

    public boolean hasMessageKey(int index) {
        return messageKeys.contains(index);
    }

    public void addMessageKey(SignalMessageKey senderMessageKey) {
        messageKeys.add(senderMessageKey);
    }

    public Optional<SignalMessageKey> removeMessageKey(int index) {
        return messageKeys.remove(index);
    }

    static final class MessageKeys extends AbstractCollection<SignalMessageKey> {
        private static final int MAX_MESSAGE_KEYS = 2000;

        private final SequencedMap<Integer, SignalMessageKey> backing;

        public MessageKeys() {
            this.backing = new LinkedHashMap<>(MAX_MESSAGE_KEYS, 0.75F, true);
        }


        public Optional<SignalMessageKey> get() {
            return backing.isEmpty()
                    ? Optional.empty()
                    : Optional.ofNullable(backing.firstEntry().getValue());
        }

        public Optional<SignalMessageKey> remove(int index) {
            return Optional.ofNullable(backing.remove(index));
        }

        public boolean contains(int index) {
            return backing.get(index) != null;
        }

        @Override
        public boolean add(SignalMessageKey senderKeyState) {
            if (backing.size() == MAX_MESSAGE_KEYS) {
                backing.pollFirstEntry();
            }

            backing.put(senderKeyState.index(), senderKeyState);
            return true;
        }

        @Override
        public Iterator<SignalMessageKey> iterator() {
            return backing.sequencedValues().iterator();
        }

        @Override
        public int size() {
            return backing.size();
        }
    }
}