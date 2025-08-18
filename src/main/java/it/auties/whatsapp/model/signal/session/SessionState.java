package it.auties.whatsapp.model.signal.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ProtobufMessage
public final class SessionState {
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    private final int version;

    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    private final int registrationId;

    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    private final byte[] baseKey;

    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    private final byte[] remoteIdentityKey;

    @ProtobufProperty(index = 5, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    private final ConcurrentHashMap<String, SessionChain> chains;

    @ProtobufProperty(index = 6, type = ProtobufType.BYTES)
    private byte[] rootKey;

    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    private SessionPreKey pendingPreKey;

    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    private SignalKeyPair ephemeralKeyPair;

    @ProtobufProperty(index = 9, type = ProtobufType.BYTES)
    private byte[] lastRemoteEphemeralKey;

    @ProtobufProperty(index = 10, type = ProtobufType.INT32)
    private int previousCounter;

    @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
    private boolean closed;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
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

    public SessionState addChain(byte[] senderEphemeral, SessionChain chain) {
        chains.put(HexFormat.of().formatHex(senderEphemeral), chain);
        return this;
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

    public SessionState rootKey(byte[] rootKey) {
        this.rootKey = rootKey;
        return this;
    }

    public SessionState pendingPreKey(SessionPreKey pendingPreKey) {
        this.pendingPreKey = pendingPreKey;
        return this;
    }

    public SessionState ephemeralKeyPair(SignalKeyPair ephemeralKeyPair) {
        this.ephemeralKeyPair = ephemeralKeyPair;
        return this;
    }

    public SessionState lastRemoteEphemeralKey(byte[] lastRemoteEphemeralKey) {
        this.lastRemoteEphemeralKey = lastRemoteEphemeralKey;
        return this;
    }

    public SessionState previousCounter(int previousCounter) {
        this.previousCounter = previousCounter;
        return this;
    }

    public SessionState closed(boolean closed) {
        this.closed = closed;
        return this;
    }

    public Map<String, SessionChain> chains() {
        return Collections.unmodifiableMap(chains);
    }
}
