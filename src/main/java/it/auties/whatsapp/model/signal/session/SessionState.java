package it.auties.whatsapp.model.signal.session;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ProtobufMessage
public final class SessionState {
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    final int version;

    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    final int registrationId;

    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    final byte[] baseKey;

    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    final byte[] remoteIdentityKey;

    @ProtobufProperty(index = 5, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentHashMap<String, SessionChain> chains;

    @ProtobufProperty(index = 6, type = ProtobufType.BYTES)
    byte[] rootKey;

    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    SessionPreKey pendingPreKey;

    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    SignalKeyPair ephemeralKeyPair;

    @ProtobufProperty(index = 9, type = ProtobufType.BYTES)
    byte[] lastRemoteEphemeralKey;

    @ProtobufProperty(index = 10, type = ProtobufType.INT32)
    int previousCounter;

    @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
    boolean closed;

    public SessionState(int version, int registrationId, byte[] baseKey, byte[] remoteIdentityKey, ConcurrentHashMap<String, SessionChain> chains, byte[] rootKey, SessionPreKey pendingPreKey, SignalKeyPair ephemeralKeyPair, byte[] lastRemoteEphemeralKey, int previousCounter, boolean closed) {
        this.version = version;
        this.registrationId = registrationId;
        this.baseKey = baseKey;
        this.remoteIdentityKey = remoteIdentityKey;
        this.chains = Objects.requireNonNullElseGet(chains, ConcurrentHashMap::new);
        this.rootKey = rootKey;
        this.pendingPreKey = pendingPreKey;
        this.ephemeralKeyPair = ephemeralKeyPair;
        this.lastRemoteEphemeralKey = lastRemoteEphemeralKey;
        this.previousCounter = previousCounter;
        this.closed = closed;
    }

    public boolean hasChain(byte[] senderEphemeral) {
        return chains.containsKey(HexFormat.of().formatHex(senderEphemeral));
    }

    public Optional<SessionChain> findChain(byte[] senderEphemeral) {
        return Optional.ofNullable(chains.get(HexFormat.of().formatHex(senderEphemeral)));
    }

    public void addChain(byte[] senderEphemeral, SessionChain chain) {
        chains.put(HexFormat.of().formatHex(senderEphemeral), chain);
    }

    public void removeChain(byte[] senderEphemeral) {
        Objects.requireNonNull(chains.remove(HexFormat.of().formatHex(senderEphemeral)), "Cannot remove chain");
    }

    public boolean hasPreKey() {
        return pendingPreKey != null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(version(), Arrays.hashCode(baseKey()));
    }

    public boolean equals(Object other) {
        return other instanceof SessionState that && contentEquals(that.version(), that.baseKey());
    }

    public boolean contentEquals(int version, byte[] baseKey) {
        return version() == version && Arrays.equals(baseKey(), baseKey);
    }

    public int version() {
        return this.version;
    }

    public int registrationId() {
        return this.registrationId;
    }

    public byte[] baseKey() {
        return this.baseKey;
    }

    public byte[] remoteIdentityKey() {
        return this.remoteIdentityKey;
    }

    public byte[] rootKey() {
        return this.rootKey;
    }

    public SessionPreKey pendingPreKey() {
        return this.pendingPreKey;
    }

    public SignalKeyPair ephemeralKeyPair() {
        return this.ephemeralKeyPair;
    }

    public byte[] lastRemoteEphemeralKey() {
        return this.lastRemoteEphemeralKey;
    }

    public int previousCounter() {
        return this.previousCounter;
    }

    public boolean closed() {
        return this.closed;
    }

    public void setRootKey(byte[] rootKey) {
        this.rootKey = rootKey;
    }

    public void setPendingPreKey(SessionPreKey pendingPreKey) {
        this.pendingPreKey = pendingPreKey;
    }

    public void setEphemeralKeyPair(SignalKeyPair ephemeralKeyPair) {
        this.ephemeralKeyPair = ephemeralKeyPair;
    }

    public void setLastRemoteEphemeralKey(byte[] lastRemoteEphemeralKey) {
        this.lastRemoteEphemeralKey = lastRemoteEphemeralKey;
    }

    public void setPreviousCounter(int previousCounter) {
        this.previousCounter = previousCounter;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public Map<String, SessionChain> chains() {
        return Collections.unmodifiableMap(chains);
    }
}
