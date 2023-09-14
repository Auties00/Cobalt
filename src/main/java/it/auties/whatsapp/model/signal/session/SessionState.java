package it.auties.whatsapp.model.signal.session;

import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import lombok.Builder;
import lombok.Builder.Default;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Builder
public class SessionState {
    private final int version;

    private final int registrationId;

    private final byte @NonNull [] baseKey;

    private final byte @NonNull [] remoteIdentityKey;

    @NonNull
    @Default
    private ConcurrentHashMap<String, SessionChain> chains = new ConcurrentHashMap<>();

    private byte @NonNull [] rootKey;

    private SessionPreKey pendingPreKey;

    @NonNull
    private SignalKeyPair ephemeralKeyPair;

    private byte @NonNull [] lastRemoteEphemeralKey;

    private int previousCounter;

    private boolean closed;

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

    public byte @NonNull [] baseKey() {
        return this.baseKey;
    }

    public byte @NonNull [] remoteIdentityKey() {
        return this.remoteIdentityKey;
    }

    public byte @NonNull [] rootKey() {
        return this.rootKey;
    }

    public SessionPreKey pendingPreKey() {
        return this.pendingPreKey;
    }

    public @NonNull SignalKeyPair ephemeralKeyPair() {
        return this.ephemeralKeyPair;
    }

    public byte @NonNull [] lastRemoteEphemeralKey() {
        return this.lastRemoteEphemeralKey;
    }

    public int previousCounter() {
        return this.previousCounter;
    }

    public boolean closed() {
        return this.closed;
    }

    public SessionState rootKey(byte @NonNull [] rootKey) {
        this.rootKey = rootKey;
        return this;
    }

    public SessionState pendingPreKey(SessionPreKey pendingPreKey) {
        this.pendingPreKey = pendingPreKey;
        return this;
    }

    public SessionState ephemeralKeyPair(@NonNull SignalKeyPair ephemeralKeyPair) {
        this.ephemeralKeyPair = ephemeralKeyPair;
        return this;
    }

    public SessionState lastRemoteEphemeralKey(byte @NonNull [] lastRemoteEphemeralKey) {
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
