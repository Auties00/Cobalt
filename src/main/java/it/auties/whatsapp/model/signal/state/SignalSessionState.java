package it.auties.whatsapp.model.signal.state;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.signal.key.SignalPublicKey;

import java.util.*;

@ProtobufMessage
public final class SignalSessionState {
    private static final int DEFAULT_SESSION_VERSION = 2;
    private static final Integer MAX_MESSAGE_KEYS = 2000;

    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    Integer sessionVersion;

    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    SignalPublicKey localIdentityPublic;

    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    SignalPublicKey remoteIdentityPublic;

    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    SignalPublicKey rootKey;

    @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
    Integer previousCounter;

    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    SignalSessionChain senderChain;

    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    final ReceiverChains receiverChains;

    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    SignalPendingKeyExchange pendingKeyExchange;

    @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
    SignalPendingPreKey pendingPreKey;

    @ProtobufProperty(index = 10, type = ProtobufType.UINT32)
    Integer remoteRegistrationId;

    @ProtobufProperty(index = 11, type = ProtobufType.UINT32)
    Integer localRegistrationId;

    @ProtobufProperty(index = 12, type = ProtobufType.BOOL)
    boolean needsRefresh;

    @ProtobufProperty(index = 13, type = ProtobufType.BYTES)
    byte[] baseKey;

    public SignalSessionState() {
        this.receiverChains = new ReceiverChains();
    }

    SignalSessionState(Integer sessionVersion, SignalPublicKey localIdentityPublic, SignalPublicKey remoteIdentityPublic, SignalPublicKey rootKey, Integer previousCounter, SignalSessionChain senderChain, ReceiverChains receiverChains, SignalPendingKeyExchange pendingKeyExchange, SignalPendingPreKey pendingPreKey, Integer remoteRegistrationId, Integer localRegistrationId, boolean needsRefresh, byte[] baseKey) {
        this.sessionVersion = sessionVersion;
        this.localIdentityPublic = localIdentityPublic;
        this.remoteIdentityPublic = remoteIdentityPublic;
        this.rootKey = rootKey;
        this.previousCounter = previousCounter;
        this.senderChain = senderChain;
        this.receiverChains = receiverChains;
        this.pendingKeyExchange = pendingKeyExchange;
        this.pendingPreKey = pendingPreKey;
        this.remoteRegistrationId = remoteRegistrationId;
        this.localRegistrationId = localRegistrationId;
        this.needsRefresh = needsRefresh;
        this.baseKey = baseKey;
    }

    public byte[] baseKey() {
        return baseKey;
    }

    public void setBaseKey(byte[] baseKey) {
        this.baseKey = baseKey;
    }

    public void setSessionVersion(Integer sessionVersion) {
        this.sessionVersion = sessionVersion;
    }

    public int sessionVersion() {
        return Objects.requireNonNullElse(sessionVersion, DEFAULT_SESSION_VERSION);
    }

    public void setRemoteIdentityPublic(SignalPublicKey remoteIdentityPublic) {
        this.remoteIdentityPublic = remoteIdentityPublic;
    }

    public void setLocalIdentityPublic(SignalPublicKey localIdentityPublic) {
        this.localIdentityPublic = localIdentityPublic;
    }

    public SignalPublicKey remoteIdentityPublic() {
        return remoteIdentityPublic;
    }

    public SignalPublicKey localIdentityPublic() {
        return localIdentityPublic;
    }

    public Integer previousCounter() {
        return previousCounter;
    }

    public void setPreviousCounter(Integer previousCounter) {
        this.previousCounter = previousCounter;
    }

    public SignalPublicKey rootKey() {
        return rootKey;
    }

    public void setRootKey(SignalPublicKey rootKey) {
        this.rootKey = rootKey;
    }

    public Optional<SignalSessionChain> getReceiverChain(SignalPublicKey senderEphemeral) {
        return senderEphemeral == null ? Optional.empty() : receiverChains.get(senderEphemeral);
    }

    public void addReceiverChain(SignalSessionChain chain) {
        receiverChains.addLast(chain);
    }

    public void setSenderChain(SignalSessionChain chain) {
       this.senderChain = chain;
    }

    public Integer localRegistrationId() {
        return localRegistrationId;
    }

    public void setLocalRegistrationId(Integer localRegistrationId) {
        this.localRegistrationId = localRegistrationId;
    }

    public Integer remoteRegistrationId() {
        return remoteRegistrationId;
    }

    public void setRemoteRegistrationId(Integer remoteRegistrationId) {
        this.remoteRegistrationId = remoteRegistrationId;
    }

    public void setPendingPreKey(SignalPendingPreKey pendingPreKey) {
        this.pendingPreKey = pendingPreKey;
    }

    public SignalSessionChain senderChain() {
        return senderChain;
    }

    static final class ReceiverChains extends AbstractCollection<SignalSessionChain> {
        private static final int MAX_RECEIVER_CHAINS = 5;

        private final SequencedMap<SignalPublicKey, SignalSessionChain> backing;

        public ReceiverChains() {
            this.backing = new LinkedHashMap<>(MAX_RECEIVER_CHAINS, 0.75F, true);
        }

        public Optional<SignalSessionChain> get(SignalPublicKey publicKey) {
            return Optional.ofNullable(backing.get(publicKey));
        }

        public void addLast(SignalSessionChain chain) {
            if (backing.size() == MAX_RECEIVER_CHAINS) {
                backing.pollFirstEntry();
            }
            backing.put(chain.senderRatchetKey(), chain);
        }

        @Override
        public Iterator<SignalSessionChain> iterator() {
            return backing.sequencedValues()
                    .iterator();
        }

        @Override
        public int size() {
            return backing.size();
        }
    }
}
