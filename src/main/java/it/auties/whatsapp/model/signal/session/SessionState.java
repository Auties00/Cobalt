package it.auties.whatsapp.model.signal.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionState {
    private final int version;

    private final int registrationId;

    private final byte[] baseKey;

    private final byte[] remoteIdentityKey;

    private final ConcurrentHashMap<String, SessionChain> chains;

    private byte[] rootKey;

    private SessionPreKey pendingPreKey;

    private SignalKeyPair ephemeralKeyPair;

    private byte[] lastRemoteEphemeralKey;

    private int previousCounter;

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
}
